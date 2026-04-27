package team.avion.velocity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.spongepowered.configurate.serialize.SerializationException;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import team.avion.common.BaseGeyserVoice;
import team.avion.common.config.ConfigTemplateWriter;
import team.avion.common.localization.PluginLocalization;
import team.avion.proxy.VoiceCraftProxySessionManager;
import team.avion.velocity.commands.VoiceCommand;
import team.avion.velocity.config.VelocityYamlConfig;
import team.avion.velocity.listeners.PlayerJoinHandler;
import team.avion.velocity.listeners.PlayerQuitHandler;
import team.avion.velocity.listeners.PluginMessageHandler;
import team.avion.velocity.tasks.PositionsTask;
import team.avion.velocity.utils.Language;
import team.avion.velocity.utils.VelocityLogger;

@Plugin(id = "geyservoice", name = "GeyserVoice", version = "0.0.0", description = "VoiceCraft bridge for Velocity", authors = { "0xAlpha" })
public class GeyserVoice implements BaseGeyserVoice {
    private final @Getter ProxyServer proxy;
    private final @Getter File dataFolder;
    private static VelocityYamlConfig config;

    private static @Getter GeyserVoice instance;
    private @Getter boolean isConnected = false;
    private @Getter String host = "";
    private @Getter int port = 0;
    private @Getter String loginToken = "";
    private final @Getter Map<String, Boolean> playerBinds = new HashMap<>();
    private @Getter String token = "";
    private String lang;
    private final @Getter PluginMessageHandler messageHandler = new PluginMessageHandler(this);
    private final @Getter VoiceCraftProxySessionManager sessionManager;

    private ScheduledTask taskRunner;
    private PositionsTask positionsTask;

    public final VelocityLogger Logger = new VelocityLogger();

    @Inject
    public GeyserVoice(ProxyServer proxy, @DataDirectory Path dataDirectory) throws IOException {
        instance = this;
        this.proxy = proxy;
        this.dataFolder = dataDirectory.toFile();
        this.sessionManager = new VoiceCraftProxySessionManager(Logger);

        ensureLocalizedConfigExists();
        saveResource("locale/en.yml");
        saveResource("locale/ru.yml");
        saveResource("locale/nl.yml");
        saveResource("locale/ja.yml");
        config = new VelocityYamlConfig(dataDirectory.resolve("config.yml"));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        reloadConfig();
        lang = resolveConfiguredLanguage();
        Language.init(this);

        proxy.getEventManager().register(this, messageHandler);
        proxy.getChannelRegistrar().register(PluginMessageHandler.CHANNEL);

        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("voice").aliases("voicecraft").plugin(this).build();
        commandManager.register(commandMeta, new VoiceCommand(this, lang));

        proxy.getEventManager().register(this, new PlayerJoinHandler(this, lang));
        proxy.getEventManager().register(this, new PlayerQuitHandler(this, lang));

        reload();
    }

    @Override
    public void reload() {
        reloadConfig();
        lang = resolveConfiguredLanguage();
        Logger.info(Language.getMessage(lang, "plugin-config-loaded"));
        Logger.info(Language.getMessage(lang, "plugin-command-executor"));

        host = getConfig().getString("config.voicecraft.host");
        port = getConfig().getInt("config.voicecraft.port");
        loginToken = getConfig().getString("config.voicecraft.login-token");
        int proximityDistance = getConfig().getInt("config.voice.proximity-distance", 30);
        boolean proximityToggle = getConfig().getBoolean("config.voice.proximity-toggle", true);
        boolean voiceEffects = getConfig().getBoolean("config.voice.voice-effects", true);
        sessionManager.configure(host, port, loginToken, proximityDistance, proximityToggle, voiceEffects);

        if (getConfig().getBoolean("config.auto-reconnect", true)) {
            isConnected = reconnect(true);
        }

        int positionTaskInterval = getConfig().getInt("config.voice.position-update-interval-ticks", 1);
        if (taskRunner != null) {
            taskRunner.cancel();
        }
        positionsTask = new PositionsTask(this, lang);
        taskRunner = proxy.getScheduler().buildTask(this, () -> {
            if (!positionsTask.run()) {
                taskRunner.cancel();
            }
        }).repeat(positionTaskInterval * 50L, TimeUnit.MILLISECONDS).schedule();

        updateSettings(proximityDistance, proximityToggle, voiceEffects);
    }

