package io.greitan.avion.common.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class BasePlaceholder extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return "voice";
    }

    @Override
    public String getAuthor() {
        return "GeyserVoice";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
