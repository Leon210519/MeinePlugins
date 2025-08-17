package com.specialitems.util;
import org.bukkit.Bukkit;
public final class Log {
    public static void info(String s) { Bukkit.getLogger().info("[SpecialItems] " + s); }
    public static void warn(String s) { Bukkit.getLogger().warning("[SpecialItems] " + s); }
    public static void error(String s) { Bukkit.getLogger().severe("[SpecialItems] " + s); }
}
