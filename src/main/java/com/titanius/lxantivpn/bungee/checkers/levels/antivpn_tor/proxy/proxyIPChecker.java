// Source code is decompiled from a .class file using FernFlower decompiler.
package com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.proxy;

import com.titanius.lxantivpn.bungee.Main;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class proxyIPChecker {
    private final Main plugin;
    private final String IpRegistryKey;
    private final Configuration config;
    private final String VPNAPIkey;

    public proxyIPChecker(Main plugin) throws IOException {
        this.plugin = plugin;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        VPNAPIkey = config.getString("antivpn_tor.vpn-api-api-key");
        IpRegistryKey = config.getString("antivpn_tor.ipregistry-api-key");
    }

    public int checkWithGithubProxyTheSpeedXhttp(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/TheSpeedX/PROXY-List/refs/heads/master/http.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list TheSpeedX http - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyTheSpeedXSock4(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/TheSpeedX/PROXY-List/refs/heads/master/socks4.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list TheSpeedX sock4 - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyTheSpeedXSock5(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/TheSpeedX/PROXY-List/refs/heads/master/socks5.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list TheSpeedX sock5 - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyActionsUser(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/proxifly/free-proxy-list/main/proxies/all/data.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String ip;
            do {
                String inputLine;
                if ((inputLine = in.readLine()) == null) {
                    in.close();
                    return 0;
                }

                String[] protocolSplit = inputLine.split("://");
                String[] ipPort = protocolSplit[protocolSplit.length - 1].split(":");
                ip = ipPort[0];
            } while(!ip.equals(ipAddress));

            in.close();
            return 1;
        } catch (Exception var9) {
            plugin.logToFile("Error checking IP with Github Proxy list ActionUser - repo: " + var9.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyZaeem20http(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/Zaeem20/FREE_PROXIES_LIST/refs/heads/master/http.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list Zaeem20 http - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyZaeem20https(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/Zaeem20/FREE_PROXIES_LIST/refs/heads/master/https.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list Zaeem20 https - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyZaeem20sock4(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/Zaeem20/FREE_PROXIES_LIST/refs/heads/master/socks4.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list Zaeem20 sock4 - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyZaeem20sock5(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/Zaeem20/FREE_PROXIES_LIST/refs/heads/master/socks5.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list Zaeem20 sock5 - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyTorwalds(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/monosans/proxy-list/main/proxies/all.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String ip;
            do {
                String inputLine;
                if ((inputLine = in.readLine()) == null) {
                    in.close();
                    return 0;
                }

                String[] protocolSplit = inputLine.split("://");
                String[] ipPort = protocolSplit[protocolSplit.length - 1].split(":");
                ip = ipPort[0];
            } while(!ip.equals(ipAddress));

            in.close();
            return 1;
        } catch (Exception var9) {
            plugin.logToFile("Error checking IP with Github Proxy list Torwalds - repo: " + var9.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyKangProxy(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/officialputuid/KangProxy/KangProxy/xResults/Proxies.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list KangProxy - repo: " + var8.getMessage());
            return 0;
        }
    }

    public int checkWithGithubProxyClarketem(String ipAddress) {
        try {
            URL url = new URL("https://raw.githubusercontent.com/clarketm/proxy-list/master/proxy-list-raw.txt");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.split(":");
                if (parts.length > 0) {
                    String ip = parts[0];
                    if (ip.equals(ipAddress)) {
                        in.close();
                        return 1;
                    }
                }
            }

            in.close();
            return 0;
        } catch (Exception var8) {
            plugin.logToFile("Error checking IP with Github Proxy list Clarketem - repo: " + var8.getMessage());
            return 0;
        }
    }
}
