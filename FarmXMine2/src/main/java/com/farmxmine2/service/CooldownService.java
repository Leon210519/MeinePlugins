package com.farmxmine2.service;

import com.farmxmine2.model.BlockKey;
import org.bukkit.Chunk;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks per-player block cooldowns. */
public class CooldownService {
    private final Map<UUID, Map<BlockKey, Long>> cooldowns = new ConcurrentHashMap<>();

    public boolean isCooling(UUID uuid, BlockKey key) {
        Map<BlockKey, Long> map = cooldowns.get(uuid);
        if (map == null) return false;
        Long end = map.get(key);
        if (end == null) return false;
        if (System.currentTimeMillis() >= end) {
            map.remove(key);
            if (map.isEmpty()) cooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public void start(UUID uuid, BlockKey key, long endMs) {
        Map<BlockKey, Long> map = cooldowns.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        map.put(key, endMs);
    }

    public void end(UUID uuid, BlockKey key) {
        Map<BlockKey, Long> map = cooldowns.get(uuid);
        if (map == null) return;
        map.remove(key);
        if (map.isEmpty()) cooldowns.remove(uuid);
    }

    public void clear(UUID uuid) {
        cooldowns.remove(uuid);
    }

    public void clear(Chunk chunk) {
        for (Iterator<Map.Entry<UUID, Map<BlockKey, Long>>> it = cooldowns.entrySet().iterator(); it.hasNext();) {
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
}
