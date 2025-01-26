package io.greitan.avion.paper.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import io.greitan.avion.paper.GeyserVoice;
import io.greitan.avion.paper.utils.Language;

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
        player.sendMessage(
            Component.text(Language.getMessage(lang, "plugin-autobind-enabled")).color(NamedTextColor.GREEN)
            .append(Component.text(" "))
            .append(
                Component.text(Language.getMessage(lang, "plugin-autobind-binding")).color(NamedTextColor.YELLOW)
            ));

        boolean isBound = plugin.bind(playerBindKey, player);

        if (!isBound) {
            player.sendMessage(
                    Component.text(Language.getMessage(lang, "plugin-autobind-failed")).color(NamedTextColor.RED));
        }
    }

    /*
     * @EventHandler
     * public void onBroadcastMessage(AsyncChatEvent event) {
     * plugin.Logger.log(Component.text("Received message ").append(event.message())
     * );
     * }
     */
}
