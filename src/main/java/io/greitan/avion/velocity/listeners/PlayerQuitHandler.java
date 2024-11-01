package io.greitan.avion.velocity.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import io.greitan.avion.velocity.GeyserVoice;
import io.greitan.avion.velocity.utils.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerQuitHandler {

    private final GeyserVoice plugin;
    private final String lang;

    public PlayerQuitHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();
        boolean isBound = plugin.getPlayerBinds().getOrDefault(player.getUsername(), false);

        if (plugin.isConnected() && isBound) {
            handlePlayerDisconnect(player);
        }
    }

    private void handlePlayerDisconnect(Player player) {
        boolean isDisconnected = plugin.disconnectPlayer(player);

        plugin.playerDataList.remove(player.getUniqueId().toString());

        String playerName = player.getUsername();
        String disconnectMessage = Language.getMessage(lang, "player-disconnect-success").replace("$player",
                playerName);

        if (isDisconnected) {
            plugin.Logger.info(disconnectMessage);

            boolean sendDisconnectMessage = GeyserVoice.getConfig().getBoolean("config.voice.send-disconnect-message");
            if (sendDisconnectMessage) {
                // @TODO send to servers
                plugin.getProxy().sendMessage(Component.text(disconnectMessage).color(NamedTextColor.YELLOW));
            }
        } else {
            plugin.Logger.error(Language.getMessage(lang, "player-disconnect-failed").replace("$player", playerName));
        }
    }
}
