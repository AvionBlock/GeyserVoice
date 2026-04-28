package team.avion.adapter.plasmo.udp.packet;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public final class SelfAudioInfoPacket implements PlasmoPacket {
    private UUID sourceId;
    private long sequenceNumber;
    private byte[] data;
    private short distance;

    public UUID sourceId() {
        return sourceId;
    }

    public long sequenceNumber() {
        return sequenceNumber;
    }

    public short distance() {
        return distance;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public void read(DataInput input) throws IOException {
        sourceId = PlasmoPacketUtil.readUuid(input);
        sequenceNumber = input.readLong();
        if (input.readBoolean()) {
            data = PlasmoPacketUtil.readBytes(input, 1, 2048);
        } else {
            data = null;
        }
        distance = input.readShort();
    }

    @Override
    public void write(DataOutput output) throws IOException {
        PlasmoPacketUtil.writeUuid(output, sourceId);
        output.writeLong(sequenceNumber);
        output.writeBoolean(data != null);
        if (data != null) {
            PlasmoPacketUtil.writeBytes(output, data);
        }
        output.writeShort(distance);
    }
}
