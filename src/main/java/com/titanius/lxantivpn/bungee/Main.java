//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.titanius.lxantivpn.bungee;

import com.titanius.lxantivpn.bungee.checkers.antivpn_tor.IPChecker;

import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.high.highIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.low.lowIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.mid.midIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.normal.normalIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.proxy.proxyIPChecker;
import com.titanius.lxantivpn.bungee.checkers.levels.antivpn_tor.superhigh.superhighIPChecker;
import com.titanius.lxantivpn.bungee.checkers.update.UpdateChecker;
import com.titanius.lxantivpn.bungee.commands.LXVPNCommands;
import com.titanius.lxantivpn.bungee.utils.CIDRUtils;
import com.titanius.lxantivpn.bungee.utils.ColorUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Main extends Plugin implements Listener {
    private Configuration config;
    private Configuration messages;
    private List<String> ipBlacklist;
    private List<String> blockedCountries;
    private Set<String> bypassList;
    private Map<String, Integer> connectionCount;
    private Map<String, Long> lastConnectionTime;
    private IPChecker ipChecker;
    private List<String> cidrBlacklist;
    private boolean isAntiVPNEnabled;
    private boolean isAntiBotEnabled;
    private boolean isTorCheckEnabled;
    private Map<String, String> playerIPMap;
    private List<String> kickMessages;
    private FileWriter logFileWriter;
    private static Main instance;
    private lowIPChecker lowIPChecker;
    private normalIPChecker normalIPChecker;
    private midIPChecker midIPChecker;
    private highIPChecker highIPChecker;
    private superhighIPChecker superhighIPChecker;
    private proxyIPChecker proxyIPChecker;
    private Set<String> blacklist = new HashSet<>();

    public Main() {
    }

    public static Main getInstance() {
        return instance;
    }

    private final File blacklistFile = new File(getDataFolder(), "blacklist.json");;

    public void onEnable() {
        instance = this;

        try {
            File logDir = new File(this.getDataFolder(), "logs");

            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String currentDate = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
            String logFileName = logDir + File.separator + "log_" + currentDate + ".txt";
            logFileWriter = new FileWriter(logFileName, true);
            logToFile("LXVPN - Proxy Enabled!");

        } catch (IOException e) {
            logToFile("Error occurred while creating the log file: " + e.getMessage());
            e.printStackTrace();
        }

        try {

            loadConfig();
            checkConfigVersion();
            loadBlacklist();
            loadPlayerBlacklist();
            cidrBlacklist = new ArrayList();
            loadCountryBlocklist();
            loadBypassList();
            saveBlockedIPs();
            loadPlayerIPs();
            loadKickMessages();
            connectionCount = new ConcurrentHashMap();
            lastConnectionTime = new ConcurrentHashMap();
            (new UpdateChecker(this, 118999)).checkForUpdates();
            ipChecker = new IPChecker(this);
            lowIPChecker = new lowIPChecker(this);
            normalIPChecker = new normalIPChecker(this);
            midIPChecker = new midIPChecker(this);
            highIPChecker = new highIPChecker(this);
            superhighIPChecker = new superhighIPChecker(this);
            proxyIPChecker = new proxyIPChecker(this);
            isAntiVPNEnabled = this.config.getBoolean("antivpn", true);
            isAntiBotEnabled = this.config.getBoolean("antibot", true);
            isTorCheckEnabled = this.config.getBoolean("tor", true);
            getProxy().getPluginManager().registerListener(this, this);
            getProxy().getPluginManager().registerCommand(this, new LXVPNCommands.LXVPNCommand(this));

        } catch (Exception e) {
            logToFile("Error during plugin initialization: " + e.getMessage() +
                    " - Please check your configuration and dependencies!");
            e.printStackTrace();
        }
    }


    public void reloadPlugin() {
        try {
            loadConfig();
            loadBlacklist();
            loadPlayerBlacklist();
            loadCountryBlocklist();
            loadBypassList();
            saveBlockedIPs();
            loadPlayerIPs();
            loadKickMessages();
            logToFile("LXAntiVPNProxy reloaded successfully!");
        } catch (Exception var2) {
            logToFile("Error during plugin reload");
        }

    }

    private void checkConfigVersion() {
        int currentVersion = this.config.getInt("version", 0);
        int CONFIG_VERSION = 8;
        if (currentVersion != CONFIG_VERSION) {
            logToFile("");
            logToFile("§eYou may be useing an outdated config.");
            logToFile("");

        } else {
            logToFile("");
            logToFile("§aYour config version is up to date!");
            logToFile("");
        }

    }

    public void loadPlayerIPs() {
        this.playerIPMap = new HashMap();

        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(this.getDataFolder(), "config.yml"));
            Iterator<String> var2 = config.getSection("players").getKeys().iterator();

            while(var2.hasNext()) {
                String playerName = (String)var2.next();
                String ip = config.getString("players." + playerName + ".ip");
                playerIPMap.put(playerName, ip);
            }
        } catch (IOException var5) {
            IOException e = var5;
            e.printStackTrace();
        }

    }

    public void loadKickMessages() {
        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(this.getDataFolder(), "messages.yml"));
            kickMessages = config.getStringList("ip-restrict-kick-messages");
        } catch (IOException var2) {
            IOException e = var2;
            e.printStackTrace();
        }

    }

    private String getFormattedKickMessage() {
        return kickMessages.isEmpty() ? "You are not allowed to join from this IP." : String.join("\n", this.kickMessages);
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        String playerName = event.getConnection().getName();
        String playerIP = event.getConnection().getAddress().getHostString();
        logToFile("Player " + playerName + " is connecting from IP: " + playerIP);
        if (playerIPMap.containsKey(playerName)) {
            String allowedIP = (String)this.playerIPMap.get(playerName);
            if (!allowedIP.equals(playerIP)) {
                event.setCancelled(true);
                event.setCancelReason(this.getFormattedKickMessage());
            }
        }

    }

    public void onDisable() {
        try {
            logToFile("LXVPN - Proxy Disabled!");
            if (logFileWriter != null) {
                logFileWriter.close();
            }
        } catch (IOException var2) {
            IOException e = var2;
            e.printStackTrace();
        }

    }

    public void loadConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            File dataDir = new File(this.getDataFolder(), "data");
            if (!dataDir.exists()) {
                dataDir.mkdir();
            }

            createFileIfNotExists("config.yml");
            createFileIfNotExists("messages.yml");
            createFileIfNotExists("data/anonymusprivacydatabase.json");
            createFileIfNotExists("data/ipdb4.json");
            createFileIfNotExists("data/protonvpn.json");
            createFileIfNotExists("data/ips.json");


            config = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File(this.getDataFolder(), "config.yml"));
            messages = YamlConfiguration.getProvider(YamlConfiguration.class).load(new File(this.getDataFolder(), "messages.yml"));
        } catch (IOException var2) {
            logToFile("Could not load config files");
        }

    }

    private void createFileIfNotExists(String filePath) throws IOException {
        File file = new File(this.getDataFolder(), filePath);
        if (!file.exists()) {
            InputStream in = this.getResourceAsStream(filePath);

            try {
                Files.copy(in, file.toPath(), new CopyOption[0]);
            } catch (Throwable var7) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (in != null) {
                in.close();
            }
        }

    }

    public void loadBlacklist() {
        File blacklistFile = new File(getDataFolder(), "data/ip-blacklist.json");
        File protonvpnFile = new File(getDataFolder(), "data/protonvpn.json");
        File ipsFile = new File(getDataFolder(), "data/ips.json");

        ipBlacklist = new ArrayList<>();
        cidrBlacklist = new ArrayList<>();


        if (blacklistFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(blacklistFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (isCIDRRange(line)) {
                        cidrBlacklist.add(line);
                    } else {
                        ipBlacklist.add(line);
                    }
                }
            } catch (IOException e) {
                logToFile("Could not load IP blacklist");
            }
        }


        if (ipsFile.exists()) {
            loadIPsFromJSON(ipsFile);
        }


        if (protonvpnFile.exists()) {
            loadIPsFromJSON(protonvpnFile);
        }
    }

    private void loadPlayerBlacklist() {
        File file = new File(getDataFolder(), "blacklist.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
                Files.write(file.toPath(), "[]".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                getLogger().severe("Cant create blacklist.json file: " + e.getMessage());
                return;
            }
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(content);
            for (int i = 0; i < jsonArray.length(); i++) {
                blacklist.add(jsonArray.getString(i));
            }
            getLogger().info("Loaded " + blacklist.size() + " IP-s from blacklist.");
        } catch (Exception e) {
            getLogger().severe("An eror accord while loading blacklist.json file: " + e.getMessage());
        }
    }

    private void loadIPsFromJSON(File jsonFile) {
        try {
            FileReader reader = new FileReader(jsonFile);

            try {
                JSONArray jsonArray = new JSONArray(new JSONTokener(reader));

                for(int i = 0; i < jsonArray.length(); ++i) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    Iterator var6 = obj.keySet().iterator();

                    while(var6.hasNext()) {
                        String key = (String)var6.next();
                        String value = obj.getString(key);
                        if (this.isCIDRRange(key)) {
                            this.cidrBlacklist.add(key.trim());
                        } else {
                            this.ipBlacklist.add(key.trim());
                        }

                        if (this.isCIDRRange(value)) {
                            this.cidrBlacklist.add(value.trim());
                        } else {
                            this.ipBlacklist.add(value.trim());
                        }
                    }
                }
            } catch (Throwable var10) {
                try {
                    reader.close();
                } catch (Throwable var9) {
                    var10.addSuppressed(var9);
                }

                throw var10;
            }

            reader.close();
        } catch (IOException var11) {
            this.logToFile("Could not load IPs from " + jsonFile.getName());
        }

    }

    private boolean isCIDRRange(String input) {
        return input.contains("/");
    }

    public void saveBlockedIPs() {
        File pluginFolder = this.getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdir();
        }

        File blockedIpsFile = new File(pluginFolder, "data/blocked-ips.json");
        JSONArray blockedIpsJson = new JSONArray(this.ipBlacklist);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(blockedIpsFile));

            try {
                writer.write(blockedIpsJson.toString(4));
            } catch (Throwable var8) {
                try {
                    writer.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }

                throw var8;
            }

            writer.close();
        } catch (IOException var9) {
            this.logToFile("Could not save IPs to blocked-ips.json");
        }

    }

    public void loadCountryBlocklist() {
        this.blockedCountries = this.config.getStringList("blocked-countries");
    }

    public void loadBypassList() {
        this.bypassList = new HashSet<>(this.config.getStringList("bypass-list"));
    }



    // TODO: konvert blacklist to mysql, or sql
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        String ipAddress = event.getPlayer().getAddress().getAddress().getHostAddress();
        String playerName = event.getPlayer().getName();
        this.logToFile("Player connected with IP: " + ipAddress);


        if (blacklist.contains(ipAddress)) {
            this.logToFile("Player " + playerName + " kicked: IP " + ipAddress + " contains in the blacklist");
            String kickMessage = getFormattedKickMessage();
            logToFile("[DEBUG] Loaded blacklist: " + blacklist.toString());


            BaseComponent[] messageComponents = TextComponent.fromLegacyText(kickMessage);
            event.getPlayer().disconnect(messageComponents);
            return;
        } else {
            logToFile("[DEBUG] not blacklisted " + playerName + " ip:" + ipAddress);
        }




        if (!this.bypassList.contains(ipAddress) && !this.bypassList.contains(playerName)) {
            CompletableFuture<Boolean> vpnCheckFuture = this.isAntiVPNEnabled ? CompletableFuture.supplyAsync(() -> {
                return this.isIpBlacklisted(ipAddress) || this.isCountryBlocked(ipAddress) || this.ipChecker.isVpnOrProxy(ipAddress, playerName);
            }) : CompletableFuture.completedFuture(false);
            CompletableFuture<Boolean> torCheckFuture = this.isTorCheckEnabled ? CompletableFuture.supplyAsync(() -> {
                return this.ipChecker.isVpnOrProxy(ipAddress, playerName);
            }) : CompletableFuture.completedFuture(false);
            CompletableFuture<Boolean> botCheckFuture = this.isAntiBotEnabled ? CompletableFuture.supplyAsync(() -> {
                return this.isSuspicious(ipAddress) || this.isBot(ipAddress);
            }) : CompletableFuture.completedFuture(false);
            CompletableFuture.allOf(vpnCheckFuture, torCheckFuture, botCheckFuture).thenAcceptAsync((Void) -> {
                try {
                    boolean shouldKick = vpnCheckFuture.get() || torCheckFuture.get() || botCheckFuture.get();
                    if (shouldKick) {
                        this.logToFile("Player disconnected due to failed checks.");
                        List<String> vpnkickMessages = this.messages.getStringList("vpn-kick-messages");
                        ComponentBuilder builder = new ComponentBuilder();
                        for (String line : vpnkickMessages) {
                            builder.append(TextComponent.fromLegacyText(ColorUtil.color(line) + "\n"));
                        }
                        event.getPlayer().disconnect(builder.create());
                        this.notifyAdmins(playerName, ipAddress);
                    } else {
                        this.trackConnection(ipAddress);
                    }
                } catch (Exception ex) {
                    this.logToFile("Error during post-login checks for player: " + playerName);
                }
            });
        } else {
            this.logToFile("Player or IP is in bypass list: " + playerName + " (" + ipAddress + ")");
        }
    }


    private boolean isIpBlacklisted(String ipAddress) {
        try {
            Iterator var2 = this.cidrBlacklist.iterator();

            while(var2.hasNext()) {
                String cidr = (String)var2.next();
                CIDRUtils cidrUtils = new CIDRUtils(cidr);
                if (cidrUtils.isInRange(ipAddress)) {
                    this.logToFile("Player IP is blacklisted: " + ipAddress);
                    return true;
                }
            }
        } catch (UnknownHostException var5) {
            this.logToFile("Error checking IP blacklist");
        }

        return false;
    }

    private boolean isCountryBlocked(String ipAddress) {
        try {
            URL url = new URL("https://freeipapi.com/api/json/" + ipAddress);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder content = new StringBuilder();

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();
            JSONObject responseJson = new JSONObject(content.toString());
            String countryCode = responseJson.getString("countryCode");
            if (this.blockedCountries.contains(countryCode)) {
                this.logToFile("Player country is blocked: " + countryCode);
                return true;
            }
        } catch (Exception var9) {
            this.logToFile("Error checking country for IP: " + ipAddress);
        }

        return false;
    }

    private boolean isSuspicious(String ipAddress) {
        int recentConnections = (Integer)this.connectionCount.getOrDefault(ipAddress, 0);
        long lastTime = (Long)this.lastConnectionTime.getOrDefault(ipAddress, 0L);
        if (recentConnections > this.config.getInt("connection-threshold", 5) && System.currentTimeMillis() - lastTime < TimeUnit.SECONDS.toMillis(60L)) {
            this.logToFile("Suspicious behavior detected from IP: " + ipAddress);
            return true;
        } else {
            return false;
        }
    }

    private boolean isBot(String ipAddress) {
        long currentTime = System.currentTimeMillis();
        long lastConnection = (Long)this.lastConnectionTime.getOrDefault(ipAddress, 0L);
        long timeDifference = currentTime - lastConnection;
        if (timeDifference < TimeUnit.SECONDS.toMillis(10L)) {
            this.logToFile("Bot-like behavior detected from IP: " + ipAddress + " (Connection within 10 seconds)");
            return true;
        } else {
            return false;
        }
    }

    private void trackConnection(String ipAddress) {
        this.connectionCount.put(ipAddress, (Integer)this.connectionCount.getOrDefault(ipAddress, 0) + 1);
        this.lastConnectionTime.put(ipAddress, System.currentTimeMillis());
    }

    private void notifyAdmins(String playerName, String ipAddress) {
        String alertMessage = String.format(this.messages.getString("alert-message"), playerName, ipAddress);
        this.getProxy().getPlayers().forEach((player) -> {
            if (player.hasPermission("lxvpn.alert")) {
                player.sendMessage(new TextComponent(ColorUtil.color(alertMessage)));
            }

        });
        this.logToFile(alertMessage);
    }

    public File getBlacklistFile() {
        return this.blacklistFile;
    }

    public void logToFile(String message) {
        try {
            String timestamp = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]")).format(new Date());
            String logMessage = timestamp + " - " + message;
            this.logFileWriter.write(logMessage + "\n");
            this.logFileWriter.flush();
            this.getLogger().info(message);
        } catch (IOException var4) {
            IOException e = var4;
            e.printStackTrace();
        }

    }
}
