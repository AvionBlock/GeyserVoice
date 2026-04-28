package team.avion.adapter.plasmo.tcp.packet;

import team.avion.adapter.plasmo.PlasmoPacket;
import team.avion.adapter.plasmo.PlasmoPacketUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerActivationDistancesPacket implements PlasmoPacket {
    private Map<UUID, Integer> distanceByActivationId = Map.of();

    public Map<UUID, Integer> distanceByActivationId() {
        return distanceByActivationId;
    }

    @Override
    public void read(DataInput input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > Byte.MAX_VALUE) {
            throw new IOException("Invalid activation distance map size " + size);
        }

        Map<UUID, Integer> distances = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            distances.put(PlasmoPacketUtil.readUuid(input), input.readInt());
        }
        distanceByActivationId = distances;
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeInt(distanceByActivationId.size());
        for (Map.Entry<UUID, Integer> entry : distanceByActivationId.entrySet()) {
            PlasmoPacketUtil.writeUuid(output, entry.getKey());
            output.writeInt(entry.getValue());
        }
    }
}
