package com.titanius.lxvpn.core.antivpn;

import com.titanius.lxvpn.core.antivpn.cache.IpScoreCache;
import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.antivpn.iprestrict.IpRestrictManager;
import com.titanius.lxvpn.core.antivpn.geoip.GeoIpManager;
import com.titanius.lxvpn.core.antivpn.levels.BotnetChecker;
import com.titanius.lxvpn.core.antivpn.levels.HighChecker;
import com.titanius.lxvpn.core.antivpn.levels.LowChecker;
import com.titanius.lxvpn.core.antivpn.levels.MidChecker;
import com.titanius.lxvpn.core.antivpn.levels.NormalChecker;
import com.titanius.lxvpn.core.antivpn.levels.ProxyChecker;
import com.titanius.lxvpn.core.antivpn.levels.SuperHighChecker;
import com.titanius.lxvpn.core.managers.ConfigManager;
import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;
import com.titanius.lxvpn.core.util.CIDRUtils;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregates every configured check into a single verdict.
 *
 * <p>The scoring model is the point of the whole design. No single source is trusted to block a
 * player on its own: each contributes points, and only the total crossing the threshold denies a
 * connection. Free reputation APIs disagree with each other constantly, and any plugin that blocks on
 * one API saying "proxy" will refuse real players every week.
 *
 * <p>Everything runs in parallel and every source fails to zero. A provider being down, slow or rate
 * limited reduces accuracy; it never blocks a login and never denies one.
 */
public class AntiVPN {

    private final LogManager log;
    private final Path blacklistFile;
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean writeInFlight = new AtomicBoolean();
    private volatile boolean blacklistDirty;

    private final IpScoreCache scoreCache;
    private final ListCache listCache;

    private final LowChecker low;
    private final NormalChecker normal;
    private final MidChecker mid;
    private final HighChecker high;
    private final SuperHighChecker superHigh;
    private final ProxyChecker proxy;
    private final BotnetChecker botnet;
    private final AsnChecker asn;
    private final GeoIpManager geo;
    private final IpRestrictManager ipRestrict;

    private volatile List<Integer> levels;
    private volatile boolean heavyLists;
    private volatile int minScore;
    private volatile int timeoutSeconds;
    private volatile List<String> bypass;

    private final AtomicLong checked = new AtomicLong();
    private final AtomicLong blocked = new AtomicLong();

    public AntiVPN(Path dataFolder, LogManager log, ConfigManager config,
                   AsnChecker asn, GeoIpManager geo, IpRestrictManager ipRestrict) {
        this.log = log;
        this.asn = asn;
        this.geo = geo;
        this.ipRestrict = ipRestrict;
        this.blacklistFile = dataFolder.resolve("blacklist.json");

        this.scoreCache = new IpScoreCache(config.getScoreCacheMinutes());
        this.listCache = new ListCache(config.getListCacheMinutes(), 3, log);

        this.low = new LowChecker(log, listCache, config);
        this.normal = new NormalChecker(log, listCache);
        this.mid = new MidChecker(log, listCache);
        this.high = new HighChecker(log, listCache, config);
        this.superHigh = new SuperHighChecker(log, listCache);
        this.proxy = new ProxyChecker(log, listCache);
        this.botnet = new BotnetChecker(log, listCache);

        applyConfig(config);
        loadBlacklist();
        warmLists();
    }

    public void applyConfig(ConfigManager config) {
        this.levels = config.getCheckLevels();
        this.heavyLists = config.isDatabaseChecksEnabled();
        this.minScore = config.getMinScore();
        this.timeoutSeconds = config.getLookupTimeoutSeconds();
        this.bypass = config.getBypassList();
        this.low.applyConfig(config);
        this.high.applyConfig(config);

        if (levels.contains(4) && !high.hasAnyKey()) {
            log.general("Check level 4 is on but no API keys are set. "
                    + "The list check still runs; add a vpnapi.io or ipregistry key for the rest.");
        }
    }

    /** Starts downloading the lists this configuration will actually use. */
    private void warmLists() {
        AsyncExecutor.get().execute(() -> {
            if (levels.contains(1)) {
                low.preload();
            }
            if (levels.contains(2)) {
                normal.preload();
            }
            if (levels.contains(3)) {
                mid.preload();
            }
        });
    }

    // ------------------------------------------------------------------ verdict

