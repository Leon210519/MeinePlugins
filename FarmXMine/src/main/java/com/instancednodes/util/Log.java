package com.instancednodes.util;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class Log {
    private static Plugin plugin;
    private static boolean debug;
    public static void init(Plugin p){ plugin=p; debug = p.getConfig().getBoolean("debug", false); }
    public static void setDebug(boolean val){ debug = val; }
    public static void d(String s){ if(debug && plugin!=null) plugin.getLogger().info("[DEBUG] " + s); }
    public static String loc(Location l){ return (l.getWorld()!=null?l.getWorld().getName():"?")+":"+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ(); }
}
