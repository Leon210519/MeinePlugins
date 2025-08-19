package com.specialitems.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public final class Configs {
    public static FileConfiguration cfg;
    public static FileConfiguration msg;
    public static FileConfiguration templates;

    public static void ensureResource(Plugin pl, String name) {
        try {
            File f = new File(pl.getDataFolder(), name);
            if (!f.exists()) {
                pl.saveResource(name, false);
            }
        } catch (Exception ignored) {}
    }

    public static void load(Plugin pl) {
        pl.saveDefaultConfig();
        ensureResource(pl, "messages.yml");
        ensureResource(pl, "templates.yml");
        cfg = pl.getConfig();
        msg = YamlConfiguration.loadConfiguration(new File(pl.getDataFolder(), "messages.yml"));
        templates = YamlConfiguration.loadConfiguration(new File(pl.getDataFolder(), "templates.yml"));
    }

    public static boolean effectEnabled(String id) { return cfg.getBoolean("effects." + id + ".enabled", true); }
    public static ConfigurationSection effectSection(String id) { return cfg.getConfigurationSection("effects." + id); }
}
