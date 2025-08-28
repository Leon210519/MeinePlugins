package com.lootpets.util;

import com.lootpets.LootPetsPlugin;
import com.lootpets.service.DebugService;

/**
 * Legacy wrapper maintained for backwards compatibility. Delegates to the
 * runtime {@link DebugService} when present.
 */
public final class DebugLogger {
    private DebugLogger() {}

    public static void debug(LootPetsPlugin plugin, String category, String message) {
        DebugService svc = plugin.getDebugService();
        if (svc != null) {
            svc.debug(category, message);
        }
    }
}
