package com.titanius.lxvpn.core.antivpn;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.AsnResponse;
import com.titanius.lxvpn.core.managers.ConfigManager;
import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Decides whether an address belongs to a hosting or datacenter network.
 *
 * <p>This is the single most effective signal in the whole plugin, and it is worth explaining why.
 * Commercial VPN providers rent their capacity from a handful of hosting companies. A residential
 * player is never on Amazon, Google Cloud, OVH or Hetzner - there is no consumer product that puts
 * them there. So an address inside those networks is either a VPN, a proxy, a bot, or a developer.
 *
 * <p>It also costs nothing per lookup. The database is downloaded once and queried in memory:
 * no API key, no rate limit, no outbound request while a player is waiting to join. During a flood,
 * when the reputation APIs are suspended, this check keeps running.
 *
 * <p>It contributes a configurable bonus rather than an outright block, because a small number of
 * legitimate players do sit behind carrier networks that look like hosting.
 */
public class AsnChecker {

    private static final List<String> MIRRORS = List.of(
            "https://github.com/P3TERX/GeoLite.mmdb/raw/download/GeoLite2-ASN.mmdb",
            "https://raw.githubusercontent.com/P3TERX/GeoLite.mmdb/download/GeoLite2-ASN.mmdb"
    );

    /** Networks whose address space is hosting capacity rather than consumer connections. */
    private static final Map<Long, String> DATACENTER_ASNS = new HashMap<>();

    static {
        DATACENTER_ASNS.put(16509L, "Amazon");
        DATACENTER_ASNS.put(14618L, "Amazon");
        DATACENTER_ASNS.put(15169L, "Google");
        DATACENTER_ASNS.put(396982L, "Google Cloud");
        DATACENTER_ASNS.put(8075L, "Microsoft");
        DATACENTER_ASNS.put(3598L, "Microsoft");
        DATACENTER_ASNS.put(16276L, "OVH");
        DATACENTER_ASNS.put(14061L, "DigitalOcean");
        DATACENTER_ASNS.put(24940L, "Hetzner");
        DATACENTER_ASNS.put(63949L, "Akamai Linode");
        DATACENTER_ASNS.put(20473L, "Vultr");
        DATACENTER_ASNS.put(13335L, "Cloudflare");
        DATACENTER_ASNS.put(31898L, "Oracle Cloud");
        DATACENTER_ASNS.put(45102L, "Alibaba Cloud");
        DATACENTER_ASNS.put(132203L, "Tencent Cloud");
        DATACENTER_ASNS.put(16265L, "Leaseweb");
        DATACENTER_ASNS.put(12876L, "Scaleway");
        DATACENTER_ASNS.put(51167L, "Contabo");
        DATACENTER_ASNS.put(60781L, "LeaseWeb NL");
        DATACENTER_ASNS.put(197540L, "netcup");
        DATACENTER_ASNS.put(211298L, "Falkenstein hosting");
        DATACENTER_ASNS.put(9009L, "M247");
        DATACENTER_ASNS.put(62240L, "Clouvider");
        DATACENTER_ASNS.put(35916L, "MULTACOM");
        DATACENTER_ASNS.put(29802L, "HIVELOCITY");
        DATACENTER_ASNS.put(53667L, "FranTech PONYNET");
    }

    private final LogManager log;
    private final Path mmdbPath;
    private final Map<String, Boolean> resultCache = new ConcurrentHashMap<>();
    private final AtomicReference<DatabaseReader> reader = new AtomicReference<>();

    private volatile boolean enabled;
    private volatile int bonusScore;

    public AsnChecker(Path dataFolder, LogManager log, ConfigManager config) {
        this.log = log;
        this.mmdbPath = dataFolder.resolve("GeoLite2-ASN.mmdb");
        applyConfig(config);
        if (enabled) {
            initialiseAsync();
        }
    }

    public void applyConfig(ConfigManager config) {
        this.enabled = config.isAsnCheckEnabled();
        this.bonusScore = config.getAsnBonusScore();
    }

    private void initialiseAsync() {
        AsyncExecutor.get().execute(() -> {
            if (Files.notExists(mmdbPath) || MmdbAccess.tooSmall(mmdbPath)) {
                if (!MmdbAccess.download(MIRRORS, mmdbPath, log, "GeoLite2-ASN")) {
                    return;
                }
            }
            openReader();
        });
    }

    private void openReader() {
        try {
            File file = mmdbPath.toFile();
            DatabaseReader opened = new DatabaseReader.Builder(file).build();
            DatabaseReader previous = reader.getAndSet(opened);
            if (previous != null) {
                previous.close();
            }
            log.general("ASN database ready; datacenter detection is active.");
        } catch (Exception ex) {
            log.warn("Could not open the ASN database: " + ex.getMessage()
                    + ". Datacenter detection is off.");
        }
    }

    /** Redownloads and reopens the database. Called by {@code /lxvpn reload}. */
    public void refresh() {
        if (!enabled) {
            return;
        }
        resultCache.clear();
        AsyncExecutor.get().execute(() -> {
            if (MmdbAccess.download(MIRRORS, mmdbPath, log, "GeoLite2-ASN")) {
                openReader();
            }
        });
    }

    public boolean isReady() {
        return reader.get() != null;
    }

    public int getBonusScore() {
        return bonusScore;
    }

    /**
     * @return whether the address sits in a known hosting network; false when the database is not
     *         available, because an unavailable database must not start blocking players
     */
    public CompletableFuture<Boolean> isDatacenter(String ip) {
        if (!enabled) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        Boolean cached = resultCache.get(ip);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        DatabaseReader current = reader.get();
        if (current == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                AsnResponse response = current.asn(InetAddress.getByName(ip));
                Long asn = response.getAutonomousSystemNumber();
                boolean datacenter = asn != null && DATACENTER_ASNS.containsKey(asn);
                if (datacenter) {
                    log.vpn(ip + " belongs to " + DATACENTER_ASNS.get(asn) + " (AS" + asn + ")");
                }
                resultCache.put(ip, datacenter);
                return datacenter;
            } catch (AddressNotFoundException ex) {
                resultCache.put(ip, Boolean.FALSE);
                return Boolean.FALSE;
            } catch (Exception ex) {
                log.vpn("ASN lookup failed for " + ip + ": " + ex.getMessage());
                return Boolean.FALSE;
            }
        }, AsyncExecutor.get());
    }

    public void close() {
        DatabaseReader current = reader.getAndSet(null);
        if (current != null) {
            try {
                current.close();
            } catch (Exception ignored) {
                // shutting down anyway
            }
        }
        resultCache.clear();
    }
}
