package io.greitan.avion.common.commands;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.greitan.avion.common.BaseGeyserVoice;
import io.greitan.avion.common.utils.IntegerOperation;
import io.greitan.avion.common.utils.StringOperation;
import io.greitan.avion.common.utils.DoubleStringOperation;
import io.greitan.avion.common.utils.EmptyOperation;

public class BaseVoiceCommand {
    private final BaseGeyserVoice plugin;

    public <T extends BaseGeyserVoice> BaseVoiceCommand(T plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(String[] args, boolean isConnected, boolean isPlayer, StringOperation hasPermission, DoubleStringOperation sendMessage, IntegerOperation bindUser, EmptyOperation clearAutoBind) {
        if (args.length == 0) {
            sendMessage.execute("cmd-invalid-args", "red");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "connect" -> handleConnect(args, isPlayer, hasPermission, sendMessage);
            case "reconnect" -> handleReconnect(args, isPlayer, hasPermission, sendMessage);
            case "disconnect" -> handleDisconnect(isConnected, hasPermission, sendMessage);
            case "settings" -> handleSettings(isConnected, hasPermission, sendMessage);
            case "bind" -> handleBind(args, isConnected, isPlayer, hasPermission, sendMessage, bindUser);
            case "bindfake" -> handleBindFake(args, isConnected, hasPermission, sendMessage);
            case "updatefake" -> handleUpdateFake(args, isConnected, isPlayer, hasPermission, sendMessage);
            case "clearautobind" -> handleClearAutoBind(isConnected, isPlayer, hasPermission, sendMessage, clearAutoBind);
            case "reload" -> handleReload(hasPermission, sendMessage);
            default -> sendMessage.execute("cmd-invalid-args", "red");
        }
        return true;
    }

    private void handleConnect(String[] args, boolean isPlayer, StringOperation hasPermission, DoubleStringOperation sendMessage) {
        if (!hasPermission.execute("voice.connect")) return;

        if (args.length < 4) {
            sendMessage.execute("commands.connect.invalid-args", "red");
            return;
        }

        String host = args[1];
        int port = parsePort(args[2]);
        String key = args[3];

        if (port == -1) {
             sendMessage.execute("commands.connect.invalid-args", "red");
             return;
        }

        sendMessage.execute("plugin-connect-connecting", "yellow");
        boolean connected = plugin.connect(host, port, key);

        if (isPlayer) {
            if (connected) sendMessage.execute("plugin-connect-connected", "green");
            else sendMessage.execute("plugin-connect-failed", "red");
        }
    }

    private void handleReconnect(String[] args, boolean isPlayer, StringOperation hasPermission, DoubleStringOperation sendMessage) {
        if (!hasPermission.execute("voice.reconnect")) return;

        boolean force = args.length >= 2 && Boolean.parseBoolean(args[1]);

        sendMessage.execute("plugin-connect-connecting", "yellow");
        boolean connected = plugin.reconnect(force);

        if (isPlayer) {
            if (connected) sendMessage.execute("plugin-connect-connected", "green");
            else sendMessage.execute("plugin-connect-failed", "red");
        }
    }

    private void handleDisconnect(boolean isConnected, StringOperation hasPermission, DoubleStringOperation sendMessage) {
        if (!hasPermission.execute("voice.disconnect")) return;

        sendMessage.execute("commands.disconnect.disconnecting", "yellow");
        if (!isConnected) {
            sendMessage.execute("commands.disconnect.already-disconnected", "red");
            return;
        }
        plugin.disconnect("Disconnection Request.");
    }

    private void handleSettings(boolean isConnected, StringOperation hasPermission, DoubleStringOperation sendMessage) {
        if (!hasPermission.execute("voice.settings") || !isConnected) return;
        // Future implementation for menu or args
        plugin.updateSettings(1, true, true);
    }

    private void handleBind(String[] args, boolean isConnected, boolean isPlayer, StringOperation hasPermission, DoubleStringOperation sendMessage, IntegerOperation bindUser) {
        if (!hasPermission.execute("voice.bind") || !isConnected) return;
        if (!isPlayer) {
            sendMessage.execute("cmd-not-player", "red");
            return;
        }
        if (args.length < 2) {
            sendMessage.execute("commands.bind.invalid-args", "red");
            return;
        }

        int bindKey = parseInt(args[1]);
        if (bindKey == -1) {
             sendMessage.execute("commands.bind.invalid-args", "red");
             return;
        }

        sendMessage.execute("commands.bind.binding", "yellow");
        if (bindUser.execute(bindKey)) {
            sendMessage.execute("commands.bind.binded", "green");
        } else {
            sendMessage.execute("commands.bind.failed", "red");
        }
    }

    private void handleBindFake(String[] args, boolean isConnected, StringOperation hasPermission, DoubleStringOperation sendMessage) {
        if (!hasPermission.execute("voice.bindfake") || !isConnected) return;
        if (args.length < 3) {
            sendMessage.execute("commands.bind.fake-invalid-args", "red");
            return;
        }

        int bindKey = parseInt(args[1]);
        if (bindKey == -1) {
            sendMessage.execute("commands.bind.fake-invalid-args", "red");
            return;
        }

        sendMessage.execute("commands.bind.binding-fake", "yellow");
        if (plugin.bindFake(bindKey, args[2])) {
            sendMessage.execute("commands.bind.binded", "green");
        } else {
            sendMessage.execute("commands.bind.failed", "red");
        }
    }

    private void handleUpdateFake(String[] args, boolean isConnected, boolean isPlayer, StringOperation hasPermission, DoubleStringOperation sendMessage) {
         if (!hasPermission.execute("voice.bindfake") || !isConnected) return;
         if (!isPlayer) {
             sendMessage.execute("cmd-not-player", "red");
             return;
         }
         // Placeholder logic retained from original code
         sendMessage.execute("commands.updatefake.invalid-args", "red");
    }

    private void handleClearAutoBind(boolean isConnected, boolean isPlayer, StringOperation hasPermission, DoubleStringOperation sendMessage, EmptyOperation clearAutoBind) {
        if (!hasPermission.execute("voice.bind") || !isConnected) return;
        if (!isPlayer) {
            sendMessage.execute("cmd-not-player", "red");
            return;
        }
        clearAutoBind.execute();
        sendMessage.execute("commands.clearautobind", "green");
    }

    private void handleReload(StringOperation hasPermission, DoubleStringOperation sendMessage) {
        if (!hasPermission.execute("voice.reload")) return;
        plugin.reload();
        sendMessage.execute("commands.reload", "green");
    }

    public List<String> onTabComplete(String[] args, StringOperation hasPermission) {
        if (args.length <= 1) {
            return List.of("connect", "reconnect", "disconnect", "settings", "bind", "bindfake", "updatefake", "clearautobind", "reload")
                    .stream()
                    .filter(cmd -> (args.length == 0 || cmd.startsWith(args[0])) && checkTabPermission(cmd, hasPermission))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();
        if (args.length == 2) {
             if (sub.equals("connect") && hasPermission.execute("voice.connect")) return List.of("host");
             if (sub.equals("reconnect") && hasPermission.execute("voice.reconnect")) return List.of("true", "false");
        }
        return List.of();
    }

    private boolean checkTabPermission(String cmd, StringOperation hasPermission) {
        String perm = switch (cmd) {
            case "clearautobind" -> "voice.bind";
            case "updatefake" -> "voice.bindfake";
            default -> "voice." + cmd;
        };
        return hasPermission.execute(perm);
    }

    private int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseInt(String num) {
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
