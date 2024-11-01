package io.greitan.mineserv.paper.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import io.greitan.mineserv.paper.GeyserVoice;
import io.greitan.mineserv.paper.utils.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.Objects;

public class PlayerJoinHandler implements Listener {

    private final GeyserVoice plugin;
    private final String lang;

    public PlayerJoinHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isConnected = plugin.isConnected();
        Player player = event.getPlayer();
        int playerBindKey = plugin.getConfig().getInt("config.players." + player.getName(), -1);

        if (!plugin.usesProxy && isConnected && Objects.nonNull(playerBindKey) && playerBindKey != -1) {
            handleAutoBind(playerBindKey, player);
        }
    }

    private void handleAutoBind(int playerBindKey, Player player) {
        boolean isBound = plugin.bind(playerBindKey, player);

        if (isBound) {
            String playerName = player.getName();
            String connectMessage = Language.getMessage(lang, "player-connect").replace("$player", playerName);

            plugin.Logger.info(connectMessage);

            boolean sendConnectMessage = plugin.getConfig().getBoolean("config.voice.send-connect-message");
            if (sendConnectMessage) {
                Bukkit.broadcast(Component.text(connectMessage).color(NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(Component.text(Language.getMessage(lang, "plugin-autobind-failed")).color(NamedTextColor.RED));
        }
    }

/*
    @EventHandler
    public void onBroadcastMessage(AsyncChatEvent event) {
        plugin.Logger.log(Component.text("Received message ").append(event.message()));
    }
*/
}
