package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.managers.LogManager;

import java.util.concurrent.CompletableFuture;

/**
 * Level 6: published open-proxy lists.
 *
 * <p>These are the lists people scraping free proxies use to find them, which is exactly why they
 * are useful here: an address advertised publicly as an open SOCKS or HTTP proxy is not somebody's
 * home connection.
 *
 * <p>Scored high because the evidence is unambiguous. Also gated behind {@code database-checks},
 * since the lists turn over quickly and each refresh is a fresh download.
 */
public class ProxyChecker {

    private static final String SPEEDX = "https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/";
    private static final String CLARKETM = "https://raw.githubusercontent.com/clarketm/proxy-list/master/proxy-list-raw.txt";
    private static final String MONOSANS = "https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/all.txt";

    private final LogManager log;
    private final ListCache lists;

    public ProxyChecker(LogManager log, ListCache lists) {
        this.log = log;
        this.lists = lists;
    }

    public CompletableFuture<Integer> socks4(String ip) {
        return score(ip, SPEEDX + "socks4.txt", "SOCKS4");
    }

    public CompletableFuture<Integer> socks5(String ip) {
        return score(ip, SPEEDX + "socks5.txt", "SOCKS5");
    }

    public CompletableFuture<Integer> http(String ip) {
        return score(ip, SPEEDX + "http.txt", "HTTP proxy");
    }

    public CompletableFuture<Integer> clarketm(String ip) {
        return score(ip, CLARKETM, "open proxy");
    }

    public CompletableFuture<Integer> monosans(String ip) {
        return score(ip, MONOSANS, "open proxy");
    }

    private CompletableFuture<Integer> score(String ip, String url, String label) {
        return lists.contains(ip, url).thenApply(found -> {
            if (found) {
                log.vpn(ip + " is published as a public " + label);
            }
            return found ? 3 : 0;
        });
    }
}
