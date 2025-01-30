package io.greitan.avion.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import io.greitan.avion.velocity.GeyserVoice;
import io.greitan.avion.velocity.utils.Language;
import io.greitan.avion.common.commands.BaseVoiceCommand;
import io.greitan.avion.common.utils.IntegerOperation;
import io.greitan.avion.common.utils.StringOperation;
import io.greitan.avion.common.utils.DoubleStringOperation;
import io.greitan.avion.common.utils.EmptyOperation;

import java.util.List;

public final class VoiceCommand implements SimpleCommand {

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
    public void execute(final Invocation invocation) {
        CommandSource sender = invocation.source();
        // Get the arguments after the command alias
        String[] args = invocation.arguments();

        this.voiceCommand.onCommand(
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
                        GeyserVoice.getConfig().set("config.players." + player.getUsername(), null);
                        return true;
                    }
                    return false;
                }
            }
        );
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("voice.cmd");
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        String[] args = invocation.arguments();
        return voiceCommand.onTabComplete(args, new StringOperation() {
            @Override
            public boolean execute(String permission) {
                return invocation.source().hasPermission(permission);
            }
        });
    }
}
