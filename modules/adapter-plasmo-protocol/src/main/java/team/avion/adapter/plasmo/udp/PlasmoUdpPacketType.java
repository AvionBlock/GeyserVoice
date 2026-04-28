package team.avion.adapter.plasmo.udp;

public enum PlasmoUdpPacketType {
    PING(1),
    PLAYER_AUDIO(2),
    SOURCE_AUDIO(3),
    SELF_AUDIO_INFO(4),
    CUSTOM(0x100);

    private final int id;

    PlasmoUdpPacketType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static PlasmoUdpPacketType byId(int id) {
        for (PlasmoUdpPacketType value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return null;
    }
}
