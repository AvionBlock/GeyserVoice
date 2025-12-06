package io.greitan.avion.fabric.tasks;

import io.greitan.avion.common.network.Network;
import io.greitan.avion.common.network.Payloads.*;
import io.greitan.avion.fabric.FabricGeyserVoice;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;

public class PositionsTask {
    private final FabricGeyserVoice plugin;
    private final AtomicBoolean isRequestPending = new AtomicBoolean(false);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private int reconnectRetries = 0;

    // Constants for cave density calculation
    private static final int CAVE_Y_THRESHOLD = 50;
    private static final double CAVE_DENSITY_DEEP = 0.8;
    private static final double CAVE_DENSITY_COVERED = 0.5;
    private static final double CAVE_DENSITY_SKY = 0.0;

    public PositionsTask(FabricGeyserVoice plugin) {
        this.plugin = plugin;
    }

    public void run() {
        if (isReconnecting.get()) return; // Don't run while reconnecting

        if (!plugin.isConnected()) {
             // If we are not connected and not reconnecting, we might want to trigger a reconnect if auto-reconnect is on?
             // But usually handleReconnect is triggered by a failure. 
             // If we start disconnected, the plugin reload() handles the initial connect.
             return;
        }

        if (isRequestPending.get()) return;

        String host = plugin.getHost();
        int port = plugin.getPort();
        String token = plugin.getToken();
        String link = "http://" + host + ":" + port;

        if (host != null && token != null) {
            List<PlayerData> players = getPlayerDataList();
            UpdatePacket updatePacket = new UpdatePacket();
            updatePacket.token = token;
            updatePacket.players = players;

            isRequestPending.set(true);
            plugin.network.sendPostRequestAsync(link, updatePacket)
                .thenAccept(response -> {
                    if (response != null) {
                        if (response.getPacketId() == PacketType.AckUpdate.ordinal()) {
                            // Success
                        } else if (response.getPacketId() == PacketType.Deny.ordinal() || response instanceof DenyPacket) {
                            plugin.loggerWrapper.error("Server Denied Update");
                            // We should probably check for Invalid Token here similar to Paper
                             try {
                                DenyPacket packetData = Network.objectMapper.convertValue(response, DenyPacket.class);
                                if ("Invalid Token!".equals(packetData.reason)) {
                                    handleReconnect();
                                } else {
                                    plugin.setNotConnected();
                                }
                            } catch (Exception e) {
                                plugin.setNotConnected();
                            }
                        }
                    } else {
                        // Network failure
                        handleReconnect();
                    }
                })
                .whenComplete((result, ex) -> {
                    isRequestPending.set(false);
                });
        }
    }

    private List<PlayerData> getPlayerDataList() {
        List<PlayerData> playerDataList = new ArrayList<>();
        if (FabricGeyserVoice.getInstance().getServer() == null) return playerDataList;

        for (ServerPlayerEntity player : FabricGeyserVoice.getInstance().getServer().getPlayerManager().getPlayerList()) {
            playerDataList.add(getPlayerData(player));
        }
        return playerDataList;
    }

    private PlayerData getPlayerData(ServerPlayerEntity player) {
        Vec3d pos = player.getEyePos();
        LocationData locationData = new LocationData(pos.x, pos.y, pos.z);

        PlayerData playerData = new PlayerData();
        playerData.playerId = player.getUuid().toString();
        playerData.dimensionId = getDimensionId(player);
        playerData.location = locationData;
        playerData.rotation = player.getYaw();
        playerData.echoFactor = getCaveDensity(player);
        playerData.muffled = player.isSubmergedInWater();
        playerData.isDead = player.isDead();

        return playerData;
    }

    private double getCaveDensity(ServerPlayerEntity player) {
        if (player.getY() < CAVE_Y_THRESHOLD) return CAVE_DENSITY_DEEP;
        
        // Check if there is a block above the player
        int topY = player.getEntityWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, (int)player.getX(), (int)player.getZ());
        if (topY > player.getY()) {
            return CAVE_DENSITY_COVERED;
        }
        
        return CAVE_DENSITY_SKY;
    }

    private String getDimensionId(ServerPlayerEntity player) {
        RegistryKey<World> dim = player.getEntityWorld().getRegistryKey();
        return dim.getValue().toString();
    }

    private void handleReconnect() {
        if (isReconnecting.getAndSet(true)) return; // Already reconnecting

        plugin.setNotConnected();
        plugin.loggerWrapper.warn("Connection lost, attempting to reconnect...");
        reconnectRetries = 0;

        CompletableFuture.runAsync(() -> {
            // TODO: Check config for auto-reconnect setting
            // For now assuming true as default or passed from config
            while (reconnectRetries < 5) {
                reconnectRetries++;
                plugin.loggerWrapper.warn("Reconnecting attempt " + reconnectRetries + "...");
                
                if (plugin.reconnect(true)) {
                    plugin.loggerWrapper.info("Reconnected successfully!");
                    isReconnecting.set(false);
                    return;
                }
                
                try {
                    Thread.sleep(1000); // Wait 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            plugin.loggerWrapper.error("Failed to reconnect after 5 attempts.");
            isReconnecting.set(false);
        });
    }
}
