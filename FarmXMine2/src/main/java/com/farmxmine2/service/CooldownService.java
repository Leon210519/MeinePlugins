package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.BlockKey;
import com.farmxmine2.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks per-player block cooldowns and restores visuals after expiry. */
public class CooldownService {
    private final FarmXMine2Plugin plugin;
    private final Map<UUID, Map<BlockKey, Long>> cooldowns = new ConcurrentHashMap<>();

    public CooldownService(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isCooling(UUID uuid, BlockKey key) {
        Map<BlockKey, Long> map = cooldowns.get(uuid);
        if (map == null) return false;
        Long end = map.get(key);
        if (end == null) return false;
        if (System.currentTimeMillis() >= end) {
            map.remove(key);
            return false;
        }
        return true;
    }

    public void start(UUID uuid, BlockKey key, long endMs) {
        Map<BlockKey, Long> map = cooldowns.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        map.put(key, endMs);
        long delayMs = endMs - System.currentTimeMillis();
        Scheduler.restoreLater(plugin, () -> restore(uuid, key), delayMs);
    }

    private void restore(UUID uuid, BlockKey key) {
        Map<BlockKey, Long> map = cooldowns.get(uuid);
        if (map == null) return;
        Long end = map.get(key);
        if (end == null || System.currentTimeMillis() < end) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            map.remove(key);
            return;
        }
        World world = Bukkit.getWorld(key.world());
        if (world == null || !world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) {
            map.remove(key);
            return;
        }
        Location loc = new Location(world, key.x(), key.y(), key.z());
        player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
        map.remove(key);
        if (map.isEmpty()) {
            cooldowns.remove(uuid);
        }
    }

    public void clear(UUID uuid) {
        cooldowns.remove(uuid);
    }

    public void clear(Chunk chunk) {
        for (Iterator<Map.Entry<UUID, Map<BlockKey, Long>>> it = cooldowns.entrySet().iterator(); it.hasNext(); ) {
            Map<BlockKey, Long> map = it.next().getValue();
            map.entrySet().removeIf(en -> {
                BlockKey v = en.getKey();
                return v.world().equalsIgnoreCase(chunk.getWorld().getName()) &&
                        (v.x() >> 4) == chunk.getX() && (v.z() >> 4) == chunk.getZ();
            });
            if (map.isEmpty()) it.remove();
        }
    }

    public void clearAll() {
        cooldowns.clear();
    }

    public void restorePending(Player player) {
        Map<BlockKey, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return;
        for (BlockKey key : map.keySet()) {
            World world = Bukkit.getWorld(key.world());
            if (world == null || !world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) continue;
            Location loc = new Location(world, key.x(), key.y(), key.z());
            player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
        }
    }
}
