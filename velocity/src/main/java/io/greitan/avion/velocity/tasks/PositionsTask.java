package io.greitan.avion.velocity.tasks;

import io.greitan.avion.velocity.GeyserVoice;
import io.greitan.avion.velocity.utils.Language;
import io.greitan.avion.common.network.Payloads.PacketType;
import io.greitan.avion.common.network.Payloads.PlayerData;
import io.greitan.avion.common.network.Payloads.UpdatePacket;
import io.greitan.avion.common.network.Payloads.DenyPacket;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class PositionsTask {
    private final GeyserVoice plugin;
    private final String lang;
    private final AtomicBoolean isRequestPending = new AtomicBoolean(false);
    private int reconnectRetries = 0;

    public PositionsTask(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public boolean run() {
        if (!plugin.isConnected()) {
            return false; // Cancels the task in GeyserVoice
        }

        // Prevent stacking requests
        if (isRequestPending.get()) return true;

        String host = plugin.getHost();
        int port = plugin.getPort();
        String token = plugin.getToken();
        String link = "http://" + host + ":" + port;

        if (host != null && token != null) {
            // 1. Gather data (Proxy is thread-safe for getting players usually, verifying thread context isn't as strict as Bukkit)
            // However, modifying collections while iterating can be an issue.
            // We clone the values to be safe.
            List<PlayerData> players = getPlayerDataList(plugin.playerDataList);
            
            UpdatePacket updatePacket = new UpdatePacket();
            updatePacket.token = token;
            updatePacket.players = players;

            // 2. Send async
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
                                plugin.setNotConnected();
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

        return true; // Keep running
    }

    public List<PlayerData> getPlayerDataList(Map<String, PlayerData> allPlayerDataList) {
        // synchronized? playerDataList is a synchronized map or HashMap?
        // In GeyserVoice it is HashMap. Accessing it from this task (which runs on proxy scheduler) should be fine 
        // if PluginMessageHandler also runs on proxy threads.
        return new ArrayList<>(allPlayerDataList.values());
    }

    private void handleReconnect() {
        plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-lost"));
        plugin.setNotConnected(); // Next run() will return false and cancel task

        if (GeyserVoice.getConfig().getBoolean("config.auto-reconnect")) {
            if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                plugin.getProxy().sendMessage(
                        Component.text(Language.getMessage(lang, "plugin-connection-lost-reconnect"))
                                .color(NamedTextColor.RED));
            }
            reconnectRetries = 0;
            scheduleReconnect();
        } else {
            if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                plugin.getProxy().sendMessage(Component.text(Language.getMessage(lang, "plugin-connection-lost"))
                        .color(NamedTextColor.RED));
            }
        }
    }

    private void scheduleReconnect() {
        plugin.getProxy().getScheduler().buildTask(plugin, this::attemptReconnect)
            .delay(1, TimeUnit.SECONDS)
            .schedule();
    }

    private void attemptReconnect() {
        // Reconnect async to avoid blocking scheduler
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if (reconnectRetries < 5) {
                reconnectRetries++;
                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-attempt")
                        .replace("$attempt", String.valueOf(reconnectRetries)));

                if (plugin.reconnect(true)) {
                    plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-success"));
                    if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                        plugin.getProxy().sendMessage(
                                Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-success"))
                                        .color(NamedTextColor.GREEN));
                    }
                    // Re-trigger reload() to restart tasks
                    plugin.reload(); 
                } else {
                     plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-failed-retry"));
                     scheduleReconnect();
                }
            } else {
                plugin.Logger.error(Language.getMessage(lang, "plugin-connection-reconnecting-failed"));
                 if (GeyserVoice.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    plugin.getProxy().sendMessage(
                            Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-failed"))
                                    .color(NamedTextColor.RED));
                }
            }
        }).schedule();
    }
}
