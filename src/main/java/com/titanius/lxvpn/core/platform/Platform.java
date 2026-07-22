package com.titanius.lxvpn.core.platform;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Everything the platform-independent core needs from whatever proxy it is running on.
 *
 * <p>Deliberately small. BungeeCord and Velocity disagree about almost everything - event models,
 * text components, scheduling, command registration - but they agree on the handful of facts below.
 * Every method the core can answer for itself is one that would otherwise be implemented twice and
 * drift apart.
 *
 * <p>This is why LXVPN 1.0 supports both proxies from one jar: the anti-VPN logic never mentions
 * either of them.
 */
public interface Platform {

    /** {@code bungeecord} or {@code velocity}. Appears in logs and the update-check user agent. */
    String name();

    /** The proxy's own version string, for diagnostics. */
    String proxyVersion();

    /** This plugin's data folder. Created by the platform layer before the core starts. */
    Path dataFolder();

    PlatformLogger logger();

    PlatformScheduler scheduler();

    /** Disconnects a player if they are online, with an already-formatted message. */
    void kick(String username, String message);

    /** Resolves an online player's name to their id. */
    Optional<UUID> playerId(String username);

    /** The current address of an online player, or empty if they are not connected. */
    Optional<String> playerAddress(String username);

    int onlinePlayers();
}
