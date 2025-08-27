package com.lootpets.util;

import org.bukkit.ChatColor;

public final class Colors {
    private Colors() {}

    public static String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String prefix(String prefix, String name) {
        return color(prefix) + color(name);
    }
}
