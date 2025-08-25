package com.specialitems2.effects.impl;

import com.specialitems2.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Harvester implements CustomEffect {
    @Override public String id() { return "harvester"; }
    @Override public String displayName() { return "Harvester"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_HOE"); }
    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        // handled in BlockListener for direct yield scaling
    }
}
