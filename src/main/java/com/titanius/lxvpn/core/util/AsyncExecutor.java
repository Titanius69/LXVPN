package com.titanius.lxvpn.core.util;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The shared executor for blocking I/O: reputation lookups, blocklist downloads, webhook posts.
 *
 * <p>Uses virtual threads on Java 21 or newer. This workload is almost entirely threads waiting on
 * sockets, which is exactly what virtual threads are for - a few hundred concurrent lookups cost
 * nothing.
 *
 * <p>The lookup is reflective on purpose. Compiling against {@code newVirtualThreadPerTaskExecutor}
 * would raise the plugin's minimum to Java 21 and lock out every network still on 17, in exchange
 * for a gain that only matters under load. Detecting it at runtime gets both.
 */
public final class AsyncExecutor {

    private static final ExecutorService INSTANCE = create();
    private static final boolean VIRTUAL = INSTANCE.getClass().getName().contains("ThreadPerTask");

    private AsyncExecutor() {
    }

    private static ExecutorService create() {
        try {
            Method factory = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            return (ExecutorService) factory.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // Java 17 path. Bounded on purpose: an unbounded pool would let a botnet flood create a
            // thread per connection attempt, which is a denial of service against the proxy
            // delivered by its own anti-VPN plugin.
            AtomicInteger counter = new AtomicInteger();
            ThreadPoolExecutor pool = new ThreadPoolExecutor(
                    4, 256, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    runnable -> {
                        Thread thread = new Thread(runnable, "LXVPN-IO-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy());
            pool.allowCoreThreadTimeOut(true);
            return pool;
        }
    }

    public static ExecutorService get() {
        return INSTANCE;
    }

    /** True when running on virtual threads. Reported once at startup. */
    public static boolean virtualThreads() {
        return VIRTUAL;
    }

    public static void shutdown() {
        INSTANCE.shutdown();
        try {
            if (!INSTANCE.awaitTermination(5, TimeUnit.SECONDS)) {
                INSTANCE.shutdownNow();
            }
        } catch (InterruptedException ex) {
            INSTANCE.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
