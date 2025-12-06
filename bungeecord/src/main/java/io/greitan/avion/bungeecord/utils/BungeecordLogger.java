package io.greitan.avion.bungeecord.utils;

import net.md_5.bungee.api.CommandSender;

import io.greitan.avion.common.utils.BaseLogger;
import io.greitan.avion.bungeecord.GeyserVoice;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.chat.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class BungeecordLogger extends BaseLogger {

    @Override
    public void log(Component msg) {
        CommandSender console = GeyserVoice.getInstance().getProxy().getConsole();
        String json = GsonComponentSerializer.gson().serialize(msg);
        BaseComponent[] components = ComponentSerializer.parse(json);
        
        BaseComponent[] coloredLogo = new ComponentBuilder("[")
                .color(ChatColor.WHITE)
                .bold(true)
                .append("GeyserVoice")
                .color(ChatColor.LIGHT_PURPLE)
                .bold(true)
                .append("] ")
                .color(ChatColor.WHITE)
                .bold(true)
                .append(components)
                .create();

        console.sendMessage(coloredLogo);
    }

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
        Boolean isDebug = GeyserVoice.getConfig().getBoolean("config.debug");
        if (isDebug) {
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
