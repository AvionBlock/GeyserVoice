package team.avion.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;

import team.avion.velocity.GeyserVoice;

public class PlayerJoinHandler {
    private final GeyserVoice plugin;

    public PlayerJoinHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerConnect(ServerPostConnectEvent event) {
        plugin.getMessageHandler().sendPlayerBindSync(event.getPlayer());
    }
}
