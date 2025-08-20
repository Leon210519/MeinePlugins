package com.farmxmine.service;

import com.farmxmine.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageService implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final File dataFolder;

    public StorageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.dataFolder.mkdirs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public PlayerData get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerData());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File f = new File(dataFolder, id + ".yml");
            if (f.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                PlayerData data = new PlayerData();
                data.setMiningLevel(yaml.getInt("miningLevel", 1));
                data.setMiningXp(yaml.getDouble("miningXp", 0.0));
                data.setFarmingLevel(yaml.getInt("farmingLevel", 1));
                data.setFarmingXp(yaml.getDouble("farmingXp", 0.0));
                data.getArtifacts().addAll(yaml.getIntegerList("artifacts"));
                cache.put(id, data);
            } else {
                cache.put(id, new PlayerData());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveAsync(event.getPlayer().getUniqueId());
        cache.remove(event.getPlayer().getUniqueId());
    }

    public void saveAsync(UUID id) {
        PlayerData data = cache.get(id);
        if (data == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File f = new File(dataFolder, id + ".yml");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("miningLevel", data.getMiningLevel());
            yaml.set("miningXp", data.getMiningXp());
            yaml.set("farmingLevel", data.getFarmingLevel());
            yaml.set("farmingXp", data.getFarmingXp());
            yaml.set("artifacts", data.getArtifacts().stream().toList());
            try {
                yaml.save(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveAll() {
        for (UUID id : cache.keySet()) {
            saveAsync(id);
        }
    }
}
