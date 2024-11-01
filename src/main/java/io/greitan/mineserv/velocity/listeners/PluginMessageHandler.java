package io.greitan.mineserv.velocity.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.kyori.adventure.text.Component;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.greitan.mineserv.velocity.GeyserVoice;
import io.greitan.mineserv.common.network.Payloads.PlayerData;

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

        // plugin.getProxy().sendMessage(Component.text("####PlayerBindSync####" + player.getUsername() + "####" + isBound + "####"));
        return trySendMessage(player, out);
    }

    public Boolean trySendMessage(Player player, ByteArrayDataOutput out) {
        Optional<ServerConnection> connection = player.getCurrentServer();
        if (connection.isPresent()) {
            // First we try using our player
            try {
                if (connection.get().sendPluginMessage(channelName, out.toByteArray())) {
                    return true;
                }
            }
            catch (Exception e) {}
            try {
                // Else we try using any connected player of this server
                if (connection.get().getServer().sendPluginMessage(channelName, out.toByteArray())) {
                    return true;
                }
            }
            catch (Exception e) {}
        }
        // Else we try any other random player... which we shouldn't do btw...
        for (Player otherPlayer : plugin.getProxy().getAllPlayers()) {
            connection = otherPlayer.getCurrentServer();
            if (connection.isPresent()) {
                try {
                    if (connection.get().sendPluginMessage(channelName, out.toByteArray())) {
                        return true;
                    }
                }
                catch (Exception e) {}
            }
        }
        // At last we just use any server...
        for (RegisteredServer server : plugin.getProxy().getAllServers()) {
            server.sendPluginMessage(channelName, out.toByteArray());
        }
        return true;
        //return plugin.getProxy().getAllServers().iterator().next().sendPluginMessage(channelName, out.toByteArray());
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

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();
        String serverName = backend.getServerInfo().getName();
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

        // Make sure to set the result to Handled, else the player will also receive our messages...
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }
}