    /**
     * The single entry point used by both platform layers.
     *
     * @param ip       the connecting address
     * @param username the name being used
     */
    public CompletableFuture<Verdict> check(String ip, String username) {
        if (ip == null || ip.isEmpty()) {
            return CompletableFuture.completedFuture(Verdict.allow());
        }
        checked.incrementAndGet();

        // Local addresses are how staff connect from the same machine, and how a test server talks
        // to itself. No reputation source has anything meaningful to say about them.
        if (CIDRUtils.isLocal(ip) || isBypassed(ip, username)) {
            return CompletableFuture.completedFuture(Verdict.bypass());
        }

        if (!ipRestrict.isAllowed(username, ip)) {
            blocked.incrementAndGet();
            return CompletableFuture.completedFuture(Verdict.deny(Verdict.Reason.IP_RESTRICTED, 0));
        }

        if (blacklist.contains(ip)) {
            blocked.incrementAndGet();
            return CompletableFuture.completedFuture(Verdict.deny(Verdict.Reason.BLACKLISTED, minScore));
        }

        return geo.isBlocked(ip).thenCompose(countryBlocked -> {
            if (countryBlocked) {
                blocked.incrementAndGet();
                return CompletableFuture.completedFuture(Verdict.deny(Verdict.Reason.COUNTRY, 0));
            }
            return reputation(ip, username);
        }).exceptionally(error -> {
            // Any unexpected failure allows the connection. An anti-VPN plugin that fails closed
            // takes the whole network offline the first time it meets an edge case.
            log.vpn("Check failed for " + username + " (" + ip + "): " + error.getMessage());
            return Verdict.allow();
        });
    }

