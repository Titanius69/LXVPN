package com.titanius.lxvpn.core.command;

/**
 * Whoever ran a command, reduced to what the core needs.
 *
 * <p>BungeeCord's {@code CommandSender} and Velocity's {@code CommandSource} have nothing in common
 * beyond these three operations, so the command logic is written against these and each platform
 * supplies a four-line adapter.
 */
public interface Sender {

    void send(String message);

    boolean hasPermission(String node);

    String name();
}
