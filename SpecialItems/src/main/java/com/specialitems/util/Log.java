package com.specialitems.util;

import org.bukkit.Bukkit;

import java.util.logging.Level;

public final class Log {
    public static void info(String s) { Bukkit.getLogger().info("[SpecialItems] " + s); }
    public static void warn(String s) { Bukkit.getLogger().warning("[SpecialItems] " + s); }
    public static void error(String s) { Bukkit.getLogger().severe("[SpecialItems] " + s); }

    /** Logs a debug-level message which is hidden unless the server logger is set to FINE. */
    public static void debug(String s) { Bukkit.getLogger().log(Level.FINE, "[SpecialItems] " + s); }

    /** Logs a debug-level message with a stack trace. */
    public static void debug(String s, Throwable t) { Bukkit.getLogger().log(Level.FINE, "[SpecialItems] " + s, t); }
}
