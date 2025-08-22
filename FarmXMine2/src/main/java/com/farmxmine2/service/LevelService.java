package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.CurveConfig;
import com.farmxmine2.model.PlayerStats;
import com.farmxmine2.model.TrackType;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelService {
    private final FarmXMine2Plugin plugin;
    private final StorageService storage;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();
    private final int mineXpPer;
    private final int farmXpPer;
    private final CurveConfig curve;

    public LevelService(FarmXMine2Plugin plugin, StorageService storage) {
        this.plugin = plugin;
        this.storage = storage;
        ConfigurationSection level = plugin.getConfig().getConfigurationSection("leveling");
        ConfigurationSection xpSec = level.getConfigurationSection("xp_per_harvest");
        mineXpPer = xpSec.getInt("mine");
        farmXpPer = xpSec.getInt("farm");
        ConfigurationSection curveSec = level.getConfigurationSection("curve");
        curve = new CurveConfig(curveSec.getString("type"), curveSec.getDouble("pow_base"), curveSec.getDouble("pow_exp"), curveSec.getInt("linear_base"));
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
        int amount = type == TrackType.MINE ? mineXpPer : farmXpPer;
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
        plugin.getBossBarService().update(player, type);
    }

    public int xpNeeded(int level) {
        if (curve.type().equalsIgnoreCase("pow")) {
            return (int) Math.ceil(curve.powBase() * Math.pow(level <= 0 ? 1 : level, curve.powExp()));
        } else {
            return curve.linearBase() + (level * curve.linearBase());
        }
    }

    public StorageService getStorage() { return storage; }

    public Iterable<PlayerStats> getAll() {
        return stats.values();
    }
}
