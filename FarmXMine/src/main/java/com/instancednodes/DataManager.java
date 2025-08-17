package com.instancednodes;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {

    private final Plugin plugin;
    private final File file;
    private FileConfiguration data;

    public DataManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try { data.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save data.yml: " + e.getMessage()); }
    }

    public String getSelection(UUID uuid, String category, String defMat) {
        String path = "selection." + uuid + "." + category;
        return data.getString(path, defMat);
    }

    public void setSelection(UUID uuid, String category, String mat) {
        String path = "selection." + uuid + "." + category;
        data.set(path, mat);
        save();
    }

    public long incrementHarvest(UUID uuid, String key) {
        String path = "harvests." + uuid + "." + key;
        long v = data.getLong(path, 0L) + 1L;
        data.set(path, v);
        save();
        return v;
    }
}
