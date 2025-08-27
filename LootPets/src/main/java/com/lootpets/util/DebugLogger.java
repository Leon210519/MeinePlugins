package com.lootpets.util;

import com.lootpets.LootPetsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class DebugLogger {
    private DebugLogger() {}

    public static void debug(LootPetsPlugin plugin, String category, String message) {
        FileConfiguration cfg = plugin.getConfig();
        String verbosity = cfg.getString("logging.verbosity", "INFO");
        if (!"DEBUG".equalsIgnoreCase(verbosity)) {
            return;
        }
        List<String> cats = cfg.getStringList("logging.debug_categories");
        if (!cats.contains(category)) {
            return;
        }
        plugin.getLogger().info("[" + category + "] " + message);
    }
}
