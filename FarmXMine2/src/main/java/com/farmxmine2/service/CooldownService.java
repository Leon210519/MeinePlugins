package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.BlockVec;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks per-player block cooldowns and restores visuals after expiry. */
public class CooldownService {
    private final FarmXMine2Plugin plugin;
    private final Map<UUID, Map<BlockVec, CooldownEntry>> cooldowns = new ConcurrentHashMap<>();

    public CooldownService(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isCooling(UUID uuid, BlockVec vec) {
        Map<BlockVec, CooldownEntry> map = cooldowns.get(uuid);
        if (map == null) return false;
        CooldownEntry entry = map.get(vec);
        if (entry == null) return false;
        if (System.currentTimeMillis() >= entry.endMs) {
            map.remove(vec);
            return false;
        }
        return true;
    }

    public void start(UUID uuid, BlockVec vec, long endMs) {
        Map<BlockVec, CooldownEntry> map = cooldowns.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        CooldownEntry existing = map.get(vec);
        if (existing != null) {
            existing.task.cancel();
        }
        long delayTicks = Math.max(1L, (endMs - System.currentTimeMillis()) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> restore(uuid, vec), delayTicks);
        map.put(vec, new CooldownEntry(endMs, task));
    }

    private void restore(UUID uuid, BlockVec vec) {
        Map<BlockVec, CooldownEntry> map = cooldowns.get(uuid);
        if (map == null) return;
        CooldownEntry entry = map.get(vec);
        if (entry == null || System.currentTimeMillis() < entry.endMs) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            map.remove(vec);
            return;
        }
        World world = Bukkit.getWorld(vec.world());
        if (world == null || !world.isChunkLoaded(vec.x() >> 4, vec.z() >> 4)) {
            map.remove(vec);
            return;
        }
        Location loc = new Location(world, vec.x(), vec.y(), vec.z());
        player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
        map.remove(vec);
        if (map.isEmpty()) {
            cooldowns.remove(uuid);
        }
    }

    public void clear(UUID uuid) {
        Map<BlockVec, CooldownEntry> map = cooldowns.remove(uuid);
        if (map != null) {
            for (CooldownEntry entry : map.values()) {
                entry.task.cancel();
            }
        }
    }

    public void clear(Chunk chunk) {
        for (Iterator<Map.Entry<UUID, Map<BlockVec, CooldownEntry>>> it = cooldowns.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Map<BlockVec, CooldownEntry>> e = it.next();
            Map<BlockVec, CooldownEntry> map = e.getValue();
            map.entrySet().removeIf(en -> {
                BlockVec v = en.getKey();
                boolean sameChunk = v.world().equalsIgnoreCase(chunk.getWorld().getName()) &&
                        (v.x() >> 4) == chunk.getX() && (v.z() >> 4) == chunk.getZ();
                if (sameChunk) en.getValue().task.cancel();
                return sameChunk;
            });
            if (map.isEmpty()) it.remove();
        }
    }

    public void clearAll() {
        for (UUID uuid : new ArrayList<>(cooldowns.keySet())) {
            clear(uuid);
        }
    }

    private record CooldownEntry(long endMs, BukkitTask task) {}
}
