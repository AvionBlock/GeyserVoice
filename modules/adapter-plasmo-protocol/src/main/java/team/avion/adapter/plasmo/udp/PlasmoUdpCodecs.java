package team.avion.adapter.plasmo.udp;

import team.avion.adapter.plasmo.udp.packet.CustomPacket;
import team.avion.adapter.plasmo.udp.packet.PingPacket;
import team.avion.adapter.plasmo.udp.packet.PlayerAudioPacket;
import team.avion.adapter.plasmo.udp.packet.SelfAudioInfoPacket;
import team.avion.adapter.plasmo.udp.packet.SourceAudioPacket;

public final class PlasmoUdpCodecs {
    private PlasmoUdpCodecs() {
    }

    public static PlasmoUdpCodec createBaseCodec() {
        PlasmoUdpCodec codec = new PlasmoUdpCodec();
        codec.register(PlasmoUdpPacketType.PING, PingPacket.class, PingPacket::new);
        codec.register(PlasmoUdpPacketType.PLAYER_AUDIO, PlayerAudioPacket.class, PlayerAudioPacket::new);
        codec.register(PlasmoUdpPacketType.SOURCE_AUDIO, SourceAudioPacket.class, SourceAudioPacket::new);
        codec.register(PlasmoUdpPacketType.SELF_AUDIO_INFO, SelfAudioInfoPacket.class, SelfAudioInfoPacket::new);
        codec.register(PlasmoUdpPacketType.CUSTOM, CustomPacket.class, CustomPacket::new);
        return codec;
    }
}
