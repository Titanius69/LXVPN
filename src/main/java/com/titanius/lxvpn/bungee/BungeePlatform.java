package com.titanius.lxvpn.bungee;

import com.titanius.lxvpn.core.platform.Platform;
import com.titanius.lxvpn.core.platform.PlatformLogger;
import com.titanius.lxvpn.core.platform.PlatformScheduler;
import com.titanius.lxvpn.core.util.Colors;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Maps the {@link Platform} contract onto BungeeCord. */
public class BungeePlatform implements Platform {

    private final BungeePlugin plugin;
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;

    public BungeePlatform(BungeePlugin plugin) {
        this.plugin = plugin;
        this.logger = new PlatformLogger() {
            @Override
            public void info(String message) {
                plugin.getLogger().info(Colors.strip(message));
            }

            @Override
            public void warn(String message) {
                plugin.getLogger().warning(Colors.strip(message));
            }

            @Override
            public void error(String message, Throwable error) {
                plugin.getLogger().severe(Colors.strip(message)
                        + (error == null ? "" : " - " + error));
            }
        };
        this.scheduler = new PlatformScheduler() {
            @Override
            public AutoCloseable repeating(Runnable task, long initialDelaySeconds, long periodSeconds) {
                ScheduledTask scheduled = ProxyServer.getInstance().getScheduler().schedule(
                        plugin, task, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
                return scheduled::cancel;
            }

            @Override
            public void async(Runnable task) {
                ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
            }
        };
    }

    @Override
    public String name() {
        return "bungeecord";
    }

    @Override
    public String proxyVersion() {
        return ProxyServer.getInstance().getVersion();
    }

    @Override
    public Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public PlatformLogger logger() {
        return logger;
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public void kick(String username, String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(username);
        if (player != null) {
            player.disconnect(TextComponent.fromLegacyText(message));
        }
    }

    @Override
    public Optional<UUID> playerId(String username) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(username);
        return player == null ? Optional.empty() : Optional.of(player.getUniqueId());
    }

    @Override
    public Optional<String> playerAddress(String username) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(username);
        if (player == null || !(player.getSocketAddress() instanceof InetSocketAddress socket)) {
            return Optional.empty();
        }
        return Optional.ofNullable(socket.getAddress()).map(java.net.InetAddress::getHostAddress);
    }

    @Override
    public int onlinePlayers() {
        return ProxyServer.getInstance().getOnlineCount();
    }
}
