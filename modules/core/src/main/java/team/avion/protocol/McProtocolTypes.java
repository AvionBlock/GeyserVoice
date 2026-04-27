package team.avion.protocol;

import java.util.UUID;

public final class McProtocolTypes {
    private McProtocolTypes() {
    }

    public record Version(int major, int minor, int build) {
    }

    public record Vector2(float x, float y) {
    }

    public record Vector3(float x, float y, float z) {
    }

    public record Guid(UUID value) {
        public static Guid empty() {
            return new Guid(new UUID(0L, 0L));
        }

        public static Guid parse(String value) {
            if (value == null || value.isBlank()) {
                return empty();
            }
            return new Guid(UUID.fromString(value));
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }
}
