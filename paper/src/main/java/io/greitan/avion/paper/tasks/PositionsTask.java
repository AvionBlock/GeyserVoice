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

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PositionsTask extends BukkitRunnable {
    private final GeyserVoice plugin;
    private final String lang;
    private boolean isConnected = false;
    private int reconnectRetries = 0;

    public PositionsTask(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void run() {
        if (plugin.usesProxy) {
            isConnected = true;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                plugin.getMessageHandler().sendPlayerData(player, getPlayerData(player));
            }
            return;
        }

        isConnected = plugin.isConnected();
        String host = plugin.getHost();
        int port = plugin.getPort();
        String token = plugin.getToken();
        String link = "http://" + host + ":" + port;

        if (isConnected) {
            if (host != null && token != null) {
                UpdatePacket updatePacket = new UpdatePacket(0, token, getPlayerDataList()); // ID 0 is placeholder, record handles it

                MCCommPacket response = plugin.network.sendPostRequest(link, updatePacket);
                if (response != null) {
                    if (response.packetId() == PacketType.AckUpdate.ordinal()) {
                        return;
                    } else if (response.packetId() == PacketType.Deny.ordinal() || response instanceof DenyPacket) {
                        DenyPacket packetData = GeyserVoice.objectMapper.convertValue(response, DenyPacket.class);
                        plugin.Logger.error(packetData.reason());
                        if (!packetData.reason().equals("Invalid Token!")) {
                            plugin.setNotConnected();
                            cancel();
                            return;
                        }
                    } else {
                        return;
                    }
                }
                
                if (!isConnected) return;

                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-lost"));
                plugin.setNotConnected();

                if (plugin.getConfig().getBoolean("config.auto-reconnect")) {
                    if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                        Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-lost-reconnect"))
                                .color(NamedTextColor.RED));
                    }
                    reconnectRetries = 0;
                    reconnect();
                    return;
                }
                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-lost"))
                            .color(NamedTextColor.RED));
                }
                cancel();
            }
        }
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

        return new PlayerData(
            player.getUniqueId().toString(),
            getDimensionId(player),
            locationData,
            player.getLocation().getYaw(),
            echoFactor,
            player.isInWater(),
            player.isDead()
        );
    }

    public double getCaveDensity(Player player) {
        if (!isConnected) {
            return 0.0;
        }

        String[] caveBlocks = {
                "STONE", "DIORITE", "GRANITE", "DEEPSLATE", "TUFF"
        };

        int blockCount = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Vector direction = new Vector(x, y, z);
                    blockCount += castRayUntilBlock(
                            new BlockIterator(player.getWorld(), player.getLocation().toVector(), direction, 0, 50),
                            caveBlocks);
                }
            }
        }

        return blockCount / 26.0;
    }

    private int castRayUntilBlock(BlockIterator blockIterator, String[] caveBlocks) {
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (block.getType().isSolid()) {
                if (Arrays.asList(caveBlocks).contains(getBlockType(block))) {
                    return 1;
                }
                break;
            }
        }
        return 0;
    }

    private String getBlockType(Block block) {
        return block.getType().toString();
    }

    private String getDimensionId(Player player) {
        return switch (player.getWorld().getName()) {
            case "world" -> "minecraft:overworld";
            case "world_nether" -> "minecraft:nether";
            case "world_the_end" -> "minecraft:the_end";
            default -> "minecraft:unknown";
        };
    }

    private boolean reconnect() {
        if (reconnectRetries < 5) {
            reconnectRetries++;

            plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-attempt").replace("$attempt",
                    String.valueOf(reconnectRetries)));

            if (plugin.reconnect(true)) {
                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-success"));

                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-success"))
                            .color(NamedTextColor.GREEN));
                }
                return true;
            } else {
                if (reconnectRetries < 5) {
                    plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-failed-retry"));
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return reconnect();
                }
                plugin.Logger.error(Language.getMessage(lang, "plugin-connection-reconnecting-failed"));

                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-failed"))
                            .color(NamedTextColor.RED));
                }
                cancel();
            }
        }
        return false;
    }
}
