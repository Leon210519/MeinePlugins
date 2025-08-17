package com.specialitems.leveling;

public enum Rarity {
    COMMON(1.00),
    UNCOMMON(1.05),
    RARE(1.10),
    EPIC(1.15),
    LEGENDARY(1.20),
    MYTHIC(1.30);

    public final double xpMultiplier;
    Rarity(double xpMultiplier) { this.xpMultiplier = xpMultiplier; }

    public static Rarity fromString(String s) {
        if (s == null) return COMMON;
        try { return valueOf(s.trim().toUpperCase()); } catch (Exception e) { return COMMON; }
    }
}
