package com.titanius.lxvpn.core.command;

import com.titanius.lxvpn.core.LXVPNCore;
import com.titanius.lxvpn.core.antivpn.Verdict;
import com.titanius.lxvpn.core.util.AsyncExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The {@code /lxvpn} command, written once for both proxies.
 *
 * <p>Only the argument parsing and output live here; registration and tab completion are done by each
 * platform layer, because that is the one part their APIs genuinely disagree about.
 */
public class CommandHandler {

    public static final String PERMISSION = "lxvpn.admin";

    private final LXVPNCore core;

    public CommandHandler(LXVPNCore core) {
        this.core = core;
    }

    public void execute(Sender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.send(core.messages().prefixed("no-permission"));
            return;
        }
        if (args.length == 0) {
            status(sender);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "check" -> check(sender, args);
            case "stats" -> status(sender);
            case "blacklist" -> blacklist(sender, args);
            case "iprestrict" -> ipRestrict(sender, args);
            default -> sender.send(core.messages().prefixed("usage"));
        }
    }

    public List<String> suggest(String[] args) {
        if (args.length <= 1) {
            return List.of("check", "reload", "stats", "blacklist", "iprestrict");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("blacklist")) {
            return List.of("add", "remove", "size");
        }
        if (args.length == 2 && sub.equals("iprestrict")) {
            return List.of("add", "remove", "clear", "info");
        }
        return List.of();
    }

    // --------------------------------------------------------------- subcommands

    private void status(Sender sender) {
        var antiVpn = core.antiVpn();
        List<String> lines = new ArrayList<>();
        lines.add("&8\u2500\u2500\u2500 &bLX&3VPN &7" + LXVPNCore.VERSION
                + " &8on &7" + core.platform().name() + " &8\u2500\u2500\u2500");
        lines.add("&7Check levels: &f" + antiVpn.levelSummary());
        lines.add("&7Threshold: &f" + core.config().getMinScore() + " &8| &7Checked: &f"
                + antiVpn.checkedCount() + " &8| &7Blocked: &f" + antiVpn.blockedCount());
        lines.add("&7ASN database: " + (core.asn().isReady() ? "&aready" : "&cunavailable")
                + " &8| &7Country database: "
                + (!core.geo().isEnabled() ? "&8disabled"
                : core.geo().isReady() ? "&aready" : "&cunavailable"));
        lines.add("&7Cached scores: &f" + antiVpn.cachedScores()
                + " &8| &7Lists: &f" + antiVpn.cachedLists()
                + " &8(&f" + antiVpn.cachedListEntries() + " &8addresses)");
        lines.add("&7Blacklist: &f" + antiVpn.blacklistSize()
                + " &8| &7IP-restricted accounts: &f" + core.ipRestrict().accountCount());
        lines.add("&7I/O: &f" + (AsyncExecutor.virtualThreads()
                ? "virtual threads" : "pooled threads (Java 17)"));
        if (core.updates().updateAvailable()) {
            lines.add("&eVersion " + core.updates().newestVersion() + " is available.");
        }
        lines.forEach(line -> sender.send(colorize(line)));
    }

    private void reload(Sender sender) {
        sender.send(core.messages().prefixed(core.reload() ? "reload-success" : "reload-failed"));
    }

    /**
     * Scores an address on demand.
     *
     * <p>Bypasses the cache on purpose: the reason anyone runs this is that they disagree with a
     * cached verdict, and answering from the same cache would be useless.
     */
    private void check(Sender sender, String[] args) {
        if (args.length < 2) {
            sender.send(colorize("&7Usage: &f/lxvpn check <ip|player>"));
            return;
        }
        String target = args[1];
        Optional<String> resolved = core.platform().playerAddress(target);
        String ip = resolved.orElse(target);

        sender.send(colorize("&7Checking &f" + ip + "&7..."));
        core.antiVpn().inspect(ip).thenAccept(verdict -> {
            String country = core.geo().countryOf(ip).orElse("unknown");
            sender.send(colorize("&7Result for &f" + ip + "&7: "
                    + (verdict.allowed() ? "&aclean" : "&cblocked")
                    + " &8(&7score &f" + verdict.score()
                    + "&8, &7threshold &f" + core.config().getMinScore() + "&8)"));
            sender.send(colorize("&7Country: &f" + country
                    + " &8| &7Blacklisted: &f" + (core.antiVpn().isBlacklisted(ip) ? "yes" : "no")));
        }).exceptionally(error -> {
            sender.send(colorize("&cThe check failed: " + error.getMessage()));
            return null;
        });
    }

    private void blacklist(Sender sender, String[] args) {
        if (args.length < 2) {
            sender.send(colorize("&7Usage: &f/lxvpn blacklist <add|remove|size> [ip]"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "size" -> sender.send(colorize("&7Blacklisted addresses: &f"
                    + core.antiVpn().blacklistSize()));
            case "add" -> {
                if (args.length < 3) {
                    sender.send(colorize("&7Usage: &f/lxvpn blacklist add <ip>"));
                    return;
                }
                boolean added = core.antiVpn().addToBlacklist(args[2]);
                sender.send(colorize(added
                        ? "&aAdded &f" + args[2] + "&a to the blacklist."
                        : "&7That address is already blacklisted."));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.send(colorize("&7Usage: &f/lxvpn blacklist remove <ip>"));
                    return;
                }
                boolean removed = core.antiVpn().removeFromBlacklist(args[2]);
                sender.send(colorize(removed
                        ? "&aRemoved &f" + args[2] + "&a from the blacklist and cleared its cached score."
                        : "&7That address was not blacklisted."));
            }
            default -> sender.send(colorize("&7Usage: &f/lxvpn blacklist <add|remove|size> [ip]"));
        }
    }

    private void ipRestrict(Sender sender, String[] args) {
        if (args.length < 3) {
            sender.send(colorize("&7Usage: &f/lxvpn iprestrict <add|remove|clear|info> <player> [ip]"));
            return;
        }
        String player = args[2];
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                var addresses = core.ipRestrict().addressesOf(player);
                sender.send(colorize(addresses.isEmpty()
                        ? "&7&f" + player + "&7 is not restricted."
                        : "&7&f" + player + "&7 may connect from: &f" + String.join(", ", addresses)));
            }
            case "add" -> {
                String ip = args.length >= 4 ? args[3]
                        : core.platform().playerAddress(player).orElse(null);
                if (ip == null) {
                    sender.send(colorize("&cGive an address, or run this while the player is online."));
                    return;
                }
                sender.send(colorize(core.ipRestrict().add(player, ip)
                        ? "&aBound &f" + player + "&a to &f" + ip
                        : "&7That binding already exists."));
            }
            case "remove" -> {
                if (args.length < 4) {
                    sender.send(colorize("&7Usage: &f/lxvpn iprestrict remove <player> <ip>"));
                    return;
                }
                sender.send(colorize(core.ipRestrict().remove(player, args[3])
                        ? "&aRemoved that binding."
                        : "&7No such binding."));
            }
            case "clear" -> sender.send(colorize(core.ipRestrict().clear(player)
                    ? "&aCleared every binding for &f" + player
                    : "&7That account was not restricted."));
            default -> sender.send(colorize("&7Usage: &f/lxvpn iprestrict <add|remove|clear|info> <player> [ip]"));
        }
    }

    private static String colorize(String input) {
        return com.titanius.lxvpn.core.util.Colors.colorize(input);
    }

    /** The kick text for a denied verdict, already coloured. */
    public String denialMessage(Verdict verdict) {
        return core.messages().get(verdict.messageKey());
    }
}
