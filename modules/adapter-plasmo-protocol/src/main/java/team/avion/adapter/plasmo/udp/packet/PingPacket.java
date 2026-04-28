package team.avion.adapter.plasmo.udp.packet;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class PingPacket implements PlasmoPacket {
    private long time = System.currentTimeMillis();
    private String serverIp;
    private int serverPort;

    public PingPacket() {
    }

    public PingPacket(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public String serverIp() {
        return serverIp;
    }

    public int serverPort() {
        return serverPort;
    }

    public long time() {
        return time;
    }

    @Override
    public void read(DataInput input) throws IOException {
        time = input.readLong();
        try {
            serverIp = PlasmoPacketUtil.readString(input);
            serverPort = input.readUnsignedShort();
        } catch (IOException ignored) {
            serverIp = null;
            serverPort = 0;
        }
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeLong(time);

        if (serverIp != null && serverPort > 0) {
            PlasmoPacketUtil.writeString(output, serverIp);
            output.writeShort(serverPort);
        }
    }
}
