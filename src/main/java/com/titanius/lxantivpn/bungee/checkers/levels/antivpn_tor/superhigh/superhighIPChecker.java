package com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.superhigh;

import com.titanius.lxantivpn.bungee.Main;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class superhighIPChecker {
    private final Main plugin;
    private final String IpRegistryKey;
    private final Configuration config;
    private final String VPNAPIkey;

    public superhighIPChecker(Main plugin) throws IOException {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        VPNAPIkey = config.getString("antivpn_tor.vpn-api-api-key");
        IpRegistryKey = config.getString("antivpn_tor.ipregistry-api-key");
    }

    public int checkWithGithubStanPurmIpsumLevel5(String ipAddress) {
        return checkIpFromUrl("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/5.txt", ipAddress, "Github Stanpurm - Ipsum Level 5");
    }

    public int checkWithBlocklist(String ipAddress) {
        return checkIpFromUrl("https://www.blocklist.de/downloads/export-ips_all.txt", ipAddress, "Blocklist");
    }

    public int checkWithGithubStanPurmIpsumLevel1(String ipAddress) {
        return checkIpFromUrl("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/1.txt", ipAddress, "Github Stanpurm - Ipsum Level 1");
    }

    public int checkWithGithubStanPurmIpsumLevel6(String ipAddress) {
        return checkIpFromUrl("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/6.txt", ipAddress, "Github Stanpurm - Ipsum Level 6");
    }

    public int checkWithGithubStanPurmIpsumLevel2(String ipAddress) {
        return checkIpFromUrl("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/2.txt", ipAddress, "Github Stanpurm - Ipsum Level 2");
    }

    public int checkWithGithubStanPurmIpsumLevel7(String ipAddress) {
        return checkIpFromUrl("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/7.txt", ipAddress, "Github Stanpurm - Ipsum Level 7");
    }

    private int checkIpFromUrl(String urlString, String ipAddress, String sourceName) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals(ipAddress)) {
                        return 1;
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking IP with " + sourceName + " repo list: " + e.getMessage());
            return 0;
        }
    }
}
