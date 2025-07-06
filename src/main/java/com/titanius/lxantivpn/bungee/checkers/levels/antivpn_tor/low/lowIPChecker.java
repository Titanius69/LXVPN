package com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.low;

import com.titanius.lxantivpn.bungee.Main;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class lowIPChecker {
    private final Main plugin;

    public lowIPChecker(Main plugin) {
        this.plugin = plugin;
    }

    public int checkWithProxycheck(String ipAddress) {
        try {
            URL url = new URL("https://proxycheck.io/v2/" + ipAddress + "&vpn=1&risk=1");
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
            JSONObject ipData = jsonResponse.optJSONObject(ipAddress);
            if (ipData != null) {
                String proxy = ipData.optString("proxy", "no");
                String vpn = ipData.optString("type", "");
                if (proxy.equals("yes") || vpn.equals("VPN")) {
                    return 1;
                }
            }

            return 0;
        } catch (Exception var11) {
            plugin.logToFile("Error checking IP with Proxycheck: " + var11.getMessage());
            return 0;
        }
    }

    public int checkWithFreeIpAPI(String ipAddress) {
        try {
            URL url = new URL("https://freeipapi.com/api/json/" + ipAddress);
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
            return jsonResponse.optString("isProxy", "false").equals("true") ? 1 : 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with FreeIpAPI: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithTorExitNodeList(String ipAddress) {
        try {
            URL url = new URL("https://check.torproject.org/torbulkexitlist");
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
            plugin.logToFile("Error checking IP with Tor exit node list: " + var6.getMessage());
            return 0;
        }
    }
}
