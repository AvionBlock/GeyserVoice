package io.greitan.mineserv.bungeecord.utils;

import org.bukkit.Bukkit;
import net.md_5.bungee.api.CommandSender;

import io.greitan.mineserv.common.utils.BaseLogger;
import io.greitan.mineserv.bungeecord.GeyserVoice;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.ChatColor;


public class BungeecordLogger extends BaseLogger {

    public void log(BaseComponent[] msg) {
        CommandSender console = GeyserVoice.getInstance().getProxy().getConsole();
        BaseComponent[] coloredLogo = new ComponentBuilder("[")
            .color(ChatColor.WHITE)
            .bold(true)
            .append("GeyserVoice")
            .color(ChatColor.LIGHT_PURPLE)
            .bold(true)
            .append("] ")
            .color(ChatColor.WHITE)
            .bold(true)
            .append(msg)
            .create();

        console.sendMessage(coloredLogo);
    }

    public void info(String msg) {
        CommandSender console = GeyserVoice.getInstance().getProxy().getConsole();
        BaseComponent[] coloredLogo = new ComponentBuilder("[")
            .color(ChatColor.WHITE)
            .bold(true)
            .append("GeyserVoice")
            .color(ChatColor.LIGHT_PURPLE)
            .bold(true)
            .append("] ")
            .color(ChatColor.WHITE)
            .bold(true)
            .append(msg).color(ChatColor.WHITE).bold(true)
            .create();

        console.sendMessage(coloredLogo);
    }

    public void warn(String msg) {
        CommandSender console = GeyserVoice.getInstance().getProxy().getConsole();
        BaseComponent[] coloredLogo = new ComponentBuilder("[")
            .color(ChatColor.WHITE)
            .bold(true)
            .append("GeyserVoice")
            .color(ChatColor.LIGHT_PURPLE)
            .bold(true)
            .append("] ")
            .color(ChatColor.WHITE)
            .bold(true)
            .append(msg).color(ChatColor.YELLOW).bold(true)
            .create();

        console.sendMessage(coloredLogo);
    }

    public void error(String msg) {
        CommandSender console = GeyserVoice.getInstance().getProxy().getConsole();
        BaseComponent[] coloredLogo = new ComponentBuilder("[")
            .color(ChatColor.WHITE)
            .bold(true)
            .append("GeyserVoice")
            .color(ChatColor.LIGHT_PURPLE)
            .bold(true)
            .append("] ")
            .color(ChatColor.WHITE)
            .bold(true)
            .append(msg).color(ChatColor.RED).bold(true)
            .create();

        console.sendMessage(coloredLogo);
    }

    public void debug(String msg) {
        CommandSender console = GeyserVoice.getInstance().getProxy().getConsole();
        Boolean isDebug = GeyserVoice.getInstance().getConfig().getBoolean("config.debug");
        if(isDebug){
            BaseComponent[] coloredLogo = new ComponentBuilder("[")
                .color(ChatColor.WHITE)
                .bold(true)
                .append("GeyserVoice")
                .color(ChatColor.LIGHT_PURPLE)
                .bold(true)
                .append("] ")
                .color(ChatColor.WHITE)
                .bold(true)
                .append(msg).color(ChatColor.BLUE).bold(true)
                .create();

            console.sendMessage(coloredLogo);
        }
    }
}
