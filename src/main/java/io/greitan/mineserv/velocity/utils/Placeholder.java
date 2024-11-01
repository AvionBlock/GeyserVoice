package io.greitan.mineserv.velocity.utils;

import org.bukkit.entity.Player;

import io.greitan.mineserv.common.utils.BasePlaceholder;
import io.greitan.mineserv.velocity.GeyserVoice;

public class Placeholder extends BasePlaceholder {
    private final GeyserVoice plugin;

    // Get the plugin interface.
    public Placeholder(GeyserVoice plugin) {
        this.plugin = plugin;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        // Voice icon placeholder "%voice_status%"
        if (identifier.equalsIgnoreCase("status")) {
            if (plugin.getPlayerBinds().getOrDefault(player.getName(), false)) {
                return plugin.getConfig().getString("config.voice.in-voice-symbol");
            } else {
                return plugin.getConfig().getString("config.voice.not-in-voice-symbol");
            }
        }
        return null;
    }
}
