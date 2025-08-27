package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles world and context rules for LootPets operations.
 */
public class RuleService {

    private final LootPetsPlugin plugin;
    private List<String> equipAllow;
    private List<String> equipDeny;
    private List<String> levelingAllow;
    private List<String> levelingDeny;
    private List<String> applyAllow;
    private List<String> applyDeny;
    private boolean disallowSpectator;

    private final Map<String, CacheEntry> equipCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> levelingCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> applyCache = new ConcurrentHashMap<>();
    private final long ttlMillis = 5000L;

    private record CacheEntry(boolean allow, long expire) {}

    public RuleService(LootPetsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        equipAllow = cfg.getStringList("rules.equip_worlds_allow");
        equipDeny = cfg.getStringList("rules.equip_worlds_deny");
        levelingAllow = cfg.getStringList("rules.leveling_worlds_allow");
        levelingDeny = cfg.getStringList("rules.leveling_worlds_deny");
        applyAllow = cfg.getStringList("rules.boost_apply_worlds_allow");
        applyDeny = cfg.getStringList("rules.boost_apply_worlds_deny");
        disallowSpectator = cfg.getBoolean("rules.disallow_in_spectator", true);
        clearCaches();
    }

    public void clearCaches() {
        equipCache.clear();
        levelingCache.clear();
        applyCache.clear();
    }

    public boolean canEquip(Player player) {
        if (disallowSpectator && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        return resolve(player.getWorld().getName(), equipAllow, equipDeny, equipCache);
    }

    public boolean canLevel(Player player) {
        if (disallowSpectator && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        return resolve(player.getWorld().getName(), levelingAllow, levelingDeny, levelingCache);
    }

    public boolean canApply(Player player) {
        if (disallowSpectator && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        return resolve(player.getWorld().getName(), applyAllow, applyDeny, applyCache);
    }

    private boolean resolve(String world, List<String> allow, List<String> deny, Map<String, CacheEntry> cache) {
        long now = System.currentTimeMillis();
        CacheEntry ce = cache.get(world);
        if (ce != null && ce.expire > now) {
            return ce.allow;
        }
        boolean allowed = true;
        if (!allow.isEmpty()) {
            allowed = allow.contains(world);
        }
        if (deny.contains(world)) {
            allowed = false;
        }
        cache.put(world, new CacheEntry(allowed, now + ttlMillis));
        return allowed;
    }
}
