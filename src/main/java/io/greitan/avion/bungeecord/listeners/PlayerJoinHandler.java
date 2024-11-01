package io.greitan.avion.bungeecord.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import io.greitan.avion.bungeecord.GeyserVoice;
import io.greitan.avion.bungeecord.utils.Language;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatColor;

import java.util.Objects;

public class PlayerJoinHandler implements Listener {

    private final GeyserVoice plugin;
    private final String lang;

    public PlayerJoinHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        boolean isConnected = plugin.isConnected();
        ProxiedPlayer player = event.getPlayer();
        int playerBindKey = GeyserVoice.getConfig().getInt("config.players." + player.getName(), -1);

        if (isConnected && Objects.nonNull(playerBindKey) && playerBindKey != -1) {
            handleAutoBind(playerBindKey, player);
        }

        plugin.getMessageHandler().sendPlayerBindSync(player);
    }

    @EventHandler
    public void onPlayerConnect(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        // Just send the message again, in case it didn't got send before...
        plugin.getMessageHandler().sendPlayerBindSync(player);
    }

    private void handleAutoBind(int playerBindKey, ProxiedPlayer player) {
        boolean isBound = plugin.bind(playerBindKey, player);

        if (isBound) {
            String playerName = player.getName();
            String connectMessage = Language.getMessage(lang, "player-connect").replace("$player", playerName);

            plugin.Logger.info(connectMessage);

            boolean sendConnectMessage = GeyserVoice.getConfig().getBoolean("config.voice.send-connect-message");
            if (sendConnectMessage) {
                plugin.getProxy().broadcast(new ComponentBuilder(connectMessage).color(ChatColor.YELLOW).create());
            }
        } else {
            player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "plugin-autobind-failed"))
                    .color(ChatColor.RED).create());
        }
    }
}
