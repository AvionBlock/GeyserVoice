package io.greitan.avion.paper.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import io.greitan.avion.paper.GeyserVoice;
import io.greitan.avion.paper.utils.Language;
import io.greitan.avion.common.commands.BaseVoiceCommand;
import io.greitan.avion.common.utils.IntegerOperation;
import io.greitan.avion.common.utils.StringOperation;
import io.greitan.avion.common.utils.DoubleStringOperation;
import io.greitan.avion.common.utils.EmptyOperation;

public class VoiceCommand implements CommandExecutor, TabCompleter {
    
    private final BaseVoiceCommand voiceCommand;
    private final GeyserVoice plugin;
    private final String lang;

    // Get the plugin and lang interfaces.
    public VoiceCommand(GeyserVoice plugin, String lang) {
        this.voiceCommand = new BaseVoiceCommand(plugin);
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return this.voiceCommand.onCommand(
            args,
            plugin.isConnected(),
            sender instanceof Player,
            new StringOperation() {
                @Override
                public boolean execute(String permission) {
                    if (sender instanceof Player)
                        return sender.hasPermission(permission);
                    else
                        return true;
                }
            },
            new DoubleStringOperation() {
                @Override
                public void execute(String text, String rawColor) {
                    NamedTextColor color = NamedTextColor.RED;
                    if (rawColor == "red") color = NamedTextColor.RED;
                    else if (rawColor == "aqua") color = NamedTextColor.AQUA;
                    else if (rawColor == "green") color = NamedTextColor.GREEN;
                    else if (rawColor == "yellow") color = NamedTextColor.YELLOW;

                    var message = Component.text(Language.getMessage(lang, text)).color(color);
                    if (sender instanceof Player)
                        sender.sendMessage(message);
                    else
                        plugin.Logger.log(message);
                }
            },
            new IntegerOperation() {
                @Override
                public boolean execute(int key) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        return plugin.bind(key, player);
                    } 
                    return false;
                }
            },
            new EmptyOperation() {
                @Override
                public boolean execute() {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        plugin.getConfig().set("config.players." + player.getName(), null);
                        return true;
                    }
                    return false;
                }
            }
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return voiceCommand.onTabComplete(args, new StringOperation() {
            @Override
            public boolean execute(String permission) {
                return sender.hasPermission(permission);
            }
        });
    }
}
