package team.avion.adapter.plasmo.udp.packet;

import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public final class PlayerAudioPacket extends BaseAudioPacket {
    private UUID activationId;
    private short distance;
    private boolean stereo;

    public UUID activationId() {
        return activationId;
    }

    public short distance() {
        return distance;
    }

    public boolean stereo() {
        return stereo;
    }

    @Override
    public void read(DataInput input) throws IOException {
        super.read(input);
        activationId = PlasmoPacketUtil.readUuid(input);
        distance = input.readShort();
        stereo = input.readBoolean();
    }

    @Override
    public void write(DataOutput output) throws IOException {
        super.write(output);
        PlasmoPacketUtil.writeUuid(output, activationId);
        output.writeShort(distance);
        output.writeBoolean(stereo);
    }
}
