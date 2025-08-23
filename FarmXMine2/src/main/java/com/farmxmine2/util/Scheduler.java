package com.farmxmine2.util;

import com.farmxmine2.FarmXMine2Plugin;
import org.bukkit.Bukkit;

/** Utility for scheduling restoration tasks. */
public final class Scheduler {
    private Scheduler() {}

    public static void restoreLater(FarmXMine2Plugin plugin, Runnable task, long delayMs) {
        long ticks = Math.max(1L, delayMs / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }
}
