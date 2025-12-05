package io.greitan.avion.common;

import java.util.Map;

public interface BaseGeyserVoice {
    boolean isConnected();
    String getHost();
    int getPort();
    String getServerKey();
    Map<String, Boolean> getPlayerBinds();
    String getToken();
    String getLang();

    /**
     * Reloads the plugin configuration and initializes connections.
     */
    void reload();

    /**
     * Connects to a new server.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @param serverKey The server key.
     * @return True if connected successfully, otherwise false.
     */
    Boolean connect(String host, int port, String serverKey);

    /**
     * Reconnects to the server.
     *
     * @param force Indicates whether to force a connection.
     * @return True if connected successfully, otherwise false.
     */
    Boolean reconnect(Boolean force);
    
    /**
     * Disconnects from the server.
     *
     * @param reason The reason why we disconnected
     */
    void disconnect(String reason);
    
    /**
     * Disconnects from the server.
     */
    void disconnect();

    /**
     * Bind a fake player
     * @param bindKey
     * @param name
     * @param tries
     * @return
     */
    Boolean bindFake(int bindKey, String name, int tries);
    
    /**
     * Bind a fake player
     * @param bindKey
     * @param name
     * @return
     */
    Boolean bindFake(int bindKey, String name);

    /**
     * Updates the voice chat settings.
     *
     * @param proximityDistance Proximity distance setting.
     * @param proximityToggle   Proximity toggle setting.
     * @param voiceEffects      Voice effects setting.
     * @return True if settings were updated successfully, otherwise false.
     */
    Boolean updateSettings(int proximityDistance, Boolean proximityToggle, Boolean voiceEffects);

    /**
     * Allows the TaskRunner to set the connected state to false
     */
    void setNotConnected();

    void saveResource(String resourcePath);

    void saveConfig();
    void reloadConfig();
}
