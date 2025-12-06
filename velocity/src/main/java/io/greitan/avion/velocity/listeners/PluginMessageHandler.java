package io.greitan.avion.velocity.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.greitan.avion.velocity.GeyserVoice;
import io.greitan.avion.common.network.Payloads.PlayerData;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PluginMessageHandler {

    private final GeyserVoice plugin;
    public static final MinecraftChannelIdentifier channelName = MinecraftChannelIdentifier.from("geyservoice:main");

    public PluginMessageHandler(GeyserVoice plugin) {
        this.plugin = plugin;
    }

    public Boolean sendPlayerBindSync(Player player) {
        boolean isBound = plugin.isConnected() && plugin.getPlayerBinds().getOrDefault(player.getUsername(), false);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerBindSync");
        out.writeUTF(player.getUsername());
        out.writeBoolean(isBound);

        // plugin.getProxy().sendMessage(Component.text("####PlayerBindSync####" +
        // player.getUsername() + "####" + isBound + "####"));
        return trySendMessage(player, out);
    }

    public Boolean trySendMessage(Player player, ByteArrayDataOutput out) {
        Optional<ServerConnection> connection = player.getCurrentServer();
        if (connection.isPresent()) {
            try {
                if (connection.get().sendPluginMessage(channelName, out.toByteArray())) {
                    return true;
                }
            } catch (Exception e) {
                plugin.Logger.error("Failed to send plugin message via player connection: " + e.getMessage());
            }
            try {
                // Fallback to server if player connection fails
                if (connection.get().getServer().sendPluginMessage(channelName, out.toByteArray())) {
                    return true;
                }
            } catch (Exception e) {
                plugin.Logger.error("Failed to send plugin message via server connection: " + e.getMessage());
            }
        }
        // Removed dangerous iteration over all players/servers which caused performance issues on large networks.
        plugin.Logger.debug("Could not send plugin message to backend for player " + player.getUsername());
        return false;
    }

    @Subscribe()
    public void onMessageReceived(PluginMessageEvent event) {
        // Ensure the identifier is what you expect before trying to handle the data
        if (event.getIdentifier() != channelName) {
            return;
        }

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        ServerConnection backend = (ServerConnection) event.getSource();

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String subchannel = in.readUTF();
            String serverName = backend.getServerInfo().getName();
            if (subchannel.equals("PlayerDataList")) {
                String rawPlayerDataList = in.readUTF();
                plugin.Logger.debug("Received playerdatalist: " + rawPlayerDataList);
                try {
                    List<PlayerData> playerDataList = Arrays
                            .asList(GeyserVoice.objectMapper.readValue(rawPlayerDataList, PlayerData[].class));
                    for (PlayerData playerData : playerDataList) {
                        playerData.dimensionId = serverName + "_" + playerData.dimensionId;
                        plugin.playerDataList.put(playerData.playerId, playerData);
                    }
                } catch (JsonProcessingException e) {
                    plugin.Logger.debug("Failed to parse PlayerDataList: " + e.getMessage());
                }
            } else if (subchannel.equals("PlayerData")) {
                PlayerData playerData = new PlayerData();
                playerData.playerId = in.readUTF();
                playerData.dimensionId = in.readUTF();
                
                // Init LocationData
                playerData.location = new io.greitan.avion.common.network.Payloads.LocationData();
                
                playerData.location.x = in.readDouble();
                playerData.location.y = in.readDouble();
                playerData.location.z = in.readDouble();
                playerData.rotation = in.readDouble();
                playerData.echoFactor = in.readDouble();
                playerData.muffled = in.readBoolean();
                playerData.isDead = in.readBoolean();

                playerData.dimensionId = serverName + "_" + playerData.dimensionId;
                plugin.playerDataList.put(playerData.playerId, playerData);
            }
        } catch (Exception e) {
            plugin.Logger.error("Error handling plugin message: " + e.getMessage());
        }

        // Make sure to set the result to Handled, else the player will also receive our
        // messages...
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }
}
