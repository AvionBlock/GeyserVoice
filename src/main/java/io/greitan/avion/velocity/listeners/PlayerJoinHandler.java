package io.greitan.avion.velocity.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import io.greitan.avion.velocity.GeyserVoice;
import io.greitan.avion.velocity.utils.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;

public class PlayerJoinHandler {

    private final GeyserVoice plugin;
    private final String lang;

    public PlayerJoinHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        boolean isConnected = plugin.isConnected();
        Player player = event.getPlayer();
        int playerBindKey = GeyserVoice.getConfig().getOrDefault("config.players." + player.getUsername(), -1);

        if (isConnected && Objects.nonNull(playerBindKey) && playerBindKey != -1) {
            handleAutoBind(playerBindKey, player);
        }
    }

    @Subscribe
    public void onPlayerConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        // Just send the message again, in case it didn't got send before...
        plugin.getMessageHandler().sendPlayerBindSync(player);
    }

    private void handleAutoBind(int playerBindKey, Player player) {
        boolean isBound = plugin.bind(playerBindKey, player);

        if (isBound) {
            String playerName = player.getUsername();
            String connectMessage = Language.getMessage(lang, "player-connect").replace("$player", playerName);

            plugin.Logger.info(connectMessage);

            boolean sendConnectMessage = GeyserVoice.getConfig().getBoolean("config.voice.send-connect-message");
            if (sendConnectMessage) {
                // @TODO send to proxies -> Already works...
                plugin.getProxy().sendMessage(Component.text(connectMessage).color(NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(
                    Component.text(Language.getMessage(lang, "plugin-autobind-failed")).color(NamedTextColor.RED));
        }
    }
}
