package io.greitan.avion.bungeecord.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import io.greitan.avion.bungeecord.GeyserVoice;
import io.greitan.avion.bungeecord.utils.Language;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatColor;

public class PlayerQuitHandler implements Listener {

    private final GeyserVoice plugin;
    private final String lang;

    public PlayerQuitHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        boolean isBound = plugin.getPlayerBinds().getOrDefault(player.getName(), false);

        if (plugin.isConnected() && isBound) {
            handlePlayerDisconnect(player);
        }
    }

    private void handlePlayerDisconnect(ProxiedPlayer player) {
        boolean isDisconnected = plugin.disconnectPlayer(player);

        plugin.playerDataList.remove(player.getUniqueId().toString());

        String playerName = player.getName();
        String disconnectMessage = Language.getMessage(lang, "player-disconnect-success").replace("$player",
                playerName);

        if (isDisconnected) {
            plugin.Logger.info(disconnectMessage);

            boolean sendDisconnectMessage = GeyserVoice.getConfig().getBoolean("config.voice.send-disconnect-message");
            if (sendDisconnectMessage) {
                plugin.getProxy().broadcast(new ComponentBuilder(disconnectMessage).color(ChatColor.YELLOW).create());
            }
        } else {
            plugin.Logger.error(Language.getMessage(lang, "player-disconnect-failed").replace("$player", playerName));
        }
    }
}
