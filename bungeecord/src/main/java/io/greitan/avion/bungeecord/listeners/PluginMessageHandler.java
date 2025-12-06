package io.greitan.avion.bungeecord.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.fasterxml.jackson.core.JsonProcessingException;

import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.event.PluginMessageEvent;

import io.greitan.avion.common.network.Payloads.PlayerData;
import io.greitan.avion.bungeecord.GeyserVoice;

import java.util.Arrays;
import java.util.List;

public class PluginMessageHandler implements Listener {

    private final GeyserVoice plugin;
    public static final String channelName = "geyservoice:main";

    public PluginMessageHandler(GeyserVoice plugin) {
        this.plugin = plugin;
    }

    public void sendPlayerBindSync(ProxiedPlayer player) {
        boolean isBound = plugin.isConnected() && plugin.getPlayerBinds().getOrDefault(player.getName(), false);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerBindSync");
        out.writeUTF(player.getName());
        out.writeBoolean(isBound);

        for (ServerInfo server : plugin.getProxy().getServers().values()) {
            server.sendData(channelName, out.toByteArray(), true);
        }
        // plugin.getProxy().getServers().entrySet().iterator().next().getValue().sendData(channelName,
        // out.toByteArray(), true);
        plugin.Logger.info("Send PariticipantJoined message");
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        // Ensure the identifier is what you expect before trying to handle the data
        if (!event.getTag().equals(channelName)) {
            return;
        }

        String serverName = "";
        if (event.getSender() instanceof Server) {
            Server backend = (Server) event.getSender();
            serverName = backend.getInfo().getName();
        } else if (event.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) event.getSender();
            serverName = player.getServer().getInfo().getName();
        } else {
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String subchannel = in.readUTF();
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
                
                // Init LocationData if null (it should be based on new Payloads structure, but to be safe)
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

        // Make sure to cancel the event after we finished handling it, else the player
        // will also receive our messages...
        event.setCancelled(true);
    }
}
