package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.antivpn.cache.ListCache;
import com.titanius.lxvpn.core.managers.LogManager;

import java.util.concurrent.CompletableFuture;

/**
 * Level 5: every remaining Ipsum tier plus a general abuse feed.
 *
 * <p>Gated behind {@code database-checks} and off by default. Together these hold well over a
 * million addresses, which is real memory and a slow first refresh, and the marginal detection over
 * levels 1 to 4 is small for a normal player population.
 *
 * <p>Worth enabling if the network is being targeted specifically. Not worth enabling because more
 * sounds better.
 */
public class SuperHighChecker {

    private static final String IPSUM = "https://raw.githubusercontent.com/stamparm/ipsum/master/levels/";
    private static final String BLOCKLIST_DE = "https://lists.blocklist.de/lists/all.txt";

    private final LogManager log;
    private final ListCache lists;

    public SuperHighChecker(LogManager log, ListCache lists) {
        this.log = log;
        this.lists = lists;
    }

    public CompletableFuture<Integer> ipsum(String ip, int level) {
        return lists.contains(ip, IPSUM + level + ".txt").thenApply(found -> {
            if (!found) {
                return 0;
            }
            log.vpn(ip + " appears on Ipsum level " + level);
            return level <= 2 ? 3 : 1;
        });
    }

    public CompletableFuture<Integer> blocklistDe(String ip) {
        return lists.contains(ip, BLOCKLIST_DE).thenApply(found -> found ? 1 : 0);
    }
}
