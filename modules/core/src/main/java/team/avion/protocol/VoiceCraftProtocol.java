package team.avion.protocol;

import team.avion.protocol.McProtocolTypes.Version;

public final class VoiceCraftProtocol {
    public static final int MAJOR = 1;
    public static final int MINOR = 6;
    public static final int PATCH = 0;

    public static final Version VERSION = new Version(MAJOR, MINOR, PATCH);
    public static final String VERSION_STRING = MAJOR + "." + MINOR + "." + PATCH;

    private VoiceCraftProtocol() {
    }
}
