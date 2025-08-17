package com.instancednodes.util;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Msg {

    private static FileConfiguration msg;
    private static String prefix = "&7[&bNodes&7] ";

    public static void load(Plugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        msg = YamlConfiguration.loadConfiguration(f);
        prefix = msg.getString("prefix", prefix);
    }

    public static String get(String key) {
        String raw = msg.getString(key, key);
        raw = raw.replace("%prefix%", prefix);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
