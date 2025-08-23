package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Greenthumb implements CustomEffect {
    @Override public String id() { return "greenthumb"; }
    @Override public String displayName() { return "Greenthumb"; }
    @Override public int maxLevel() { return 5; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_HOE"); }
    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        // Growth handled in BlockListener after replanting.
    }
}
