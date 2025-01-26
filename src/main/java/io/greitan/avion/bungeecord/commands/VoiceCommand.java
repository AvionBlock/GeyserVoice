package io.greitan.avion.bungeecord.commands;

import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatColor;

import io.greitan.avion.bungeecord.GeyserVoice;
import io.greitan.avion.bungeecord.utils.Language;
import io.greitan.avion.common.commands.BaseVoiceCommand;
import io.greitan.avion.common.utils.IntegerOperation;
import io.greitan.avion.common.utils.StringOperation;
import io.greitan.avion.common.utils.DoubleStringOperation;
import io.greitan.avion.common.utils.EmptyOperation;

public class VoiceCommand extends Command implements TabExecutor {

    private final BaseVoiceCommand voiceCommand;
    private final GeyserVoice plugin;
    private final String lang;

    // Get the plugin and lang interfaces.
    public VoiceCommand(GeyserVoice plugin, String lang) {
        super("voice");
        this.voiceCommand = new BaseVoiceCommand(plugin);
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        this.voiceCommand.onCommand(
            args,
            plugin.isConnected(),
            sender instanceof ProxiedPlayer,
            new StringOperation() {
                @Override
                public boolean execute(String permission) {
                    if (sender instanceof ProxiedPlayer)
                        return sender.hasPermission(permission);
                    else
                        return true;
                }
            },
            new DoubleStringOperation() {
                @Override
                public void execute(String text, String rawColor) {
                    ChatColor color = ChatColor.RED;
                    if (rawColor == "red") color = ChatColor.RED;
                    else if (rawColor == "aqua") color = ChatColor.AQUA;
                    else if (rawColor == "green") color = ChatColor.GREEN;
                    else if (rawColor == "yellow") color = ChatColor.YELLOW;

                    var message = new ComponentBuilder(Language.getMessage(lang, text)).color(color).create();
                    if (sender instanceof ProxiedPlayer)
                        sender.sendMessage(message);
                    else
                        plugin.Logger.log(message);
                }
            },
            new IntegerOperation() {
                @Override
                public boolean execute(int key) {
                    if (sender instanceof ProxiedPlayer) {
                        ProxiedPlayer player = (ProxiedPlayer) sender;
                        return plugin.bind(key, player);
                    } 
                    return false;
                }
            },
            new EmptyOperation() {
                @Override
                public boolean execute() {
                    if (sender instanceof ProxiedPlayer) {
                        ProxiedPlayer player = (ProxiedPlayer) sender;
                        GeyserVoice.getConfig().set("config.players." + player.getName(), null);
                        return true;
                    }
                    return false;
                }
            }
        );
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return voiceCommand.onTabComplete(args, new StringOperation() {
            @Override
            public boolean execute(String permission) {
                return sender.hasPermission(permission);
            }
        });
    }
}
