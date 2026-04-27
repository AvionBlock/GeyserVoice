package team.avion.bungeecord.listeners;

import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import team.avion.bungeecord.GeyserVoice;

public class PlayerJoinHandler implements Listener {
    private final GeyserVoice plugin;

    public PlayerJoinHandler(GeyserVoice plugin, String lang) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerConnect(ServerConnectedEvent event) {
        plugin.getMessageHandler().sendPlayerBindSync(event.getPlayer());
    }
}
