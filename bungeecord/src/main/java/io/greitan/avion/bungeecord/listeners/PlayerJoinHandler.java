package io.greitan.avion.bungeecord.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatColor;

import io.greitan.avion.bungeecord.GeyserVoice;
import io.greitan.avion.bungeecord.utils.Language;

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
        player.sendMessage(
            new ComponentBuilder(Language.getMessage(lang, "plugin-autobind-enabled")).color(ChatColor.GREEN)
            .append(new ComponentBuilder(" ").create())
            .append(
                new ComponentBuilder(Language.getMessage(lang, "plugin-autobind-binding")).color(ChatColor.YELLOW).create()
            ).create());

        boolean isBound = plugin.bind(playerBindKey, player);

        if (!isBound) {
            player.sendMessage(new ComponentBuilder(Language.getMessage(lang, "plugin-autobind-failed"))
                    .color(ChatColor.RED).create());
        }
    }
}
