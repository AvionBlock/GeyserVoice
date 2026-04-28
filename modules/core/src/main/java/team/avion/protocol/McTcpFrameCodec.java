package team.avion.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class McTcpFrameCodec {
    public static final int FRAME_HEADER_SIZE = 12;
    public static final int MAX_FRAME_PAYLOAD_LENGTH = 1024 * 1024;
    public static final int FRAME_MAGIC = 0x4D435450; // MCTP
    public static final short FRAME_VERSION = 1;
    public static final short REQUEST_KIND = 1;
    public static final short RESPONSE_KIND = 2;

    private McTcpFrameCodec() {
    }

    public static byte[] encodeRequest(String token, List<byte[]> packets) {
        return encodeFrame(REQUEST_KIND, encodePayload(token, packets));
    }

    public static byte[] encodeResponse(List<byte[]> packets) {
        return encodeFrame(RESPONSE_KIND, encodePayload("", packets));
    }

    public static DecodedFrame decodeFrame(byte[] frame) {
        if (frame.length < FRAME_HEADER_SIZE) {
            throw new IllegalArgumentException("Frame is shorter than the MCTP header.");
        }

        ByteBuffer header = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
        int magic = header.getInt();
        short version = header.getShort();
        short kind = header.getShort();
        int payloadLength = header.getInt();

        if (magic != FRAME_MAGIC) {
            throw new IllegalArgumentException("Unexpected frame magic: " + magic);
        }
        if (version != FRAME_VERSION) {
            throw new IllegalArgumentException("Unsupported frame version: " + version);
        }
        if (payloadLength < 0 || payloadLength > MAX_FRAME_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("Invalid frame payload length: " + payloadLength);
        }
        if (frame.length != FRAME_HEADER_SIZE + payloadLength) {
            throw new IllegalArgumentException("Frame length does not match encoded payload length.");
        }

        byte[] payload = new byte[payloadLength];
        System.arraycopy(frame, FRAME_HEADER_SIZE, payload, 0, payloadLength);
        return new DecodedFrame(kind, decodePayload(payload));
    }

    public static byte[] encodePayload(String token, List<byte[]> packets) {
        byte[] tokenBytes = token == null || token.isEmpty() ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeInt(output, tokenBytes.length);
        output.writeBytes(tokenBytes);
        writeInt(output, packets.size());
        for (byte[] packet : packets) {
            writeInt(output, packet.length);
            output.writeBytes(packet);
        }
        return output.toByteArray();
    }

    public static DecodedPayload decodePayload(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        if (buffer.remaining() < 8) {
            throw new IllegalArgumentException("Payload is too short.");
        }

        int tokenLength = buffer.getInt();
        if (tokenLength < 0 || buffer.remaining() < tokenLength + 4) {
            throw new IllegalArgumentException("Payload token section is invalid.");
        }

        byte[] tokenBytes = new byte[tokenLength];
        buffer.get(tokenBytes);
        String token = new String(tokenBytes, StandardCharsets.UTF_8);

        int packetCount = buffer.getInt();
        if (packetCount < 0) {
            throw new IllegalArgumentException("Payload packet count is invalid.");
        }

        List<byte[]> packets = new ArrayList<>(packetCount);
        for (int i = 0; i < packetCount; i++) {
            if (buffer.remaining() < 4) {
                throw new IllegalArgumentException("Packet length prefix is missing.");
            }
            int packetLength = buffer.getInt();
            if (packetLength <= 0 || buffer.remaining() < packetLength) {
                throw new IllegalArgumentException("Packet length is invalid.");
            }
            byte[] packet = new byte[packetLength];
            buffer.get(packet);
            packets.add(packet);
        }

        if (buffer.hasRemaining()) {
            throw new IllegalArgumentException("Payload has trailing bytes.");
        }

        return new DecodedPayload(token, packets);
    }

    private static byte[] encodeFrame(short kind, byte[] payload) {
        if (payload.length > MAX_FRAME_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("Frame payload exceeds the maximum allowed size.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(FRAME_HEADER_SIZE + payload.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(FRAME_MAGIC);
        buffer.putShort(FRAME_VERSION);
        buffer.putShort(kind);
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    private static void writeInt(ByteArrayOutputStream output, int value) {
        output.write((value >>> 24) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    public record DecodedFrame(short kind, DecodedPayload payload) {
    }

    public record DecodedPayload(String token, List<byte[]> packets) {
    }
}
