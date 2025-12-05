package io.greitan.avion.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import io.greitan.avion.common.commands.BaseVoiceCommand;
import io.greitan.avion.common.utils.IntegerOperation;
import io.greitan.avion.common.utils.StringOperation;
import io.greitan.avion.common.utils.DoubleStringOperation;
import io.greitan.avion.common.utils.EmptyOperation;
import io.greitan.avion.fabric.FabricGeyserVoice;

public class FabricVoiceCommand {
    private static final BaseVoiceCommand voiceCommand = new BaseVoiceCommand(FabricGeyserVoice.getInstance());

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("voice")
            .requires(source -> source.hasPermissionLevel(2)) // OP level 2
            .then(CommandManager.literal("connect")
                .then(CommandManager.argument("host", StringArgumentType.string())
                    .then(CommandManager.argument("port", IntegerArgumentType.integer())
                        .then(CommandManager.argument("key", StringArgumentType.string())
                            .executes(context -> executeCommand(context, new String[]{"connect", StringArgumentType.getString(context, "host"), String.valueOf(IntegerArgumentType.getInteger(context, "port")), StringArgumentType.getString(context, "key")}))
                        )
                    )
                )
            )
            .then(CommandManager.literal("reconnect")
                 .then(CommandManager.argument("force", BoolArgumentType.bool())
                    .executes(context -> executeCommand(context, new String[]{"reconnect", String.valueOf(BoolArgumentType.getBool(context, "force"))}))
                 )
                 .executes(context -> executeCommand(context, new String[]{"reconnect", "false"}))
            )
            .then(CommandManager.literal("disconnect")
                .executes(context -> executeCommand(context, new String[]{"disconnect"}))
            )
            .then(CommandManager.literal("reload")
                .executes(context -> executeCommand(context, new String[]{"reload"}))
            )
            // Bind commands require player
            .then(CommandManager.literal("bind")
                .requires(source -> source.isExecutedByPlayer())
                .then(CommandManager.argument("key", IntegerArgumentType.integer())
                     .executes(context -> executeCommand(context, new String[]{"bind", String.valueOf(IntegerArgumentType.getInteger(context, "key"))}))
                )
            )
        );
    }

    private static int executeCommand(CommandContext<ServerCommandSource> context, String[] args) {
        ServerCommandSource source = context.getSource();
        boolean isPlayer = source.isExecutedByPlayer();
        
        voiceCommand.onCommand(
            args,
            FabricGeyserVoice.getInstance().isConnected(),
            isPlayer,
            new StringOperation() {
                @Override
                public boolean execute(String permission) {
                    return source.hasPermissionLevel(2); // Simplified permission check
                }
            },
            new DoubleStringOperation() {
                @Override
                public void execute(String text, String colorName) {
                    Formatting color = Formatting.WHITE;
                    if (colorName.equals("red")) color = Formatting.RED;
                    else if (colorName.equals("green")) color = Formatting.GREEN;
                    else if (colorName.equals("yellow")) color = Formatting.YELLOW;
                    
                    source.sendFeedback(() -> Text.literal(text).formatted(color), false);
                }
            },
            new IntegerOperation() {
                @Override
                public boolean execute(int key) {
                    if (isPlayer) {
                        String playerName = source.getName();
                        // We need to implement bind(key, player) in FabricGeyserVoice but since we don't have easy Player object access in BaseVoiceCommand logic directly matching Bukkit
                        // We will use bindFake logic or similar binding but with real player UUID/Name
                        return FabricGeyserVoice.getInstance().bindFake(key, playerName);
                    }
                    return false;
                }
            },
            new EmptyOperation() {
                @Override
                public boolean execute() {
                    return true; 
                }
            }
        );
        return 1;
    }
}
