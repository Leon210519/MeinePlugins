package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.PlayerStats;
import com.farmxmine2.model.TrackType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public class StorageService {
    private final FarmXMine2Plugin plugin;
    private final File dataFolder;

    public StorageService(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public void load(UUID uuid, Consumer<PlayerStats> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerStats stats = new PlayerStats(uuid);
            File file = new File(dataFolder, uuid.toString() + ".yml");
            if (file.exists()) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                stats.setLevel(TrackType.MINE, cfg.getInt("mine.level"));
                stats.setXp(TrackType.MINE, cfg.getInt("mine.xp"));
                stats.setLevel(TrackType.FARM, cfg.getInt("farm.level"));
                stats.setXp(TrackType.FARM, cfg.getInt("farm.xp"));
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(stats));
        });
    }

    public void save(UUID uuid, PlayerStats stats) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(dataFolder, uuid.toString() + ".yml");
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("mine.level", stats.getLevel(TrackType.MINE));
            cfg.set("mine.xp", stats.getXp(TrackType.MINE));
            cfg.set("farm.level", stats.getLevel(TrackType.FARM));
            cfg.set("farm.xp", stats.getXp(TrackType.FARM));
            try {
                cfg.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveAllSync(Iterable<PlayerStats> all) {
        for (PlayerStats ps : all) {
            File file = new File(dataFolder, ps.getUuid().toString() + ".yml");
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("mine.level", ps.getLevel(TrackType.MINE));
            cfg.set("mine.xp", ps.getXp(TrackType.MINE));
            cfg.set("farm.level", ps.getLevel(TrackType.FARM));
            cfg.set("farm.xp", ps.getXp(TrackType.FARM));
            try {
                cfg.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
