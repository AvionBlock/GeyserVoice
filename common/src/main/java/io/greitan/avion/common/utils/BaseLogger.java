package io.greitan.avion.common.utils;

import net.kyori.adventure.text.Component;

public abstract class BaseLogger {

    public abstract void log(Component msg);

    public abstract void info(String msg);

    public abstract void warn(String msg);

    public abstract void error(String msg);

    public abstract void debug(String msg);

}
