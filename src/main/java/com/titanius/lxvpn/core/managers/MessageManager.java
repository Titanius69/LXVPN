package com.titanius.lxvpn.core.managers;

import com.titanius.lxvpn.core.platform.PlatformLogger;
import com.titanius.lxvpn.core.util.Colors;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code messages.yml} and hands out colourised, placeholder-filled strings.
 *
 * <p>The core produces strings, never platform text components. The Bungee and Velocity layers turn
 * them into {@code TextComponent} and Adventure {@code Component} respectively, which is the only
 * place the two differ - and the reason there is one messages file rather than two.
 */
public class MessageManager {

    private static final Map<String, String> FALLBACKS = new HashMap<>();

    static {
        FALLBACKS.put("prefix", "&8[&bLX&3VPN&8] &r");
        FALLBACKS.put("vpn-blocked", "&cConnections through a VPN or proxy are not allowed on this network.");
        FALLBACKS.put("country-blocked", "&cConnections from your country are not allowed on this network.");
        FALLBACKS.put("ip-restricted", "&cThis account is locked to a different IP address.");
        FALLBACKS.put("no-permission", "&cYou do not have permission to use that.");
        FALLBACKS.put("reload-success", "&aConfiguration reloaded.");
        FALLBACKS.put("reload-failed", "&cReload failed. The previous configuration is still active; see the console.");
        FALLBACKS.put("usage", "&7Usage: &f/lxvpn <check|reload|whitelist|blacklist|iprestrict|stats>");
    }

    private final Path file;
    private final PlatformLogger logger;
    private volatile Map<String, String> messages = Collections.emptyMap();

    public MessageManager(Path dataFolder, PlatformLogger logger) {
        this.logger = logger;
        this.file = dataFolder.resolve("messages.yml");
        saveDefaultIfMissing(dataFolder);
        reload();
    }

    private void saveDefaultIfMissing(Path dataFolder) {
        try {
            Files.createDirectories(dataFolder);
            if (Files.notExists(file)) {
                try (InputStream bundled = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
                    if (bundled != null) {
                        Files.copy(bundled, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("Could not write the default messages.yml", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean reload() {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            if (!(loaded instanceof Map)) {
                return false;
            }
            Map<String, String> parsed = new HashMap<>();
            ((Map<String, Object>) loaded).forEach((key, value) -> {
                if (value != null) {
                    parsed.put(key, String.valueOf(value));
                }
            });
            messages = Collections.unmodifiableMap(parsed);
            return true;
        } catch (Exception ex) {
            logger.error("Could not read messages.yml; using built-in text", ex);
            return false;
        }
    }

    /** The raw configured value with colours applied, or the built-in fallback. */
    public String get(String key) {
        String value = messages.get(key);
        if (value == null) {
            value = FALLBACKS.getOrDefault(key, key);
        }
        return Colors.colorize(value);
    }

    /** As {@link #get}, prefixed. Used for command output rather than kick screens. */
    public String prefixed(String key) {
        return get("prefix") + get(key);
    }

    /**
     * @param replacements alternating placeholder and value, e.g. {@code "%ip%", "1.2.3.4"}
     */
    public String get(String key, String... replacements) {
        String value = get(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace(replacements[i], replacements[i + 1]);
        }
        return value;
    }

    public String prefixed(String key, String... replacements) {
        return get("prefix") + get(key, replacements);
    }
}
