package io.greitan.avion.paper;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.greitan.avion.common.BaseGeyserVoice;
import io.greitan.avion.paper.commands.VoiceCommand;
import io.greitan.avion.paper.listeners.*;
import io.greitan.avion.common.network.Network;
import io.greitan.avion.paper.tasks.PositionsTask;
import io.greitan.avion.paper.utils.*;

import java.io.File;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;

/**
 * Main plugin class for GeyserVoice.
 */
public class GeyserVoice extends JavaPlugin implements BaseGeyserVoice {
    private static @Getter GeyserVoice instance;
    private @Getter boolean isConnected = false;
    private @Getter String host = "";
    private @Getter int port = 0;
    private @Getter String serverKey = "";
    private @Getter Map<String, Boolean> playerBinds = new HashMap<>();
    private @Getter String token = "";
    private String lang;
    public boolean usesProxy = false;
    private @Getter PluginMessageHandler messageHandler = new PluginMessageHandler(this);

    private BukkitTask taskRunner;

    public PaperLogger Logger = new PaperLogger();
    public Network network = new Network(Logger);

    public static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executes upon enabling the plugin.
     */
    @Override
    public void onEnable() {
        instance = this;

        lang = getConfig().getString("config.lang");
        int positionTaskInterval = getConfig().getInt("config.voice.position-task-interval", 1);
        Language.init(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, PluginMessageHandler.channelName);
        getServer().getMessenger().registerIncomingPluginChannel(this, PluginMessageHandler.channelName,
                messageHandler);
        if (!getServer().getMessenger().isOutgoingChannelRegistered(this, PluginMessageHandler.channelName))
            Logger.warn("Outgoing Channel failed to register!");
        if (!getServer().getMessenger().isIncomingChannelRegistered(this, PluginMessageHandler.channelName))
            Logger.warn("Incoming Channel failed to register!");

        usesProxy = getConfig().getBoolean("config.server-behind-proxy", false);
        // if
        // (getServer().spigot().getConfig().getConfigurationSection("settings").getBoolean("bungeecord"))
        // usesProxy = true;

        VoiceCommand voiceCommand = new VoiceCommand(this, lang);
        getCommand("voice").setExecutor(voiceCommand);
        getCommand("voice").setTabCompleter(voiceCommand);
        taskRunner = new PositionsTask(this, lang).runTaskTimer(this, 1, positionTaskInterval);
        getServer().getPluginManager().registerEvents(new PlayerJoinHandler(this, lang), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitHandler(this, lang), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholder(this).register();
        }

        this.reload();
    }

    @Override
    public void onDisable() {
        // make sure to unregister the registered channels in case of a reload
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    /**
     * Reloads the plugin configuration and initializes connections.
     */
    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        Logger.info(Language.getMessage(lang, "plugin-config-loaded"));
        Logger.info(Language.getMessage(lang, "plugin-command-executor"));

        usesProxy = getConfig().getBoolean("config.server-behind-proxy", false);

        host = getConfig().getString("config.host");
        port = getConfig().getInt("config.port");
        serverKey = getConfig().getString("config.server-key");

        if (getConfig().getBoolean("config.auto-reconnect"))
            isConnected = reconnect(true);

        int positionTaskInterval = getConfig().getInt("config.voice.position-task-interval", 1);
        if (!taskRunner.isCancelled())
            taskRunner.cancel();
        taskRunner = new PositionsTask(this, lang).runTaskTimer(this, 1, positionTaskInterval);

        int proximityDistance = getConfig().getInt("config.voice.proximity-distance");
        Boolean proximityToggle = getConfig().getBoolean("config.voice.proximity-toggle");
        Boolean voiceEffects = getConfig().getBoolean("config.voice.voice-effects");

        updateSettings(proximityDistance, proximityToggle, voiceEffects);
    }

    /**
     * Connects to a new server.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param serverKey The server key.
     * @return True if connected successfully, otherwise false.
     */
    public Boolean connect(String host, int port, String serverKey) {
        if (Objects.nonNull(host) && Objects.nonNull(serverKey)) {
            getConfig().set("config.host", host);
            getConfig().set("config.port", port);
            getConfig().set("config.server-key", serverKey);
            saveConfig();
            reloadConfig();
            reload();

            return isConnected;
        } else {
            Logger.warn(Language.getMessage(lang, "plugin-connect-invalid-data"));
            return false;
        }
    }

    /**
     * Connects to the server.
     *
     * @param force Indicates whether to force a connection.
     * @return True if connected successfully, otherwise false.
     */
    public Boolean reconnect(Boolean force) {
        if (isConnected && !force)
            return true;
        if (isConnected) {
            disconnect("Reconnecting to another server.");
        }

        if (usesProxy) {
            Logger.info(Language.getMessage(lang, "plugin-connect-proxy"));
            return false;
        }

        if (Objects.nonNull(host) && Objects.nonNull(serverKey)) {
            String link = "http://" + host + ":" + port;
            String Token = network.sendLoginRequest(link, serverKey);
            if (Objects.nonNull(Token)) {
                Logger.info(Language.getMessage(lang, "plugin-connect-connected"));
                isConnected = true;
                token = Token;
            } else {
                Logger.warn(Language.getMessage(lang, "plugin-connect-failed"));
            }
            return isConnected;
        } else {
            Logger.warn(Language.getMessage(lang, "plugin-connect-invalid-data"));
            return false;
        }
    }

