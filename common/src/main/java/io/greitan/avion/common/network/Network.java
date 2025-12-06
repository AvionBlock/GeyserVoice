package io.greitan.avion.common.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.greitan.avion.common.utils.BaseLogger;
import io.greitan.avion.common.utils.Constants;
import io.greitan.avion.common.network.Payloads.*;

public class Network {
    public static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final BaseLogger logger;

    public <T extends BaseLogger> Network(T logger) {
        this.logger = logger;
    }

    /**
     * Sends an asynchronous POST request.
     */
    public CompletableFuture<MCCommPacket> sendPostRequestAsync(String url, MCCommPacket data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        int statusCode = response.statusCode();
                        String body = response.body();

                        if (statusCode == 200) {
                            try {
                                return objectMapper.readValue(body, MCCommPacket.class);
                            } catch (Exception e) {
                                logger.error("Failed to parse response: " + e.getMessage());
                                return null;
                            }
                        } else {
                            logger.error("HTTP Packet Failed. Status: " + statusCode);
                            return null;
                        }
                    })
                    .exceptionally(e -> {
                        String message = e.getMessage() != null ? e.getMessage() : e.toString();
                        // Suppress common connection errors to avoid log spam during outages
                        logger.error("Connection error: " + message);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Failed to build request: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Blocking wrapper for synchronous contexts (e.g. initialization).
     */
    public MCCommPacket sendPostRequest(String url, MCCommPacket data) {
        try {
            return sendPostRequestAsync(url, data).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Synchronous request failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sends the login request to the server.
     */
    public String sendLoginRequest(String link, String serverKey) {
        LoginPacket loginPacket = new LoginPacket(serverKey, Constants.VERSION);
        MCCommPacket response = sendPostRequest(link, loginPacket);
        
        if (response != null) {
            if (response.getPacketId() == PacketType.Accept.ordinal()) {
                if (response instanceof AcceptPacket acceptPacket) {
                    return acceptPacket.getToken();
                }
                AcceptPacket packetData = objectMapper.convertValue(response, AcceptPacket.class);
                return packetData.getToken();
            } else if (response.getPacketId() == PacketType.Deny.ordinal() || response instanceof DenyPacket) {
                DenyPacket packetData = objectMapper.convertValue(response, DenyPacket.class);
                logger.error("Login Denied: " + packetData.reason);
            }
        } else {
            logger.error("Could not contact server. Check IP and Port.");
        }
        return null;
    }

    /**
     * Sends the logout request to the server.
     */
    public void sendLogoutRequest(String link, String token) {
        LogoutPacket logoutPacket = new LogoutPacket(token);
        sendPostRequestAsync(link, logoutPacket); // Fire and forget
    }

    /**
     * Sends the bind request to the server.
     */
    public String sendBindRequest(String link, String token, Integer playerKey, String playerId, String playerName) {
        BindPacket bindPacket = new BindPacket(token, playerId, playerKey, playerName);
        MCCommPacket bindStatus = sendPostRequest(link, bindPacket);
        
        if (bindStatus == null) return null;

        if (bindStatus.getPacketId() == PacketType.Accept.ordinal()) {
            return "SUCCESS";
        } else if (bindStatus instanceof DenyPacket denyPacket) {
            logger.error("Binding " + playerName + " failed: " + denyPacket.reason);
            return denyPacket.reason;
        }
        return null;
    }

    /**
     * Sends the disconnect request to the server.
     */
    public String sendDisconnectRequest(String link, String token, String playerId, String playerName) {
        DisconnectParticipantPacket packet = new DisconnectParticipantPacket(token, playerId);
        MCCommPacket status = sendPostRequest(link, packet);
        
        if (status == null) return null;

        if (status.getPacketId() == PacketType.Accept.ordinal()) {
            return "SUCCESS";
        } else if (status instanceof DenyPacket denyPacket) {
            logger.error("Disconnecting player " + playerName + " failed: " + denyPacket.reason);
            return denyPacket.reason;
        }
        return null;
    }

    /**
     * Updates the voice chat settings.
     */
    public boolean sendUpdateSettingsRequest(String link, String token, int proximityDistance, boolean proximityToggle, boolean voiceEffects) {
        SetDefaultSettingsPacket packet = new SetDefaultSettingsPacket(token, proximityDistance, proximityToggle, voiceEffects);
        return sendPostRequest(link, packet) != null;
    }
}
