package com.titanius.lxvpn.bungee;

import com.titanius.lxvpn.core.LXVPNCore;
import com.titanius.lxvpn.core.command.CommandHandler;
import com.titanius.lxvpn.core.command.Sender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;
import java.util.Locale;

/** Registers {@code /lxvpn} on BungeeCord and forwards to the shared handler. */
public class BungeeCommand extends Command implements TabExecutor {

    private final CommandHandler handler;

    public BungeeCommand(LXVPNCore core) {
        super("lxvpn", null, "antivpn", "lxantivpn");
        this.handler = new CommandHandler(core);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        handler.execute(wrap(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(CommandHandler.PERMISSION)) {
            return List.of();
        }
        List<String> options = handler.suggest(args);
        String partial = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(partial)).toList();
    }

    private static Sender wrap(CommandSender sender) {
        return new Sender() {
            @Override
            public void send(String message) {
                sender.sendMessage(TextComponent.fromLegacyText(message));
            }

            @Override
            public boolean hasPermission(String node) {
                return sender.hasPermission(node);
            }

            @Override
            public String name() {
                return sender.getName();
            }
        };
    }
}
