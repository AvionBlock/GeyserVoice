package io.greitan.avion.bungeecord.commands;

import java.util.Objects;
import java.lang.NumberFormatException;

import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatColor;

import io.greitan.avion.bungeecord.GeyserVoice;
import io.greitan.avion.bungeecord.utils.Language;
import io.greitan.avion.common.utils.HasPermissionOperation;
import io.greitan.avion.common.utils.VoiceCommandCompletions;

public class VoiceCommand extends Command implements TabExecutor {

    private final GeyserVoice plugin;
    private final String lang;
    private boolean isConnected = false;

    // Get the plugin and lang interfaces.
    public VoiceCommand(GeyserVoice plugin, String lang) {
        super("voice");
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        isConnected = plugin.isConnected();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (args.length >= 1) {
                // Bind command - bind player.
                if (args[0].equalsIgnoreCase("bind") && player.hasPermission("voice.bind") && isConnected) {
                    if (args.length >= 2 && Objects.nonNull(args[1])) {
                        int bindKey;
                        try {
                            bindKey = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-invalid-args"))
                                    .color(ChatColor.RED).create());
                            return;
                        }
                        /*
                         * if (bindKey == 0) {
                         * player.sendMessage("Here is your open key!");
                         * return;
                         * }
                         */
                        Boolean isBound = plugin.bind(bindKey, player);
                        if (isBound) {
                            player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-bind-connect"))
                                    .color(ChatColor.AQUA).create());
                        } else {
                            player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-bind-disconnect"))
                                    .color(ChatColor.RED).create());
                        }
                    } else {
                        player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-invalid-args"))
                                .color(ChatColor.RED).create());
                    }
                }
                // Setup command - setup the configuration.
                else if (args[0].equalsIgnoreCase("setup") && player.hasPermission("voice.setup")) {
                    String newHost = args[1];
                    String newPortString = args[2];
                    Integer newPort = -1;
                    String newKey = args[3];
                    try {
                        if (Objects.nonNull(newPortString)) {
                            newPort = Integer.parseInt(newPortString);
                        }
                    } catch (NumberFormatException e) {
                        newPort = -1;
                    }

                    if (Objects.nonNull(newHost) && Objects.nonNull(newPortString) && Objects.nonNull(newKey) && newPort != -1) {
                        plugin.getConfig().set("config.host", newHost);
                        plugin.getConfig().set("config.port", newPort);
                        plugin.getConfig().set("config.server-key", newKey);
                        plugin.saveConfig();
                        plugin.reloadConfig();
                        plugin.reload();

                        player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-setup-success"))
                                .color(ChatColor.AQUA).create());
                    } else {
                        player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-setup-invalid-data"))
                                .color(ChatColor.RED).create());
                    }
                }
                // Connect command - connect to the VoiceCraft server.
                else if (args[0].equalsIgnoreCase("connect") && player.hasPermission("voice.connect")) {
                    if (Objects.nonNull(args[1])) {
                        Boolean force = Boolean.valueOf(args[1]);

                        Boolean connected = plugin.connect(force);
                        if (connected) {
                            player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "plugin-connect-connect"))
                                    .color(ChatColor.AQUA).create());
                        } else {
                            player.sendMessage(
                                    new ComponentBuilder(Language.getMessage(lang, "plugin-connect-disconnect"))
                                            .color(ChatColor.RED).create());
                        }
                    } else {
                        player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-invalid-args"))
                                .color(ChatColor.RED).create());
                    }
                }
                // Reload command - reload the configs.
                else if (args[0].equalsIgnoreCase("reload") && player.hasPermission("voice.reload")) {
                    plugin.reload();
                    player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-reload"))
                            .color(ChatColor.GREEN).create());
                } else if (args[0].equalsIgnoreCase("settings") && player.hasPermission("voice.settings")) {
                    int proximityDistance = 1;
                    Boolean proximityToggle = true;
                    Boolean voiceEffects = true;

                    plugin.updateSettings(proximityDistance, proximityToggle, voiceEffects);
                }
                // Command select invalid.
                else {
                    player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-invalid-args"))
                            .color(ChatColor.RED).create());
                }
            }
        }
        // Commands runned by console.
        else if (args.length >= 1) {
            // Reload command - reload the configs.
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reload();
                plugin.Logger.log(
                        new ComponentBuilder(Language.getMessage(lang, "cmd-reload")).color(ChatColor.GREEN).create());
            }
            // Command not for console.
            else {
                sender.sendMessage(new ComponentBuilder(Language.getMessage(lang, "cmd-not-player"))
                        .color(ChatColor.RED).create());
            }
        }
        // Invalid command arguments.
        else {
            sender.sendMessage(
                    new ComponentBuilder(Language.getMessage(lang, "cmd-invalid-args")).color(ChatColor.RED).create());
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return VoiceCommandCompletions.execute(args, new HasPermissionOperation() {
            @Override
            public boolean execute(String permission) {
                return sender.hasPermission(permission);
            }
        });
    }
}
