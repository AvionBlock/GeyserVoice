package team.avion.adapter.plasmo.udp;

import team.avion.adapter.plasmo.PlasmoPacket;

import java.util.UUID;

public record PlasmoUdpEnvelope(UUID secret, long timestamp, PlasmoUdpPacketType type, PlasmoPacket packet) {
}
