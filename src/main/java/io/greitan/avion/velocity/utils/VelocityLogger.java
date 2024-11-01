package io.greitan.avion.velocity.utils;

import com.velocitypowered.api.proxy.ConsoleCommandSource;

import io.greitan.avion.common.utils.BaseLogger;
import io.greitan.avion.velocity.GeyserVoice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class VelocityLogger extends BaseLogger {

        public void log(Component msg) {
                ConsoleCommandSource console = GeyserVoice.getInstance().getProxy().getConsoleCommandSource();
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
                ConsoleCommandSource console = GeyserVoice.getInstance().getProxy().getConsoleCommandSource();
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
                ConsoleCommandSource console = GeyserVoice.getInstance().getProxy().getConsoleCommandSource();
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
                ConsoleCommandSource console = GeyserVoice.getInstance().getProxy().getConsoleCommandSource();
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
                ConsoleCommandSource console = GeyserVoice.getInstance().getProxy().getConsoleCommandSource();
                Boolean isDebug = GeyserVoice.getConfig().getBoolean("config.debug");
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
                                        .append(Component.text(msg).color(NamedTextColor.BLUE)
                                                        .decorate(TextDecoration.BOLD));

                        console.sendMessage(coloredLogo);
                }
        }
}
