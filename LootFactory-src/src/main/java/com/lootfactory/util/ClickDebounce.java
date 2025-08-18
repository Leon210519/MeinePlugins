package com.lootfactory.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple per-player action debounce to prevent double-execution of GUI clicks etc.
 * Usage:
 *   if (!ClickDebounce.shouldRun(player.getUniqueId(), "prestige-click", 300)) return;
 */
public final class ClickDebounce {
    private static final Map<String, Long> last = new ConcurrentHashMap<>();

    private ClickDebounce() {}

    public static boolean shouldRun(UUID playerId, String actionKey, long cooldownMs) {
        long now = System.currentTimeMillis();
        String key = playerId.toString() + ":" + actionKey;
        Long prev = last.get(key);
        if (prev != null && (now - prev) < cooldownMs) {
            return false;
        }
        last.put(key, now);
        return true;
    }
}
