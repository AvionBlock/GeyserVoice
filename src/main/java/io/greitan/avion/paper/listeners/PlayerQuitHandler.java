package io.greitan.avion.paper.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import io.greitan.avion.paper.GeyserVoice;
import io.greitan.avion.paper.utils.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerQuitHandler implements Listener {

    private final GeyserVoice plugin;
    private final String lang;

    public PlayerQuitHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean isBound = plugin.getPlayerBinds().getOrDefault(player.getName(), false);

        if (isBound) {
            if (!plugin.usesProxy && plugin.isConnected()) {
                handlePlayerDisconnect(player);
            } else if (plugin.usesProxy) {
                // this will make sure the player bind is removed on leave, even if it cannot be
                // received...
                // it will be set again if the player joins this server again...
                plugin.getPlayerBinds().remove(player.getName());
            }
        }
    }

    private void handlePlayerDisconnect(Player player) {
        boolean isDisconnected = plugin.disconnectPlayer(player);

        String playerName = player.getName();
        String disconnectMessage = Language.getMessage(lang, "player-disconnect-success").replace("$player",
                playerName);

        if (isDisconnected) {
            plugin.Logger.info(disconnectMessage);

            boolean sendDisconnectMessage = plugin.getConfig().getBoolean("config.voice.send-disconnect-message");
            if (sendDisconnectMessage) {
                Bukkit.broadcast(Component.text(disconnectMessage).color(NamedTextColor.YELLOW));
            }
        } else {
            plugin.Logger.error(Language.getMessage(lang, "player-disconnect-failed").replace("$player", playerName));
        }
    }
}
