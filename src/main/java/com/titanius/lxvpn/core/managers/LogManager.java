package com.titanius.lxvpn.core.managers;

import com.titanius.lxvpn.core.platform.PlatformLogger;
import com.titanius.lxvpn.core.util.AsyncExecutor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Routes plugin messages to the console and, optionally, to {@code logs/lxvpn.log}.
 *
 * <p>Two channels. {@link #general} is for things an operator should see; {@link #vpn} is the
 * per-connection verdict trace, which is enormous on a busy proxy and off unless debugging is on.
 * Keeping them separate means turning on the detail does not drown the useful lines.
 *
 * <p>File writes are queued and drained by a single writer. A verdict is produced on every login
 * attempt, and during a flood that is thousands per minute; blocking the connection path on a disk
 * write would make the plugin the bottleneck it exists to prevent.
 */
public class LogManager {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlatformLogger logger;
    private final Path logFile;
    private final ConcurrentLinkedQueue<String> pending = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean draining = new AtomicBoolean();

    private volatile boolean debug;
    private volatile boolean toFile;

    public LogManager(Path dataFolder, PlatformLogger logger, boolean debug, boolean toFile) {
        this.logger = logger;
        this.debug = debug;
        this.toFile = toFile;
        this.logFile = dataFolder.resolve("logs").resolve("lxvpn.log");
        if (toFile) {
            try {
                Files.createDirectories(logFile.getParent());
            } catch (IOException ex) {
                logger.error("Could not create the log directory; file logging is off", ex);
                this.toFile = false;
            }
        }
    }

    public void applySettings(boolean debug, boolean toFile) {
        this.debug = debug;
        this.toFile = toFile;
    }

    /** Operator-facing. Always shown. */
    public void general(String message) {
        logger.info(message);
        append("INFO", message);
    }

    public void warn(String message) {
        logger.warn(message);
        append("WARN", message);
    }

    public void error(String message, Throwable error) {
        logger.error(message, error);
        append("ERROR", message + " - " + error);
    }

    /** Per-connection verdict trace. Console output only when debug is on; always written to file. */
    public void vpn(String message) {
        if (debug) {
            logger.info("[vpn] " + message);
        }
        append("VPN", message);
    }

    private void append(String level, String message) {
        if (!toFile) {
            return;
        }
        pending.add(LocalDateTime.now().format(STAMP) + " [" + level + "] " + message + System.lineSeparator());
        drain();
    }

    /**
     * Writes whatever has queued up, with a single writer at a time.
     *
     * <p>Lines that arrive while a drain is running are picked up by the same pass, so a burst costs
     * one file open rather than one per line.
     */
    private void drain() {
        if (!draining.compareAndSet(false, true)) {
            return;
        }
        AsyncExecutor.get().execute(() -> {
            try {
                StringBuilder batch = new StringBuilder(4096);
                String line;
                while ((line = pending.poll()) != null) {
                    batch.append(line);
                    if (batch.length() > 262_144) {
                        break; // bound the buffer; the rest goes out on the next pass
                    }
                }
                if (batch.length() > 0) {
                    Files.writeString(logFile, batch, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            } catch (IOException ex) {
                toFile = false;
                logger.error("Log file write failed; file logging is now off", ex);
            } finally {
                draining.set(false);
                if (!pending.isEmpty()) {
                    drain();
                }
            }
        });
    }
}
