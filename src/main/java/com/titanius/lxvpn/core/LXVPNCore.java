package com.titanius.lxvpn.core;

import com.titanius.lxvpn.core.antivpn.AntiVPN;
import com.titanius.lxvpn.core.antivpn.AsnChecker;
import com.titanius.lxvpn.core.antivpn.Verdict;
import com.titanius.lxvpn.core.antivpn.geoip.GeoIpManager;
import com.titanius.lxvpn.core.antivpn.iprestrict.IpRestrictManager;
import com.titanius.lxvpn.core.managers.ConfigManager;
import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.managers.MessageManager;
import com.titanius.lxvpn.core.platform.Platform;
import com.titanius.lxvpn.core.update.UpdateChecker;
import com.titanius.lxvpn.core.util.AsyncExecutor;
import com.titanius.lxvpn.core.webhook.DiscordWebhookClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Owns every component and exposes the one method the platform layers actually call.
 *
 * <p>LXVPN 1.0 is anti-VPN and nothing else. The bot filtering, captcha and limbo work that grew out
 * of this codebase lives in LuminShield, and keeping the two apart is deliberate: they solve
 * different problems, on different schedules, and a network that only wants one should not be made
 * to run both.
 */
public class LXVPNCore {

    public static final String VERSION = "1.0";

    private final Platform platform;
    private final ConfigManager config;
    private final MessageManager messages;
    private final LogManager log;
    private final AsnChecker asn;
    private final GeoIpManager geo;
    private final IpRestrictManager ipRestrict;
    private final AntiVPN antiVpn;
    private final DiscordWebhookClient webhook;
    private final UpdateChecker updates;

    private final List<AutoCloseable> tasks = new ArrayList<>();

    public LXVPNCore(Platform platform) {
        this.platform = platform;
        this.config = new ConfigManager(platform.dataFolder(), platform.logger());
        this.log = new LogManager(platform.dataFolder(), platform.logger(),
                config.isDebugLogging(), config.isLogToFile());
        this.messages = new MessageManager(platform.dataFolder(), platform.logger());

        this.asn = new AsnChecker(platform.dataFolder(), log, config);
        this.geo = new GeoIpManager(platform.dataFolder(), log, config);
        this.ipRestrict = new IpRestrictManager(platform.dataFolder(), log, config.isIpRestrictEnabled());
        this.antiVpn = new AntiVPN(platform.dataFolder(), log, config, asn, geo, ipRestrict);

        this.webhook = new DiscordWebhookClient(log);
        this.webhook.configure(config.isWebhookEnabled(), config.getWebhookUrl(),
                config.getWebhookCooldownSeconds());

        this.updates = new UpdateChecker(log, VERSION, platform.name(), config.getUpdateResourceId());

        startTasks();
        announce();
    }

    private void announce() {
        log.general("LXVPN " + VERSION + " starting on " + platform.name()
                + " (" + platform.proxyVersion() + ")");
        log.general("Check levels: " + antiVpn.levelSummary()
                + " | threshold: " + config.getMinScore()
                + " | I/O: " + (AsyncExecutor.virtualThreads()
                ? "virtual threads" : "pooled threads (Java 17)"));
        if (!config.isAntiVpnEnabled()) {
            log.warn("antivpn.enabled is false; every connection will be allowed through.");
        }
    }

    private void startTasks() {
        // Expired score entries would otherwise sit in memory until an address happened to return.
        tasks.add(platform.scheduler().repeating(antiVpn::cleanupCaches, 300, 300));

        if (config.isUpdateCheckEnabled() && config.getUpdateResourceId() > 0) {
            tasks.add(platform.scheduler().repeating(updates::check, 30, 6 * 3600));
        }
    }

    /**
     * The one call the platform layers make on the connection path.
     *
     * <p>Returns a future because every source is off-thread. Neither proxy's login event should ever
     * wait on a socket.
     */
    public CompletableFuture<Verdict> check(String ip, String username) {
        if (!config.isAntiVpnEnabled()) {
            return CompletableFuture.completedFuture(Verdict.allow());
        }
        return antiVpn.check(ip, username).thenApply(verdict -> {
            if (!verdict.allowed() && verdict.reason() != Verdict.Reason.BLACKLISTED) {
                webhook.blocked(username, ip, verdict.reason().description(), verdict.score());
            }
            return verdict;
        });
    }

    /** Whether a denied verdict that arrives late should still disconnect the player. */
    public boolean isFailOpen() {
        return config.isFailOpen();
    }

    public boolean reload() {
        boolean ok = config.reload();
        messages.reload();
        if (!ok) {
            return false;
        }
        log.applySettings(config.isDebugLogging(), config.isLogToFile());
        asn.applyConfig(config);
        geo.applyConfig(config);
        ipRestrict.setEnabled(config.isIpRestrictEnabled());
        antiVpn.applyConfig(config);
        webhook.configure(config.isWebhookEnabled(), config.getWebhookUrl(),
                config.getWebhookCooldownSeconds());
        return true;
    }

    public void shutdown() {
        for (AutoCloseable task : tasks) {
            try {
                task.close();
            } catch (Exception ignored) {
                // shutting down anyway
            }
        }
        tasks.clear();
        asn.close();
        geo.close();
        AsyncExecutor.shutdown();
        log.general("LXVPN stopped.");
    }

    public Platform platform() {
        return platform;
    }

    public ConfigManager config() {
        return config;
    }

    public MessageManager messages() {
        return messages;
    }

    public LogManager log() {
        return log;
    }

    public AntiVPN antiVpn() {
        return antiVpn;
    }

    public AsnChecker asn() {
        return asn;
    }

    public GeoIpManager geo() {
        return geo;
    }

    public IpRestrictManager ipRestrict() {
        return ipRestrict;
    }

    public UpdateChecker updates() {
        return updates;
    }
}
