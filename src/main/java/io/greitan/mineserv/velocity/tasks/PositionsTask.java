package io.greitan.mineserv.velocity.tasks;

import io.greitan.mineserv.velocity.GeyserVoice;
import io.greitan.mineserv.velocity.utils.Language;
import io.greitan.mineserv.common.network.Payloads.PacketType;
import io.greitan.mineserv.common.network.Payloads.PlayerData;
import io.greitan.mineserv.common.network.Payloads.MCCommPacket;
import io.greitan.mineserv.common.network.Payloads.UpdatePacket;
import io.greitan.mineserv.common.network.Payloads.AckUpdatePacket;
import io.greitan.mineserv.common.network.Payloads.DenyPacket;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PositionsTask {
    private final GeyserVoice plugin;
    private final String lang;
    private boolean isConnected = false;
    private Integer ReconnectRetries = 0;

    public PositionsTask(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public Boolean run() {
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
                        //AckUpdatePacket packetData = plugin.objectMapper.convertValue(response, AckUpdatePacket.class);
                        //You can do stuff with the AckUpdate packet data here...
                        return true;
                    } else if (response.PacketId == PacketType.Deny.ordinal()) {
                        DenyPacket packetData = plugin.objectMapper.convertValue(response, DenyPacket.class);
                        plugin.Logger.error(packetData.Reason);
                        if (!packetData.Reason.equals("Invalid Token!")) {
                            plugin.setNotConnected();
                            // http.cancelAll(packetData.Reason);
                            // cancel();
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                if (!isConnected) return true; //do nothing.

                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-lost"));
                plugin.setNotConnected();

                if (plugin.getConfig().getBoolean("config.auto-reconnect")) {
                    if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                        plugin.getProxy().sendMessage(Component.text(Language.getMessage(lang, "plugin-connection-lost-reconnect")).color(NamedTextColor.RED));
                    }
                    ReconnectRetries = 0;
                    return Reconnect();
                }
                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    plugin.getProxy().sendMessage(Component.text(Language.getMessage(lang, "plugin-connection-lost")).color(NamedTextColor.RED));
                }
                return false;
            }
        }
        return true;
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

            plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-attempt").replace("$attempt", ReconnectRetries.toString()));

            if (plugin.connect(true)) {
                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-success"));

                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    plugin.getProxy().sendMessage(Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-success")).color(NamedTextColor.GREEN));
                }
                return true;
            } else {
                if (ReconnectRetries < 5) {
                    plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-failed-retry"));
                    try {
                         TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {}
                    return Reconnect();
                }
                plugin.Logger.error(Language.getMessage(lang, "plugin-connection-reconnecting-failed"));

                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    plugin.getProxy().sendMessage(Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-failed")).color(NamedTextColor.RED));
                }
            }
        }
        return false;
    }
}
