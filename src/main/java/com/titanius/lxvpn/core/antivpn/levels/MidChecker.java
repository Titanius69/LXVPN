package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.managers.LogManager;

import java.util.concurrent.CompletableFuture;

/**
 * Level 3: the large aggregated abuse lists.
 *
 * <p>Each of these is tens of thousands of lines. They are downloaded once and held in memory, so the
 * cost is memory and one refresh per interval rather than anything on the connection path - which is
 * the difference between this and the old implementation, where the same data was read from disk.
 *
 * <p>Ipsum levels count how many independent sources agree an address is abusive. A lower number
 * means more sources agreed, so level 3 is stronger evidence than level 8.
 */
public class MidChecker {

    private static final String IPSUM = "https://raw.githubusercontent.com/stamparm/ipsum/master/levels/";
    private static final String NORDVPN = "https://raw.githubusercontent.com/pikachuwww/nordvpn-server-list/main/nordvpn-ips.txt";

    private final LogManager log;
    private final ListCache lists;

    public MidChecker(LogManager log, ListCache lists) {
        this.log = log;
        this.lists = lists;
    }

    public void preload() {
        lists.preload(IPSUM + "3.txt");
    }

    public CompletableFuture<Integer> ipsum(String ip, int level) {
        return lists.contains(ip, IPSUM + level + ".txt").thenApply(found -> {
            if (!found) {
                return 0;
            }
            log.vpn(ip + " appears on Ipsum level " + level);
            // Fewer sources agreeing is weaker evidence, so higher levels score less.
            return level <= 3 ? 2 : 1;
        });
    }

    public CompletableFuture<Integer> nordVpn(String ip) {
        return lists.contains(ip, NORDVPN).thenApply(found -> found ? 2 : 0);
    }
}
