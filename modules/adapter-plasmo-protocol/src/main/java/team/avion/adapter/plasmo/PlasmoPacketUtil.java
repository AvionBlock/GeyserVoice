package team.avion.adapter.plasmo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;

public final class PlasmoPacketUtil {
    private PlasmoPacketUtil() {
    }

    public static UUID readUuid(DataInput input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }

    public static void writeUuid(DataOutput output, UUID value) throws IOException {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    public static byte[] readBytes(DataInput input, int minLength, int maxLength) throws IOException {
        int length = input.readInt();
        if (length < minLength || length > maxLength) {
            throw new IOException("Invalid byte array length " + length);
        }
        byte[] data = new byte[length];
        input.readFully(data);
        return data;
    }

    public static void writeBytes(DataOutput output, byte[] value) throws IOException {
        output.writeInt(value.length);
        output.write(value);
    }

    public static String readString(DataInput input) throws IOException {
        return input.readUTF();
    }

    public static void writeString(DataOutput output, String value) throws IOException {
        output.writeUTF(value);
    }
}
