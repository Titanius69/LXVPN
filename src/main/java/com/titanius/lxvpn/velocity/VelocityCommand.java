package com.titanius.lxvpn.velocity;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.titanius.lxvpn.core.LXVPNCore;
import com.titanius.lxvpn.core.command.CommandHandler;
import com.titanius.lxvpn.core.command.Sender;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Registers {@code /lxvpn} on Velocity.
 *
 * <p>Brigadier wants a tree; the shared handler wants a string array. Rather than duplicating the
 * subcommand logic in two shapes, the tree is a single greedy argument that is split and handed
 * straight over, with completions supplied by the same handler that executes them.
 */
public class VelocityCommand {

    private final LXVPNCore core;
    private final CommandHandler handler;

    public VelocityCommand(LXVPNCore core) {
        this.core = core;
        this.handler = new CommandHandler(core);
    }

    public BrigadierCommand build() {
        LiteralCommandNode<CommandSource> root =
                LiteralArgumentBuilder.<CommandSource>literal("lxvpn")
                        .requires(source -> source.hasPermission(CommandHandler.PERMISSION))
                        .executes(context -> {
                            handler.execute(wrap(context.getSource()), new String[0]);
                            return 1;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument(
                                        "arguments", StringArgumentType.greedyString())
                                .suggests(this::suggest)
                                .executes(context -> {
                                    String raw = StringArgumentType.getString(context, "arguments");
                                    handler.execute(wrap(context.getSource()), raw.trim().split("\\s+"));
                                    return 1;
                                }))
                        .build();
        return new BrigadierCommand(root);
    }

    private CompletableFuture<Suggestions> suggest(CommandContext<CommandSource> context,
                                                   SuggestionsBuilder builder) {
        String input = builder.getRemaining();
        String[] parts = input.isEmpty() ? new String[0] : input.split("\\s+", -1);
        String partial = parts.length == 0 ? "" : parts[parts.length - 1].toLowerCase(Locale.ROOT);

        // Offset to the start of the word being typed so a completion replaces that word rather
        // than the whole line, which is what a greedy argument would otherwise do.
        int wordStart = builder.getStart() + input.length() - partial.length();
        SuggestionsBuilder offset = builder.createOffset(wordStart);
        for (String option : handler.suggest(parts)) {
            if (option.startsWith(partial)) {
                offset.suggest(option);
            }
        }
        return offset.buildFuture();
    }

    private Sender wrap(CommandSource source) {
        return new Sender() {
            @Override
            public void send(String message) {
                source.sendMessage(VelocityPlatform.LEGACY.deserialize(message));
            }

            @Override
            public boolean hasPermission(String node) {
                return source.hasPermission(node);
            }

            @Override
            public String name() {
                return source instanceof com.velocitypowered.api.proxy.Player player
                        ? player.getUsername() : "console";
            }
        };
    }
}
