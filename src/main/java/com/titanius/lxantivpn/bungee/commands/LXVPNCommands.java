//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.titanius.lxantivpn.bungee.commands;



import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import com.titanius.lxantivpn.bungee.Main;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.json.*;

public class LXVPNCommands {
    private final Main plugin;
    private final File blacklistFile;

    public LXVPNCommands(Main plugin) throws IOException {
        this.plugin = plugin;
        blacklistFile = new File(plugin.getDataFolder(), "blacklist.json");

        if (!blacklistFile.exists()) {
            blacklistFile.createNewFile();
            try (FileWriter writer = new FileWriter(blacklistFile)) {
                writer.write("[]");
            }
        }
    }

    public File getBlacklistFile() {
        return blacklistFile;
    }

    public static class LXVPNCommand extends Command implements TabExecutor {
        private final Main plugin;
        private final File configFile;

        public LXVPNCommand(Main plugin) {
            super("lxvpn", "lxvpn.command", "lxvpnc", "antivpn", "delusionalvpn", "deluluvpn");
            this.plugin = plugin;
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                displayHelp(sender);
                return;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    plugin.reloadPlugin();
                    sender.sendMessage(new TextComponent("§aLXAntiVPNProxy reloaded successfully!"));
                    break;

                case "help":
                    displayHelp(sender);
                    break;

                case "checkip":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponent("\u00a7cUsage: /lxvpn checkip <player>"));
                        return;
                    }

                    ProxiedPlayer target = this.plugin.getProxy().getPlayer(args[1]);
                    if (target != null) {
                        String ipAddress = target.getAddress().getAddress().getHostAddress();
                        sender.sendMessage(new TextComponent("\u00a7aPlayer \u00a7b" + target.getName() + "\u00a7a has IP: \u00a7b" + ipAddress));
                    } else {
                        sender.sendMessage(new TextComponent("\u00a7cPlayer not found: " + args[1]));
                    }
                    break;

                case "blacklist":
                    if (args.length < 3) {
                        sender.sendMessage(new TextComponent("§cUsage: /lxvpn blacklist add|remove <ip>"));
                        return;
                    }

                    handleBlacklist(sender, args[1], args[2]);
                    break;

                case "addiprestrict":
                    if (args.length < 3) {
                        sender.sendMessage(new TextComponent("§cUsage: /lxvpn addiprestrict <player> <ip>"));
                        return;
                    }

                    addIPRestrict(sender, args[1], args[2]);
                    break;

                default:
                    sender.sendMessage(new TextComponent("§cUnknown subcommand. Use /lxvpn help for a list of commands."));
                    break;
            }
        }

        private void displayHelp(CommandSender sender) {
            sender.sendMessage(new TextComponent("§5LXVPN Commands:"));
            sender.sendMessage(new TextComponent("§b/lxvpn reload §7- Reloads the plugin configuration."));
            sender.sendMessage(new TextComponent("§b/lxvpn help §7- Shows this help message."));
            sender.sendMessage(new TextComponent("§b/lxvpn checkip <player> §7- Shows the IP address of the specified player."));
            sender.sendMessage(new TextComponent("§b/lxvpn blacklist add|remove <ip> §7- Add or remove an IP from the blacklist."));
            sender.sendMessage(new TextComponent("§b/lxvpn addiprestrict <player> <ip> §7- Binds someones IP to a player."));
        }

        private void handleBlacklist(CommandSender sender, String action, String ip) {
            try {
                Set<String> blacklistedIPs = loadBlacklist();

                if ("add".equalsIgnoreCase(action)) {
                    if (blacklistedIPs.add(ip)) {
                        saveBlacklist(blacklistedIPs);
                        sender.sendMessage(new TextComponent("§aIP added to blacklist: §b" + ip));
                    } else {
                        sender.sendMessage(new TextComponent("§cIP is already blacklisted: §b" + ip));
                    }
                } else if ("remove".equalsIgnoreCase(action)) {
                    if (blacklistedIPs.remove(ip)) {
                        saveBlacklist(blacklistedIPs);
                        sender.sendMessage(new TextComponent("§aIP removed from blacklist: §b" + ip));
                    } else {
                        sender.sendMessage(new TextComponent("§cIP not found in blacklist: §b" + ip));
                    }
                } else {
                    sender.sendMessage(new TextComponent("§cInvalid action. Use add or remove."));
                }
            } catch (IOException e) {
                sender.sendMessage(new TextComponent("§cError updating blacklist: " + e.getMessage()));
            }
        }

        public Set<String> loadBlacklist() throws IOException {
            try (FileReader reader = new FileReader(plugin.getBlacklistFile())) {
                JSONArray array;
                try {
                    array = new JSONArray(new JSONTokener(reader));
                } catch (JSONException e) {
                    array = new JSONArray(); // Handle empty or invalid JSON
                }
                Set<String> result = new HashSet<>();
                for (int i = 0; i < array.length(); i++) {
                    result.add(array.getString(i));
                }
                return result;
            }
        }

        private void saveBlacklist(Set<String> blacklistedIPs) throws IOException {
            JSONArray array = new JSONArray(blacklistedIPs);
            try (FileWriter writer = new FileWriter(plugin.getBlacklistFile())) {
                writer.write(array.toString(2));
            }
        }

        private void addIPRestrict(CommandSender sender, String player, String ip) {
            try {
                Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
                config.set("players." + player + ".ip", ip);
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
                sender.sendMessage(new TextComponent("§aAdded IP restriction for player: §b" + player + " §7with IP: §b" + ip));
            } catch (IOException e) {
                sender.sendMessage(new TextComponent("§cError updating IP restriction: " + e.getMessage()));
            }
        }



        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Arrays.asList("reload", "help","checkip", "blacklist", "addiprestrict").stream()
                        .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("blacklist")) {
                return Arrays.asList("add", "remove").stream()
                        .filter(action -> action.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("blacklist")) {
                try {
                    return loadBlacklist().stream()
                            .filter(ip -> ip.startsWith(args[2]))
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    return Collections.emptyList();
                }
            }

            return Collections.emptyList();
        }
    }

}
