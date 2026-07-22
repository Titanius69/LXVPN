package com.titanius.lxvpn.bungee;

import com.titanius.lxvpn.core.LXVPNCore;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord entry point.
 *
 * <p>Thin by design. Everything here is either "translate a Bungee concept into a core one" or
 * "register something"; no anti-VPN logic lives on this side of the boundary, which is what makes one
 * jar work on two proxies without the two copies drifting apart.
 */
public class BungeePlugin extends Plugin {

    private LXVPNCore core;

    @Override
    public void onEnable() {
        try {
            core = new LXVPNCore(new BungeePlatform(this));
        } catch (Exception ex) {
            getLogger().severe("LXVPN failed to start: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }
        BungeeLoginListener.BungeePluginHolder.set(this);
        getProxy().getPluginManager().registerListener(this, new BungeeLoginListener(core));
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand(core));
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
        }
    }
}
