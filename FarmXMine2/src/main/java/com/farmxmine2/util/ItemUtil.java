package com.farmxmine2.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

/** Utility for giving items to a player and dropping leftovers naturally. */
public final class ItemUtil {
    private ItemUtil() {}

    public static void giveAll(Player player, Collection<ItemStack> items) {
        if (items == null || items.isEmpty()) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        for (ItemStack l : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), l);
        }
    }
}
