package team.avion.bungeecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import team.avion.bungeecord.commands.VoiceCommand;
import team.avion.bungeecord.listeners.PlayerJoinHandler;
import team.avion.bungeecord.listeners.PlayerQuitHandler;
import team.avion.bungeecord.listeners.PluginMessageHandler;
import team.avion.bungeecord.tasks.PositionsTask;
import team.avion.bungeecord.utils.BungeecordLogger;
import team.avion.bungeecord.utils.Language;
import team.avion.common.BaseGeyserVoice;
import team.avion.common.config.ConfigTemplateWriter;
import team.avion.common.localization.PluginLocalization;
import team.avion.proxy.VoiceCraftProxySessionManager;

public class GeyserVoice extends Plugin implements BaseGeyserVoice {
    private static @Getter Configuration config;
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

    private @Getter ScheduledTask taskRunner;

    public final BungeecordLogger Logger = new BungeecordLogger();

    public GeyserVoice() {
        this.sessionManager = new VoiceCraftProxySessionManager(Logger);
    }

    @Override
    public void onEnable() {
        instance = this;

        ensureLocalizedConfigExists();
        reloadConfig();
        lang = resolveConfiguredLanguage();
        Language.init(this);

        getProxy().registerChannel(PluginMessageHandler.CHANNEL);
        getProxy().getPluginManager().registerListener(this, messageHandler);
        getProxy().getPluginManager().registerCommand(this, new VoiceCommand(this, lang));
        getProxy().getPluginManager().registerListener(this, new PlayerJoinHandler(this, lang));
        getProxy().getPluginManager().registerListener(this, new PlayerQuitHandler(this, lang));

        reload();
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(PluginMessageHandler.CHANNEL);
        if (taskRunner != null) {
            taskRunner.cancel();
        }
        sessionManager.close();
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
        taskRunner = getProxy().getScheduler().schedule(this, new PositionsTask(this, lang), 1,
                50L * positionTaskInterval, TimeUnit.MILLISECONDS);

        updateSettings(proximityDistance, proximityToggle, voiceEffects);
    }

    @Override
    public Boolean connect(String host, int port, String loginToken) {
        if (host == null || loginToken == null) {
            Logger.warn(Language.getMessage(lang, "plugin-connect-invalid-data"));
            return false;
        }

        getConfig().set("config.voicecraft.host", host);
        getConfig().set("config.voicecraft.port", port);
        getConfig().set("config.voicecraft.login-token", loginToken);
        saveConfig();
        reload();
        return isConnected;
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
            getProxy().broadcast(new ComponentBuilder(disconnectMessage).color(ChatColor.YELLOW).create());
        }
    }

    @Override
    public void disconnect() {
        disconnect("N.A.");
    }

    public Boolean bind(int playerKey, ProxiedPlayer player, int tries) {
        if (!isConnected) {
            return false;
        }
        if (playerBinds.getOrDefault(player.getName(), false)) {
            return true;
        }

        boolean bound = sessionManager.bindPlayer(playerKey, player.getUniqueId().toString(), player.getName());
        if (!bound && tries == 0 && !sessionManager.isConnected()) {
            isConnected = reconnect(true);
            return bind(playerKey, player, 1);
        }
        if (!bound) {
            messageHandler.sendPlayerBindSync(player);
            return false;
        }

        playerBinds.put(player.getName(), true);
        messageHandler.sendPlayerBindSync(player);
        Logger.info(Language.getMessage(lang, "player-binded").replace("$player", player.getName()));
        if (getConfig().getBoolean("config.voice.send-bind-message", true)) {
            getProxy().broadcast(new ComponentBuilder(player.getName()).bold(true)
                    .append(new ComponentBuilder(Language.getMessage(lang, "player-binded").replace("$player", "")).color(ChatColor.DARK_GREEN).create())
                    .create());
        }
        return true;
    }

    public Boolean bind(int playerKey, ProxiedPlayer player) {
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

    public Boolean disconnectPlayer(ProxiedPlayer player, int tries) {
        if (!isConnected) {
            return false;
        }

        boolean disconnected = sessionManager.unbindPlayer(player.getUniqueId().toString());
        if (!disconnected && tries == 0 && !sessionManager.isConnected()) {
            isConnected = reconnect(true);
            return disconnectPlayer(player, 1);
        }
        if (disconnected) {
            playerBinds.remove(player.getName());
            messageHandler.sendBindSync(player.getName(), false);
        }
        return disconnected;
    }

    public Boolean disconnectPlayer(ProxiedPlayer player) {
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

    public void reloadConfig() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException exception) {
            Logger.error("Could not reload config: " + exception.getMessage());
        }
    }

    @Override
    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(getDataFolder(), "config.yml"));
        } catch (IOException exception) {
            Logger.error("Could not save config: " + exception.getMessage());
        }
    }

    @Override
    public void saveResource(String resourcePath) {
        saveResource(resourcePath, false);
    }

    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace("\\", "/");
        try (InputStream in = getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
            }

            File outFile = new File(getDataFolder(), resourcePath);
            File outDir = outFile.getParentFile();
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            if (!outFile.exists() || replace) {
                try (OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.error("Could not save " + resourcePath + " to " + new File(getDataFolder(), resourcePath));
        }
    }

    private String resolveConfiguredLanguage() {
        return PluginLocalization.resolveConfiguredLanguage(getConfig().getString("config.lang", "system"));
    }

    private void ensureLocalizedConfigExists() {
        File target = new File(getDataFolder(), "config.yml");
        if (target.exists()) {
            return;
        }

        String language = PluginLocalization.resolveSystemLanguage();
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (InputStream input = openConfigTemplate(language)) {
            if (input == null) {
                throw new IOException("Missing embedded config template for language " + language);
            }
            ConfigTemplateWriter.write(input, target.toPath(),
                    Map.of("__GENERATED_LOGIN_TOKEN__", UUID.randomUUID().toString()));
        } catch (IOException exception) {
            Logger.error("Could not create localized config.yml: " + exception.getMessage());
        }
    }

    private InputStream openConfigTemplate(String language) {
        InputStream input = getResourceAsStream("config/" + language + ".yml");
        return input != null ? input : getResourceAsStream("config/en.yml");
    }
}
