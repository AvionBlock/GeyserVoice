package team.avion.adapter.plasmo.tcp.packet;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public final class ConnectionPacket implements PlasmoPacket {
    private UUID secret;
    private String ip;
    private int port;

    public ConnectionPacket() {
        this(new UUID(0L, 0L), "0.0.0.0", 0);
    }

    public ConnectionPacket(UUID secret, String ip, int port) {
        this.secret = secret;
        this.ip = ip;
        this.port = port;
    }

    public UUID secret() {
        return secret;
    }

    public String ip() {
        return ip;
    }

    public int port() {
        return port;
    }

    @Override
    public void read(DataInput input) throws IOException {
        secret = PlasmoPacketUtil.readUuid(input);
        ip = PlasmoPacketUtil.readString(input);
        port = input.readInt();
    }

    @Override
    public void write(DataOutput output) throws IOException {
        PlasmoPacketUtil.writeUuid(output, secret);
        PlasmoPacketUtil.writeString(output, ip);
        output.writeInt(port);
    }
}
