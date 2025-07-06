package com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.mid;

import com.titanius.lxantivpn.bungee.Main;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class midIPChecker {
    private final Main plugin;

    public midIPChecker(Main plugin) {
        this.plugin = plugin;
    }

    public int checkWithGithubIpBlocklistIpList(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/bitwire-it/ipblocklist/main/ip-list.txt");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals(ipAddress)) {
                    in.close();
                    return 1;
                }
            }
            in.close();
            return 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking IP with Github IpBlocklist - IpList repo list: " + e.getMessage());
            return 0;
        }
    }

    public int checkWithGithubStanPurmIpsumLevel3(String ipAddress) {
        return checkWithGithubList("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/3.txt", ipAddress, "Github Stanpurm - Ipsum Level 3");
    }

    public int checkWithGithubStanPurmIpsumLevel4(String ipAddress) {
        return checkWithGithubList("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/4.txt", ipAddress, "Github Stanpurm - Ipsum Level 4");
    }

    public int checkWithGithubStanPurmIpsumLevel8(String ipAddress) {
        return checkWithGithubList("https://raw.githubusercontent.com/stamparm/ipsum/master/levels/8.txt", ipAddress, "Github Stanpurm - Ipsum Level 8");
    }

    public int checkGithubNordVPN(String ipAddress) {
        return checkWithGithubList("https://gist.githubusercontent.com/triggex/c6bc554410a84ea1b3ef1c19c5a92d49/raw/1d5a60401f631356d156c21b32471d15dff2e0e1/NordVPN-Server-IP-List-2020.txt", ipAddress, "Github NordVPN");
    }

    private int checkWithGithubList(String urlString, String ipAddress, String sourceName) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equals(ipAddress)) {
                    in.close();
                    return 1;
                }
            }
            in.close();
            return 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking IP with " + sourceName + " repo list: " + e.getMessage());
            return 0;
        }
    }
}
