// Source code is decompiled from a .class file using FernFlower decompiler.
package com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.normal;

import com.titanius.lxantivpn.bungee.Main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class normalIPChecker {
    private final Main plugin;

    public normalIPChecker(Main plugin) {
        this.plugin = plugin;
    }

    public int checkWithNebLinkTor(String ipAddress) {
        try {
            URL url = new URL("https://www.neblink.net/blocklist/TorExitNodes.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
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
            } while(!cleanedLine.equals(ipAddress));

            in.close();
            return 1;
        } catch (Exception var7) {
            plugin.getLogger().log(Level.SEVERE, "Error checking IP with neblink list - Tor: " + var7.getMessage());
            return 0;
        }
    }

    public int checkWithLocalDatabase1(String ipAddress) {
        File databaseFile = new File(plugin.getDataFolder(), "data/anonymusprivacydatabase.json");
        if (!databaseFile.exists()) {
            plugin.logToFile("Cannot find database file: " + databaseFile.getAbsolutePath());
            return 0;
        } else {
            try {
                FileReader fileReader = new FileReader(databaseFile);

                byte var17;
                label80: {
                    byte var11;
                    try {
                        BufferedReader bufferedReader;
                        label87: {
                            bufferedReader = new BufferedReader(fileReader);

                            try {
                                JSONTokener tokener = new JSONTokener(bufferedReader);
                                JSONArray jsonArray = new JSONArray(tokener);
                                int i = 0;

                                while(true) {
                                    if (i >= jsonArray.length()) {
                                        var17 = 0;
                                        break;
                                    }

                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    String startIp = jsonObject.getString("start_ip");
                                    String endIp = jsonObject.getString("end_ip");
                                    if (isIpInRange(ipAddress, startIp, endIp) && (jsonObject.optBoolean("vpn", false) || jsonObject.optBoolean("proxy", false) || jsonObject.optBoolean("tor", false) || jsonObject.optBoolean("hosting", false) || jsonObject.optBoolean("relay", false))) {
                                        var11 = 1;
                                        break label87;
                                    }

                                    ++i;
                                }
                            } catch (Throwable var14) {
                                try {
                                    bufferedReader.close();
                                } catch (Throwable var13) {
                                    var14.addSuppressed(var13);
                                }

                                throw var14;
                            }

                            bufferedReader.close();
                            break label80;
                        }

                        bufferedReader.close();
                    } catch (Throwable var15) {
                        try {
                            fileReader.close();
                        } catch (Throwable var12) {
                            var15.addSuppressed(var12);
                        }

                        throw var15;
                    }

                    fileReader.close();
                    return var11;
                }

                fileReader.close();
                return var17;
            } catch (Exception var16) {
                plugin.logToFile("An error occurred when checking the IP against the local database: " + var16.getMessage());
                return 0;
            }
        }
    }

    public int checkWithLocalDatabase2(String ipAddress) {
        File databaseFile = new File(plugin.getDataFolder(), "data/ipdb4.json");
        if (!databaseFile.exists()) {
            plugin.logToFile("The database file cannot be found: " + databaseFile.getAbsolutePath());
            return 0;
        } else {
            try {
                FileReader fileReader = new FileReader(databaseFile);

                byte var17;
                label80: {
                    byte var11;
                    try {
                        BufferedReader bufferedReader;
                        label87: {
                            bufferedReader = new BufferedReader(fileReader);

                            try {
                                JSONTokener tokener = new JSONTokener(bufferedReader);
                                JSONArray jsonArray = new JSONArray(tokener);
                                int i = 0;

                                while(true) {
                                    if (i >= jsonArray.length()) {
                                        var17 = 0;
                                        break;
                                    }

                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    String startIp = jsonObject.getString("start_ip");
                                    String endIp = jsonObject.getString("end_ip");
                                    if (isIpInRange(ipAddress, startIp, endIp) && (jsonObject.optBoolean("vpn", false) || jsonObject.optBoolean("proxy", false) || jsonObject.optBoolean("tor", false) || jsonObject.optBoolean("hosting", false) || jsonObject.optBoolean("relay", false))) {
                                        var11 = 1;
                                        break label87;
                                    }

                                    ++i;
                                }
                            } catch (Throwable var14) {
                                try {
                                    bufferedReader.close();
                                } catch (Throwable var13) {
                                    var14.addSuppressed(var13);
                                }

                                throw var14;
                            }

                            bufferedReader.close();
                            break label80;
                        }

                        bufferedReader.close();
                    } catch (Throwable var15) {
                        try {
                            fileReader.close();
                        } catch (Throwable var12) {
                            var15.addSuppressed(var12);
                        }

                        throw var15;
                    }

                    fileReader.close();
                    return var11;
                }

                fileReader.close();
                return var17;
            } catch (Exception var16) {
                plugin.logToFile("An error occurred when checking the IP against the local database: " + var16.getMessage());
                return 0;
            }
        }
    }

    public boolean isIpInRange(String ipAddress, String startIp, String endIp) {
        long ip = this.ipToLong(ipAddress);
        long start = this.ipToLong(startIp);
        long end = this.ipToLong(endIp);
        return ip >= start && ip <= end;
    }

    private long ipToLong(String ipAddress) {
        if (ipAddress.contains(":")) {
            return 0L;
        } else {
            String[] octets = ipAddress.split("\\.");
            long result = 0L;

            for(int i = 0; i < 4; ++i) {
                result |= Long.parseLong(octets[i]) << 24 - 8 * i;
            }

            return result;
        }
    }
}
