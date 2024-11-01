package io.greitan.mineserv.bungeecord.listeners;

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

import io.greitan.mineserv.common.network.Payloads.PlayerData;
import io.greitan.mineserv.bungeecord.GeyserVoice;

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
        //plugin.getProxy().getServers().entrySet().iterator().next().getValue().sendData(channelName, out.toByteArray(), true);
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

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();
        if (subchannel.equals("PlayerDataList")) {
            String rawPlayerDataList = in.readUTF();
            plugin.Logger.debug("Received playerdatalist: " + rawPlayerDataList);
            try {
                List<PlayerData> playerDataList = Arrays.asList(plugin.objectMapper.readValue(rawPlayerDataList, PlayerData[].class));
                for (PlayerData playerData : playerDataList) {
                    playerData.DimensionId = serverName + "_" + playerData.DimensionId;
                    plugin.playerDataList.put(playerData.PlayerId, playerData);
                }
            } catch (JsonProcessingException e) {}
        }
        else if (subchannel.equals("PlayerData")) {
            PlayerData playerData = new PlayerData();
            playerData.PlayerId = in.readUTF();
            playerData.DimensionId = in.readUTF();
            playerData.Location.x = in.readDouble();
            playerData.Location.y = in.readDouble();
            playerData.Location.z = in.readDouble();
            playerData.Rotation = in.readDouble();
            playerData.EchoFactor = in.readDouble();
            playerData.Muffled = in.readBoolean();
            playerData.IsDead = in.readBoolean();

            playerData.DimensionId = serverName + "_" + playerData.DimensionId;
            plugin.playerDataList.put(playerData.PlayerId, playerData);
        }

        // Make sure to cancel the event after we finished handling it, else the player will also receive our messages...
        event.setCancelled(true);
    }
}
