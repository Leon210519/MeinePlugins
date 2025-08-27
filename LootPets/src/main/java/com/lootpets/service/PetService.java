package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PetService {

    private final File file;
    private final YamlConfiguration config;

    public PetService(LootPetsPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "pets.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("players")) {
            config.createSection("players");
            save();
        }
    }

    public void ensurePlayer(UUID uuid) {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            players = config.createSection("players");
        }
        if (!players.isConfigurationSection(uuid.toString())) {
            ConfigurationSection section = players.createSection(uuid.toString());
            section.set("owned", Collections.emptyList());
            section.set("active", Collections.emptyList());
            save();
        }
    }

    public List<String> getOwned(UUID uuid) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        return sec == null ? Collections.emptyList() : sec.getStringList("owned");
    }

    public List<String> getActive(UUID uuid) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        return sec == null ? Collections.emptyList() : sec.getStringList("active");
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }
}
