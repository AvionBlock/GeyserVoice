package io.greitan.avion.paper.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import io.greitan.avion.paper.GeyserVoice;
import io.greitan.avion.paper.utils.Language;
import io.greitan.avion.common.network.Payloads.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PositionsTask extends BukkitRunnable {
    private final GeyserVoice plugin;
    private final String lang;
    private final AtomicBoolean isRequestPending = new AtomicBoolean(false);
    private int reconnectRetries = 0;

    // Constants for Echo/Cave density
    private static final int CAVE_Y_THRESHOLD = 0;
    private static final double CAVE_DENSITY_DEEP = 1.0;
    private static final double CAVE_DENSITY_COVERED = 0.5;
    private static final double CAVE_DENSITY_SKY = 0.0;

    public PositionsTask(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void run() {
        if (plugin.usesProxy) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                plugin.getMessageHandler().sendPlayerData(player, getPlayerData(player));
            }
            return;
        }

        if (!plugin.isConnected()) return;

        // Prevent stacking requests if the network is slow
        if (isRequestPending.get()) return;

        String host = plugin.getHost();
        int port = plugin.getPort();
        String token = plugin.getToken();
        String link = "http://" + host + ":" + port;

        if (host != null && token != null) {
            // 1. Gather data on the main thread (Thread-safe)
            List<PlayerData> players = getPlayerDataList();
            UpdatePacket updatePacket = new UpdatePacket();
            updatePacket.token = token;
            updatePacket.players = players;

            // 2. Send network request asynchronously
            isRequestPending.set(true);
            plugin.network.sendPostRequestAsync(link, updatePacket)
                .thenAccept(response -> {
                    if (response != null) {
                        if (response.getPacketId() == PacketType.AckUpdate.ordinal()) {
                            // Success
                        } else if (response.getPacketId() == PacketType.Deny.ordinal() || response instanceof DenyPacket) {
                            DenyPacket packetData = GeyserVoice.objectMapper.convertValue(response, DenyPacket.class);
                            plugin.Logger.error("Server Denied Update: " + packetData.reason);
                            
                            if (!"Invalid Token!".equals(packetData.reason)) {
                                handleDisconnect();
                            } else {
                                handleReconnect();
                            }
                        }
                    } else {
                        // Network failure (null response)
                        handleReconnect();
                    }
                })
                .whenComplete((result, ex) -> {
                    isRequestPending.set(false);
                });
        }
    }

    private void handleDisconnect() {
        plugin.setNotConnected();
        this.cancel();
    }

    private void handleReconnect() {
        if (!plugin.isConnected()) return; // Already disconnected

        // Schedule reconnect on main thread to avoid concurrency issues with plugin state
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-lost"));
                plugin.setNotConnected();

                if (plugin.getConfig().getBoolean("config.auto-reconnect")) {
                    if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                        Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-lost-reconnect"))
                                .color(NamedTextColor.RED));
                    }
                    reconnectRetries = 0;
                    attemptReconnect();
                } else {
                     if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                        Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-lost"))
                                .color(NamedTextColor.RED));
                    }
                    PositionsTask.this.cancel();
                }
            }
        }.runTask(plugin);
    }

    private void attemptReconnect() {
        // Reconnect logic moved to async task
        new BukkitRunnable() {
            @Override
            public void run() {
                if (reconnectRetries < 5) {
                    reconnectRetries++;
                    // Log on main thread or async is fine for logging usually
                    plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-attempt")
                        .replace("$attempt", String.valueOf(reconnectRetries)));
                    
                    // reconnect() is blocking, so we run it here in async task
                    if (plugin.reconnect(true)) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-success"));
                                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                                    Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-success"))
                                            .color(NamedTextColor.GREEN));
                                }
                            }
                        }.runTask(plugin);
                    } else {
                         plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-failed-retry"));
                         // Retry after delay
                         new BukkitRunnable() {
                             @Override
                             public void run() {
                                 attemptReconnect();
                             }
                         }.runTaskLaterAsynchronously(plugin, 20L); // 1 second (20 ticks)
                    }
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.Logger.error(Language.getMessage(lang, "plugin-connection-reconnecting-failed"));
                            if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                                Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-failed"))
                                        .color(NamedTextColor.RED));
                            }
                            PositionsTask.this.cancel();
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public List<PlayerData> getPlayerDataList() {
        List<PlayerData> playerDataList = new ArrayList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            playerDataList.add(getPlayerData(player));
        }
        return playerDataList;
    }

    public PlayerData getPlayerData(Player player) {
        Location headLocation = player.getEyeLocation();

        LocationData locationData = new LocationData(
            headLocation.getX(),
            headLocation.getY(),
            headLocation.getZ()
        );

        double echoFactor = 0.0;
        if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            echoFactor = getCaveDensity(player);
        }

        PlayerData playerData = new PlayerData();
        playerData.playerId = player.getUniqueId().toString();
        playerData.dimensionId = getDimensionId(player);
        playerData.location = locationData;
        playerData.rotation = player.getLocation().getYaw();
        playerData.echoFactor = echoFactor;
        playerData.muffled = player.isInWater();
        playerData.isDead = player.isDead();

        return playerData;
    }

    public double getCaveDensity(Player player) {
        // Optimized: Check only above (sky) and below (deep slate/caves)
        // This is a rough approximation but much faster than 26 rays.
        Location loc = player.getLocation();
        if (loc.getBlockY() < CAVE_Y_THRESHOLD) return CAVE_DENSITY_DEEP; // Deep underground
        if (loc.getWorld().getHighestBlockYAt(loc) > loc.getBlockY()) return CAVE_DENSITY_COVERED; // Under something (cave/building)
        return CAVE_DENSITY_SKY; // Open sky
    }

    private String getDimensionId(Player player) {
        // Improved: Use Environment
        return switch (player.getWorld().getEnvironment()) {
            case NORMAL -> "minecraft:overworld";
            case NETHER -> "minecraft:nether";
            case THE_END -> "minecraft:the_end";
            default -> "minecraft:unknown";
        };
    }
}
