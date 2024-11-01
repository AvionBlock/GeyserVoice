package io.greitan.mineserv.paper.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import io.greitan.mineserv.common.network.Payloads.PlayerData;
import io.greitan.mineserv.paper.GeyserVoice;

import net.kyori.adventure.text.Component;

import java.util.List;

public class PluginMessageHandler implements PluginMessageListener {

    private final GeyserVoice plugin;
    public static final String channelName = "geyservoice:main";

    public PluginMessageHandler(GeyserVoice plugin) {
        this.plugin = plugin;
    }

    public void sendPlayerData(Player player, PlayerData playerData) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerData");
        out.writeUTF(playerData.PlayerId);
        out.writeUTF(playerData.DimensionId);
        out.writeDouble(playerData.Location.x);
        out.writeDouble(playerData.Location.y);
        out.writeDouble(playerData.Location.z);
        out.writeDouble(playerData.Rotation);
        out.writeDouble(playerData.EchoFactor);
        out.writeBoolean(playerData.Muffled);
        out.writeBoolean(playerData.IsDead);
        player.sendPluginMessage(plugin, channelName, out.toByteArray());
    }

    public void sendPlayerDataList(Server server, List<PlayerData> playerDataList) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerDataList");
            out.writeUTF(plugin.objectMapper.writeValueAsString(playerDataList));
            server.sendPluginMessage(plugin, channelName, out.toByteArray());
            // The response will be handled in onPluginMessageReceived
        } catch (JsonProcessingException e) {}
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(channelName)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (subchannel.equals("PlayerBindSync")) {
            String username = in.readUTF();
            boolean isBound = in.readBoolean();
            if (plugin.getPlayerBinds().getOrDefault(username, false) != isBound) {
                if (isBound) {
                    plugin.Logger.info(username + " has joined the voicechat!");
                } else {
                    plugin.Logger.info(username + " has left the voicechat!");
                }
                plugin.getPlayerBinds().put(username, isBound);
            }
        }
    }
}
