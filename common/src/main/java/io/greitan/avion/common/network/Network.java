package io.greitan.avion.common.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.greitan.avion.common.utils.BaseLogger;
import io.greitan.avion.common.network.Payloads.*;

public class Network {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final BaseLogger logger;

    private static final String VERSION = "1.0.0";

    public <T extends BaseLogger> Network(T logger) {
        this.logger = logger;
    }

    public MCCommPacket sendPostRequest(String url, MCCommPacket data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            logger.debug("Request: " + jsonData);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            logger.debug("Response: " + body);

            if (statusCode == 200) {
                return objectMapper.readValue(body, MCCommPacket.class);
            } else {
                logger.error("Sending HTTP Packet Failed, Reason: HTTP_EXCEPTION, STATUS_CODE: " + statusCode);
                return null;
            }
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.toString();
            logger.error("Can't connect to voice chat server! " + message);
            return null;
        }
    }

    /**
     * Sends the login request to the server.
     *
     * @param link      HTTP POST link
     * @param serverKey The server key
     * @return Token if connected successfully, otherwise null.
     */
    public String sendLoginRequest(String link, String serverKey) {
        LoginPacket loginPacket = new LoginPacket(serverKey, VERSION);

        MCCommPacket response = sendPostRequest(link, loginPacket);
        if (response != null) {
            if (response.packetId() == PacketType.Accept.ordinal()) {
                // Cast is safe because of PacketId check, but for records we can use pattern matching in future
                if (response instanceof AcceptPacket acceptPacket) {
                    return acceptPacket.token();
                }
                // Fallback if Jackson deserialized to a different subtype but ID matches (unlikely with proper setup)
                AcceptPacket packetData = objectMapper.convertValue(response, AcceptPacket.class);
                return packetData.token();
            } else if (response.packetId() == PacketType.Deny.ordinal() || response instanceof DenyPacket) {
                DenyPacket packetData = objectMapper.convertValue(response, DenyPacket.class);
                logger.error("Login Denied. Server denied link request! Reason: " + packetData.reason());
            }
        } else {
            logger.error("Could not contact server. Please check if your IPAddress and Port are correct!");
        }
        return null;
    }

    /**
     * Sends the logout request to the server.
     *
     * @param link  HTTP POST link
     * @param token The session token
     */
    public void sendLogoutRequest(String link, String token) {
        LogoutPacket logoutPacket = new LogoutPacket(token);
        sendPostRequest(link, logoutPacket);
    }

    /**
     * Sends the bind request to the server.
     *
     * @param link       HTTP POST link
     * @param token      The token from the login
     * @param playerKey  The bind key for the player
     * @param playerId   The unique but consistent ID of the player
     * @param playerName The name of the player
     * @return "SUCCESS" if binded successfully, otherwise null or reason for failure.
     */
    public String sendBindRequest(String link, String token, Integer playerKey, String playerId, String playerName) {
        BindPacket bindPacket = new BindPacket(token, playerId, playerKey, playerName);

        MCCommPacket bindStatus = sendPostRequest(link, bindPacket);
        if (bindStatus == null)
            return null;

        if (bindStatus.packetId() == PacketType.Accept.ordinal()) {
            return "SUCCESS";
        } else if (bindStatus instanceof DenyPacket denyPacket) {
            logger.error("Binding " + playerName + " to " + playerKey + " failed. Reason: " + denyPacket.reason());
            return denyPacket.reason();
        }
        return null;
    }

    /**
     * Sends the disconnect request to the server.
     *
     * @param link       HTTP POST link
     * @param token      The token from the login
     * @param playerId   The unique but consistent ID of the player
     * @param playerName The name of the player
     * @return "SUCCESS" if disconnected successfully, otherwise null or reason for failure.
     */
    public String sendDisconnectRequest(String link, String token, String playerId, String playerName) {
        DisconnectParticipantPacket disconnectParticipantPacket = new DisconnectParticipantPacket(token, playerId);

        MCCommPacket disconnectStatus = sendPostRequest(link, disconnectParticipantPacket);
        if (disconnectStatus == null)
            return null;

        if (disconnectStatus.packetId() == PacketType.Accept.ordinal()) {
            return "SUCCESS";
        } else if (disconnectStatus instanceof DenyPacket denyPacket) {
            logger.error("Disconnecting player " + playerName + " failed. Reason: " + denyPacket.reason());
            return denyPacket.reason();
        }
        return null;
    }

    /**
     * Updates the voice chat settings.
     *
     * @param link              HTTP POST link
     * @param token             The token from the login
     * @param proximityDistance Proximity distance setting.
     * @param proximityToggle   Proximity toggle setting.
     * @param voiceEffects      Voice effects setting.
     * @return True if settings were updated successfully, otherwise false.
     */
    public boolean sendUpdateSettingsRequest(String link, String token, int proximityDistance, boolean proximityToggle,
            boolean voiceEffects) {
        SetDefaultSettingsPacket setDefaultSettingsPacket = new SetDefaultSettingsPacket(
            token, proximityDistance, proximityToggle, voiceEffects
        );

        return sendPostRequest(link, setDefaultSettingsPacket) != null;
    }
}
