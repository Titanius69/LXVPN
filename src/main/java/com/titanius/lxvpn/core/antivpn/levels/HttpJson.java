package com.titanius.lxvpn.core.antivpn.levels;

import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * One place for the HTTP-and-JSON boilerplate every API-backed checker needs.
 *
 * <p>Written once rather than nine times because the important parts are easy to get subtly wrong in
 * one copy and not the others: both timeouts set, the connection always disconnected, and any
 * failure turning into a score of zero instead of an exception.
 *
 * <p>That last point is the rule this whole subsystem runs on. A reputation provider being down,
 * slow, rate limited or returning nonsense must never block a player. An anti-VPN plugin that
 * refuses legitimate logins because someone else's free API is having a bad afternoon does far more
 * damage than the VPN users it was installed to stop.
 */
final class HttpJson {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private HttpJson() {
    }

    /**
     * Fetches a URL, parses the body as JSON and maps it to a score.
     *
     * @param label used in the log line when something fails
     * @param scorer turns the parsed response into a score contribution
     */
    static CompletableFuture<Integer> score(String url, String label, LogManager log,
                                            Function<JSONObject, Integer> scorer) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("User-Agent", "LXVPN");
                connection.setRequestProperty("Accept", "application/json");

                int status = connection.getResponseCode();
                if (status != 200) {
                    // 429 in particular is worth naming: it means the free tier is exhausted and
                    // this source is contributing nothing until it resets.
                    log.vpn(label + " returned HTTP " + status
                            + (status == 429 ? " (rate limited)" : ""));
                    return 0;
                }

                StringBuilder body = new StringBuilder(512);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }

                Integer result = scorer.apply(new JSONObject(body.toString()));
                return result == null ? 0 : result;
            } catch (Exception ex) {
                log.vpn(label + " failed: " + ex.getMessage());
                return 0;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, AsyncExecutor.get());
    }

    /** True when a config value looks like a real key rather than the placeholder shipped by default. */
    static boolean usableKey(String key) {
        return key != null && !key.isBlank()
                && !key.toLowerCase(java.util.Locale.ROOT).startsWith("your_");
    }
}
