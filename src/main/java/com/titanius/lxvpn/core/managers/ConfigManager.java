package com.titanius.lxvpn.core.managers;

import com.titanius.lxvpn.core.platform.PlatformLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads {@code config.yml} with a shaded SnakeYAML rather than either proxy's own config API.
 *
 * <p>BungeeCord and Velocity ship different configuration stacks, and using either would tie the jar
 * to one platform. One parser means one config file, one set of defaults and one place where a
 * missing key falls back.
 *
 * <p>Every getter takes a default. A configuration file that has drifted behind the plugin should
 * lose one setting, not stop the plugin from starting.
 */
public class ConfigManager {

    private final Path file;
    private final PlatformLogger logger;
    private volatile Map<String, Object> root = Collections.emptyMap();

    public ConfigManager(Path dataFolder, PlatformLogger logger) {
        this.logger = logger;
        this.file = dataFolder.resolve("config.yml");
        saveDefaultIfMissing(dataFolder);
        reload();
    }

    private void saveDefaultIfMissing(Path dataFolder) {
        try {
            Files.createDirectories(dataFolder);
            if (Files.notExists(file)) {
                try (InputStream bundled = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (bundled == null) {
                        logger.warn("config.yml is missing from the jar; running on built-in defaults.");
                        return;
                    }
                    Files.copy(bundled, file, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ex) {
            logger.error("Could not write the default config.yml", ex);
        }
    }

    /**
     * Rereads the file.
     *
     * <p>Keeps the previous values if the new file does not parse. An operator who typos a config
     * during an attack should get an error message, not an anti-VPN plugin that reverts to defaults
     * and starts letting everything through.
     */
    @SuppressWarnings("unchecked")
    public boolean reload() {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            if (loaded instanceof Map) {
                root = (Map<String, Object>) loaded;
                return true;
            }
            logger.warn("config.yml did not parse into a map; keeping the previous configuration.");
            return false;
        } catch (Exception ex) {
            logger.error("Could not read config.yml; keeping the previous configuration", ex);
            return false;
        }
    }

    // ------------------------------------------------------------------ lookup

    @SuppressWarnings("unchecked")
    private Object raw(String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
        }
        return current;
    }

    public boolean getBoolean(String path, boolean fallback) {
        Object value = raw(path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return fallback;
    }

    public int getInt(String path, int fallback) {
        Object value = raw(path);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public String getString(String path, String fallback) {
        Object value = raw(path);
        return value == null ? fallback : String.valueOf(value);
    }

    public List<String> getStringList(String path) {
        Object value = raw(path);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item != null) {
                out.add(String.valueOf(item));
            }
        }
        return Collections.unmodifiableList(out);
    }

    public List<Integer> getIntList(String path) {
        Object value = raw(path);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Number number) {
                out.add(number.intValue());
            } else if (item != null) {
                try {
                    out.add(Integer.parseInt(String.valueOf(item).trim()));
                } catch (NumberFormatException ignored) {
                    // one bad entry should not invalidate the list
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** Country codes are compared uppercase; normalising here means nowhere else has to. */
    public List<String> getUpperStringList(String path) {
        List<String> source = getStringList(path);
        List<String> out = new ArrayList<>(source.size());
        for (String item : source) {
            out.add(item.trim().toUpperCase(Locale.ROOT));
        }
        return Collections.unmodifiableList(out);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(String path) {
        Object value = raw(path);
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    // ---------------------------------------------------------- typed accessors

    public boolean isAntiVpnEnabled() {
        return getBoolean("antivpn.enabled", true);
    }

    public List<Integer> getCheckLevels() {
        List<Integer> levels = getIntList("antivpn.checklevels");
        return levels.isEmpty() ? List.of(1, 2, 4) : levels;
    }

    /**
     * Whether the heavy remote blocklist tiers (levels 5 and 6) may run.
     *
     * <p>Named after what it gates rather than where the data lives: these are remote, cached
     * blocklists, not files inside the jar. LXVPN 1.0 ships no bundled IP databases at all - a list
     * frozen at build time is stale the week after release, and it was most of the jar size.
     */
    public boolean isDatabaseChecksEnabled() {
        return getBoolean("antivpn.database-checks", false);
    }

    public int getMinScore() {
        return Math.max(1, getInt("antivpn.min-score", 2));
    }

    public int getScoreCacheMinutes() {
        return Math.max(1, getInt("antivpn.cache.score-ttl-minutes", 10));
    }

    public int getListCacheMinutes() {
        return Math.max(1, getInt("antivpn.cache.list-refresh-minutes", 30));
    }

    public String getVpnApiKey() {
        return getString("antivpn.api-keys.vpnapi", "");
    }

    public String getIpRegistryKey() {
        return getString("antivpn.api-keys.ipregistry", "");
    }

    public String getProxyCheckKey() {
        return getString("antivpn.api-keys.proxycheck", "");
    }

    public boolean isAsnCheckEnabled() {
        return getBoolean("antivpn.asn.enabled", true);
    }

    public int getAsnBonusScore() {
        return getInt("antivpn.asn.bonus-score", 3);
    }

    public boolean isGeoEnabled() {
        return getBoolean("geo.enabled", false);
    }

    public String getGeoMode() {
        return getString("geo.mode", "blacklist").trim().toLowerCase(Locale.ROOT);
    }

    public List<String> getGeoCountries() {
        return getUpperStringList("geo.countries");
    }

    public List<String> getBypassList() {
        return getStringList("bypass");
    }

    public boolean isIpRestrictEnabled() {
        return getBoolean("ip-restrict.enabled", false);
    }

    public boolean isWebhookEnabled() {
        return getBoolean("webhook.enabled", false);
    }

    public String getWebhookUrl() {
        return getString("webhook.url", "");
    }

    public int getWebhookCooldownSeconds() {
        return Math.max(0, getInt("webhook.cooldown-seconds", 60));
    }

    public boolean isUpdateCheckEnabled() {
        return getBoolean("update-checker.enabled", true);
    }

    public int getUpdateResourceId() {
        return getInt("update-checker.resource-id", 0);
    }

    public boolean isDebugLogging() {
        return getBoolean("logging.debug", false);
    }

    public boolean isLogToFile() {
        return getBoolean("logging.file", true);
    }

    /** How a denied connection is handled when the verdict arrives after login. */
    public boolean isFailOpen() {
        return getBoolean("antivpn.fail-open", true);
    }

    public int getLookupTimeoutSeconds() {
        return Math.max(1, getInt("antivpn.lookup-timeout-seconds", 6));
    }
}
