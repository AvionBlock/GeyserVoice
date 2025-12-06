package io.greitan.avion.bungeecord.tasks;

import io.greitan.avion.bungeecord.GeyserVoice;
import io.greitan.avion.bungeecord.utils.Language;
import io.greitan.avion.common.network.Payloads.PacketType;
import io.greitan.avion.common.network.Payloads.PlayerData;
import io.greitan.avion.common.network.Payloads.UpdatePacket;
import io.greitan.avion.common.network.Payloads.DenyPacket;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatColor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class PositionsTask implements Runnable {
    private final GeyserVoice plugin;
    private final String lang;
    private final AtomicBoolean isRequestPending = new AtomicBoolean(false);
    private int reconnectRetries = 0;

    public PositionsTask(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void run() {
        if (!plugin.isConnected()) return;

        if (isRequestPending.get()) return;

        String host = plugin.getHost();
        int port = plugin.getPort();
        String token = plugin.getToken();
        String link = "http://" + host + ":" + port;

        if (host != null && token != null) {
            List<PlayerData> players = getPlayerDataList(plugin.playerDataList);

            UpdatePacket updatePacket = new UpdatePacket();
            updatePacket.token = token;
            updatePacket.players = players;

            isRequestPending.set(true);
            plugin.network.sendPostRequestAsync(link, updatePacket)
                .thenAccept(response -> {
                    if (response != null) {
                        if (response.getPacketId() == PacketType.AckUpdate.ordinal()) {
                            // OK
                        } else if (response.getPacketId() == PacketType.Deny.ordinal()) {
                            DenyPacket packetData = GeyserVoice.objectMapper.convertValue(response, DenyPacket.class);
                            plugin.Logger.error("Server Denied: " + packetData.reason);
                            if (!"Invalid Token!".equals(packetData.reason)) {
                                handleDisconnect();
                            } else {
                                handleReconnect();
                            }
                        }
                    } else {
                        handleReconnect();
                    }
                })
                .whenComplete((res, ex) -> isRequestPending.set(false));
        }
    }

    public List<PlayerData> getPlayerDataList(Map<String, PlayerData> allPlayerDataList) {
        return new ArrayList<>(allPlayerDataList.values());
    }

    private void handleDisconnect() {
        plugin.setNotConnected();
        plugin.getTaskRunner().cancel();
    }

    private void handleReconnect() {
        plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-lost"));
        plugin.setNotConnected();
        plugin.getTaskRunner().cancel(); // Stop this task to avoid spamming while reconnecting

        if (GeyserVoice.getConfig().getBoolean("config.auto-reconnect")) {
            if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                plugin.getProxy().broadcast(
                        new ComponentBuilder(Language.getMessage(lang, "plugin-connection-lost-reconnect"))
                                .color(ChatColor.RED).create());
            }
            reconnectRetries = 0;
            scheduleReconnect();
        } else {
             if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                plugin.getProxy().broadcast(
                    new ComponentBuilder(Language.getMessage(lang, "plugin-connection-lost"))
                            .color(ChatColor.RED).create());
            }
        }
    }

    private void scheduleReconnect() {
        plugin.getProxy().getScheduler().schedule(plugin, this::attemptReconnect, 1, TimeUnit.SECONDS);
    }

    private void attemptReconnect() {
        if (reconnectRetries < 5) {
            reconnectRetries++;
            plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-attempt")
                    .replace("$attempt", String.valueOf(reconnectRetries)));

            if (plugin.reconnect(true)) {
                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-success"));
                if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                     plugin.getProxy().broadcast(
                        new ComponentBuilder(Language.getMessage(lang, "plugin-connection-reconnecting-success"))
                                .color(ChatColor.GREEN).create());
                }
                // Restart the task via reload or manually rescheduling
                plugin.reload(); 
            } else {
                 plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-failed-retry"));
                 scheduleReconnect();
            }
        } else {
            plugin.Logger.error(Language.getMessage(lang, "plugin-connection-reconnecting-failed"));
            if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                 plugin.getProxy().broadcast(
                        new ComponentBuilder(Language.getMessage(lang, "plugin-connection-reconnecting-failed"))
                                .color(ChatColor.RED).create());
            }
        }
    }
}
