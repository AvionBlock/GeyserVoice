package team.avion.adapter.plasmo.tcp.packet;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class PlayerInfoPacket implements PlasmoPacket {
    private String minecraftVersion;
    private String voiceVersion;
    private byte[] publicKey;
    private boolean voiceDisabled;
    private boolean microphoneMuted;

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public String voiceVersion() {
        return voiceVersion;
    }

    public byte[] publicKey() {
        return publicKey;
    }

    public boolean voiceDisabled() {
        return voiceDisabled;
    }

    public boolean microphoneMuted() {
        return microphoneMuted;
    }

    @Override
    public void read(DataInput input) throws IOException {
        voiceDisabled = input.readBoolean();
        microphoneMuted = input.readBoolean();
        minecraftVersion = PlasmoPacketUtil.readString(input);
        voiceVersion = PlasmoPacketUtil.readString(input);
        publicKey = PlasmoPacketUtil.readBytes(input, 1, 2048);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeBoolean(voiceDisabled);
        output.writeBoolean(microphoneMuted);
        PlasmoPacketUtil.writeString(output, minecraftVersion);
        PlasmoPacketUtil.writeString(output, voiceVersion);
        PlasmoPacketUtil.writeBytes(output, publicKey);
    }
}
