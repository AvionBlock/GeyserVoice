package team.avion.adapter.plasmo.udp;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;
import team.avion.adapter.plasmo.PlasmoProtocolConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlasmoUdpCodec {
    private final Map<Integer, Supplier<? extends PlasmoPacket>> packetFactories = new LinkedHashMap<>();
    private final Map<Class<?>, Integer> packetIds = new LinkedHashMap<>();

    public <T extends PlasmoPacket> void register(PlasmoUdpPacketType type, Class<T> packetClass, Supplier<T> factory) {
        packetFactories.put(type.id(), factory);
        packetIds.put(packetClass, type.id());
    }

    public byte[] encode(PlasmoPacket packet, UUID secret) throws IOException {
        Integer id = packetIds.get(packet.getClass());
        if (id == null) {
            throw new IOException("Unregistered Plasmo UDP packet " + packet.getClass().getName());
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(PlasmoProtocolConstants.UDP_MAGIC);
        output.writeByte(id);
        PlasmoPacketUtil.writeUuid(output, secret);
        output.writeLong(System.currentTimeMillis());
        packet.write(output);
        output.flush();
        return bytes.toByteArray();
    }

    public PlasmoUdpEnvelope decode(byte[] data) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        int magic = input.readInt();
        if (magic != PlasmoProtocolConstants.UDP_MAGIC) {
            throw new IOException("Invalid Plasmo UDP magic");
        }

        int id = input.readUnsignedByte();
        PlasmoUdpPacketType type = PlasmoUdpPacketType.byId(id);
        Supplier<? extends PlasmoPacket> factory = packetFactories.get(id);
        if (type == null || factory == null) {
            throw new IOException("Unknown Plasmo UDP packet id " + id);
        }

        UUID secret = PlasmoPacketUtil.readUuid(input);
        long timestamp = input.readLong();
        PlasmoPacket packet = factory.get();
        packet.read(input);
        return new PlasmoUdpEnvelope(secret, timestamp, type, packet);
    }
}
