package com.lootfactory.prestige;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Appends a gold ★<level> to the display name of a factory item.
 * Ensures NO italics on the final name.
 */
public final class FactoryItemDisplayUtil {

    private FactoryItemDisplayUtil() {}

    public static ItemStack withPrestigeStar(ItemStack item, int prestigeLevel) {
        if (item == null || prestigeLevel <= 0) return item;
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        Component base = meta.hasDisplayName() ? meta.displayName() : Component.empty();
        int capped = Math.min(prestigeLevel, 5);
        Component star;
        if (capped <= 0) {
            star = Component.empty();
        } else if (capped == 1) {
            star = Component.text(" ★", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);
        } else {
            star = Component.text(" ★", NamedTextColor.GOLD)
                    .append(Component.text(Integer.toString(capped), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false);
        }
        Component finalName = (base == null ? Component.empty() : base).decoration(TextDecoration.ITALIC, false).append(star);
        meta.displayName(finalName);
        clone.setItemMeta(meta);
        return clone;
    }
}