    private boolean isBypassed(String ip, String username) {
        for (String entry : bypass) {
            if (entry.equalsIgnoreCase(username) || entry.equals(ip)) {
                return true;
            }
            if (CIDRUtils.isRange(entry) && CIDRUtils.matches(entry, ip)) {
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<Verdict> reputation(String ip, String username) {
        Integer cached = scoreCache.getScore(ip);
        if (cached != null) {
            log.vpn("Cached score for " + ip + " (" + username + "): " + cached);
            return CompletableFuture.completedFuture(toVerdict(ip, cached, true));
        }

        List<CompletableFuture<Integer>> sources = new ArrayList<>(24);

        if (levels.contains(1)) {
            sources.add(low.proxycheck(ip));
            sources.add(low.freeIpApi(ip));
            sources.add(low.torExitNode(ip));
        }
        if (levels.contains(2)) {
            sources.add(normal.torList(ip));
            sources.add(normal.knownVpnRanges(ip));
            sources.add(normal.datacenterRanges(ip));
        }
        if (levels.contains(3)) {
            sources.add(mid.ipsum(ip, 3));
            sources.add(mid.ipsum(ip, 4));
            sources.add(mid.ipsum(ip, 8));
            sources.add(mid.nordVpn(ip));
        }
        if (levels.contains(4)) {
            sources.add(high.badGuysList(ip));
            sources.add(high.vpnApi(ip));
            sources.add(high.ipRegistry(ip));
            sources.add(high.ipApi(ip));
        }
        if (levels.contains(5) && heavyLists) {
            sources.add(superHigh.ipsum(ip, 1));
            sources.add(superHigh.ipsum(ip, 2));
            sources.add(superHigh.ipsum(ip, 5));
            sources.add(superHigh.ipsum(ip, 6));
            sources.add(superHigh.ipsum(ip, 7));
            sources.add(superHigh.blocklistDe(ip));
        }
        if (levels.contains(6) && heavyLists) {
            sources.add(proxy.socks4(ip));
            sources.add(proxy.socks5(ip));
            sources.add(proxy.http(ip));
            sources.add(proxy.clarketm(ip));
            sources.add(proxy.monosans(ip));
        }
        if (levels.contains(7)) {
            sources.add(botnet.sefinek(ip));
            sources.add(botnet.firehol(ip));
            sources.add(botnet.badGuys(ip));
        }

        sources.add(asn.isDatacenter(ip)
                .thenApply(datacenter -> datacenter ? asn.getBonusScore() : 0));

        CompletableFuture<Integer> total = CompletableFuture
                .allOf(sources.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    int sum = 0;
                    for (CompletableFuture<Integer> source : sources) {
                        try {
                            sum += source.join();
                        } catch (Exception ex) {
                            // already logged by the source; a failed source contributes nothing
                        }
                    }
                    return sum;
                });

        // A hard ceiling on how long a player waits. Without it, one hung provider holds the login
        // open for as long as its socket timeout allows, and the player sees a frozen screen.
        return total.orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(error -> {
                    log.vpn("Scoring timed out for " + ip + " (" + username + "); allowing.");
                    return 0;
                })
                .thenApply(score -> {
                    log.vpn(username + " (" + ip + ") scored " + score + " against a threshold of " + minScore);
                    scoreCache.putScore(ip, score);
                    return toVerdict(ip, score, false);
                });
    }

    private Verdict toVerdict(String ip, int score, boolean fromCache) {
        if (score < minScore) {
            return Verdict.allow();
        }
        blocked.incrementAndGet();
        if (!fromCache) {
            addToBlacklist(ip);
        }
        return Verdict.deny(Verdict.Reason.VPN, score);
    }

    // --------------------------------------------------------------- blacklist

    public boolean isBlacklisted(String ip) {
        return ip != null && blacklist.contains(ip);
    }

    public boolean addToBlacklist(String ip) {
        if (!blacklist.add(ip)) {
            return false;
        }
        log.vpn("Added " + ip + " to the blacklist.");
        blacklistDirty = true;
        scheduleWrite();
        return true;
    }

    public boolean removeFromBlacklist(String ip) {
        if (!blacklist.remove(ip)) {
            return false;
        }
        scoreCache.invalidate(ip);
        blacklistDirty = true;
        scheduleWrite();
        return true;
    }

    public int blacklistSize() {
        return blacklist.size();
    }

    private void loadBlacklist() {
        if (Files.notExists(blacklistFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(blacklistFile, StandardCharsets.UTF_8)) {
            JSONArray array = new JSONArray(new JSONTokener(reader));
            for (int i = 0; i < array.length(); i++) {
                blacklist.add(array.getString(i));
            }
            log.general("Loaded " + blacklist.size() + " blacklisted address(es).");
        } catch (Exception ex) {
            log.warn("Could not read blacklist.json: " + ex.getMessage());
        }
    }

    /**
     * Persists the blacklist with a single in-flight writer.
     *
     * <p>The in-memory set is the authority and is updated immediately. Rewriting a growing file once
     * per address during a flood is quadratic disk work on the connection path; coalescing turns a
     * burst of thousands of additions into a handful of writes.
     */
    private void scheduleWrite() {
        if (!writeInFlight.compareAndSet(false, true)) {
            return;
        }
        AsyncExecutor.get().execute(() -> {
            try {
                do {
                    blacklistDirty = false;
                    writeSnapshot();
                } while (blacklistDirty);
            } finally {
                writeInFlight.set(false);
                if (blacklistDirty) {
                    scheduleWrite();
                }
            }
        });
    }

    private void writeSnapshot() {
        Path temporary = blacklistFile.resolveSibling(blacklistFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(blacklistFile.getParent());
            JSONArray snapshot = new JSONArray(new HashSet<>(blacklist));
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write(snapshot.toString(2));
            }
            Files.move(temporary, blacklistFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            log.warn("Could not save blacklist.json: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------- admin

    /** Runs the full check for an address regardless of cache, for {@code /lxvpn check}. */
    public CompletableFuture<Verdict> inspect(String ip) {
        scoreCache.invalidate(ip);
        return reputation(ip, "console");
    }

    public void cleanupCaches() {
        scoreCache.cleanup();
    }

    public void clearCaches() {
        scoreCache.clear();
        listCache.clear();
    }

    public long checkedCount() {
        return checked.get();
    }

    public long blockedCount() {
        return blocked.get();
    }

    public int cachedScores() {
        return scoreCache.size();
    }

    public int cachedListEntries() {
        return listCache.totalEntries();
    }

    public int cachedLists() {
        return listCache.listCount();
    }

    public String levelSummary() {
        StringBuilder out = new StringBuilder();
        for (Integer level : levels) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(level);
            if ((level == 5 || level == 6) && !heavyLists) {
                out.append(" (off: database-checks is false)");
            }
        }
        return out.length() == 0 ? "none" : out.toString().toLowerCase(Locale.ROOT);
    }
}
