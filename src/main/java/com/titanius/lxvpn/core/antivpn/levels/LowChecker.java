package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.managers.ConfigManager;
import com.titanius.lxvpn.core.managers.LogManager;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * Level 1: the cheap sources that need no account.
 *
 * <p>This is the tier almost every network should leave on. The two APIs have free tiers generous
 * enough for a normal player population, and the Tor exit-node list is a cached download rather than
 * a per-connection request, so it keeps working when the APIs do not.
 */
public class LowChecker {

    /** Published by the Tor project itself, so it is authoritative rather than inferred. */
    private static final String TOR_EXIT_LIST = "https://check.torproject.org/torbulkexitlist";

    private final LogManager log;
    private final ListCache lists;
    private volatile String proxycheckKey;

    public LowChecker(LogManager log, ListCache lists, ConfigManager config) {
        this.log = log;
        this.lists = lists;
        applyConfig(config);
    }

    public void applyConfig(ConfigManager config) {
        this.proxycheckKey = config.getProxyCheckKey();
    }

    public void preload() {
        lists.preload(TOR_EXIT_LIST);
    }

    public CompletableFuture<Integer> proxycheck(String ip) {
        String url = "https://proxycheck.io/v2/" + ip + "?vpn=1&risk=1"
                + (HttpJson.usableKey(proxycheckKey) ? "&key=" + proxycheckKey : "");
        return HttpJson.score(url, "Proxycheck", log, json -> {
            JSONObject data = json.optJSONObject(ip);
            if (data == null) {
                return 0;
            }
            boolean proxy = "yes".equalsIgnoreCase(data.optString("proxy", "no"));
            String type = data.optString("type", "");
            return proxy || "VPN".equalsIgnoreCase(type) ? 1 : 0;
        });
    }

    public CompletableFuture<Integer> freeIpApi(String ip) {
        return HttpJson.score("https://freeipapi.com/api/json/" + ip, "FreeIpAPI", log,
                json -> json.optBoolean("isProxy", false) ? 1 : 0);
    }

    public CompletableFuture<Integer> torExitNode(String ip) {
        return lists.contains(ip, TOR_EXIT_LIST).thenApply(found -> {
            if (found) {
                log.vpn(ip + " is a Tor exit node");
            }
            // Weighted above the API sources: a Tor exit node is a fact from the Tor project's own
            // list, not a third party's guess, so it deserves more than one point.
            return found ? 2 : 0;
        });
    }
}
