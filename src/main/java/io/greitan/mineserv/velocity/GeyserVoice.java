package io.greitan.mineserv.velocity;

import lombok.Getter;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;

import de.leonhard.storage.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.greitan.mineserv.common.utils.Constants;
import io.greitan.mineserv.common.BaseGeyserVoice;
import io.greitan.mineserv.velocity.commands.VoiceCommand;
import io.greitan.mineserv.velocity.listeners.*;
import io.greitan.mineserv.common.network.Network;
import io.greitan.mineserv.common.network.Payloads.PlayerData;
import io.greitan.mineserv.velocity.tasks.PositionsTask;
import io.greitan.mineserv.velocity.utils.*;

import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Main plugin class for GeyserVoice.
 */
@Plugin(
    id = "geyservoice",
    name = Constants.NAME,
    version = Constants.VERSION,
    description = Constants.DESCRIPTION,
    authors = {"Alpha"},
    url = Constants.URL
)
public class GeyserVoice implements BaseGeyserVoice {
    private final @Getter ProxyServer proxy;
    private final @Getter File dataFolder;
    private static @Getter Yaml config;

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

    private ScheduledTask taskRunner;
    private PositionsTask positionsTask = new PositionsTask(this, lang);

    public VelocityLogger Logger = new VelocityLogger();
    public Network network = new Network(Logger);

    public static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public GeyserVoice(final ProxyServer proxy, @DataDirectory Path dataDirectory) {
        instance = this;
        this.proxy = proxy;
        this.dataFolder = dataDirectory.toFile();

        saveResource("/config.yml");
        config = new Yaml("config", this.dataFolder.toString());
    }

    /**
     * Executes upon enabling the plugin.
     */
    @Subscribe
    public void onProxyInitialization(final ProxyInitializeEvent event) {
        Logger.info("Enabling GeyserVoice");

        lang = getConfig().getString("config.lang");
        int positionTaskInterval = getConfig().getOrDefault("config.voice.position-task-interval", 1);
        Language.init(this);

        proxy.getEventManager().register(this, messageHandler);
        proxy.getChannelRegistrar().register(PluginMessageHandler.channelName);

        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("voice").aliases("voicecraft").plugin(this).build();
        VoiceCommand voiceCommand = new VoiceCommand(this, lang);
        commandManager.register(commandMeta, voiceCommand);

	taskRunner = proxy.getScheduler()
            .buildTask(this, () -> {
                if (!positionsTask.run()) {
                    taskRunner.cancel();
                }
            })
            .repeat(positionTaskInterval * 50, TimeUnit.MILLISECONDS) // positionTaskInterval is in ticks, 1 tick = 0.050 seconds = 50 ms
            .schedule();

        proxy.getEventManager().register(this, new PlayerJoinHandler(this, lang));
        proxy.getEventManager().register(this, new PlayerQuitHandler(this, lang));

        this.reload();
    }

    /**
     * Reloads the plugin configuration and initializes connections.
     */
    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        Logger.info(Language.getMessage(lang, "plugin-config-loaded"));
        Logger.info(Language.getMessage(lang, "plugin-command-executor"));

        host = getConfig().getString("config.host");
        port = getConfig().getInt("config.port");
        serverKey = getConfig().getString("config.server-key");

        isConnected = connect(true);

        int positionTaskInterval = getConfig().getOrDefault("config.voice.position-task-interval", 1);
        if (taskRunner.status() != TaskStatus.CANCELLED) taskRunner.cancel();
	taskRunner = proxy.getScheduler()
            .buildTask(this, () -> {
                if (!positionsTask.run()) {
                    taskRunner.cancel();
                }
            })
            .repeat(positionTaskInterval * 50, TimeUnit.MILLISECONDS) // positionTaskInterval is in ticks, 1 tick = 0.050 seconds = 50 ms
            .schedule();

        int proximityDistance = getConfig().getInt("config.voice.proximity-distance");
        Boolean proximityToggle = getConfig().getBoolean("config.voice.proximity-toggle");
        Boolean voiceEffects = getConfig().getBoolean("config.voice.voice-effects");

        updateSettings(proximityDistance, proximityToggle, voiceEffects);
    }

    /**
     * Connects to the server.
     *
     * @param force Indicates whether to force a connection.
     * @return True if connected successfully, otherwise false.
     */
    public Boolean connect(Boolean force) {
        if (isConnected && !force) return true;
        isConnected = false;

        if (Objects.nonNull(host) && Objects.nonNull(serverKey)) {
            String link = "http://" + host + ":" + port;

            String Token = network.sendLoginRequest(link, serverKey);
            if (Objects.nonNull(Token)) {
                Logger.info(Language.getMessage(lang, "plugin-connect-connect"));
                isConnected = true;
                token = Token;
            } else {
                Logger.warn(Language.getMessage(lang, "plugin-connect-disconnect"));
            }
            return isConnected;
        } else {
            Logger.warn(Language.getMessage(lang, "plugin-connect-invalid-data"));
            return false;
        }
    }

    /**
     * Binds a player to the voice chat server.
     *
     * @param playerKey The key associated with the player.
     * @param player    The player to bind.
     * @return True if the binding was successful, otherwise false.
     */
    public Boolean bind(int playerKey, Player player, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey)) return false;
        String link = "http://" + host + ":" + port;

        getConfig().set("config.players." + player.getUsername(), playerKey);
        saveConfig();

        String result = network.sendBindRequest(link, token, playerKey, player.getUniqueId().toString(), player.getUsername());
        playerBinds.put(player.getUsername(), false);
        if (result != null) {
            if (result == "SUCCESS") {
                playerBinds.put(player.getUsername(), true);
                messageHandler.sendPlayerBindSync(player);
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = connect(true);
                return bind(playerKey, player, 1);
            }
        }
        messageHandler.sendPlayerBindSync(player);
        return false;
    }

    public Boolean bind(int playerKey, Player player) {
        return bind(playerKey, player, 0);
    }

    /**
     * Disconnects a player from the voice chat server.
     *
     * @param player The player to disconnect.
     * @return True if the disconnection was successful, otherwise false.
     */
    public Boolean disconnectPlayer(Player player, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey)) return false;
        String link = "http://" + host + ":" + port;

        String result = network.sendDisconnectRequest(link, token, player.getUniqueId().toString(), player.getUsername());
        if (result != null) {
            if (result == "SUCCESS") {
                playerBinds.remove(player.getUsername());
                messageHandler.sendPlayerBindSync(player);
                return true;
            } else if (result == "Invalid Token!" && tries == 0) {
                Logger.info("Invalid Token detected, reconnecting...");
                isConnected = connect(true);
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
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey)) return false;
        String link = "http://" + host + ":" + port;

        return network.sendUpdateSettingsRequest(link, token, proximityDistance, proximityToggle, voiceEffects);
    }

    public void setNotConnected() {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey)) return;
        isConnected = false;
    }

    public void reloadConfig() {
        config.forceReload();
    }

    public void saveConfig() {
        config.write();
    }

    public void saveDefaultConfig() {
        config.addDefaultsFromInputStream(getClass().getResourceAsStream("/config.yaml"));
    }

    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace("\\", "/");
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf("/");
        File outDir = new File(dataFolder, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

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
