package com.focusnpc.data;

import com.focusnpc.FocusNPCPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerData {
    private final FocusNPCPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public PlayerData(FocusNPCPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public Material getFarmFocus(UUID uuid) {
        String path = uuid.toString() + ".farm_focus";
        String name = config.getString(path, "WHEAT");
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.WHEAT;
    }

    public void setFarmFocus(UUID uuid, Material mat) {
        config.set(uuid.toString() + ".farm_focus", mat.name());
        save();
    }

    public Material getMineFocus(UUID uuid) {
        String path = uuid.toString() + ".mine_focus";
        String name = config.getString(path, "COAL_ORE");
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.COAL_ORE;
    }

    public void setMineFocus(UUID uuid, Material mat) {
        config.set(uuid.toString() + ".mine_focus", mat.name());
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }
}
