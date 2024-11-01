package io.greitan.mineserv.common.utils;

import net.kyori.adventure.text.Component;

public abstract class BaseLogger {

    public void log(Component msg) {}

    public void info(String msg) {}

    public void warn(String msg) {}

    public void error(String msg) {}

    public void debug(String msg) {}

}
