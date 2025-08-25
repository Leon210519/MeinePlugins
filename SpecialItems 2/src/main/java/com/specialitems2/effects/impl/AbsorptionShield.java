package com.specialitems2.effects.impl;

import com.specialitems2.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AbsorptionShield implements CustomEffect {
    @Override public String id() { return "absorption_shield"; }
    @Override public String displayName() { return "Absorption Shield"; }
    @Override public int maxLevel() { return 4; }
    @Override public boolean supports(Material type) {
        String n = type.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }
    @Override
    public void onTick(Player player, ItemStack item, int level) {
        // Effect applied periodically in PlayerListener.
    }
}
