package io.greitan.avion.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.greitan.avion.common.BaseGeyserVoice;
import io.greitan.avion.common.network.Network;
import io.greitan.avion.fabric.utils.FabricLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Simple config placeholder until a proper library is used
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FabricGeyserVoice implements ModInitializer, BaseGeyserVoice {
    public static final Logger LOGGER = LoggerFactory.getLogger("geyservoice");
    private static FabricGeyserVoice instance;
    
    private boolean isConnected = false;
    private String host = "localhost";
    private int port = 9050;
    private String serverKey = "key";
    private Map<String, Boolean> playerBinds = new HashMap<>();
    private String token = "";
    private String lang = "en";

    private MinecraftServer server;
    public FabricLogger loggerWrapper = new FabricLogger();
    public Network network = new Network(loggerWrapper);
    
    // Config
    private Properties config = new Properties();
    private Path configPath;

    @Override
    public void onInitialize() {
        instance = this;
        configPath = FabricLoader.getInstance().getConfigDir().resolve("GeyserVoice/config.properties");
        
        LOGGER.info("Initializing GeyserVoice for Fabric...");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.server = server;
            reload();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            disconnect("Server stopping");
            this.server = null;
        });
        
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            io.greitan.avion.fabric.commands.FabricVoiceCommand.register(dispatcher);
        });
    }

    public static FabricGeyserVoice getInstance() {
        return instance;
    }

    @Override
    public boolean isConnected() { return isConnected; }
    @Override
    public String getHost() { return host; }
    @Override
    public int getPort() { return port; }
    @Override
    public String getServerKey() { return serverKey; }
    @Override
    public Map<String, Boolean> getPlayerBinds() { return playerBinds; }
    @Override
    public String getToken() { return token; }
    @Override
    public String getLang() { return lang; }

    @Override
    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        
        this.lang = config.getProperty("lang", "en");
        this.host = config.getProperty("host", "localhost");
        try {
            this.port = Integer.parseInt(config.getProperty("port", "9050"));
        } catch (NumberFormatException e) {
            this.port = 9050;
        }
        this.serverKey = config.getProperty("server-key", "key");
        
        boolean autoReconnect = Boolean.parseBoolean(config.getProperty("auto-reconnect", "true"));

        loggerWrapper.info("Configuration loaded.");

        if (autoReconnect)
            isConnected = reconnect(true);
            
        int proximityDistance = Integer.parseInt(config.getProperty("voice.proximity-distance", "30"));
        boolean proximityToggle = Boolean.parseBoolean(config.getProperty("voice.proximity-toggle", "true"));
        boolean voiceEffects = Boolean.parseBoolean(config.getProperty("voice.voice-effects", "true"));

        updateSettings(proximityDistance, proximityToggle, voiceEffects);
    }

    @Override
    public Boolean connect(String host, int port, String serverKey) {
        if (Objects.nonNull(host) && Objects.nonNull(serverKey)) {
            config.setProperty("host", host);
            config.setProperty("port", String.valueOf(port));
            config.setProperty("server-key", serverKey);
            saveConfig();
            reloadConfig();
            reload();
            return isConnected;
        } else {
            loggerWrapper.warn("Invalid connection data provided.");
            return false;
        }
    }

    @Override
    public Boolean reconnect(Boolean force) {
        if (isConnected && !force)
            return true;
        if (isConnected) {
            disconnect("Reconnecting to another server.");
        }

        if (Objects.nonNull(host) && Objects.nonNull(serverKey)) {
            String link = "http://" + host + ":" + port;
            String newToken = network.sendLoginRequest(link, serverKey);
            if (Objects.nonNull(newToken)) {
                loggerWrapper.info("Connected to VoiceCraft server successfully.");
                isConnected = true;
                token = newToken;
            } else {
                loggerWrapper.warn("Failed to connect to VoiceCraft server.");
            }
            return isConnected;
        } else {
            loggerWrapper.warn("Invalid connection data.");
            return false;
        }
    }

    @Override
    public void disconnect(String reason) {
        if (!isConnected) return;

        if (Objects.nonNull(host) && Objects.nonNull(serverKey)) {
            String link = "http://" + host + ":" + port;
            network.sendLogoutRequest(link, token);
            isConnected = false;
            loggerWrapper.info("Disconnected from VoiceCraft server: " + reason);
        }
    }

    @Override
    public void disconnect() {
        disconnect("N.A.");
    }

    @Override
    public Boolean bindFake(int bindKey, String name, int tries) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
            return false;
            
        if (playerBinds.containsKey(name) && playerBinds.get(name)) {
            return true;
        }
            
        String link = "http://" + host + ":" + port;
        // Fake players might need a dummy UUID or handled differently
        // Using hashcode of name as dummy ID for now
        String dummyId = java.util.UUID.nameUUIDFromBytes(name.getBytes()).toString();

        String result = network.sendBindRequest(link, token, bindKey, dummyId, name);
        playerBinds.put(name, false);
        if (result != null) {
            if (result.equals("SUCCESS")) {
                playerBinds.put(name, true);
                loggerWrapper.info("Bound fake player " + name);
                return true;
            } else if (result.equals("Invalid Token!") && tries == 0) {
                loggerWrapper.info("Invalid Token detected, reconnecting...");
                isConnected = reconnect(true);
                return bindFake(bindKey, name, 1);
            }
        }
        return false;
    }

    @Override
    public Boolean bindFake(int bindKey, String name) {
        return bindFake(bindKey, name, 0);
    }

    @Override
    public Boolean updateSettings(int proximityDistance, Boolean proximityToggle, Boolean voiceEffects) {
        if (!isConnected || Objects.isNull(host) || Objects.isNull(serverKey))
            return false;
        String link = "http://" + host + ":" + port;

        return network.sendUpdateSettingsRequest(link, token, proximityDistance, proximityToggle, voiceEffects);
    }

    @Override
    public void setNotConnected() {
        this.isConnected = false;
    }

    @Override
    public void saveResource(String resourcePath) {
        // Not implemented for Fabric basic
    }

    private void saveDefaultConfig() {
        try {
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            if (!Files.exists(configPath)) {
                config.setProperty("debug", "false");
                config.setProperty("lang", "en");
                config.setProperty("host", "localhost");
                config.setProperty("port", "9050");
                config.setProperty("server-key", "key");
                config.setProperty("auto-reconnect", "true");
                config.setProperty("voice.proximity-distance", "30");
                config.setProperty("voice.proximity-toggle", "true");
                config.setProperty("voice.voice-effects", "true");
                saveConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveConfig() {
        try (FileOutputStream out = new FileOutputStream(configPath.toFile())) {
            config.store(out, "GeyserVoice Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reloadConfig() {
        try (FileInputStream in = new FileInputStream(configPath.toFile())) {
            config.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
