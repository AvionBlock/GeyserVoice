package team.avion.adapter.plasmo.udp.packet;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class CustomPacket implements PlasmoPacket {
    private String channel;
    private byte[] data = new byte[0];

    public String channel() {
        return channel;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public void read(DataInput input) throws IOException {
        channel = PlasmoPacketUtil.readString(input);
        data = PlasmoPacketUtil.readBytes(input, 0, 32767);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        PlasmoPacketUtil.writeString(output, channel);
        PlasmoPacketUtil.writeBytes(output, data);
    }
}
