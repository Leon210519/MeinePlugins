package com.specialitems.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Utility to normalize CustomModelData values on items. */
public final class CmdFixer {

    private CmdFixer() {}

    /**
     * Ensures the item's CustomModelData is stored as a strict integer via the
     * Bukkit API. If the item has no meta or CMD, it is returned unchanged.
     */
    public static ItemStack normalize(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        Integer cmd = meta.getCustomModelData();
        if (cmd == null) return item;

        // Reapply via Bukkit API to ensure integer storage
        ItemUtil.forceSetCustomModelData(item, cmd);
        return item;
    }
}

