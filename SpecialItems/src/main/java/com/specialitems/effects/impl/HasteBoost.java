package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HasteBoost implements CustomEffect {
    @Override public String id() { return "haste_boost"; }
    @Override public String displayName() { return "Haste Boost"; }
    @Override public int maxLevel() { return 5; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_PICKAXE"); }
    @Override
    public void onTick(Player player, ItemStack item, int level) {
        // Effect handled on block break in BlockListener.
    }
    @Override
    public void onItemHeld(Player player, ItemStack item, int level) {
        // Effect handled on block break in BlockListener.
    }
}
