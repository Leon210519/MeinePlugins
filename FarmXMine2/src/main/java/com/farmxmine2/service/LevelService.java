package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.PlayerStats;
import com.farmxmine2.model.TrackType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelService {
    private final FarmXMine2Plugin plugin;
    private final StorageService storage;
    private final ConfigService config;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();

    public LevelService(FarmXMine2Plugin plugin, StorageService storage, ConfigService config) {
        this.plugin = plugin;
        this.storage = storage;
        this.config = config;
    }

    public PlayerStats getStats(UUID uuid) {
        return stats.computeIfAbsent(uuid, PlayerStats::new);
    }

    public void loadStats(UUID uuid, PlayerStats loaded) {
        stats.put(uuid, loaded);
    }

    public void remove(UUID uuid) {
        stats.remove(uuid);
    }

    public void addXp(Player player, TrackType type) {
        int amount = type == TrackType.MINE ? config.getMineXpPer() : config.getFarmXpPer();
        PlayerStats ps = getStats(player.getUniqueId());
        ps.addXp(type, amount);
        boolean leveled = false;
        while (ps.getXp(type) >= xpNeeded(ps.getLevel(type))) {
            ps.setXp(type, ps.getXp(type) - xpNeeded(ps.getLevel(type)));
            ps.setLevel(type, ps.getLevel(type) + 1);
            leveled = true;
        }
        if (leveled) {
            String msg = plugin.color(plugin.getMessages().getString("level-up"))
                    .replace("%level%", String.valueOf(ps.getLevel(type)))
                    .replace("%track%", type.name().toLowerCase());
            player.sendMessage(msg);
        }
        player.sendActionBar("+" + amount + " XP");
        plugin.getBossBarService().update(player, type);
    }

    public int xpNeeded(int level) {
        return config.getBaseXpPerLevel() + level * config.getXpGrowthPerLevel();
    }

    public StorageService getStorage() { return storage; }

    public Iterable<PlayerStats> getAll() {
        return stats.values();
    }
}
