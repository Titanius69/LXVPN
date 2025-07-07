package com.titanius.lxantivpn.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import javax.inject.Inject;
import java.util.logging.Logger;

@Plugin(
        id = "lxantivpn",
        name = "LXVPN",
        version = "1.0-APLHA",
        authors = {"SufniX"}
)
public class Main {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public Main(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        logger.info(event.getPlayer().getUsername() + " csatlakozott a szerverhez.");
    }
}
