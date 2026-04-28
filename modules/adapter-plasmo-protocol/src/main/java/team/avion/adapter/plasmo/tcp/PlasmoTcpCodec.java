package team.avion.adapter.plasmo.tcp;

import team.avion.adapter.plasmo.PlasmoPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PlasmoTcpCodec {
    private final Map<Integer, Supplier<? extends PlasmoPacket>> packetFactories = new LinkedHashMap<>();
    private final Map<Class<?>, Integer> packetIds = new LinkedHashMap<>();

    public <T extends PlasmoPacket> void register(PlasmoTcpPacketType type, Class<T> packetClass, Supplier<T> factory) {
        packetFactories.put(type.id(), factory);
        packetIds.put(packetClass, type.id());
    }

    public byte[] encode(PlasmoPacket packet) throws IOException {
        Integer id = packetIds.get(packet.getClass());
        if (id == null) {
            throw new IOException("Unregistered Plasmo TCP packet " + packet.getClass().getName());
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeByte(id);
        packet.write(output);
        output.flush();
        return bytes.toByteArray();
    }

    public PlasmoPacket decode(byte[] data) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        int id = input.readUnsignedByte();
        Supplier<? extends PlasmoPacket> factory = packetFactories.get(id);
        if (factory == null) {
            throw new IOException("Unknown Plasmo TCP packet id " + id);
        }

        PlasmoPacket packet = factory.get();
        packet.read(input);
        return packet;
    }
}
