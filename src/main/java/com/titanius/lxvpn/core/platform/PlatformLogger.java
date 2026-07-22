package com.titanius.lxvpn.core.platform;

/**
 * The logging surface the core uses.
 *
 * <p>An interface rather than a concrete logger because Velocity hands out SLF4J and BungeeCord
 * hands out {@code java.util.logging}. A core that imports either one stops being portable.
 */
public interface PlatformLogger {

    void info(String message);

    void warn(String message);

    void error(String message, Throwable error);
}
