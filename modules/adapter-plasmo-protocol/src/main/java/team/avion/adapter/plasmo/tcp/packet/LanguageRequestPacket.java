package team.avion.adapter.plasmo.tcp.packet;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class LanguageRequestPacket implements PlasmoPacket {
    private String language;

    public String language() {
        return language;
    }

    @Override
    public void read(DataInput input) throws IOException {
        language = PlasmoPacketUtil.readString(input);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        PlasmoPacketUtil.writeString(output, language);
    }
}
