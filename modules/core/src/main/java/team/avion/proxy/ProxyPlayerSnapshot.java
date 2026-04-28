package team.avion.proxy;

public record ProxyPlayerSnapshot(
        String playerId,
        String playerName,
        String dimensionId,
        double x,
        double y,
        double z,
        double rotation,
        double echoFactor,
        boolean muffled,
        boolean dead) {

    public ProxyPlayerSnapshot withDimensionId(String newDimensionId) {
        return new ProxyPlayerSnapshot(playerId, playerName, newDimensionId, x, y, z, rotation, echoFactor, muffled,
                dead);
    }
}
