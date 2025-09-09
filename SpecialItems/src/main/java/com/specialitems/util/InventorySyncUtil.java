package com.specialitems.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/** Utility for safely synchronizing inventory changes to the client. */
public final class InventorySyncUtil {

    private InventorySyncUtil() {}

    /** When true forces Player#updateInventory after each change. */
    public static boolean DEBUG_FORCE_UPDATE = false;

    /** Schedule setting a slot in the player's inventory on the next tick. */
    public static void setSlotNextTick(Plugin plugin, Player player, int slot, @Nullable ItemStack stack) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().setItem(slot, stack);
            updateIfNeeded(player);
        });
    }

    /** Clears the player's cursor on the next tick. */
    public static void clearCursorNextTick(Plugin plugin, Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setItemOnCursor(null);
            updateIfNeeded(player);
        });
    }

    /** Give the item to the player or drop it at their feet if inventory is full. */
    public static void giveOrDrop(Plugin plugin, Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            var leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
            updateIfNeeded(player);
        });
    }

    /** Calls {@link Player#updateInventory()} if debugging is enabled. */
    public static void updateIfNeeded(Player player) {
        if (DEBUG_FORCE_UPDATE) {
            player.updateInventory();
        }
    }
}
