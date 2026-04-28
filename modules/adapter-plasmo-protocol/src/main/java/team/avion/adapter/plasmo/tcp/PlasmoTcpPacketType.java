package team.avion.adapter.plasmo.tcp;

public enum PlasmoTcpPacketType {
    CONNECTION(1),
    PLAYER_INFO_REQUEST(2),
    CONFIG(3),
    CONFIG_PLAYER_INFO(4),
    LANGUAGE_REQUEST(5),
    LANGUAGE(6),
    PLAYER_LIST(7),
    PLAYER_INFO_UPDATE(8),
    PLAYER_DISCONNECT(9),
    PLAYER_INFO(10),
    PLAYER_STATE(11),
    PLAYER_AUDIO_END(12),
    PLAYER_ACTIVATION_DISTANCES(13),
    DISTANCE_VISUALIZE(14),
    SOURCE_INFO_REQUEST(15),
    SOURCE_INFO(16),
    SELF_SOURCE_INFO(17),
    SOURCE_AUDIO_END(18),
    ACTIVATION_REGISTER(19),
    ACTIVATION_UNREGISTER(20),
    SOURCE_LINE_REGISTER(21),
    SOURCE_LINE_UNREGISTER(22),
    SOURCE_LINE_PLAYER_ADD(23),
    SOURCE_LINE_PLAYER_REMOVE(24),
    SOURCE_LINE_PLAYERS_LIST(25),
    ANIMATED_ACTION_BAR(26);

    private final int id;

    PlasmoTcpPacketType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static PlasmoTcpPacketType byId(int id) {
        for (PlasmoTcpPacketType value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return null;
    }
}