    @Override
    public Boolean connect(String host, int port, String loginToken) {
        if (host == null || loginToken == null) {
            Logger.warn(Language.getMessage(lang, "plugin-connect-invalid-data"));
            return false;
        }

        try {
            getConfig().set("config.voicecraft.host", host);
            getConfig().set("config.voicecraft.port", port);
            getConfig().set("config.voicecraft.login-token", loginToken);
            saveConfig();
            reload();
            return isConnected;
        } catch (SerializationException exception) {
            Logger.error("Failed to update config: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public Boolean reconnect(Boolean force) {
        if (isConnected && !force) {
            return true;
        }
        if (isConnected) {
            disconnect("Reconnecting to another server.");
        }

        if (Objects.nonNull(host) && Objects.nonNull(loginToken)) {
            boolean connected = sessionManager.connect();
            String sessionToken = connected ? sessionManager.getSessionToken() : null;
            if (sessionToken != null && !sessionToken.isBlank()) {
                Logger.info(Language.getMessage(lang, "plugin-connect-connected"));
                isConnected = true;
                token = sessionToken;
            } else {
                Logger.warn(Language.getMessage(lang, "plugin-connect-failed"));
            }
            return isConnected;
        }

        Logger.warn(Language.getMessage(lang, "plugin-connect-invalid-data"));
        return false;
    }

    @Override
    public void disconnect(String reason) {
        if (!isConnected) {
            return;
        }

        if (sessionManager.isConnected()) {
            sessionManager.disconnect();
        }
        isConnected = false;
        token = "";
        playerBinds.clear();

        String disconnectMessage = Language.getMessage(lang, "plugin-connection-disconnect").replace("$reason", reason);
        Logger.info(disconnectMessage);
        if (getConfig().getBoolean("config.voice.send-voicecraft-disconnect-message", true)) {
            proxy.sendMessage(Component.text(disconnectMessage).color(NamedTextColor.YELLOW));
        }
    }

    @Override
    public void disconnect() {
        disconnect("N.A.");
    }

    public Boolean bind(int playerKey, Player player, int tries) {
        if (!isConnected) {
            return false;
        }
        if (playerBinds.getOrDefault(player.getUsername(), false)) {
            return true;
        }

        boolean bound = sessionManager.bindPlayer(playerKey, player.getUniqueId().toString(), player.getUsername());
        if (!bound && tries == 0 && !sessionManager.isConnected()) {
            Logger.info("VoiceCraft session dropped during bind, reconnecting...");
            isConnected = reconnect(true);
            return bind(playerKey, player, 1);
        }
        if (!bound) {
            messageHandler.sendPlayerBindSync(player);
            return false;
        }

        playerBinds.put(player.getUsername(), true);
        messageHandler.sendPlayerBindSync(player);
        Logger.info(Language.getMessage(lang, "player-binded").replace("$player", player.getUsername()));
        if (getConfig().getBoolean("config.voice.send-bind-message", true)) {
            proxy.sendMessage(Component.text(player.getUsername()).decorate(TextDecoration.BOLD)
                    .append(Component.text(Language.getMessage(lang, "player-binded").replace("$player", "")).color(NamedTextColor.DARK_GREEN)));
        }
        return true;
    }

    public Boolean bind(int playerKey, Player player) {
        return bind(playerKey, player, 0);
    }

    @Override
    public Boolean bindFake(int playerKey, String name, int tries) {
        if (!isConnected) {
            return false;
        }

        boolean bound = sessionManager.bindFakePlayer(playerKey, "fake:" + playerKey, name);
        if (!bound && tries == 0 && !sessionManager.isConnected()) {
            isConnected = reconnect(true);
            return bindFake(playerKey, name, 1);
        }
        if (bound) {
            playerBinds.put(name, true);
        }
        return bound;
    }

    @Override
    public Boolean bindFake(int playerKey, String name) {
        return bindFake(playerKey, name, 0);
    }

    public Boolean disconnectPlayer(Player player, int tries) {
        if (!isConnected) {
            return false;
        }

        boolean disconnected = sessionManager.unbindPlayer(player.getUniqueId().toString());
        if (!disconnected && tries == 0 && !sessionManager.isConnected()) {
            isConnected = reconnect(true);
            return disconnectPlayer(player, 1);
        }
        if (disconnected) {
            playerBinds.remove(player.getUsername());
            messageHandler.sendBindSync(player.getUsername(), false);
        }
        return disconnected;
    }

    public Boolean disconnectPlayer(Player player) {
        return disconnectPlayer(player, 0);
    }

    public boolean handleProxyBindRequest(int bindingKey, String playerId, String playerName) {
        boolean bound = sessionManager.bindPlayer(bindingKey, playerId, playerName);
        if (bound) {
            playerBinds.put(playerName, true);
        }
        return bound;
    }

    public void handleProxyUnbindRequest(String playerId, String playerName) {
        sessionManager.unbindPlayer(playerId);
        playerBinds.remove(playerName);
    }

    @Override
    public Boolean updateSettings(int proximityDistance, Boolean proximityToggle, Boolean voiceEffects) {
        if (!isConnected) {
            return false;
        }
        return sessionManager.updateSettings(proximityDistance, proximityToggle, voiceEffects);
    }

    @Override
    public void setNotConnected() {
        if (!isConnected) {
            return;
        }
        isConnected = false;
        token = "";
        playerBinds.clear();
        sessionManager.close();
    }

    public static VelocityYamlConfig getConfig() {
        return config;
    }

    @Override
    public void saveResource(String resourcePath) {
        Path target = dataFolder.toPath().resolve(resourcePath);
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (input == null) {
                    return;
                }
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            Logger.error("Could not save " + resourcePath + ": " + exception.getMessage());
        }
    }

    @Override
    public void saveConfig() {
        try {
            config.save();
        } catch (IOException exception) {
            Logger.error("Could not save config: " + exception.getMessage());
        }
    }

    @Override
    public void reloadConfig() {
        try {
            config.reload();
        } catch (IOException exception) {
            Logger.error("Could not reload config: " + exception.getMessage());
        }
    }

    private String resolveConfiguredLanguage() {
        return PluginLocalization.resolveConfiguredLanguage(getConfig().getString("config.lang", "system"));
    }

    private void ensureLocalizedConfigExists() {
        Path target = dataFolder.toPath().resolve("config.yml");
        if (Files.exists(target)) {
            return;
        }

        String language = PluginLocalization.resolveSystemLanguage();
        try {
            try (InputStream input = openConfigTemplate(language)) {
                if (input == null) {
                    throw new IOException("Missing embedded config template for language " + language);
                }
                ConfigTemplateWriter.write(input, target,
                        Map.of("__GENERATED_LOGIN_TOKEN__", UUID.randomUUID().toString()));
            }
        } catch (IOException exception) {
            Logger.error("Could not create localized config.yml: " + exception.getMessage());
        }
    }

    private InputStream openConfigTemplate(String language) {
        InputStream input = getClass().getClassLoader().getResourceAsStream("config/" + language + ".yml");
        return input != null ? input : getClass().getClassLoader().getResourceAsStream("config/en.yml");
    }
}
