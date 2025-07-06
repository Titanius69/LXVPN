package com.titanius.lxantivpn.bungee.checkers.update;

import com.titanius.lxantivpn.bungee.Main;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final Main plugin;
    private final int resourceId;

    public UpdateChecker(Main plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkForUpdates() {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
                int responseCode = connection.getResponseCode();
                if (responseCode == 403) {
                    plugin.logToFile("Access denied (403) when trying to check for updates. The request may have been blocked.");
                    return;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = in.readLine();
                in.close();
                String currentVersion = plugin.getDescription().getVersion();
                if (currentVersion.equalsIgnoreCase(latestVersion)) {
                    plugin.logToFile("");
                    plugin.logToFile("");
                    plugin.logToFile("\u00a7e\u2588\u2591\u2591\u2003\u2580\u2584\u2580\u2003\u00a7f\u2588\u2591\u2588\u2003\u2588\u2580\u2588\u2003\u2588\u2584\u2591\u2588");
                    plugin.logToFile("\u00a7e\u2588\u2584\u2584\u2003\u2588\u2591\u2588\u2003\u00a7f\u2580\u2584\u2580\u2003\u2588\u2580\u2580\u2003\u2588\u2591\u2580\u2588");
                    plugin.logToFile("");
                    plugin.logToFile("\u00a7eRate and review us on spigot!");
                    plugin.logToFile("");
                    plugin.logToFile("\u00a7aNo updates found. You are using the latest version.");
                    plugin.logToFile("");
                    plugin.logToFile("\u00a76Link: \u00a7ehttps://www.spigotmc.org/resources/lxvpn.118999/");
                    plugin.logToFile("");
                } else {
                    plugin.logToFile("");
                    plugin.logToFile("");
                    plugin.logToFile("\u00a7e\u2588\u2591\u2591\u2003\u2580\u2584\u2580\u2003\u00a7f\u2588\u2591\u2588\u2003\u2588\u2580\u2588\u2003\u2588\u2584\u2591\u2588");
                    plugin.logToFile("\u00a7e\u2588\u2584\u2584\u2003\u2588\u2591\u2588\u2003\u00a7f\u2580\u2584\u2580\u2003\u2588\u2580\u2580\u2003\u2588\u2591\u2580\u2588");
                    plugin.logToFile("");
                    plugin.logToFile("");
                    plugin.logToFile("\u00a7aNew update available! Current version: \u00a7f" + currentVersion + "\u00a7a, Latest version: \u00a7f" + latestVersion);
                    plugin.logToFile("");
                    plugin.logToFile("\u00a7eRate and review us on spigot!");
                    plugin.logToFile("");
                    plugin.logToFile("\u00a76Link: \u00a7ehttps://www.spigotmc.org/resources/lxvpn.118999/");
                    plugin.logToFile("");
                }
            } catch (Exception e) {
                plugin.logToFile("");
                plugin.logToFile("");
                plugin.logToFile("\u00a7e\u2588\u2591\u2591\u2003\u2580\u2584\u2580\u2003\u00a7f\u2588\u2591\u2588\u2003\u2588\u2580\u2588\u2003\u2588\u2584\u2591\u2588");
                plugin.logToFile("\u00a7e\u2588\u2584\u2584\u2003\u2588\u2591\u2588\u2003\u00a7f\u2580\u2584\u2580\u2003\u2588\u2580\u2580\u2003\u2588\u2591\u2580\u2588");
                plugin.logToFile("");
                plugin.logToFile("\u00a7eRate and review us on spigot!");
                plugin.logToFile("");
                plugin.logToFile("\u00a7cFailed to check for updates on SpigotMC.");
                plugin.logToFile("");
                plugin.logToFile("\u00a76Link: \u00a7ehttps://www.spigotmc.org/resources/lxvpn.118999/");
                plugin.logToFile("");
            }
        });
    }
}
