package io.greitan.avion.common.utils;

import java.util.List;
import java.util.stream.Collectors;

import io.greitan.avion.common.utils.HasPermissionOperation;

public class VoiceCommandCompletions {
    public static List<String> execute(String[] args, HasPermissionOperation hasPermission) {
        List<String> completions = List.of();

        // Main command arguments.
        if (args.length == 1 || args.length == 0) {
            List<String> options = List.of("bind", "setup", "connect", "reload", "settings");
            completions = options.stream().filter(val -> (args.length == 0 || val.startsWith(args[0])) && hasPermission.execute("voice." + val)).collect(Collectors.toList());
        }

        // Setup command arguments.
        if (args.length == 2 && args[0].equalsIgnoreCase("setup") && hasPermission.execute("voice.setup")) {
            List<String> options = List.of("host port key");
            completions = options.stream().filter(val -> val.startsWith(args[1])).collect(Collectors.toList());
        }

        // Connect command arguments.
        if (args.length == 2 && args[0].equalsIgnoreCase("connect") && hasPermission.execute("voice.connect")) {
            List<String> options = List.of("true", "false");
            completions = options.stream().filter(val -> val.startsWith(args[1])).collect(Collectors.toList());
        }

        return completions;
    }
}
