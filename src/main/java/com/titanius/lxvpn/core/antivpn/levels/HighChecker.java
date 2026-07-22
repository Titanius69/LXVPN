package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.managers.ConfigManager;
import com.titanius.lxvpn.core.managers.LogManager;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * Level 4: the commercial providers, plus one large reputation list.
 *
 * <p>The API sources here are the most accurate in the plugin and the only ones that need a key.
 * Each is skipped silently when its key is missing, so this level can be left on with no keys at all
 * and still contribute the list check.
 *
 * <p>They also score higher than the free sources. A provider that maintains its own detection is
 * more trustworthy than a public list, and pretending otherwise means either false positives from
 * over-weighted lists or false negatives from under-weighted APIs.
 */
public class HighChecker {

    private static final String BAD_GUYS = "https://cinsscore.com/list/ci-badguys.txt";

    private final LogManager log;
    private final ListCache lists;

    private volatile String vpnApiKey;
    private volatile String ipRegistryKey;

    public HighChecker(LogManager log, ListCache lists, ConfigManager config) {
        this.log = log;
        this.lists = lists;
        applyConfig(config);
    }

    public void applyConfig(ConfigManager config) {
        this.vpnApiKey = config.getVpnApiKey();
        this.ipRegistryKey = config.getIpRegistryKey();
    }

    public boolean hasAnyKey() {
        return HttpJson.usableKey(vpnApiKey) || HttpJson.usableKey(ipRegistryKey);
    }

    public CompletableFuture<Integer> badGuysList(String ip) {
        return lists.contains(ip, BAD_GUYS).thenApply(found -> found ? 1 : 0);
    }

    public CompletableFuture<Integer> vpnApi(String ip) {
        if (!HttpJson.usableKey(vpnApiKey)) {
            return CompletableFuture.completedFuture(0);
        }
        return HttpJson.score("https://vpnapi.io/api/" + ip + "?key=" + vpnApiKey, "VPNAPI", log, json -> {
            JSONObject security = json.optJSONObject("security");
            if (security == null) {
                return 0;
            }
            boolean flagged = security.optBoolean("vpn", false)
                    || security.optBoolean("proxy", false)
                    || security.optBoolean("tor", false)
                    || security.optBoolean("relay", false);
            if (flagged) {
                log.vpn(ip + " flagged by vpnapi.io");
            }
            return flagged ? 3 : 0;
        });
    }

    public CompletableFuture<Integer> ipRegistry(String ip) {
        if (!HttpJson.usableKey(ipRegistryKey)) {
            return CompletableFuture.completedFuture(0);
        }
        return HttpJson.score("https://api.ipregistry.co/" + ip + "?key=" + ipRegistryKey,
                "ipregistry", log, json -> {
                    JSONObject security = json.optJSONObject("security");
                    if (security == null) {
                        return 0;
                    }
                    boolean flagged = security.optBoolean("is_vpn", false)
                            || security.optBoolean("is_proxy", false)
                            || security.optBoolean("is_tor", false)
                            || security.optBoolean("is_tor_exit", false)
                            || security.optBoolean("is_relay", false);
                    if (flagged) {
                        log.vpn(ip + " flagged by ipregistry");
                    }
                    return flagged ? 3 : 0;
                });
    }

    /** Free, no key, but rate limited hard enough that it cannot be the only source. */
    public CompletableFuture<Integer> ipApi(String ip) {
        return HttpJson.score("https://api.ipapi.is/?q=" + ip, "ipapi.is", log, json -> {
            boolean flagged = json.optBoolean("is_vpn", false)
                    || json.optBoolean("is_proxy", false)
                    || json.optBoolean("is_tor", false);
            return flagged ? 2 : 0;
        });
    }
}
