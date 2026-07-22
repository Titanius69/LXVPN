package com.titanius.lxvpn.core.antivpn.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Bounded, time-expiring cache of computed reputation scores, keyed by address.
 *
 * <p>Most of the value of this plugin comes from here. Without it, every reconnect - and players
 * reconnect constantly - would fire another round of outbound lookups, which is slow for the player,
 * rude to the free API providers, and the fastest way to get the proxy's own address rate limited.
 *
 * <p>Bounded as well as expiring, because a botnet flood presents tens of thousands of distinct
 * addresses in minutes and an unbounded cache would turn that into an out-of-memory error.
 */
public class IpScoreCache {

    private static final int DEFAULT_MAX_SIZE = 50_000;

    private final Map<String, CachedScore> cache = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final int maxSize;

    public IpScoreCache(long ttlMinutes) {
        this(ttlMinutes, DEFAULT_MAX_SIZE);
    }

    public IpScoreCache(long ttlMinutes, int maxSize) {
        this.ttlMs = TimeUnit.MINUTES.toMillis(ttlMinutes);
        this.maxSize = Math.max(1, maxSize);
    }

    public Integer getScore(String ip) {
        CachedScore entry = cache.get(ip);
        if (entry != null && System.currentTimeMillis() - entry.timestamp < ttlMs) {
            return entry.score;
        }
        if (entry != null) {
            cache.remove(ip, entry);
        }
        return null;
    }

    public void putScore(String ip, int score) {
        if (cache.size() >= maxSize && !cache.containsKey(ip)) {
            evict();
        }
        cache.put(ip, new CachedScore(score, System.currentTimeMillis()));
    }

    public void invalidate(String ip) {
        cache.remove(ip);
    }

    /** Drops every expired entry. Called periodically so addresses that never return do not linger. */
    public void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now - entry.getValue().timestamp >= ttlMs);
    }

    private void evict() {
        cleanup();
        if (cache.size() < maxSize) {
            return;
        }
        String oldestKey = null;
        long oldestTimestamp = Long.MAX_VALUE;
        for (Map.Entry<String, CachedScore> entry : cache.entrySet()) {
            if (entry.getValue().timestamp < oldestTimestamp) {
                oldestTimestamp = entry.getValue().timestamp;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    private static final class CachedScore {
        final int score;
        final long timestamp;

        CachedScore(int score, long timestamp) {
            this.score = score;
            this.timestamp = timestamp;
        }
    }
}
