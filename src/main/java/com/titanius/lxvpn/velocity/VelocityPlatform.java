package com.titanius.lxvpn.velocity;

import com.titanius.lxvpn.core.platform.Platform;
import com.titanius.lxvpn.core.platform.PlatformLogger;
import com.titanius.lxvpn.core.platform.PlatformScheduler;
import com.titanius.lxvpn.core.util.Colors;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Maps the {@link Platform} contract onto Velocity. */
public class VelocityPlatform implements Platform {

    static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ProxyServer server;
    private final Path dataDirectory;
    private final PlatformLogger platformLogger;
    private final PlatformScheduler scheduler;

    public VelocityPlatform(VelocityPlugin plugin, ProxyServer server, Logger logger, Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
        this.platformLogger = new PlatformLogger() {
            @Override
            public void info(String message) {
                logger.info(Colors.strip(message));
            }

            @Override
            public void warn(String message) {
                logger.warn(Colors.strip(message));
            }

            @Override
            public void error(String message, Throwable error) {
                logger.error(Colors.strip(message), error);
            }
        };
        this.scheduler = new PlatformScheduler() {
            @Override
            public AutoCloseable repeating(Runnable task, long initialDelaySeconds, long periodSeconds) {
                ScheduledTask scheduled = server.getScheduler().buildTask(plugin, task)
                        .delay(initialDelaySeconds, TimeUnit.SECONDS)
                        .repeat(periodSeconds, TimeUnit.SECONDS)
                        .schedule();
                return scheduled::cancel;
            }

            @Override
            public void async(Runnable task) {
                server.getScheduler().buildTask(plugin, task).schedule();
            }
        };
    }

    @Override
    public String name() {
        return "velocity";
    }

    @Override
    public String proxyVersion() {
        return server.getVersion().getVersion();
    }

    @Override
    public Path dataFolder() {
        return dataDirectory;
    }

    @Override
    public PlatformLogger logger() {
        return platformLogger;
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public void kick(String username, String message) {
        server.getPlayer(username).ifPresent(player ->
                player.disconnect(LEGACY.deserialize(message)));
    }

    @Override
    public Optional<UUID> playerId(String username) {
        return server.getPlayer(username).map(Player::getUniqueId);
    }

    @Override
    public Optional<String> playerAddress(String username) {
        return server.getPlayer(username)
                .map(Player::getRemoteAddress)
                .map(InetSocketAddress::getAddress)
                .map(java.net.InetAddress::getHostAddress);
    }

    @Override
    public int onlinePlayers() {
        return server.getPlayerCount();
    }
}
