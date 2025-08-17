package com.specialitems.util;

import com.specialitems.leveling.Keys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Utility to mark an ItemStack as "Special Item" so the leveling system applies. */
public final class Tagger {
    private Tagger() {}

    /**
     * Tag the item with a stable identifier for your template (e.g. "omega_sword").
     * If already tagged, the method is a no-op.
     */
    public static void tagAsSpecial(Plugin plugin, ItemStack item, String templateId) {
        if (plugin == null || item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        var keys = new Keys(plugin);
        if (!pdc.has(keys.SI_ID, PersistentDataType.STRING)) {
            pdc.set(keys.SI_ID, PersistentDataType.STRING, templateId == null ? "unknown" : templateId);
            item.setItemMeta(meta);
        }
    }
}