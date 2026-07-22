package com.titanius.lxvpn.core.antivpn;

import com.titanius.lxvpn.core.managers.LogManager;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

/**
 * Fetches a GeoLite2 database from public mirrors.
 *
 * <p>Shared by the ASN and country lookups, which need the same download-once-then-work-offline
 * behaviour and differ only in which file they want. Doing it once here also means one place to fix
 * when a mirror disappears, which they periodically do.
 *
 * <p>No MaxMind account or licence key is involved: these are public redistributions. That matters
 * for a free plugin, because a setup step that requires registering with a third party is a setup
 * step most people never finish.
 */
public final class MmdbAccess {

    /** Below this, whatever arrived is an error page rather than a database. */
    private static final long MIN_VALID_SIZE_BYTES = 1024L;

    private MmdbAccess() {
    }

    /** True when the file is missing or too small to be a real database. */
    public static boolean tooSmall(Path path) {
        try {
            return Files.size(path) < MIN_VALID_SIZE_BYTES;
        } catch (Exception ex) {
            return true;
        }
    }

    /**
     * Downloads to {@code target}, trying each mirror in turn.
     *
     * @return true when a plausible database is in place afterwards
     */
    public static boolean download(List<String> mirrors, Path target, LogManager log, String label) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        for (String mirror : mirrors) {
            Path temporary = target.resolveSibling(target.getFileName() + ".part");
            try {
                Files.createDirectories(target.getParent());
                HttpRequest request = HttpRequest.newBuilder(URI.create(mirror))
                        .timeout(Duration.ofMinutes(2))
                        .header("User-Agent", "LXVPN")
                        .GET()
                        .build();

                HttpResponse<InputStream> response =
                        http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    log.vpn(label + " mirror returned HTTP " + response.statusCode() + ": " + mirror);
                    continue;
                }

                try (InputStream body = response.body()) {
                    Files.copy(body, temporary, StandardCopyOption.REPLACE_EXISTING);
                }

                if (Files.size(temporary) < MIN_VALID_SIZE_BYTES) {
                    log.vpn(label + " mirror returned a file too small to be a database: " + mirror);
                    Files.deleteIfExists(temporary);
                    continue;
                }

                // Move into place only once the download is complete and plausible, so a failure
                // halfway through leaves the previous working database untouched.
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                log.general(label + " database downloaded.");
                return true;
            } catch (Exception ex) {
                log.vpn(label + " download failed from " + mirror + ": " + ex.getMessage());
                try {
                    Files.deleteIfExists(temporary);
                } catch (Exception ignored) {
                    // nothing useful to do
                }
            }
        }

        log.warn(label + " database could not be downloaded from any mirror. "
                + "That check is disabled until the next restart or reload.");
        return false;
    }
}
