package com.titanius.lxvpn.core.platform;

/**
 * Periodic and one-off housekeeping, scheduled the way the host proxy expects.
 *
 * <p>Blocking I/O never goes here - that belongs on {@link
 * com.titanius.lxvpn.core.util.AsyncExecutor}. This is for cache cleanup, list refreshes and the
 * update check.
 */
public interface PlatformScheduler {

    /** Runs the task repeatedly off the main thread. Closing the handle cancels it. */
    AutoCloseable repeating(Runnable task, long initialDelaySeconds, long periodSeconds);

    /** Runs the task once, off the main thread. */
    void async(Runnable task);
}
