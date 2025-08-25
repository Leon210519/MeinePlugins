package com.specialitems2.leveling;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    public final NamespacedKey SI_ID;               // special-item marker (string)
    public final NamespacedKey LEVEL;               // int
    public final NamespacedKey XP;                  // double
    public final NamespacedKey BONUS_YIELD_PCT;     // double (hoe-only)
    public final NamespacedKey TOOL_CLASS;          // optional cache
    public final NamespacedKey RARITY;              // string, optional
    public final NamespacedKey PITY;                // double [0..1]

    public Keys(Plugin plugin) {
        SI_ID = new NamespacedKey(plugin, "si_id"); // adjust if your marker differs
        LEVEL = new NamespacedKey(plugin, "si_level");
        XP = new NamespacedKey(plugin, "si_xp");
        BONUS_YIELD_PCT = new NamespacedKey(plugin, "si_bonus_yield_pct");
        TOOL_CLASS = new NamespacedKey(plugin, "si_tool_class");
        RARITY = new NamespacedKey(plugin, "si_rarity");
        PITY = new NamespacedKey(plugin, "si_pity");
    }
}
