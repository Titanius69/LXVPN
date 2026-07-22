package com.titanius.lxvpn.bungee;

import com.titanius.lxvpn.core.LXVPNCore;
import com.titanius.lxvpn.core.antivpn.Verdict;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Runs the verdict during PreLogin.
 *
 * <p>BungeeCord's asynchronous events use an intent registered on the event itself: the connection
 * waits until the intent is completed, and nothing blocks a proxy thread in the meantime. That is the
 * whole reason the core returns a future rather than a boolean.
 *
 * <p>The timeout is a safety net on top of the core's own. If the intent were never completed - a bug
 * in this plugin, not in a provider - the player's connection would hang forever, and a hung login is
 * a worse failure than an unchecked one.
 */
public class BungeeLoginListener implements Listener {

    private final LXVPNCore core;

    public BungeeLoginListener(LXVPNCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(PreLoginEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getConnection().getSocketAddress() instanceof InetSocketAddress socket)
                || socket.getAddress() == null) {
            return;
        }

        String ip = socket.getAddress().getHostAddress();
        String username = event.getConnection().getName();

        event.registerIntent(BungeePluginHolder.instance());

        core.check(ip, username)
                .orTimeout(15, TimeUnit.SECONDS)
                .whenComplete((verdict, error) -> {
                    try {
                        if (error != null) {
                            core.log().vpn("PreLogin check errored for " + username + ": " + error);
                            return;
                        }
                        if (verdict != null && !verdict.allowed()) {
                            event.setCancelled(true);
                            event.setCancelReason(TextComponent.fromLegacyText(
                                    core.messages().get(verdict.messageKey())));
                            core.log().general("Blocked " + username + " from " + ip
                                    + " (" + verdict.reason().description()
                                    + ", score " + verdict.score() + ")");
                        }
                    } finally {
                        // Must run whatever happened. A completed intent is what releases the
                        // connection; missing one leaves the player stuck on the connecting screen.
                        event.completeIntent(BungeePluginHolder.instance());
                    }
                });
    }

    /** Holds the plugin instance the intent has to be registered against. */
    static final class BungeePluginHolder {
        private static volatile BungeePlugin instance;

        private BungeePluginHolder() {
        }

        static void set(BungeePlugin plugin) {
            instance = plugin;
        }

        static BungeePlugin instance() {
            return instance;
        }
    }
}
