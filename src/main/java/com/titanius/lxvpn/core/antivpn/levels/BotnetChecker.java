package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.managers.LogManager;

import java.util.concurrent.CompletableFuture;

/**
 * Level 7: compromised hosts and botnet nodes.
 *
 * <p>Worth being honest about this tier. It catches machines participating in attacks, which
 * overlaps with proxy abuse but is not the same thing - a home computer infected with malware is on
 * these lists, and the person sitting at it is a legitimate player who now cannot connect.
 *
 * <p>Scored at one point each rather than blocking outright, so it can tip a borderline verdict
 * without deciding one on its own. Off by default.
 */
public class BotnetChecker {

    private static final String SEFINEK = "https://raw.githubusercontent.com/sefinek/Sefinek-Blocklist-Collection/main/blocklist/generated/ips.txt";
    private static final String FIREHOL = "https://raw.githubusercontent.com/firehol/blocklist-ipsets/master/firehol_level1.netset";
    private static final String BAD_GUYS = "https://cinsscore.com/list/ci-badguys.txt";

    private final LogManager log;
    private final ListCache lists;

    public BotnetChecker(LogManager log, ListCache lists) {
        this.log = log;
        this.lists = lists;
    }

    public CompletableFuture<Integer> sefinek(String ip) {
        return lists.contains(ip, SEFINEK).thenApply(found -> found ? 1 : 0);
    }

    public CompletableFuture<Integer> firehol(String ip) {
        return lists.contains(ip, FIREHOL).thenApply(found -> {
            if (found) {
                log.vpn(ip + " appears on the FireHOL level 1 set");
            }
            return found ? 1 : 0;
        });
    }

    public CompletableFuture<Integer> badGuys(String ip) {
        return lists.contains(ip, BAD_GUYS).thenApply(found -> found ? 1 : 0);
    }
}
