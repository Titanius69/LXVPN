package com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.ddos;

import com.titanius.lxantivpn.bungee.Main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class botnetIPChecker {
    private final Main plugin;

    public botnetIPChecker(Main plugin) {
        this.plugin = plugin;
    }

    public int checkWithMalGithub3(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/sefinek/Malicious-IP-Addresses/refs/heads/main/lists/main.txt");
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


    public int checkWithBotnetGithub2(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/bitwire-it/ipblocklist/refs/heads/main/ip-list.txt");
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

    public int checkWithBotnetGithub1(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/duggytuxy/Intelligence_IPv4_Blocklist/refs/heads/main/agressive_ips_dst_fr_be_blocklist.txt");
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
