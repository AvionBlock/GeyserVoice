package io.greitan.avion.bungeecord.tasks;

import io.greitan.avion.bungeecord.GeyserVoice;
import io.greitan.avion.bungeecord.utils.Language;
import io.greitan.avion.common.network.Payloads.PacketType;
import io.greitan.avion.common.network.Payloads.PlayerData;
import io.greitan.avion.common.network.Payloads.MCCommPacket;
import io.greitan.avion.common.network.Payloads.UpdatePacket;
import io.greitan.avion.common.network.Payloads.DenyPacket;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatColor;

import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.lang.Runnable;

public class PositionsTask implements Runnable {
    private final GeyserVoice plugin;
    private final String lang;
    private boolean isConnected = false;
    private Integer ReconnectRetries = 0;

    public PositionsTask(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void run() {
        isConnected = plugin.isConnected();
        String host = plugin.getHost();
        int port = plugin.getPort();
        String token = plugin.getToken();
        String link = "http://" + host + ":" + port;

        if (isConnected) {
            if (host != null && token != null) {
                UpdatePacket updatePacket = new UpdatePacket();
                updatePacket.Token = token;
                updatePacket.Players = getPlayerDataList(plugin.playerDataList);

                MCCommPacket response = plugin.network.sendPostRequest(link, updatePacket);
                if (response != null) {
                    if (response.PacketId == PacketType.AckUpdate.ordinal()) {
                        // AckUpdatePacket packetData = plugin.objectMapper.convertValue(response,
                        // AckUpdatePacket.class);
                        // You can do stuff with the AckUpdate packet data here...
                        return;
                    } else if (response.PacketId == PacketType.Deny.ordinal()) {
                        DenyPacket packetData = GeyserVoice.objectMapper.convertValue(response, DenyPacket.class);
                        plugin.Logger.error(packetData.Reason);
                        if (!packetData.Reason.equals("Invalid Token!")) {
                            plugin.setNotConnected();
                            // http.cancelAll(packetData.Reason);
                            cancel();
                            return;
                        }
                    } else {
                        return;
                    }
                }
                if (!isConnected)
                    return; // do nothing.

                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-lost"));
                plugin.setNotConnected();

                if (GeyserVoice.getConfig().getBoolean("config.auto-reconnect")) {
                    if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                        plugin.getProxy().broadcast(
                                new ComponentBuilder(Language.getMessage(lang, "plugin-connection-lost-reconnect"))
                                        .color(ChatColor.RED).create());
                    }
                    ReconnectRetries = 0;
                    Reconnect();
                    return;
                }
                if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    plugin.getProxy()
                            .broadcast(new ComponentBuilder(Language.getMessage(lang, "plugin-connection-lost"))
                                    .color(ChatColor.RED).create());
                }
                cancel();
            }
        }
    }

    public List<PlayerData> getPlayerDataList(Map<String, PlayerData> allPlayerDataList) {
        List<PlayerData> playerDataList = new ArrayList<>();

        for (String playerId : allPlayerDataList.keySet()) {
            playerDataList.add(allPlayerDataList.get(playerId));
        }

        return playerDataList;
    }

    private Boolean Reconnect() {
        if (ReconnectRetries < 5) {
            ReconnectRetries++;

            plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-attempt").replace("$attempt",
                    ReconnectRetries.toString()));

            if (plugin.reconnect(true)) {
                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-success"));

                if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    plugin.getProxy().broadcast(
                            new ComponentBuilder(Language.getMessage(lang, "plugin-connection-reconnecting-success"))
                                    .color(ChatColor.GREEN).create());
                }
                return true;
            } else {
                if (ReconnectRetries < 5) {
                    plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-failed-retry"));
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {
                    }
                    return Reconnect();
                }
                plugin.Logger.error(Language.getMessage(lang, "plugin-connection-reconnecting-failed"));

                if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    plugin.getProxy().broadcast(
                            new ComponentBuilder(Language.getMessage(lang, "plugin-connection-reconnecting-failed"))
                                    .color(ChatColor.RED).create());
                }
                cancel();
            }
        }
        return false;
    }

    private void cancel() {
        plugin.getTaskRunner().cancel();
    }
}
