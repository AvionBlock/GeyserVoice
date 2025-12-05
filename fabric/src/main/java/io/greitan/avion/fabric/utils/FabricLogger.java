package io.greitan.avion.fabric.utils;

import io.greitan.avion.common.utils.BaseLogger;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricLogger extends BaseLogger {
    private final Logger logger;

    public FabricLogger() {
        this.logger = LoggerFactory.getLogger("GeyserVoice");
    }

    @Override
    public void log(Component msg) {
        // Ideally convert Component to string or use adventure-platform-fabric if available.
        // For now, simple toString/serializing
        logger.info(msg.toString());
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }
}
