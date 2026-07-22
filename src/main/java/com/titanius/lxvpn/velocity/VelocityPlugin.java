package com.titanius.lxvpn.velocity;

import com.google.inject.Inject;
import com.titanius.lxvpn.core.LXVPNCore;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity entry point.
 *
 * <p>In LXVPN 1.0 this is a real implementation rather than the stub the previous build shipped, and
 * it runs exactly the same checks as the BungeeCord side because both call the same core.
 *
 * <p>The plugin is declared in {@code velocity-plugin.json} rather than through the annotation
 * processor, because one jar has to declare itself to two different proxies and the processor would
 * only know about this half.
 */
public class VelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private LXVPNCore core;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            core = new LXVPNCore(new VelocityPlatform(this, server, logger, dataDirectory));
        } catch (Exception ex) {
            logger.error("LXVPN failed to start", ex);
            return;
        }

        server.getEventManager().register(this, new VelocityLoginListener(core));

        CommandManager commands = server.getCommandManager();
        CommandMeta meta = commands.metaBuilder("lxvpn")
                .aliases("antivpn", "lxantivpn")
                .plugin(this)
                .build();
        commands.register(meta, new VelocityCommand(core).build());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            core.shutdown();
        }
    }
}
