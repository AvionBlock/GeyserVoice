package io.greitan.mineserv.common;

// import lombok.Getter;
import java.util.Map;
import java.util.HashMap;

public interface BaseGeyserVoice {
    public boolean isConnected = false;
    public String host = "";
    public int port = 0;
    public String serverKey = "";
    public Map<String, Boolean> playerBinds = new HashMap<>();
    public String token = "";
    public String lang = "";

    /**
     * Reloads the plugin configuration and initializes connections.
     */
    abstract public void reload();

    /**
     * Connects to the server.
     *
     * @param force Indicates whether to force a connection.
     * @return True if connected successfully, otherwise false.
     */
    abstract public Boolean connect(Boolean force);

    /**
     * Updates the voice chat settings.
     *
     * @param proximityDistance Proximity distance setting.
     * @param proximityToggle   Proximity toggle setting.
     * @param voiceEffects      Voice effects setting.
     * @return True if settings were updated successfully, otherwise false.
     */
    abstract public Boolean updateSettings(int proximityDistance, Boolean proximityToggle, Boolean voiceEffects);

    /**
     * Allows the TaskRunner to set the connected state to false
     */
    abstract public void setNotConnected();

    abstract public void saveResource(String resourcePath);
}
