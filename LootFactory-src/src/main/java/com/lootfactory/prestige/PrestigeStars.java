package com.lootfactory.prestige;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;

/** Utilities to append a gold star suffix based on prestige (max 5). */
public final class PrestigeStars {
    private PrestigeStars() {}

    /** Legacy string suffix for inventory titles etc. */
    public static String legacyStarSuffix(int prestige) {
        if (prestige <= 0) return "";
        int capped = Math.min(prestige, 5);
        if (capped == 1) return " " + ChatColor.GOLD + "★";
        return " " + ChatColor.GOLD + "★" + ChatColor.WHITE + capped;
    }

    /** Append stars to a legacy base name. */
    public static String withStarsLegacy(String base, int prestige) {
        return base + legacyStarSuffix(prestige);
    }

    /** Component suffix for item names, with no italics. */
    public static Component compStarSuffix(int prestige) {
        if (prestige <= 0) return Component.empty();
        int capped = Math.min(prestige, 5);
        Component star = Component.text(" ★", NamedTextColor.GOLD);
        if (capped == 1) {
            return star.decoration(TextDecoration.ITALIC, false);
        } else {
            return star.append(Component.text(Integer.toString(capped), NamedTextColor.WHITE))
                       .decoration(TextDecoration.ITALIC, false);
        }
    }

    /** Append stars to a component base name, removing italics. */
    public static Component withStars(Component base, int prestige) {
        if (base == null) base = Component.empty();
        return base.decoration(TextDecoration.ITALIC, false).append(compStarSuffix(prestige));
    }
}
