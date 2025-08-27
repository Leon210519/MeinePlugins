package com.farmxmine2.service;

import com.farmxmine2.model.BlockKey;
import org.bukkit.Chunk;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks block cooldowns globally and optionally per player.
 */
public class CooldownService {
    /** Global block cooldowns. */
    private final Map<BlockKey, Long> blockCooldowns = new ConcurrentHashMap<>();
    /** Optional per-player block cooldowns. */
    private final Map<UUID, Map<BlockKey, Long>> playerCooldowns = new ConcurrentHashMap<>();

    // Global cooldowns ------------------------------------------------------

    public boolean isCooling(BlockKey key) {
        Long end = blockCooldowns.get(key);
        if (end == null) return false;
        if (System.currentTimeMillis() >= end) {
            blockCooldowns.remove(key);
            return false;
        }
        return true;
    }

    /** Returns remaining global cooldown in milliseconds or 0 if none. */
    public long getRemaining(BlockKey key) {
        Long end = blockCooldowns.get(key);
        if (end == null) return 0L;
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0L) {
            blockCooldowns.remove(key);
            return 0L;
        }
        return remaining;
    }

    public void start(BlockKey key, long endMs) {
        blockCooldowns.put(key, endMs);
    }

    public void end(BlockKey key) {
        blockCooldowns.remove(key);
    }

    // Optional per-player cooldowns ---------------------------------------

    public boolean isCooling(UUID uuid, BlockKey key) {
        Map<BlockKey, Long> map = playerCooldowns.get(uuid);
        if (map == null) return false;
        Long end = map.get(key);
        if (end == null) return false;
        if (System.currentTimeMillis() >= end) {
            map.remove(key);
            if (map.isEmpty()) playerCooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    /** Returns remaining player-specific cooldown in milliseconds or 0 if none. */
    public long getRemaining(UUID uuid, BlockKey key) {
        Map<BlockKey, Long> map = playerCooldowns.get(uuid);
        if (map == null) return 0L;
        Long end = map.get(key);
        if (end == null) return 0L;
        long remaining = end - System.currentTimeMillis();
        if (remaining <= 0L) {
            map.remove(key);
            if (map.isEmpty()) playerCooldowns.remove(uuid);
            return 0L;
        }
        return remaining;
    }

    public void start(UUID uuid, BlockKey key, long endMs) {
        Map<BlockKey, Long> map = playerCooldowns.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        map.put(key, endMs);
    }

    public void end(UUID uuid, BlockKey key) {
        Map<BlockKey, Long> map = playerCooldowns.get(uuid);
        if (map == null) return;
        map.remove(key);
        if (map.isEmpty()) playerCooldowns.remove(uuid);
    }

    public void clear(UUID uuid) {
        playerCooldowns.remove(uuid);
    }

    // Clearing -------------------------------------------------------------

    public void clear(Chunk chunk) {
        blockCooldowns.entrySet().removeIf(en -> {
            BlockKey v = en.getKey();
            return v.world().equalsIgnoreCase(chunk.getWorld().getName()) &&
                    (v.x() >> 4) == chunk.getX() && (v.z() >> 4) == chunk.getZ();
        });

        for (Iterator<Map.Entry<UUID, Map<BlockKey, Long>>> it = playerCooldowns.entrySet().iterator(); it.hasNext();) {
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
        blockCooldowns.clear();
        playerCooldowns.clear();
    }
}