    /**
     * Disconnects from the server.
     *
     * @param reason The reason why we disconnected
     */
    public void disconnect(String reason) {
        if (!isConnected)
            return;

        if (Objects.nonNull(host) && Objects.nonNull(serverKey)) {
            String link = "http://" + host + ":" + port;
            network.sendLogoutRequest(link, token);
            isConnected = false;

            String disconnectMessage = Language.getMessage(lang, "plugin-connection-disconnect").replace("$reason", reason);
            Logger.info(disconnectMessage);

            boolean sendVoipDisconnectMessage = getConfig().getBoolean("config.voice.send-voip-disconnect-message");
            if (sendVoipDisconnectMessage) {
                Bukkit.broadcast(Component.text(disconnectMessage).color(NamedTextColor.YELLOW));
            }
        } else {
            Logger.warn(Language.getMessage(lang, "plugin-connect-invalid-data"));
        }
    }
    
    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        disconnect("N.A.");
    }

    /**
     * Binds a player to the voice chat server.
     *
     * @param playerKey The key associated with the player.
     * @param player    The player to bind.
     * @return True if the binding was successful, otherwise false.
     */
    public Boolean bind(int playerKey, Player player, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey) || usesProxy)
            return false;
            
        if (playerBinds.containsKey(player.getName()) && playerBinds.get(player.getName())) {
            return true;
        }

        String link = "http://" + host + ":" + port;

        getConfig().set("config.players." + player.getName(), playerKey);
        saveConfig();

        String result = network.sendBindRequest(link, token, playerKey, player.getUniqueId().toString(),
                player.getName());
        playerBinds.put(player.getName(), false);
        if (result != null) {
            if (result == "SUCCESS") {
                playerBinds.put(player.getName(), true);

                Logger.info(Language.getMessage(lang, "player-binded").replace("$player",player.getName()));

                boolean sendBindedMessage = getConfig().getBoolean("config.voice.send-binded-message");
                if (sendBindedMessage) {
                    Bukkit.broadcast(
                        Component.text(player.getName()).decorate(TextDecoration.BOLD)
                        .append(
                            Component.text(
                                Language.getMessage(lang, "player-binded")
                                    .replace("$player", "")
                            )
                            .color(NamedTextColor.DARK_GREEN)
                        )
                    );
                }
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = reconnect(true);
                return bind(playerKey, player, 1);
            }
        }
        return false;
    }

    public Boolean bind(int playerKey, Player player) {
        return bind(playerKey, player, 0);
    }

    /**
     * Bind a fake player
     * @param bindKey
     * @param name
     * @return
     */
    public Boolean bindFake(int playerKey, String name, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
            return false;
            
        if (playerBinds.containsKey(name) && playerBinds.get(name)) {
            return true;
        }
            
        String link = "http://" + host + ":" + port;

        String result = network.sendBindRequest(link, token, playerKey, String.format("%0", playerKey), name);
        playerBinds.put(name, false);
        if (result != null) {
            if (result == "SUCCESS") {
                playerBinds.put(name, true);

                Logger.info(Language.getMessage(lang, "player-binded").replace("$player", name));

                boolean sendBindedMessage = getConfig().getBoolean("config.voice.send-binded-message");
                if (sendBindedMessage) {
                    Bukkit.broadcast(
                        Component.text(name).decorate(TextDecoration.BOLD)
                        .append(
                            Component.text(
                                Language.getMessage(lang, "player-binded")
                                    .replace("$player", "")
                            )
                            .color(NamedTextColor.DARK_GREEN)
                        )
                    );
                }
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = reconnect(true);
                return bindFake(playerKey, name, 1);
            }
        }
        return false;
    }

    public Boolean bindFake(int playerKey, String name) {
        return bindFake(playerKey, name, 0);
    }

    /**
     * Disconnects a player from the voice chat server.
     *
     * @param player The player to disconnect.
     * @return True if the disconnection was successful, otherwise false.
     */
    public Boolean disconnectPlayer(Player player, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey) || usesProxy)
            return false;
        String link = "http://" + host + ":" + port;

        String result = network.sendDisconnectRequest(link, token, player.getUniqueId().toString(), player.getName());
        if (result != null) {
            if (result == "SUCCESS") {
                playerBinds.remove(player.getName());
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = reconnect(true);
                return disconnectPlayer(player, 1);
            }
        }
        return false;
    }

    public Boolean disconnectPlayer(Player player) {
        return disconnectPlayer(player, 0);
    }

    /**
     * Updates the voice chat settings.
     *
     * @param proximityDistance Proximity distance setting.
     * @param proximityToggle   Proximity toggle setting.
     * @param voiceEffects      Voice effects setting.
     * @return True if settings were updated successfully, otherwise false.
     */
    public Boolean updateSettings(int proximityDistance, Boolean proximityToggle, Boolean voiceEffects) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey) || usesProxy)
            return false;
        String link = "http://" + host + ":" + port;

        return network.sendUpdateSettingsRequest(link, token, proximityDistance, proximityToggle, voiceEffects);
    }

    public void setNotConnected() {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
            return;
        isConnected = false;
    }

    public void saveResource(String resourcePath) {
        File outFile = new File(getDataFolder(), resourcePath);
        // Default Spigot saveResource gives a warning when the file already exists and
        // when you don't override
        // Now just skip it if the file exists
        if (!outFile.exists()) {
            saveResource(resourcePath, false);
        }
    }
}
