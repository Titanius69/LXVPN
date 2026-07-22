package com.titanius.lxvpn.core.update;

import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks SpigotMC for a newer release.
 *
 * <p>Asynchronous, never blocks startup, and quiet about failures: a proxy that cannot reach
 * spigotmc.org has bigger problems than this plugin's version, and complaining about it on every
 * boot trains people to ignore the console.
 */
public class UpdateChecker {

    private static final String ENDPOINT = "https://api.spigotmc.org/legacy/update.php?resource=";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .executor(AsyncExecutor.get())
            .build();

    private final LogManager log;
    private final String currentVersion;
    private final String platform;
    private final int resourceId;
    private final AtomicReference<String> newest = new AtomicReference<>();
    private final AtomicBoolean announcedUpToDate = new AtomicBoolean();

    public UpdateChecker(LogManager log, String currentVersion, String platform, int resourceId) {
        this.log = log;
        this.currentVersion = currentVersion;
        this.platform = platform;
        this.resourceId = resourceId;
    }

    public String newestVersion() {
        return newest.get();
    }

    public boolean updateAvailable() {
        String remote = newest.get();
        return remote != null && isNewer(remote, currentVersion);
    }

    public CompletableFuture<Void> check() {
        if (resourceId <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + resourceId))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "LXVPN/" + currentVersion + " (" + platform + ")")
                .GET()
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        return;
                    }
                    String remote = response.body().trim();
                    if (remote.isEmpty() || remote.length() > 32) {
                        return;
                    }
                    newest.set(remote);
                    report(remote);
                })
                .exceptionally(error -> null);
    }

    /**
     * Names both versions in both cases.
     *
     * <p>The up-to-date line is printed once. An available update is worth repeating because it is a
     * nudge; confirmation that nothing changed, repeated every six hours, is log noise.
     */
    private void report(String remote) {
        if (isNewer(remote, currentVersion)) {
            log.general("LXVPN: running version " + currentVersion + ", newest version " + remote
                    + " is available. Download: https://www.spigotmc.org/resources/" + resourceId + "/");
        } else if (announcedUpToDate.compareAndSet(false, true)) {
            log.general("LXVPN: running version " + currentVersion + ", newest version " + remote
                    + ". You are up to date.");
        }
    }

    static boolean isNewer(String remote, String local) {
        int[] a = parse(remote);
        int[] b = parse(local);
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int left = i < a.length ? a[i] : 0;
            int right = i < b.length ? b[i] : 0;
            if (left != right) {
                return left > right;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        String cleaned = version.split("[-+]", 2)[0].trim();
        String[] parts = cleaned.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ex) {
                out[i] = 0;
            }
        }
        return out;
    }
}
