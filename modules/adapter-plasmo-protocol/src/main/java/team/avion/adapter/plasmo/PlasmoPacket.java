package team.avion.adapter.plasmo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface PlasmoPacket {
    void read(DataInput input) throws IOException;

    void write(DataOutput output) throws IOException;
}
