package com.titanius.lxvpn.velocity;

import com.titanius.lxvpn.core.LXVPNCore;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Runs the verdict during PreLogin.
 *
 * <p>Velocity's {@link EventTask#resumeWhenComplete} is the equivalent of BungeeCord's intents: the
 * connection is held while the future runs, and no proxy thread blocks. The two platform listeners
 * therefore differ only in this mechanism - the decision itself is made by the same core.
 *
 * <p>Runs late so that any plugin with a reason to deny first has already done so, and this one does
 * not spend outbound lookups on a connection that was going to be refused anyway.
 */
public class VelocityLoginListener {

    private final LXVPNCore core;

    public VelocityLoginListener(LXVPNCore core) {
        this.core = core;
    }

    @Subscribe(order = PostOrder.LATE)
    public EventTask onPreLogin(PreLoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return null;
        }
        InetSocketAddress socket = event.getConnection().getRemoteAddress();
        if (socket == null || socket.getAddress() == null) {
            return null;
        }

        String ip = socket.getAddress().getHostAddress();
        String username = event.getUsername();

        return EventTask.resumeWhenComplete(
                core.check(ip, username)
                        .orTimeout(15, TimeUnit.SECONDS)
                        .handle((verdict, error) -> {
                            if (error != null) {
                                core.log().vpn("PreLogin check errored for " + username + ": " + error);
                                return null;
                            }
                            if (verdict != null && !verdict.allowed()) {
                                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                                        VelocityPlatform.LEGACY.deserialize(
                                                core.messages().get(verdict.messageKey()))));
                                core.log().general("Blocked " + username + " from " + ip
                                        + " (" + verdict.reason().description()
                                        + ", score " + verdict.score() + ")");
                            }
                            return null;
                        }));
    }
}
