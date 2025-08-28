package com.lootpets.storage;

import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.UUID;

/** YAML based implementation compatible with the historic pets.yml layout. */
public class YamlStorageAdapter implements StorageAdapter {
    private final File file;
    private YamlConfiguration config;

    public YamlStorageAdapter(File dataFolder) {
        this.file = new File(dataFolder, "pets.yml");
    }

    @Override
    public void init() {
        this.config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("players")) {
            config.createSection("players");
        }
    }

    @Override
    public synchronized PlayerData loadPlayer(UUID uuid) {
        ConfigurationSection sec = config.getConfigurationSection("players." + uuid);
        if (sec == null) {
            return null;
        }
        PlayerData data = new PlayerData();
        ConfigurationSection ownedSec = sec.getConfigurationSection("owned");
        if (ownedSec != null) {
            for (String id : ownedSec.getKeys(false)) {
                ConfigurationSection ps = ownedSec.getConfigurationSection(id);
                if (ps == null) continue;
                String rarity = ps.getString("rarity", null);
                int level = ps.getInt("level", 0);
                int stars = ps.getInt("stars", 0);
                int progress = ps.getInt("evolve_progress", 0);
                int xp = ps.getInt("xp", 0);
                String suffix = ps.getString("suffix", null);
                data.owned.put(id, new OwnedPetState(rarity, level, stars, progress, xp, suffix));
            }
        }
        data.active.addAll(sec.getStringList("active"));
        data.shards = sec.getInt("shards", 0);
        data.renameTokens = sec.getInt("rename_tokens", 0);
        ConfigurationSection cos = sec.getConfigurationSection("cosmetics");
        if (cos != null) {
            data.albumFrameStyle = cos.getString("album_frame_style", null);
        }
        ConfigurationSection limits = sec.getConfigurationSection("limits_daily");
        if (limits != null) {
            data.limitsDate = limits.getString("date", null);
            ConfigurationSection buys = limits.getConfigurationSection("buys");
            if (buys != null) {
                for (String k : buys.getKeys(false)) {
                    data.dailyLimits.put(k, buys.getInt(k, 0));
                }
            }
        }
        return data;
    }

    @Override
    public synchronized void savePlayer(UUID uuid, PlayerData data) {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            players = config.createSection("players");
        }
        ConfigurationSection sec = players.getConfigurationSection(uuid.toString());
        if (sec == null) {
            sec = players.createSection(uuid.toString());
        }
        ConfigurationSection ownedSec = sec.createSection("owned");
        for (Map.Entry<String, OwnedPetState> e : data.owned.entrySet()) {
            ConfigurationSection ps = ownedSec.createSection(e.getKey());
            OwnedPetState st = e.getValue();
            ps.set("rarity", st.rarity());
            ps.set("level", st.level());
            ps.set("stars", st.stars());
            ps.set("evolve_progress", st.evolveProgress());
            ps.set("xp", st.xp());
            ps.set("suffix", st.suffix());
        }
        sec.set("active", new ArrayList<>(data.active));
        sec.set("shards", data.shards);
        sec.set("rename_tokens", data.renameTokens);
        ConfigurationSection cos = sec.createSection("cosmetics");
        cos.set("album_frame_style", data.albumFrameStyle);
        ConfigurationSection limits = sec.createSection("limits_daily");
        limits.set("date", data.limitsDate);
        ConfigurationSection buys = limits.createSection("buys");
        for (Map.Entry<String,Integer> e : data.dailyLimits.entrySet()) {
            buys.set(e.getKey(), e.getValue());
        }
        data.version++;
        data.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public synchronized void flush() throws IOException {
        config.save(file);
    }

    @Override
    public synchronized Map<UUID, PlayerData> loadAllPlayers() {
        Map<UUID, PlayerData> map = new HashMap<>();
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return map;
        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            PlayerData data = loadPlayer(uuid);
            if (data != null) {
                map.put(uuid, data);
            }
        }
        return map;
    }

    @Override
    public synchronized boolean isEmpty() {
        ConfigurationSection players = config.getConfigurationSection("players");
        return players == null || players.getKeys(false).isEmpty();
    }
}
