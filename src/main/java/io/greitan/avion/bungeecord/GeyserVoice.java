package io.greitan.avion.bungeecord;

import lombok.Getter;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.greitan.avion.bungeecord.commands.VoiceCommand;
import io.greitan.avion.bungeecord.listeners.*;
import io.greitan.avion.common.BaseGeyserVoice;
import io.greitan.avion.common.network.Network;
import io.greitan.avion.common.network.Payloads.PlayerData;
import io.greitan.avion.bungeecord.tasks.PositionsTask;
import io.greitan.avion.bungeecord.utils.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;

public class GeyserVoice extends Plugin implements BaseGeyserVoice {
    private static @Getter Configuration config;

    private static @Getter GeyserVoice instance;
    private @Getter boolean isConnected = false;
    private @Getter String host = "";
    private @Getter int port = 0;
    private @Getter String serverKey = "";
    private @Getter Map<String, Boolean> playerBinds = new HashMap<>();
    private @Getter String token = "";
    private String lang;
    private @Getter PluginMessageHandler messageHandler = new PluginMessageHandler(this);
    public Map<String, PlayerData> playerDataList = new HashMap<>();

    private @Getter ScheduledTask taskRunner;

    public BungeecordLogger Logger = new BungeecordLogger();
    public Network network = new Network(Logger);

    public static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executes upon enabling the plugin.
     */
    @Override
    public void onEnable() {
        instance = this;

        this.reloadConfig();
        lang = getConfig().getString("config.lang");
        int positionTaskInterval = getConfig().getInt("config.voice.position-task-interval", 1);
        Language.init(this);

        getProxy().registerChannel(PluginMessageHandler.channelName);
        getProxy().getPluginManager().registerListener(this, messageHandler);

        getProxy().getPluginManager().registerCommand(this, new VoiceCommand(this, lang));

        // 1s / 20 ticks = 0.050s = 50ms
        taskRunner = getProxy().getScheduler().schedule(this, new PositionsTask(this, lang), 1,
                50 * positionTaskInterval, TimeUnit.MILLISECONDS);

        getProxy().getPluginManager().registerListener(this, new PlayerJoinHandler(this, lang));
        getProxy().getPluginManager().registerListener(this, new PlayerQuitHandler(this, lang));

        this.reload();
    }

    @Override
    public void onDisable() {
        // make sure to unregister the registered channels in case of a reload
        getProxy().unregisterChannel(PluginMessageHandler.channelName);
        taskRunner.cancel();
    }

    /**
     * Reloads the plugin configuration and initializes connections.
     */
    public void reload() {
        // saveDefaultConfig();
        reloadConfig();
        Logger.info(Language.getMessage(lang, "plugin-config-loaded"));
        Logger.info(Language.getMessage(lang, "plugin-command-executor"));

        host = getConfig().getString("config.host");
        port = getConfig().getInt("config.port");
        serverKey = getConfig().getString("config.server-key");

        if (getConfig().getBoolean("config.auto-reconnect"))
            isConnected = reconnect(true);

        int positionTaskInterval = getConfig().getInt("config.voice.position-task-interval", 1);
        taskRunner.cancel();
        // 1s / 20 ticks = 0.050s = 50ms
        taskRunner = getProxy().getScheduler().schedule(this, new PositionsTask(this, lang), 1,
                50 * positionTaskInterval, TimeUnit.MILLISECONDS);

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
                getProxy().broadcast(new ComponentBuilder(disconnectMessage).color(ChatColor.YELLOW).create());
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
    public Boolean bind(int playerKey, ProxiedPlayer player, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
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
                messageHandler.sendPlayerBindSync(player);

                Logger.info(Language.getMessage(lang, "player-binded").replace("$player",player.getName()));

                boolean sendBindedMessage = getConfig().getBoolean("config.voice.send-binded-message");
                if (sendBindedMessage) {
                    getProxy().broadcast(
                        new ComponentBuilder(player.getName()).bold(true)
                        .append(
                            new ComponentBuilder(
                                Language.getMessage(lang, "player-binded")
                                    .replace("$player", "")
                            )
                            .color(ChatColor.DARK_GREEN)
                            .create()
                        ).create()
                    );
                }
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = reconnect(true);
                return bind(playerKey, player, 1);
            }
        }
        messageHandler.sendPlayerBindSync(player);
        return false;
    }

    public Boolean bind(int playerKey, ProxiedPlayer player) {
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
                // messageHandler.sendPlayerBindSync(player);

                Logger.info(Language.getMessage(lang, "player-binded").replace("$player", name));

                boolean sendBindedMessage = getConfig().getBoolean("config.voice.send-binded-message");
                if (sendBindedMessage) {
                    getProxy().broadcast(
                        new ComponentBuilder(name).bold(true)
                        .append(
                            new ComponentBuilder(
                                Language.getMessage(lang, "player-binded")
                                    .replace("$player", "")
                            )
                            .color(ChatColor.DARK_GREEN)
                            .create()
                        ).create()
                    );
                }
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = reconnect(true);
                return bindFake(playerKey, name, 1);
            }
        }
        // messageHandler.sendPlayerBindSync(player);
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
    public Boolean disconnectPlayer(ProxiedPlayer player, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
            return false;
        String link = "http://" + host + ":" + port;

        String result = network.sendDisconnectRequest(link, token, player.getUniqueId().toString(), player.getName());
        if (result != null) {
            if (result == "SUCCESS") {
                playerBinds.remove(player.getName());
                messageHandler.sendPlayerBindSync(player);
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = reconnect(true);
                return disconnectPlayer(player, 1);
            }
        }
        return false;
    }

    public Boolean disconnectPlayer(ProxiedPlayer player) {
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
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
            return false;
        String link = "http://" + host + ":" + port;

        return network.sendUpdateSettingsRequest(link, token, proximityDistance, proximityToggle, voiceEffects);
    }

    public void setNotConnected() {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
            return;
        isConnected = false;
    }

    public void reloadConfig() {
        saveResource("config.yml");

        File configFile = new File(getDataFolder(), "config.yml");
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config,
                    new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace("\\", "/");
        InputStream in = getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf("/");
        File outDir = new File(getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            Logger.error("Could not save " + outFile.getName() + " to " + outFile);
        }
    }

    public void saveResource(String resourcePath) {
        saveResource(resourcePath, false);
    }
}
