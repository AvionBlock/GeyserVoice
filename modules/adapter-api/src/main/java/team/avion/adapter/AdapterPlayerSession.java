package team.avion.adapter;

import java.util.UUID;

public record AdapterPlayerSession(UUID playerId, String playerName, int voiceCraftEntityId) {
}
