package team.avion.adapter.plasmo.tcp;

import team.avion.adapter.plasmo.tcp.packet.ConnectionPacket;
import team.avion.adapter.plasmo.tcp.packet.ConfigPacket;
import team.avion.adapter.plasmo.tcp.packet.PlayerListPacket;
import team.avion.adapter.plasmo.tcp.packet.PlayerInfoPacket;
import team.avion.adapter.plasmo.tcp.packet.PlayerInfoRequestPacket;

public final class PlasmoTcpCodecs {
    private PlasmoTcpCodecs() {
    }

    public static PlasmoTcpCodec createBaseCodec() {
        PlasmoTcpCodec codec = new PlasmoTcpCodec();
        codec.register(PlasmoTcpPacketType.CONNECTION, ConnectionPacket.class, ConnectionPacket::new);
        codec.register(PlasmoTcpPacketType.PLAYER_INFO_REQUEST, PlayerInfoRequestPacket.class, PlayerInfoRequestPacket::new);
        codec.register(PlasmoTcpPacketType.CONFIG, ConfigPacket.class, ConfigPacket::new);
        codec.register(PlasmoTcpPacketType.PLAYER_LIST, PlayerListPacket.class, PlayerListPacket::new);
        codec.register(PlasmoTcpPacketType.PLAYER_INFO, PlayerInfoPacket.class, PlayerInfoPacket::new);
        return codec;
    }
}
