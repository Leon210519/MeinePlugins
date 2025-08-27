package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically applies permission tiers based on player multipliers.
 */
public class PermissionTierService {
    private final LootPetsPlugin plugin;
    private final BoostService boostService;
    private final Permission permission;
    private final List<Tier> tiers = new ArrayList<>();
    private final String prefix;
    private final boolean replace;
    private final int intervalSeconds;
    private final Map<UUID, String> applied = new ConcurrentHashMap<>();
    private int taskId = -1;
    private boolean warned;

    private static class Tier {
        final double min;
        final double max;
        final String node;
        Tier(double min, double max, String node) {
            this.min = min;
            this.max = max;
            this.node = node;
        }
    }

    public PermissionTierService(LootPetsPlugin plugin, BoostService boostService, Permission permission, ConfigurationSection cfg) {
        this.plugin = plugin;
        this.boostService = boostService;
        this.permission = permission;
        this.intervalSeconds = cfg.getInt("update_interval_seconds", 10);
        this.prefix = cfg.getString("prefix", "lootpets.tier.");
        String mode = cfg.getString("node_mode", "REPLACE");
        this.replace = !"ADD_ONLY".equalsIgnoreCase(mode);
        List<?> list = cfg.getList("tiers");
        if (list != null) {
            for (Object o : list) {
                if (o instanceof Map<?,?> map) {
                    double min = toDouble(map.get("min"));
                    double max = toDouble(map.get("max"));
                    String node = String.valueOf(map.get("node"));
                    tiers.add(new Tier(min, max, node));
                }
            }
        }
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }

    public void start() {
        if (tiers.isEmpty()) {
            plugin.getLogger().warning("permission_tiers.tiers is empty; service disabled");
            return;
        }
        long period = intervalSeconds * 20L;
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, period, period);
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
        applied.clear();
    }

    public void invalidate(UUID uuid) {
        applied.remove(uuid);
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            update(player);
        }
    }

    private void update(Player player) {
        try {
            int bestIndex = -1;
            for (EarningType type : EarningType.values()) {
                double mult = boostService.getMultiplier(player, type);
                int idx = tierIndex(mult);
                if (idx > bestIndex) {
                    bestIndex = idx;
                }
            }
            String target = bestIndex >= 0 ? prefix + tiers.get(bestIndex).node : null;
            UUID id = player.getUniqueId();
            String current = applied.get(id);
            if (Objects.equals(current, target)) {
                return;
            }
            if (replace && current != null) {
                permission.playerRemove(player, current);
            }
            if (target != null && !permission.playerHas(player, target)) {
                permission.playerAdd(player, target);
                applied.put(id, target);
            } else if (target == null) {
                applied.remove(id);
            } else {
                applied.put(id, target);
            }
        } catch (Exception e) {
            if (!warned) {
                plugin.getLogger().warning("Permission tiers update failed: " + e.getMessage());
                warned = true;
            }
        }
    }

    private int tierIndex(double mult) {
        for (int i = 0; i < tiers.size(); i++) {
            Tier t = tiers.get(i);
            if (mult >= t.min && mult < t.max) {
                return i;
            }
        }
        return -1;
    }
}
