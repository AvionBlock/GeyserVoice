package io.greitan.avion.common.commands;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.lang.NumberFormatException;

import io.greitan.avion.common.BaseGeyserVoice;
import io.greitan.avion.common.utils.IntegerOperation;
import io.greitan.avion.common.utils.StringOperation;
import io.greitan.avion.common.utils.DoubleStringOperation;
import io.greitan.avion.common.utils.EmptyOperation;

public class BaseVoiceCommand {
    private final BaseGeyserVoice plugin;

    // Get the plugin and lang interfaces.
    public <T extends BaseGeyserVoice> BaseVoiceCommand(T plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(String[] args, boolean isConnected, boolean isPlayer, StringOperation hasPermission, DoubleStringOperation sendMessage, IntegerOperation bindUser, EmptyOperation clearAutoBind) {
        if (args.length >= 1) {
            // Connect command - connect to the server.
            if (args[0].equalsIgnoreCase("connect") && hasPermission.execute("voice.connect")) {
                if (args.length >= 4) {
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
                        sendMessage.execute("plugin-connect-connecting", "yellow");
                        Boolean connected = plugin.connect(newHost, newPort, newKey);

                        // Console will always get a message, so only send message when player sending command!
                        if (isPlayer) {
                            if (connected)
                                sendMessage.execute("plugin-connect-connected", "green");
                            else
                                sendMessage.execute("plugin-connect-failed", "red");
                        }
                        return true;
                    }
                }
                sendMessage.execute("commands.connect.invalid-args", "red");
            }
            // Reconnect command - (re)connect to the VoiceCraft server.
            else if (args[0].equalsIgnoreCase("reconnect") && hasPermission.execute("voice.reconnect")) {
                Boolean force = false;
                if (args.length >= 2 && Objects.nonNull(args[1])) {
                    force = Boolean.valueOf(args[1]);
                }

                sendMessage.execute("plugin-connect-connecting", "yellow");
                Boolean connected = plugin.reconnect(force);
                // Console will always get a message, so only send message when player sending command!
                if (isPlayer) {
                    if (connected)
                        sendMessage.execute("plugin-connect-connected", "green");
                    else 
                        sendMessage.execute("plugin-connect-failed", "red");
                }
            }
            // Disconnect command - disconnect from the VoiceCraft server.
            else if (args[0].equalsIgnoreCase("disconnect") && hasPermission.execute("voice.disconnect")) {
                sendMessage.execute("commands.disconnect.disconnecting", "yellow");
                if (!isConnected) {
                    sendMessage.execute("commands.disconnect.already-disconnected", "red");
                    return true;
                }

                plugin.disconnect("Disconnection Request.");
            }
            // Settings command - allows to change settings?
            else if (args[0].equalsIgnoreCase("settings") && hasPermission.execute("voice.settings") && isConnected) {
                int proximityDistance = 1;
                Boolean proximityToggle = true;
                Boolean voiceEffects = true;

                // Show a menu?
                plugin.updateSettings(proximityDistance, proximityToggle, voiceEffects);
            }
            // Bind command - bind player.
            else if (args[0].equalsIgnoreCase("bind") && hasPermission.execute("voice.bind") && isConnected) {
                if (isPlayer) {
                    if (args.length >= 2 && Objects.nonNull(args[1])) {
                        int bindKey;
                        try {
                            bindKey = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sendMessage.execute("commands.bind.invalid-args", "red");
                            return true;
                        }

                        sendMessage.execute("commands.bind.binding", "yellow");
                        Boolean isBound = bindUser.execute(bindKey);
                        if (isBound) {
                            sendMessage.execute("commands.bind.binded", "green");
                        } else {
                            sendMessage.execute("commands.bind.failed", "red");
                        }
                    } else {
                        sendMessage.execute("commands.bind.invalid-args", "red");
                    }
                }
                // Player only command.
                else {
                    sendMessage.execute("cmd-not-player", "red");
                }
            }
            // Bind fake command - bind fake player.
            else if (args[0].equalsIgnoreCase("bindfake") && hasPermission.execute("voice.bindfake") && isConnected) {
                if (args.length >= 3 && Objects.nonNull(args[1]) && Objects.nonNull(args[2])) {
                    int bindKey;
                    try {
                        bindKey = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sendMessage.execute("commands.bind.fake-invalid-args", "red");
                        return true;
                    }
                    String name = args[2];

                    sendMessage.execute("commands.bind.binding-fake", "yellow");
                    Boolean isBound = plugin.bindFake(bindKey, name);
                    if (isBound) {
                        sendMessage.execute("commands.bind.binded", "green");
                    } else {
                        sendMessage.execute("commands.bind.failed", "red");
                    }
                } else {
                    sendMessage.execute("commands.bind.fake-invalid-args", "red");
                }
            }
            // Update fake command - update fake player.
            else if (args[0].equalsIgnoreCase("updatefake") && hasPermission.execute("voice.bindfake") && isConnected) {
                if (isPlayer) {
                    if (args.length >= 2 && Objects.nonNull(args[1])) {
                        int bindKey;
                        try {
                            bindKey = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sendMessage.execute("commands.updatefake.invalid-args", "red");
                            return true;
                        }

                        // Not doing this now, since this isn't needed nor simple to implement
                        sendMessage.execute("commands.updatefake.updating", "yellow");
                        Boolean updated = bindKey == -1; // updateFake.execute(bindKey);
                        if (updated) {
                            sendMessage.execute("commands.updatefake.updated", "green");
                        } else {
                            sendMessage.execute("commands.updatefake.failed", "red");
                        }
                    } else {
                        sendMessage.execute("commands.updatefake.invalid-args", "red");
                    }
                } else {
                    sendMessage.execute("cmd-not-player", "red");
                }
            }
            // Clear auto bind command - clear auto bind player.
            else if (args[0].equalsIgnoreCase("clearautobind") && hasPermission.execute("voice.bind") && isConnected) {
                if (isPlayer) {
                    clearAutoBind.execute();
                    sendMessage.execute("commands.clearautobind", "green");
                }
                // Player only command.
                else {
                    sendMessage.execute("cmd-not-player", "red");
                }
            }
            // Reload command - reload the configs.
            else if (args[0].equalsIgnoreCase("reload") && hasPermission.execute("voice.reload")) {
                plugin.reload();
                sendMessage.execute("commands.reload", "green");
            }
            else {
                sendMessage.execute("cmd-invalid-args", "red");
            }
        }
        // Invalid command arguments.
        else {
            sendMessage.execute("cmd-invalid-args", "red");
        }
        return true;
    }

    public List<String> onTabComplete(String[] args, StringOperation hasPermission) {
        List<String> completions = List.of();

        // Main command arguments.
        if (args.length == 1 || args.length == 0) {
            List<String> options = List.of("connect", "reconnect", "disconnect", "settings", "bind", "bindfake", "updatefake", "clearautobind", "reload");
            completions = options.stream().filter(val -> (args.length == 0 || val.startsWith(args[0])) && hasPermission.execute(val == "clearautobind" ? "voice.bind" : (val == "updatefake" ? "voice.bindfake" : "voice." + val))).collect(Collectors.toList());
        }

        // Setup command arguments.
        if (args.length == 2 && args[0].equalsIgnoreCase("connect") && hasPermission.execute("voice.connect")) {
            List<String> options = List.of("host port key");
            completions = options.stream().filter(val -> val.startsWith(args[1])).collect(Collectors.toList());
        }

        // Connect command arguments.
        if (args.length == 2 && args[0].equalsIgnoreCase("reconnect") && hasPermission.execute("voice.reconnect")) {
            List<String> options = List.of("true", "false");
            completions = options.stream().filter(val -> val.startsWith(args[1])).collect(Collectors.toList());
        }

        return completions;
    }
}
