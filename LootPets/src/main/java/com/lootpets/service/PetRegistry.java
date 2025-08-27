package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.PetDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Collection;

public class PetRegistry {

    private final LootPetsPlugin plugin;
    private Map<String, PetDefinition> pets = Collections.emptyMap();

    public PetRegistry(LootPetsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "pets_definitions.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Map<String, PetDefinition> map = new LinkedHashMap<>();
        ConfigurationSection petsSec = cfg.getConfigurationSection("pets");
        if (petsSec != null) {
            for (String id : petsSec.getKeys(false)) {
                ConfigurationSection sec = petsSec.getConfigurationSection(id);
                if (sec == null) {
                    continue;
                }
                String displayName = sec.getString("display_name", id);
                ConfigurationSection icon = sec.getConfigurationSection("icon");
                if (icon == null) {
                    plugin.getLogger().warning("Pet " + id + " missing icon");
                    continue;
                }
                String matName = icon.getString("material");
                Material material = matName == null ? null : Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
                if (material == null) {
                    plugin.getLogger().warning("Pet " + id + " has invalid material: " + matName);
                    continue;
                }
                Integer cmd = null;
                if (icon.contains("custom_model_data")) {
                    int raw = icon.getInt("custom_model_data");
                    if (raw < 0) {
                        plugin.getLogger().warning("Pet " + id + " has negative custom_model_data");
                        continue;
                    }
                    cmd = raw;
                }
                Map<String, Double> weights = new LinkedHashMap<>();
                ConfigurationSection weightsSec = sec.getConfigurationSection("weights");
                if (weightsSec != null) {
                    for (String key : weightsSec.getKeys(false)) {
                        double val = weightsSec.getDouble(key);
                        if (val >= 0 && val <= 1) {
                            weights.put(key, val);
                        } else {
                            plugin.getLogger().warning("Pet " + id + " weight " + key + " out of range (" + val + ")");
                        }
                    }
                }
                map.put(id, new PetDefinition(id, displayName, material, cmd, Collections.unmodifiableMap(weights)));
            }
        }
        this.pets = Collections.unmodifiableMap(map);
        plugin.getLogger().info("Loaded " + pets.size() + " pets");
    }

    public Collection<PetDefinition> all() {
        return pets.values();
    }

    public PetDefinition byId(String id) {
        return pets.get(id);
    }

    public int size() {
        return pets.size();
    }
}
