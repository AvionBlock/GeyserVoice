package io.greitan.avion.paper.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import io.greitan.avion.common.utils.BaseLogger;
import io.greitan.avion.paper.GeyserVoice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PaperLogger extends BaseLogger {

    public void log(Component msg) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Component coloredLogo = Component.text("[")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("GeyserVoice")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("] ")
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD))
                .append(msg);

        console.sendMessage(coloredLogo);
    }

    public void info(String msg) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Component coloredLogo = Component.text("[")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("GeyserVoice")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("] ")
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(msg).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));

        console.sendMessage(coloredLogo);
    }

    public void warn(String msg) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Component coloredLogo = Component.text("[")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("GeyserVoice")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("] ")
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(msg).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));

        console.sendMessage(coloredLogo);
    }

    public void error(String msg) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Component coloredLogo = Component.text("[")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("GeyserVoice")
                        .color(NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("] ")
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(msg).color(NamedTextColor.RED).decorate(TextDecoration.BOLD));

        console.sendMessage(coloredLogo);
    }

    public void debug(String msg) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        Boolean isDebug = GeyserVoice.getInstance().getConfig().getBoolean("config.debug");
        if (isDebug) {
            Component coloredLogo = Component.text("[")
                    .color(NamedTextColor.WHITE)
                    .decorate(TextDecoration.BOLD)
                    .append(Component.text("GeyserVoice")
                            .color(NamedTextColor.LIGHT_PURPLE)
                            .decorate(TextDecoration.BOLD))
                    .append(Component.text("] ")
                            .color(NamedTextColor.WHITE)
                            .decorate(TextDecoration.BOLD))
                    .append(Component.text(msg).color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD));

            console.sendMessage(coloredLogo);
        }
    }
}
