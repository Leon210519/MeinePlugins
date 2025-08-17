package com.lootfactory.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GuiAutoRefresher drives 1 Hz refreshes for open GUIs so dynamic content like XP progress,
 * costs, and progress panes stay accurate while the inventory is open.
 *
 * Usage:
 *  1) When you open a GUI, call:
 *     GuiAutoRefresher.start(plugin, player, inventory, () -> YourGuiClass.refresh(player, inventory));
 *  2) Implement the refresh(...) method to recompute and update only the dynamic slots (XP item,
 *     cost lore, progress glass panes, etc.).
 *  3) On InventoryCloseEvent, call GuiAutoRefresher.stop(inventory) (the provided GUIListener
 *     in this project already does this).
 *
 * This class is independent from the rest of the plugin and can be dropped in without changing API.
 */
public final class GuiAutoRefresher {

    private static final Map<Inventory, BukkitRunnable> tasks = new ConcurrentHashMap<>();

    private GuiAutoRefresher() {}

    /**
     * Start a 1 Hz refresh for the given inventory. If a task already exists for this inventory, it is kept.
     */
    public static void start(Plugin plugin, Player viewer, Inventory inventory, Runnable refresher) {
        // Avoid double-scheduling for the same inventory
        tasks.computeIfAbsent(inventory, inv -> {
            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    // If nobody is viewing this inv, stop.
                    if (inv.getViewers().isEmpty()) {
                        stop(inv);
                        return;
                    }
                    // If original viewer is no longer viewing this inv, but others are, keep refreshing.
                    // Ensure we are on the main thread and run the refresher.
                    try {
                        refresher.run();
                    } catch (Throwable t) {
                        // Fail-safe: never let a GUI refresh crash the server. Stop this inventory on exception.
                        t.printStackTrace();
                        stop(inv);
                    }
                }
            };
            task.runTaskTimer(plugin, 20L, 20L); // 1 second
            return task;
        });
    }

    /**
     * Stop refresh for a specific inventory.
     */
    public static void stop(Inventory inventory) {
        BukkitRunnable task = tasks.remove(inventory);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Stop all running refresh tasks (e.g., on plugin disable).
     */
    public static void stopAll() {
        for (BukkitRunnable t : tasks.values()) {
            try {
                t.cancel();
            } catch (Exception ignored) {}
        }
        tasks.clear();
    }
}
