package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.OwnedPetState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.UUID;
import java.util.function.Consumer;

public class PetService {

    public record EvolveResult(OwnedPetState state, boolean starUp, boolean capped) {}

    private final LootPetsPlugin plugin;
    private final File file;
    private YamlConfiguration config;
    private final List<Consumer<UUID>> changeListeners = new ArrayList<>();

    public PetService(LootPetsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pets.yml");
        reload();
    }

    public void addChangeListener(Consumer<UUID> listener) {
        changeListeners.add(listener);
    }

    private void notifyChange(UUID uuid) {
        for (Consumer<UUID> c : changeListeners) {
            try {
                c.accept(uuid);
            } catch (Exception ignored) {
            }
        }
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
                        if (ps != null) {
                            if (!ps.isInt("xp")) {
                                ps.set("xp", 0);
                                changed = true;
                            }
                            if (!ps.isSet("suffix")) {
                                ps.set("suffix", null);
                                changed = true;
                            }
                        }
                    }
                }
                if (!sec.isInt("shards")) {
                    sec.set("shards", 0);
                    changed = true;
                }
                if (!sec.isInt("rename_tokens")) {
                    sec.set("rename_tokens", 0);
                    changed = true;
                }
                ConfigurationSection cos = sec.getConfigurationSection("cosmetics");
                if (cos == null) {
                    cos = sec.createSection("cosmetics");
                    changed = true;
                }
                if (!cos.isSet("album_frame_style")) {
                    cos.set("album_frame_style", null);
                    changed = true;
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
            section.set("shards", 0);
            section.set("rename_tokens", 0);
            ConfigurationSection cos = section.createSection("cosmetics");
            cos.set("album_frame_style", null);
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
            String suffix = ps.getString("suffix", null);
            map.put(id, new OwnedPetState(rarity, level, stars, progress, xp, suffix));
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
        ps.set("suffix", null);
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
        if (starUp) {
            notifyChange(uuid);
        }
        OwnedPetState state = new OwnedPetState(sec.getString("rarity", null), sec.getInt("level", 0), stars, progress, sec.getInt("xp", 0), sec.getString("suffix", null));
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
        notifyChange(uuid);
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
        notifyChange(uuid);
        return true;
    }

    public void addXpToActivePets(UUID uuid, int xpPerTick, int xpPerLevel, int baseCap, int extraPerStar) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid + ".owned");
        if (sec == null) {
            return;
        }
        List<String> active = getActivePetIds(uuid, Integer.MAX_VALUE);
        boolean changed = false;
        boolean levelChanged = false;
        for (String petId : active) {
            ConfigurationSection ps = sec.getConfigurationSection(petId);
            if (ps == null) {
                continue;
            }
            int xp = ps.getInt("xp", 0) + xpPerTick;
            int level = ps.getInt("level", 0);
            int oldLevel = level;
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
            if (level != oldLevel) {
                levelChanged = true;
            }
            changed = true;
        }
        if (changed) {
            save();
        }
        if (levelChanged) {
            notifyChange(uuid);
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
        notifyChange(uuid);
        return true;
    }

    public boolean setStars(UUID uuid, String petId, int stars) {
        ConfigurationSection ps = config.getConfigurationSection("players." + uuid + ".owned." + petId);
        if (ps == null) {
            return false;
        }
        ps.set("stars", stars);
        save();
        notifyChange(uuid);
        return true;
    }

    public boolean addXp(UUID uuid, String petId, int amount, int xpPerLevel, int baseCap, int extraPerStar) {
        ConfigurationSection ps = config.getConfigurationSection("players." + uuid + ".owned." + petId);
        if (ps == null) {
            return false;
        }
        int xp = ps.getInt("xp", 0) + Math.max(0, amount);
        int level = ps.getInt("level", 0);
        int stars = ps.getInt("stars", 0);
        int cap = baseCap + extraPerStar * stars;
        boolean levelChanged = false;
        if (xp >= xpPerLevel && level < cap) {
            int gained = xp / xpPerLevel;
            xp = xp % xpPerLevel;
            level += gained;
            if (level > cap) {
                level = cap;
                xp = 0;
            }
            levelChanged = true;
        }
        ps.set("xp", xp);
        ps.set("level", level);
        save();
        if (levelChanged) {
            notifyChange(uuid);
        }
        return true;
    }

    public int getShards(UUID uuid) {
        ensurePlayerNode(uuid);
        return config.getInt("players." + uuid + ".shards", 0);
    }

    public void addShards(UUID uuid, int amount) {
        ensurePlayerNode(uuid);
        int cur = getShards(uuid);
        config.set("players." + uuid + ".shards", Math.max(0, cur + amount));
        save();
    }

    public boolean spendShards(UUID uuid, int amount) {
        ensurePlayerNode(uuid);
        int cur = getShards(uuid);
        if (cur < amount) {
            return false;
        }
        config.set("players." + uuid + ".shards", cur - amount);
        save();
        return true;
    }

    public int getRenameTokens(UUID uuid) {
        ensurePlayerNode(uuid);
        return config.getInt("players." + uuid + ".rename_tokens", 0);
    }

    public void addRenameTokens(UUID uuid, int amount) {
        ensurePlayerNode(uuid);
        int cur = getRenameTokens(uuid);
        config.set("players." + uuid + ".rename_tokens", Math.max(0, cur + amount));
        save();
    }

    public boolean consumeRenameToken(UUID uuid) {
        ensurePlayerNode(uuid);
        int cur = getRenameTokens(uuid);
        if (cur <= 0) {
            return false;
        }
        config.set("players." + uuid + ".rename_tokens", cur - 1);
        save();
        return true;
    }

    public String getAlbumFrameStyle(UUID uuid) {
        ensurePlayerNode(uuid);
        ConfigurationSection cos = config.getConfigurationSection("players." + uuid + ".cosmetics");
        return cos != null ? cos.getString("album_frame_style", null) : null;
    }

    public void setAlbumFrameStyle(UUID uuid, String style) {
        ensurePlayerNode(uuid);
        ConfigurationSection cos = config.getConfigurationSection("players." + uuid + ".cosmetics");
        if (cos == null) {
            cos = config.createSection("players." + uuid + ".cosmetics");
        }
        cos.set("album_frame_style", style);
        save();
    }

    public String getSuffix(UUID uuid, String petId) {
        ConfigurationSection ps = config.getConfigurationSection("players." + uuid + ".owned." + petId);
        if (ps == null) {
            return null;
        }
        return ps.getString("suffix", null);
    }

    public void setSuffix(UUID uuid, String petId, String suffix) {
        ConfigurationSection ps = config.getConfigurationSection("players." + uuid + ".owned." + petId);
        if (ps == null) {
            return;
        }
        ps.set("suffix", suffix);
        save();
    }

    public int getDailyBuys(UUID uuid, String today, String itemId) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid + ".limits_daily");
        if (sec == null || !today.equals(sec.getString("date"))) {
            return 0;
        }
        ConfigurationSection buys = sec.getConfigurationSection("buys");
        if (buys == null) {
            return 0;
        }
        return buys.getInt(itemId, 0);
    }

    public void incrementDailyBuys(UUID uuid, String today, String itemId) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid + ".limits_daily");
        if (sec == null) {
            sec = config.createSection("players." + uuid + ".limits_daily");
        }
        if (!today.equals(sec.getString("date"))) {
            sec.set("date", today);
            sec.createSection("buys");
        }
        ConfigurationSection buys = sec.getConfigurationSection("buys");
        int cur = buys.getInt(itemId, 0);
        buys.set(itemId, cur + 1);
        save();
    }

    public void reset(UUID uuid) {
        ensurePlayerNode(uuid);
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec != null) {
            sec.createSection("owned");
            sec.set("active", new ArrayList<>());
            save();
            notifyChange(uuid);
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
