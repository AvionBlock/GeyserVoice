package team.avion.adapter.plasmo.session;

import java.net.SocketAddress;
import java.util.UUID;

public final class PlasmoClientSession {
    private final UUID playerId;
    private final String playerName;
    private final int voiceCraftEntityId;
    private final UUID udpSecret;
    private SocketAddress remoteAddress;
    private long lastSeenMillis;

    public PlasmoClientSession(UUID playerId, String playerName, int voiceCraftEntityId, UUID udpSecret) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.voiceCraftEntityId = voiceCraftEntityId;
        this.udpSecret = udpSecret;
        this.lastSeenMillis = System.currentTimeMillis();
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public int voiceCraftEntityId() {
        return voiceCraftEntityId;
    }

    public UUID udpSecret() {
        return udpSecret;
    }

    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    public long lastSeenMillis() {
        return lastSeenMillis;
    }

    public void touch(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.lastSeenMillis = System.currentTimeMillis();
    }
}
