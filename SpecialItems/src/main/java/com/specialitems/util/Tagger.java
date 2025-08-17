package com.specialitems.util;

import com.specialitems.leveling.Keys;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
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
        }
        if (!pdc.has(keys.LEVEL, PersistentDataType.INTEGER)) {
            pdc.set(keys.LEVEL, PersistentDataType.INTEGER, 1);
        }
        if (!pdc.has(keys.XP, PersistentDataType.DOUBLE)) {
            pdc.set(keys.XP, PersistentDataType.DOUBLE, 0.0);
        }

        meta.setUnbreakable(true);
        try { meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS); } catch (Throwable ignored) {}
        if (meta.getEnchants().isEmpty()) {
            try { meta.addEnchant(Enchantment.UNBREAKING, 1, true); } catch (Throwable ignored) {}
        }

        item.setItemMeta(meta);
        ItemUtil.setLevelLore(item, 1);
    }
}