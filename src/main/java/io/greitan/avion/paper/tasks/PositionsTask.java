package io.greitan.avion.paper.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import io.greitan.avion.paper.GeyserVoice;
import io.greitan.avion.paper.utils.Language;
import io.greitan.avion.common.network.Payloads.PacketType;
import io.greitan.avion.common.network.Payloads.LocationData;
import io.greitan.avion.common.network.Payloads.PlayerData;
import io.greitan.avion.common.network.Payloads.MCCommPacket;
import io.greitan.avion.common.network.Payloads.UpdatePacket;
import io.greitan.avion.common.network.Payloads.DenyPacket;

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
    private Integer ReconnectRetries = 0;

    public PositionsTask(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Override
    public void run() {
        if (plugin.usesProxy) {
            isConnected = true; // Only local variable... Needed for CaveEchoFactor
            // plugin.getMessageHandler().sendPlayerDataList(plugin.getServer(),
            // getPlayerDataList());
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
                UpdatePacket updatePacket = new UpdatePacket();
                updatePacket.Token = token;
                updatePacket.Players = getPlayerDataList();

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

                if (plugin.getConfig().getBoolean("config.auto-reconnect")) {
                    if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                        Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-lost-reconnect"))
                                .color(NamedTextColor.RED));
                    }
                    ReconnectRetries = 0;
                    Reconnect();
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
            PlayerData playerData = getPlayerData(player);
            playerDataList.add(playerData);
        }

        return playerDataList;
    }

    public PlayerData getPlayerData(Player player) {
        Location headLocation = player.getEyeLocation();

        LocationData locationData = new LocationData();
        locationData.x = headLocation.getX();
        locationData.y = headLocation.getY();
        locationData.z = headLocation.getZ();

        PlayerData playerData = new PlayerData();
        playerData.PlayerId = player.getUniqueId().toString();
        playerData.DimensionId = getDimensionId(player);
        playerData.Location = locationData;
        playerData.Rotation = player.getLocation().getYaw();

        if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            playerData.EchoFactor = getCaveDensity(player);
        } else {
            playerData.EchoFactor = 0.0;
        }
        playerData.Muffled = player.isInWater();
        playerData.IsDead = player.isDead();
        return playerData;
    }

    public double getCaveDensity(Player player) {
        if (!isConnected) {
            return 0.0;
        }

        String[] caveBlocks = {
                "STONE",
                "DIORITE",
                "GRANITE",
                "DEEPSLATE",
                "TUFF"
        };

        int blockCount = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue; // a vector of 0,0,0 won't go anywhere, so skip it...
                    Vector direction = new Vector(x, y, z);
                    blockCount += castRayUntilBlock(
                            new BlockIterator(player.getWorld(), player.getLocation().toVector(), direction, 0, 50),
                            caveBlocks);
                }
            }
        }

        // (3 * 3 * 3) - 1 = 26.0
        return blockCount / 26.0; // Total blocks checked
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
        String worldName = player.getWorld().getName();
        return switch (worldName) {
            case "world" -> "minecraft:overworld";
            case "world_nether" -> "minecraft:nether";
            case "world_the_end" -> "minecraft:the_end";
            default -> "minecraft:unknown";
        };
    }

    private Boolean Reconnect() {
        if (ReconnectRetries < 5) {
            ReconnectRetries++;

            plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-attempt").replace("$attempt",
                    ReconnectRetries.toString()));

            if (plugin.connect(true)) {
                plugin.Logger.warn(Language.getMessage(lang, "plugin-connection-reconnecting-success"));

                if (plugin.getConfig().getBoolean("config.voice.send-connection-lost-message")) {
                    Bukkit.broadcast(Component.text(Language.getMessage(lang, "plugin-connection-reconnecting-success"))
                            .color(NamedTextColor.GREEN));
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
