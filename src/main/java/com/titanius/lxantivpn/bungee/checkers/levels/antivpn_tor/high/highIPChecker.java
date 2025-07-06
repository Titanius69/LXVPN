//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.high;

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
import org.json.JSONObject;

public class highIPChecker {
    private final Main plugin;
    private final String IpRegistryKey;
    private final Configuration config;
    private final String VPNAPIkey;

    public highIPChecker(Main plugin) throws IOException {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        VPNAPIkey = config.getString("antivpn_tor.vpn-api-api-key");
        IpRegistryKey = config.getString("antivpn_tor.ipregistry-api-key");
    }

    public int checkWithIpApi(String ipAddress) {
        try {
            URL url = new URL("https://api.ipapi.is/?q=" + ipAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();
            JSONObject jsonResponse = new JSONObject(content.toString());
            Object isVpn = jsonResponse.opt("is_vpn");
            Object isTor = jsonResponse.opt("is_tor");
            Object isProxy = jsonResponse.opt("is_proxy");
            return (isVpn == null || isVpn.equals(false)) && (isTor == null || isTor.equals(false)) && (isProxy == null || isProxy.equals(false)) ? 0 : 1;
        } catch (Exception var11) {
            Exception e = var11;
            plugin.logToFile("Error checking IP with IpApi: " + e.getMessage());
            return 0;
        }
    }


    public long ipToLong(String ipAddress) {
        if (ipAddress.contains(":")) {
            return 0L;
        } else {
            String[] octets = ipAddress.split("\\.");
            long result = 0L;

            for (int i = 0; i < 4; ++i) {
                result |= Long.parseLong(octets[i]) << 24 - 8 * i;
            }

            return result;
        }
    }

    /*public int checkWithNebLinkIpBlockListClean(String ipAddress) {
        try {
            URL url = new URL("https://www.neblink.net/blocklist/IP-Blocklist-clean.txt");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String cleanedLine;
            do {
                String inputLine;
                if ((inputLine = in.readLine()) == null) {
                    in.close();
                    return 0;
                }

                cleanedLine = inputLine.split("#")[0].trim();
            } while (!cleanedLine.equals(ipAddress));

            in.close();
            return 1;
        } catch (Exception var7) {
            Exception e = var7;
            plugin.getLogger().log(Level.SEVERE, "Error checking IP with neblink list - IpBlockListClean: " + e.getMessage());
            return 0;
        }
    }*/



    public int checkWithIpRegistry(String ipAddress) {
        HttpURLConnection connection = null;

        try {
            try {
                URL url = new URL("https://api.ipregistry.co/" + ipAddress + "?key=" + IpRegistryKey);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.logToFile("Failed to query ipregistry: Response Code " + responseCode + " URL: " + url);
                    return 0;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                label191: {
                    int var13;
                    try {
                        StringBuilder content = new StringBuilder();

                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }

                        JSONObject responseJson = new JSONObject(content.toString());
                        if (!responseJson.has("security")) {
                            plugin.logToFile("Security object not found in the response JSON for IP: " + ipAddress);
                            break label191;
                        }

                        JSONObject security = responseJson.getJSONObject("security");
                        boolean isVpn = security.optBoolean("is_vpn", false);
                        boolean isProxy = security.optBoolean("is_proxy", false);
                        boolean isTor = security.optBoolean("is_tor", false);
                        var13 = !isVpn && !isProxy && !isTor ? 0 : 1;
                    } catch (Throwable var20) {
                        try {
                            in.close();
                        } catch (Throwable var19) {
                            var20.addSuppressed(var19);
                        }

                        throw var20;
                    }

                    in.close();
                    return var13;
                }

                in.close();
            } catch (Exception var21) {
                Exception e = var21;
                plugin.logToFile("Error querying ipregistry for IP: " + ipAddress + ". Exception: " + e.getMessage());
            }

            return 0;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

        }
    }

    public int checkWithVPNAPI(String ipAddress) {
        HttpURLConnection connection = null;

        try {
            try {
                URL url = new URL("https://vpnapi.io/api/" + ipAddress + "?key=" + VPNAPIkey);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    plugin.logToFile("Failed to query vpnapi.io: Response Code " + responseCode);
                    return 0;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                label191: {
                    int var13;
                    try {
                        StringBuilder content = new StringBuilder();

                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }

                        JSONObject responseJson = new JSONObject(content.toString());
                        if (!responseJson.has("security")) {
                            plugin.logToFile("Security object not found in the response JSON for IP: " + ipAddress + " URL:" + url);
                            break label191;
                        }

                        JSONObject security = responseJson.getJSONObject("security");
                        boolean isVpn = security.optBoolean("vpn", false);
                        boolean isProxy = security.optBoolean("proxy", false);
                        boolean isTor = security.optBoolean("tor", false);
                        var13 = !isVpn && !isProxy && !isTor ? 0 : 1;
                    } catch (Throwable var20) {
                        try {
                            in.close();
                        } catch (Throwable var19) {
                            var20.addSuppressed(var19);
                        }

                        throw var20;
                    }

                    in.close();
                    return var13;
                }

                in.close();
            } catch (Exception var21) {
                plugin.logToFile("Error querying vpnapi.io for IP: " + ipAddress);
            }

            return 0;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

        }
    }

    public int checkWithCinnScoreCiBadGuys(String ipAddress) {
        try {
            URL url = new URL("https://cinsscore.com/list/ci-badguys.txt");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            do {
                if ((inputLine = in.readLine()) == null) {
                    in.close();
                    return 0;
                }
            } while (!inputLine.equals(ipAddress));

            in.close();
            return 1;
        } catch (Exception var6) {
            Exception e = var6;
            plugin.getLogger().log(Level.SEVERE, "Error checking IP with Github Stanpurm - Ipsum Level 8 repo list: " + e.getMessage());
            return 0;
        }
    }
}
