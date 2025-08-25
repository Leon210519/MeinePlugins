package com.specialitems2.util;

import com.specialitems2.SpecialItems2Plugin;
import com.specialitems2.leveling.LevelingService;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** Utility to initialise an ItemStack as a Special Item for the leveling system. */
public final class Tagger {
    private Tagger() {}

    /**
     * Initialises the item for the leveling system.
     * The templateId is currently unused but kept for API compatibility.
     */
    public static void tagAsSpecial(Plugin plugin, ItemStack item, String templateId) {
        if (plugin == null || item == null) return;
        LevelingService svc = SpecialItems2Plugin.getInstance().leveling();
        if (svc != null) {
            svc.initItem(item);
        }
    }
}
