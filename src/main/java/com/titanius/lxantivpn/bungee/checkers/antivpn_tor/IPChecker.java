// Source code is decompiled from a .class file using FernFlower decompiler.
package com.titanius.lxantivpn.bungee.checkers.antivpn_tor;

import com.titanius.lxantivpn.bungee.Main;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.ddos.botnetIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.high.highIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.low.lowIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.mid.midIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.normal.normalIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.proxy.proxyIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.superhigh.superhighIPChecker;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.json.JSONArray;
import org.json.JSONTokener;

public class IPChecker {
    private final Main plugin;
    private final Configuration config;
    private final File blacklistFile;
    private lowIPChecker lowIPChecker;
    private normalIPChecker normalIPChecker;
    private midIPChecker midIPChecker;
    private highIPChecker highIPChecker;
    private superhighIPChecker superhighIPChecker;
    private proxyIPChecker proxyIPChecker;
    private botnetIPChecker botnetIPChecker;
    private final List<Integer> checkLevels;
    private final boolean databaseChecks;

    public IPChecker(Main plugin) throws IOException {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        checkLevels = config.getIntList("antivpn_tor.checklevels");
        databaseChecks = config.getBoolean("antivpn_tor.database_checks", true);
        if (checkLevels.contains(1)) {
            lowIPChecker = new lowIPChecker(plugin);
        }

        if (checkLevels.contains(2)) {
            normalIPChecker = new normalIPChecker(plugin);
        }

        if (checkLevels.contains(3)) {
            midIPChecker = new midIPChecker(plugin);
        }

        if (checkLevels.contains(4)) {
            highIPChecker = new highIPChecker(plugin);
        }

        if (checkLevels.contains(5)) {
            superhighIPChecker = new superhighIPChecker(plugin);
        }

        if (checkLevels.contains(6)) {
            proxyIPChecker = new proxyIPChecker(plugin);
        }

        if (checkLevels.contains(7)) {
            botnetIPChecker = new botnetIPChecker(plugin);
        }

        this.blacklistFile = new File(plugin.getDataFolder(), "blacklist.json");
        if (!blacklistFile.exists()) {
            blacklistFile.createNewFile();
            try (FileWriter writer = new FileWriter(blacklistFile)) {
                writer.write("[]");
            }
        }

    }

    public boolean isVpnOrProxy(String ipAddress, String playerName) {
        int proxyScore = 0;
        plugin.logToFile("Checking IP address: " + ipAddress);
        if (checkLevels.contains(1) && lowIPChecker != null) {
            proxyScore += lowIPChecker.checkWithProxycheck(ipAddress);
            proxyScore += lowIPChecker.checkWithFreeIpAPI(ipAddress);
            proxyScore += lowIPChecker.checkWithTorExitNodeList(ipAddress);
        }

        if (checkLevels.contains(2) && normalIPChecker != null) {
            proxyScore += normalIPChecker.checkWithNebLinkTor(ipAddress);
            if (databaseChecks) {
                proxyScore += normalIPChecker.checkWithLocalDatabase1(ipAddress);
                proxyScore += normalIPChecker.checkWithLocalDatabase2(ipAddress);
            }
        }

        if (checkLevels.contains(3) && midIPChecker != null) {
            proxyScore += midIPChecker.checkWithGithubIpBlocklistIpList(ipAddress);
            proxyScore += midIPChecker.checkWithGithubStanPurmIpsumLevel4(ipAddress);
            proxyScore += midIPChecker.checkWithGithubStanPurmIpsumLevel3(ipAddress);
            proxyScore += midIPChecker.checkWithGithubStanPurmIpsumLevel8(ipAddress);
            proxyScore += midIPChecker.checkGithubNordVPN(ipAddress);
        }

        if (checkLevels.contains(4) && highIPChecker != null) {
            if (databaseChecks) {
                proxyScore += highIPChecker.checkWithCinnScoreCiBadGuys(ipAddress);
            }

            proxyScore += highIPChecker.checkWithVPNAPI(ipAddress);
            proxyScore += highIPChecker.checkWithIpRegistry(ipAddress);
            proxyScore += highIPChecker.checkWithIpApi(ipAddress);
        }

        if (this.checkLevels.contains(5) && superhighIPChecker != null && databaseChecks) {
            proxyScore += superhighIPChecker.checkWithGithubStanPurmIpsumLevel1(ipAddress);
            proxyScore += superhighIPChecker.checkWithGithubStanPurmIpsumLevel2(ipAddress);
            proxyScore += superhighIPChecker.checkWithGithubStanPurmIpsumLevel5(ipAddress);
            proxyScore += superhighIPChecker.checkWithGithubStanPurmIpsumLevel6(ipAddress);
            proxyScore += superhighIPChecker.checkWithGithubStanPurmIpsumLevel7(ipAddress);
            proxyScore += superhighIPChecker.checkWithBlocklist(ipAddress);
        }

        if (checkLevels.contains(6) && proxyIPChecker != null && databaseChecks) {
            proxyScore += proxyIPChecker.checkWithGithubProxyActionsUser(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyClarketem(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyKangProxy(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyTheSpeedXSock4(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyTheSpeedXSock5(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyTheSpeedXhttp(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyTorwalds(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyZaeem20http(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyZaeem20https(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyZaeem20sock4(ipAddress);
            proxyScore += proxyIPChecker.checkWithGithubProxyZaeem20sock5(ipAddress);
        }

        if (checkLevels.contains(7) && proxyIPChecker != null && databaseChecks) {
            proxyScore += botnetIPChecker.checkWithMalGithub3(ipAddress);
            proxyScore += botnetIPChecker.checkWithBotnetGithub1(ipAddress);
            proxyScore += botnetIPChecker.checkWithBotnetGithub2(ipAddress);
        }

        plugin.logToFile(playerName + " (" + ipAddress + ") connection tried with a score of " + proxyScore);
        if (proxyScore >= 1) {
            addToBlacklist(ipAddress);
            return true;
        }

        return proxyScore >= 1;


    }

    private void addToBlacklist(String ipAddress) {
        try {
            Set<String> blacklistedIPs = new HashSet<>();
            try (FileReader reader = new FileReader(blacklistFile)) {
                JSONArray blacklistArray = new JSONArray(new JSONTokener(reader));
                for (int i = 0; i < blacklistArray.length(); i++) {
                    blacklistedIPs.add(blacklistArray.getString(i));
                }
            }

            if (!blacklistedIPs.contains(ipAddress)) {
                blacklistedIPs.add(ipAddress);
                JSONArray updatedBlacklist = new JSONArray(blacklistedIPs);
                try (FileWriter writer = new FileWriter(blacklistFile)) {
                    writer.write(updatedBlacklist.toString(2)); // Save with indentation
                }
                plugin.logToFile("Added IP " + ipAddress + " to blacklist.");
            }
        } catch (IOException e) {
            plugin.logToFile("Error updating blacklist for IP: " + ipAddress + ". Exception: " + e.getMessage());
        }
    }
}
