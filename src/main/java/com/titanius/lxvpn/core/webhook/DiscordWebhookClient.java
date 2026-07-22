package com.titanius.lxvpn.core.webhook;

import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Posts block notifications to a Discord webhook.
 *
 * <p>Rate limited by a plain cooldown rather than a queue. During a botnet flood the interesting
 * information is "we are being hit", not each of the eleven thousand addresses involved - and
 * Discord would rate limit the plugin long before it finished trying.
 */
public class DiscordWebhookClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(AsyncExecutor.get())
            .build();

    private final LogManager log;
    private final AtomicLong lastSent = new AtomicLong();

    private volatile String url = "";
    private volatile boolean enabled;
    private volatile long cooldownMillis = 60_000L;

    public DiscordWebhookClient(LogManager log) {
        this.log = log;
    }

    public void configure(boolean enabled, String url, int cooldownSeconds) {
        this.enabled = enabled;
        this.url = url == null ? "" : url.trim();
        this.cooldownMillis = Math.max(0L, cooldownSeconds) * 1000L;
    }

    /** Reports a blocked connection, subject to the cooldown. */
    public void blocked(String username, String ip, String reason, int score) {
        if (!enabled || url.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long previous = lastSent.get();
        if (now - previous < cooldownMillis || !lastSent.compareAndSet(previous, now)) {
            return;
        }

        String description = "**Player:** " + escape(username)
                + "\\n**Address:** " + escape(mask(ip))
                + "\\n**Reason:** " + escape(reason)
                + "\\n**Score:** " + score;

        String payload = "{\"username\":\"LXVPN\",\"embeds\":[{"
                + "\"title\":\"Connection blocked\","
                + "\"description\":\"" + description + "\","
                + "\"color\":10559797}]}";

        post(payload);
    }

    /**
     * Masks the final octet.
     *
     * <p>A Discord channel is one screenshot away from being public, and a full address is personal
     * data in most of Europe. The masked form is still enough to recognise a repeat offender.
     */
    private static String mask(String ip) {
        if (ip == null) {
            return "unknown";
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".x";
        }
        int lastColon = ip.lastIndexOf(':');
        return lastColon > 0 ? ip.substring(0, lastColon) + ":x" : ip;
    }

    private void post(String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "LXVPN")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 300) {
                            log.warn("Discord webhook returned HTTP " + response.statusCode());
                        }
                    })
                    .exceptionally(error -> {
                        log.vpn("Discord webhook failed: " + error.getMessage());
                        return null;
                    });
        } catch (IllegalArgumentException ex) {
            log.warn("The configured webhook URL is not a valid URL; webhook notifications are off.");
            enabled = false;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n', '\r', '\t' -> out.append(' ');
                default -> {
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
