package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PetService {

    private final LootPetsPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public PetService(LootPetsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pets.yml");
        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        if (!config.isInt("schema")) {
            config.set("schema", 1);
            changed = true;
        }
        if (!config.isConfigurationSection("players")) {
            config.createSection("players");
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    public void ensurePlayerNode(UUID uuid) {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            players = config.createSection("players");
        }
        if (!players.isConfigurationSection(uuid.toString())) {
            ConfigurationSection section = players.createSection(uuid.toString());
            section.set("owned", new ArrayList<String>());
            section.set("active", new ArrayList<String>());
            save();
        }
    }

    public List<String> getOwnedPetIds(UUID uuid) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec == null) {
            return Collections.emptyList();
        }
        List<String> list = sec.getStringList("owned");
        return list == null ? Collections.emptyList() : list;
    }

    public List<String> getActivePetIds(UUID uuid) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec == null) {
            return Collections.emptyList();
        }
        List<String> list = sec.getStringList("active");
        return list == null ? Collections.emptyList() : list;
    }

    public void addOwnedPet(UUID uuid, String petId) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec == null) {
            return;
        }
        List<String> owned = new ArrayList<>(sec.getStringList("owned"));
        if (!owned.contains(petId)) {
            owned.add(petId);
            sec.set("owned", owned);
            save();
        }
    }

    public void reset(UUID uuid) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec != null) {
            sec.set("owned", new ArrayList<String>());
            sec.set("active", new ArrayList<String>());
            save();
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save pets.yml: " + e.getMessage());
        }
    }
}
