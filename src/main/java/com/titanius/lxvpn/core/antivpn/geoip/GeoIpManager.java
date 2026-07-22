package com.titanius.lxvpn.core.antivpn.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.titanius.lxvpn.core.antivpn.MmdbAccess;
import com.titanius.lxvpn.core.managers.ConfigManager;
import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Country-based access control, backed by a local GeoLite2 database rather than a remote API.
 *
 * <p>Deliberately not an HTTP lookup. Country filtering runs on every single connection attempt, and
 * a per-connection API call means a rate limit, an API key, and a login that stalls whenever the
 * provider is slow. Downloaded once, every lookup after that is an in-memory query.
 *
 * <p>Supports both directions. A blacklist blocks the listed countries; a whitelist blocks
 * everything else. The second is much stronger for a network whose players are all in one region,
 * and much more dangerous if the list is wrong, which is why neither is enabled by default.
 *
 * <p>This only ever denies. It never overrides a decision to allow made elsewhere.
 */
public class GeoIpManager {

    private static final List<String> MIRRORS = List.of(
            "https://github.com/P3TERX/GeoLite.mmdb/raw/download/GeoLite2-Country.mmdb",
            "https://raw.githubusercontent.com/P3TERX/GeoLite.mmdb/download/GeoLite2-Country.mmdb"
    );

    private static final int MAX_CACHE_SIZE = 50_000;
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(6);

    private final LogManager log;
    private final Path mmdbPath;
    private final Map<String, CachedCountry> cache = new ConcurrentHashMap<>();
    private final AtomicReference<DatabaseReader> reader = new AtomicReference<>();

    private volatile boolean enabled;
    private volatile boolean whitelistMode;
    private volatile List<String> countries = List.of();

    public GeoIpManager(Path dataFolder, LogManager log, ConfigManager config) {
        this.log = log;
        this.mmdbPath = dataFolder.resolve("GeoLite2-Country.mmdb");
        applyConfig(config);
        if (enabled) {
            initialiseAsync();
        }
    }

    public void applyConfig(ConfigManager config) {
        this.enabled = config.isGeoEnabled();
        this.whitelistMode = "whitelist".equals(config.getGeoMode());
        this.countries = config.getGeoCountries();

        if (enabled && countries.isEmpty()) {
            // A whitelist with no entries would block the entire internet; a blacklist with none
            // does nothing. Both are configuration mistakes worth naming out loud.
            log.warn("Country filtering is enabled but no countries are listed. "
                    + (whitelistMode ? "In whitelist mode that would block everyone, so it stays off."
                    : "In blacklist mode it has no effect."));
            this.enabled = false;
        }
    }

    private void initialiseAsync() {
        AsyncExecutor.get().execute(() -> {
            if (Files.notExists(mmdbPath) || MmdbAccess.tooSmall(mmdbPath)) {
                if (!MmdbAccess.download(MIRRORS, mmdbPath, log, "GeoLite2-Country")) {
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
            log.general("Country database ready; geographic filtering is active in "
                    + (whitelistMode ? "whitelist" : "blacklist") + " mode.");
        } catch (Exception ex) {
            log.warn("Could not open the country database: " + ex.getMessage()
                    + ". Geographic filtering is off.");
        }
    }

    public void refresh() {
        if (!enabled) {
            return;
        }
        cache.clear();
        AsyncExecutor.get().execute(() -> {
            if (MmdbAccess.download(MIRRORS, mmdbPath, log, "GeoLite2-Country")) {
                openReader();
            }
        });
    }

    public boolean isReady() {
        return reader.get() != null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** The two-letter country code for an address, if it is known. */
    public Optional<String> countryOf(String ip) {
        DatabaseReader current = reader.get();
        if (current == null) {
            return Optional.empty();
        }
        CachedCountry cached = cache.get(ip);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return Optional.ofNullable(cached.code);
        }
        try {
            CountryResponse response = current.country(InetAddress.getByName(ip));
            String code = response.getCountry().getIsoCode();
            store(ip, code);
            return Optional.ofNullable(code);
        } catch (AddressNotFoundException ex) {
            store(ip, null);
            return Optional.empty();
        } catch (Exception ex) {
            log.vpn("Country lookup failed for " + ip + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * @return true when this address should be refused on geographic grounds
     */
    public CompletableFuture<Boolean> isBlocked(String ip) {
        if (!enabled || reader.get() == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        return CompletableFuture.supplyAsync(() -> {
            Optional<String> code = countryOf(ip);
            if (code.isEmpty()) {
                // An address the database does not recognise is not evidence of anything. Blocking
                // on absent data would refuse legitimate players every time a new range is allocated.
                return Boolean.FALSE;
            }
            String upper = code.get().toUpperCase(Locale.ROOT);
            boolean listed = countries.contains(upper);
            boolean blocked = whitelistMode != listed;
            if (blocked) {
                log.vpn(ip + " refused on country " + upper
                        + " (" + (whitelistMode ? "not on the whitelist" : "on the blacklist") + ")");
            }
            return blocked;
        }, AsyncExecutor.get()).exceptionally(error -> {
            log.vpn("Country check failed for " + ip + ": " + error.getMessage());
            return Boolean.FALSE;
        });
    }

    private void store(String ip, String code) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.clear(); // a flood of unique addresses should cost memory, not grow forever
        }
        cache.put(ip, new CachedCountry(code, System.currentTimeMillis()));
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
        cache.clear();
    }

    private static final class CachedCountry {
        final String code;
        final long timestamp;

        CachedCountry(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }
    }
}
