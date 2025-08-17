package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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
        int radius = Math.min(com.specialitems.util.Configs.cfg.getInt("effects.harvester.radius", 1) + (level-1), 3);
        int max = com.specialitems.util.Configs.cfg.getInt("general.max-broken-blocks", 128);
        Block origin = e.getBlock();
        int broken = 0;
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++) {
            if (dx==0 && dz==0) continue;
            Block b = origin.getRelative(dx, 0, dz);
            if (b.getBlockData() instanceof Ageable age && age.getAge() == age.getMaximumAge()) {
                b.breakNaturally(tool, true); broken++; if (broken >= max) return;
            }
        }
    }
}
