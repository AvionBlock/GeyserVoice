package team.avion.adapter;

import team.avion.common.utils.BaseLogger;

import java.nio.file.Path;
import java.util.Objects;

public final class AdapterContext {
    private final BaseLogger logger;
    private final Path dataDirectory;
    private final VoiceCraftAudioBridge audioBridge;

    public AdapterContext(BaseLogger logger, Path dataDirectory, VoiceCraftAudioBridge audioBridge) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.audioBridge = Objects.requireNonNull(audioBridge, "audioBridge");
    }

    public BaseLogger logger() {
        return logger;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public VoiceCraftAudioBridge audioBridge() {
        return audioBridge;
    }
}
