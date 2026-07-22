package com.titanius.lxvpn.core.antivpn.cache;

import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fetches and caches the remote blocklists the level checkers query.
 *
 * <p>These lists are the reason LXVPN 1.0 ships no bundled IP databases. A list baked into the jar
 * is stale the week after release and cannot be corrected without a new build; the same list fetched
 * and refreshed at runtime is current, and takes no space in the download.
 *
 * <p>A membership query never blocks on the network. If the list is not loaded yet, the first caller
 * triggers the download; if it is stale, a refresh is started in the background and the caller is
 * answered from the copy already in memory. Slightly old data beats a login that hangs.
 */
public class ListCache {

    private final Map<String, CachedList> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> refreshing = new ConcurrentHashMap<>();
    private final long refreshIntervalMs;
    private final int maxRetries;
    private final LogManager log;

    public ListCache(long refreshMinutes, int maxRetries, LogManager log) {
        this.refreshIntervalMs = TimeUnit.MINUTES.toMillis(refreshMinutes);
        this.maxRetries = Math.max(1, maxRetries);
        this.log = log;
    }

    public CompletableFuture<Boolean> contains(String ip, String url) {
        CachedList list = cache.get(url);
        if (list == null) {
            return CompletableFuture.supplyAsync(() -> {
                CachedList loaded = cache.get(url);
                if (loaded == null) {
                    loaded = load(url);
                    cache.put(url, loaded);
                }
                return loaded.addresses.contains(ip);
            }, AsyncExecutor.get()).exceptionally(error -> {
                log.vpn("List lookup failed for " + url + ": " + error.getMessage());
                return Boolean.FALSE;
            });
        }

        if (System.currentTimeMillis() - list.loadedAt > refreshIntervalMs) {
            refreshAsync(url);
        }
        return CompletableFuture.completedFuture(list.addresses.contains(ip));
    }

    /** Warms a list at startup so the first player of the day does not pay for the download. */
    public void preload(String url) {
        if (cache.containsKey(url)) {
            return;
        }
        refreshAsync(url);
    }

    private void refreshAsync(String url) {
        AtomicBoolean guard = refreshing.computeIfAbsent(url, key -> new AtomicBoolean());
        if (!guard.compareAndSet(false, true)) {
            return; // a refresh for this list is already running
        }
        AsyncExecutor.get().execute(() -> {
            try {
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    CachedList loaded = load(url);
                    if (!loaded.addresses.isEmpty()) {
                        cache.put(url, loaded);
                        return;
                    }
                    if (attempt == maxRetries) {
                        // An empty result is kept out of the cache on purpose. Replacing a good list
                        // with an empty one because a mirror was briefly down would silently disable
                        // that check until the next refresh.
                        log.vpn("List refresh produced nothing usable: " + url);
                    }
                }
            } finally {
                guard.set(false);
            }
        });
    }

    private CachedList load(String urlString) {
        Set<String> addresses = new HashSet<>();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "LXVPN");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String entry = line.trim();
                    if (entry.isEmpty() || entry.charAt(0) == '#' || entry.charAt(0) == ';') {
                        continue;
                    }
                    // Several of these lists publish "address:port" or "address # comment".
                    int cut = entry.indexOf(':');
                    if (cut > 0 && entry.indexOf(':', cut + 1) < 0) {
                        entry = entry.substring(0, cut);
                    }
                    cut = entry.indexOf(' ');
                    if (cut > 0) {
                        entry = entry.substring(0, cut);
                    }
                    if (!entry.isEmpty()) {
                        addresses.add(entry);
                    }
                }
            }
        } catch (Exception ex) {
            log.vpn("Error loading list " + urlString + ": " + ex.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new CachedList(addresses, System.currentTimeMillis());
    }

    public void clear() {
        cache.clear();
    }

    /** Total addresses currently held across every cached list, for the stats command. */
    public int totalEntries() {
        int total = 0;
        for (CachedList list : cache.values()) {
            total += list.addresses.size();
        }
        return total;
    }

    public int listCount() {
        return cache.size();
    }

    private static final class CachedList {
        final Set<String> addresses;
        final long loadedAt;

        CachedList(Set<String> addresses, long loadedAt) {
            this.addresses = Collections.unmodifiableSet(addresses);
            this.loadedAt = loadedAt;
        }
    }
}
