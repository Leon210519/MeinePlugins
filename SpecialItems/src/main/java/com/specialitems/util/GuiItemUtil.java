package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

/** Utility to build GUI display items for special items. */
public final class GuiItemUtil {
    private GuiItemUtil() {}

    private static boolean hasSpecialPdc(ItemStack it) {
        if (it == null) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (pdc == null) return false;
        for (NamespacedKey key : pdc.getKeys()) {
            String k = key.getKey();
            if (k != null && k.startsWith("ench_")) return true;
        }
        return false;
    }

    /**
     * Build a display item suitable for GUI views. Returns {@code null} if the item is not a special item.
     */
    public static ItemStack forDisplay(SpecialItemsPlugin plugin, ItemStack it) {
        if (plugin == null || it == null || it.getType() == Material.AIR) return null;
        ItemStack display = it.clone();
        var svc = plugin.leveling();
        boolean special = svc.isSpecialItem(display) || hasSpecialPdc(display);
        if (!special) return null;

        ItemLoreService.renderLore(display, plugin);
        return display;
    }
}
