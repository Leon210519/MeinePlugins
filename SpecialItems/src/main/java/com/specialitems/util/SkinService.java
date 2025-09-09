package com.specialitems.util;

import com.specialitems.leveling.Rarity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Applies the correct CustomModelData for armor pieces based on rarity.
 * This only modifies the existing item meta in-place and never replaces the
 * stack or other metadata.
 */
public final class SkinService {

    private static final Logger LOGGER = Logger.getLogger("SpecialItems/Textures");

    /** Immutable mapping of rarities to their model data. */
    private static final Map<Rarity, Integer> CMD_MAP = Map.of(
            Rarity.EPIC, 2104,
            Rarity.LEGENDARY, 2105,
            Rarity.STARFORGED, 2106
    );

    private SkinService() {}

    /**
     * Applies the skin for the given rarity if a mapping exists.
     *
     * @param item   item to modify
     * @param rarity rarity of the item
     * @param slot   armor slot (unused but reserved for future expansion)
     */
    public static void applyForRarity(ItemStack item, Rarity rarity, EquipmentSlot slot) {
        if (item == null || rarity == null) return;
        Integer cmd = CMD_MAP.get(rarity);
        if (cmd == null) {
            LOGGER.warning("MISSING CMD MAP for " + rarity);
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        Integer before = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
        Integer after = meta.hasCustomModelData() ? meta.getCustomModelData() : null;
        LOGGER.fine("rarity=" + rarity + " intendedCMD=" + cmd + " before=" + before + " after=" + after);
    }
}
