package com.specialitems.leveling;

import org.bukkit.ChatColor;

/**
 * Defines the rarity tiers for special items.
 * <p>
 * Colors are aligned with the LootFactory plugin so that items across
 * different plugins share the same visual language.
 */
public enum Rarity {
    COMMON(1.00, ChatColor.GRAY),
    UNCOMMON(1.05, ChatColor.GREEN),
    RARE(1.10, ChatColor.BLUE),
    EPIC(1.15, ChatColor.LIGHT_PURPLE),
    LEGENDARY(1.20, ChatColor.GOLD),
    STARFORGED(1.25, ChatColor.LIGHT_PURPLE);

    public final double xpMultiplier;
    public final ChatColor color;

    Rarity(double xpMultiplier, ChatColor color) {
        this.xpMultiplier = xpMultiplier;
        this.color = color;
    }

    /**
     * Parse a rarity from string, falling back to {@link #COMMON} when unknown.
     */
    public static Rarity fromString(String s) {
        if (s == null) return COMMON;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return COMMON;
        }
    }

    /**
     * Return the rarity name colored appropriately.
     */
    public String displayName() {
        String n = name().toLowerCase();
        if (this == STARFORGED) {
            n = "StarForged";
        } else {
            n = Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
        return color + n;
    }
}
