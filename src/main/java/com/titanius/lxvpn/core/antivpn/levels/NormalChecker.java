package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.managers.LogManager;

import java.util.concurrent.CompletableFuture;

/**
 * Level 2: additional Tor and known-VPN ranges, from cached lists.
 *
 * <p>In LXVPN 1.0 this tier no longer reads bundled JSON files. The old build shipped five IP
 * databases inside the jar, which made up most of its size and were frozen at release: an address
 * that changed hands stayed wrong until someone published a new version. These lists are fetched and
 * refreshed at runtime instead.
 */
public class NormalChecker {

    private static final String NEBLINK_TOR = "https://www.dan.me.uk/torlist/?exit";
    private static final String VPN_RANGES = "https://raw.githubusercontent.com/X4BNet/lists_vpn/main/output/vpn/ipv4.txt";
    private static final String DATACENTER_RANGES = "https://raw.githubusercontent.com/X4BNet/lists_vpn/main/output/datacenter/ipv4.txt";

    private final LogManager log;
    private final ListCache lists;

    public NormalChecker(LogManager log, ListCache lists) {
        this.log = log;
        this.lists = lists;
    }

    public void preload() {
        lists.preload(VPN_RANGES);
    }

    public CompletableFuture<Integer> torList(String ip) {
        return lists.contains(ip, NEBLINK_TOR).thenApply(found -> found ? 2 : 0);
    }

    public CompletableFuture<Integer> knownVpnRanges(String ip) {
        return lists.contains(ip, VPN_RANGES).thenApply(found -> {
            if (found) {
                log.vpn(ip + " is in a published commercial VPN range");
            }
            return found ? 2 : 0;
        });
    }

    public CompletableFuture<Integer> datacenterRanges(String ip) {
        return lists.contains(ip, DATACENTER_RANGES).thenApply(found -> found ? 1 : 0);
    }
}
