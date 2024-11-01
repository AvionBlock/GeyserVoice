package io.greitan.mineserv.velocity.commands;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.lang.NumberFormatException;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import io.greitan.mineserv.velocity.GeyserVoice;
import io.greitan.mineserv.velocity.utils.Language;

public final class VoiceCommand implements SimpleCommand {

    private final GeyserVoice plugin;
    private final String lang;
    private boolean isConnected = false;

    // Get the plugin and lang interfaces.
    public VoiceCommand(GeyserVoice plugin, String lang)
    {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void execute(final Invocation invocation)
    {
        CommandSource sender = invocation.source();
        // Get the arguments after the command alias
        String[] args = invocation.arguments();

        isConnected = plugin.isConnected();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length >= 1)
            {
                // Bind command - bind player.
                if (args[0].equalsIgnoreCase("bind") && player.hasPermission("voice.bind") && isConnected)
                {
                    if(args.length >= 2 && Objects.nonNull(args[1]))
                    {
                        int bindKey;
                        try {
                            bindKey = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(Component.text(Language.getMessage(lang, "cmd-invalid-args")).color(NamedTextColor.RED));
                            return;
                        }
                        /*
                        if (bindKey == 0) {
                            player.sendMessage("Here is your open key!");
                            return;
                        }
                        */
                        Boolean isBound = plugin.bind(bindKey, player);
                        if(isBound){
                            player.sendMessage(Component.text(Language.getMessage(lang, "cmd-bind-connect")).color(NamedTextColor.AQUA));
                        } else {
                            player.sendMessage(Component.text(Language.getMessage(lang, "cmd-bind-disconnect")).color(NamedTextColor.RED));
                        }
                    }
                    else
                    {
                        player.sendMessage(Component.text(Language.getMessage(lang, "cmd-invalid-args")).color(NamedTextColor.RED));
                    }
                }
                // Setup command - setup the configuration.
                else if (args[0].equalsIgnoreCase("setup") && player.hasPermission("voice.setup"))
                {
                    String newHost = args[1];
                    String newPort = args[2];
                    String newKey = args[3];

                    if(Objects.nonNull(newHost) && Objects.nonNull(newPort) && Objects.nonNull(newKey)){
                        plugin.getConfig().set("config.host", newHost);
                        plugin.getConfig().set("config.port", newPort);
                        plugin.getConfig().set("config.server-key", newKey);
                        plugin.saveConfig();
                        plugin.reloadConfig();
                        plugin.reload();

                        player.sendMessage(Component.text(Language.getMessage(lang, "cmd-setup-success")).color(NamedTextColor.AQUA));
                    } else {
                        player.sendMessage(Component.text(Language.getMessage(lang, "cmd-setup-invalid-data")).color(NamedTextColor.RED));
                    }
                }
                // Connect command - connect to the VoiceCraft server.
                else if (args[0].equalsIgnoreCase("connect") && player.hasPermission("voice.connect"))
                {
                    if(Objects.nonNull(args[1])){
                        Boolean force = Boolean.valueOf(args[1]);

                        Boolean connected = plugin.connect(force);
                        if(connected){
                            player.sendMessage(Component.text(Language.getMessage(lang, "plugin-connect-connect")).color(NamedTextColor.AQUA));
                        } else {
                            player.sendMessage(Component.text(Language.getMessage(lang, "plugin-connect-disconnect")).color(NamedTextColor.RED));
                        }
                    }
                    else
                    {
                        player.sendMessage(Component.text(Language.getMessage(lang, "cmd-invalid-args")).color(NamedTextColor.RED));
                    }
                }
                // Reload command - reload the configs.
                else if (args[0].equalsIgnoreCase("reload") && player.hasPermission("voice.reload"))
                {
                    plugin.reload();
                    player.sendMessage(Component.text(Language.getMessage(lang, "cmd-reload")).color(NamedTextColor.GREEN));
                }
                else if (args[0].equalsIgnoreCase("settings") && player.hasPermission("voice.settings"))
                {
                    int proximityDistance = 1;
                    Boolean proximityToggle = true;
                    Boolean voiceEffects = true;

                    plugin.updateSettings(proximityDistance, proximityToggle, voiceEffects);
                }
                // Command select invalid.
                else
                {
                    player.sendMessage(Component.text(Language.getMessage(lang, "cmd-invalid-args")).color(NamedTextColor.RED));
                }
            }
        }
        // Commands runned by console.
        else if(args.length >= 1)
        {
            // Reload command - reload the configs.
            if (args[0].equalsIgnoreCase("reload"))
            {
                plugin.reload();
                plugin.Logger.log(Component.text(Language.getMessage(lang, "cmd-reload")).color(NamedTextColor.GREEN));
            }
            // Command not for console.
            else
            {
                plugin.getProxy().sendMessage(Component.text("####CheckMessaging####"));

                sender.sendMessage(Component.text(Language.getMessage(lang, "cmd-not-player")).color(NamedTextColor.RED));
            }
        }
        // Invalid command arguments.
        else
        {
            sender.sendMessage(Component.text(Language.getMessage(lang, "cmd-invalid-args")).color(NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return true;
        //return invocation.source().hasPermission("command.test");
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        String[] args = invocation.arguments();
        return CompletableFuture.supplyAsync(()-> {
            List<String> completions = List.of();

            // Main command arguments.
            if(args.length == 1){
                List<String> options = List.of("bind", "setup", "connect", "reload");
                completions = options.stream().filter(val -> val.startsWith(args[0])).collect(Collectors.toList());
            }

            // Setup command arguments.
            if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
                List<String> options = List.of("host port key");
                completions = options.stream().filter(val -> val.startsWith(args[1])).collect(Collectors.toList());
            }

            // Connect command arguments.
            if (args.length == 2 && args[0].equalsIgnoreCase("connect")) {
                List<String> options = List.of("true", "false");
                completions = options.stream().filter(val -> val.startsWith(args[1])).collect(Collectors.toList());
            }

            return completions;
        });
    }
}
