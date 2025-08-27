package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.OwnedPetState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.UUID;

public class PetService {

    public record EvolveResult(OwnedPetState state, boolean starUp, boolean capped) {}

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
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            players = config.createSection("players");
            changed = true;
        } else {
            for (String key : players.getKeys(false)) {
                ConfigurationSection sec = players.getConfigurationSection(key);
                if (sec == null) {
                    continue;
                }
                Object ownedObj = sec.get("owned");
                if (ownedObj instanceof List<?> list) {
                    ConfigurationSection ownedSec = sec.createSection("owned");
                    for (Object o : list) {
                        if (o == null) {
                            continue;
                        }
                        String id = String.valueOf(o);
                        ConfigurationSection ps = ownedSec.createSection(id);
                        ps.set("rarity", null);
                        ps.set("level", 0);
                        ps.set("stars", 0);
                        ps.set("evolve_progress", 0);
                        ps.set("xp", 0);
                    }
                    changed = true;
                } else if (!sec.isConfigurationSection("owned")) {
                    sec.createSection("owned");
                    changed = true;
                } else {
                    for (String pid : sec.getConfigurationSection("owned").getKeys(false)) {
                        ConfigurationSection ps = sec.getConfigurationSection("owned." + pid);
                        if (ps != null && !ps.isInt("xp")) {
                            ps.set("xp", 0);
                            changed = true;
                        }
                    }
                }
                List<String> act = sec.getStringList("active");
                if (act == null) {
                    sec.set("active", new ArrayList<>());
                    changed = true;
                } else {
                    LinkedHashSet<String> set = new LinkedHashSet<>(act);
                    if (set.size() != act.size()) {
                        sec.set("active", new ArrayList<>(set));
                        changed = true;
                    }
                }
            }
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
            section.createSection("owned");
            section.set("active", new ArrayList<>());
            save();
        }
    }

    public Map<String, OwnedPetState> getOwnedPets(UUID uuid) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid + ".owned");
        if (sec == null) {
            return Collections.emptyMap();
        }
        Map<String, OwnedPetState> map = new LinkedHashMap<>();
        for (String id : sec.getKeys(false)) {
            ConfigurationSection ps = sec.getConfigurationSection(id);
            if (ps == null) {
                continue;
            }
            String rarity = ps.getString("rarity", null);
            int level = ps.getInt("level", 0);
            int stars = ps.getInt("stars", 0);
            int progress = ps.getInt("evolve_progress", 0);
            int xp = ps.getInt("xp", 0);
            map.put(id, new OwnedPetState(rarity, level, stars, progress, xp));
        }
        return Collections.unmodifiableMap(map);
    }

    public boolean addOwnedPet(UUID uuid, String petId, String rarityId) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid + ".owned");
        if (sec == null) {
            return false;
        }
        if (sec.isConfigurationSection(petId)) {
            return false;
        }
        ConfigurationSection ps = sec.createSection(petId);
        ps.set("rarity", rarityId);
        ps.set("level", 0);
        ps.set("stars", 0);
        ps.set("evolve_progress", 0);
        ps.set("xp", 0);
        save();
        return true;
    }

    public EvolveResult incrementEvolve(UUID uuid, String petId) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid + ".owned." + petId);
        if (sec == null) {
            return new EvolveResult(new OwnedPetState(null, 0, 0, 0, 0), false, false);
        }
        int progress = sec.getInt("evolve_progress", 0) + 1;
        int stars = sec.getInt("stars", 0);
        boolean starUp = false;
        boolean capped = false;
        if (progress >= 5) {
            progress = 0;
            if (stars < 5) {
                stars++;
                starUp = true;
            } else {
                capped = true;
            }
        }
        sec.set("evolve_progress", progress);
        sec.set("stars", stars);
        save();
        OwnedPetState state = new OwnedPetState(sec.getString("rarity", null), sec.getInt("level", 0), stars, progress, sec.getInt("xp", 0));
        return new EvolveResult(state, starUp, capped);
    }

    public List<String> getActivePetIds(UUID uuid, int maxSlots) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec == null) {
            return Collections.emptyList();
        }
        List<String> list = sec.getStringList("active");
        if (list == null) {
            list = new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>(list);
        List<String> cleaned = new ArrayList<>(set);
        if (cleaned.size() > maxSlots) {
            cleaned = new ArrayList<>(cleaned.subList(0, maxSlots));
        }
        if (!cleaned.equals(list)) {
            sec.set("active", cleaned);
            save();
        }
        return Collections.unmodifiableList(cleaned);
    }

    public boolean equipPet(UUID uuid, String petId, int maxSlots) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec == null) {
            return false;
        }
        List<String> list = sec.getStringList("active");
        if (list == null) {
            list = new ArrayList<>();
        }
        if (list.contains(petId)) {
            return false;
        }
        if (list.size() >= maxSlots) {
            return false;
        }
        list.add(petId);
        sec.set("active", list);
        save();
        return true;
    }

    public boolean unequipPet(UUID uuid, String petId) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec == null) {
            return false;
        }
        List<String> list = sec.getStringList("active");
        if (list == null || !list.remove(petId)) {
            return false;
        }
        sec.set("active", list);
        save();
        return true;
    }

    public void addXpToActivePets(UUID uuid, int xpPerTick, int xpPerLevel, int baseCap, int extraPerStar) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid + ".owned");
        if (sec == null) {
            return;
        }
        List<String> active = getActivePetIds(uuid, Integer.MAX_VALUE);
        boolean changed = false;
        for (String petId : active) {
            ConfigurationSection ps = sec.getConfigurationSection(petId);
            if (ps == null) {
                continue;
            }
            int xp = ps.getInt("xp", 0) + xpPerTick;
            int level = ps.getInt("level", 0);
            int stars = ps.getInt("stars", 0);
            int cap = baseCap + extraPerStar * stars;
            if (xp >= xpPerLevel && level < cap) {
                int gained = xp / xpPerLevel;
                xp = xp % xpPerLevel;
                level += gained;
                if (level > cap) {
                    level = cap;
                    xp = 0;
                }
            }
            ps.set("xp", xp);
            ps.set("level", level);
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    public boolean setLevel(UUID uuid, String petId, int level, int baseCap, int extraPerStar) {
        ConfigurationSection ps = config.getConfigurationSection("players." + uuid + ".owned." + petId);
        if (ps == null) {
            return false;
        }
        int stars = ps.getInt("stars", 0);
        int cap = baseCap + extraPerStar * stars;
        int clamped = Math.max(0, Math.min(level, cap));
        ps.set("level", clamped);
        ps.set("xp", 0);
        save();
        return true;
    }

    public boolean setStars(UUID uuid, String petId, int stars) {
        ConfigurationSection ps = config.getConfigurationSection("players." + uuid + ".owned." + petId);
        if (ps == null) {
            return false;
        }
        ps.set("stars", stars);
        save();
        return true;
    }

    public void reset(UUID uuid) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec != null) {
            sec.createSection("owned");
            sec.set("active", new ArrayList<>());
            save();
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save pets.yml: " + e.getMessage());
        }
    }
}
