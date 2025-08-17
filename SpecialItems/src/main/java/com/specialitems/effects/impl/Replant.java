package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Replant implements CustomEffect {
    @Override public String id() { return "replant"; }
    @Override public String displayName() { return "Replant"; }
    @Override public int maxLevel() { return 1; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_HOE"); }
    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        Block b = e.getBlock();
        if (!(b.getBlockData() instanceof Ageable age)) return;
        if (age.getMaximumAge() != age.getAge()) return;
        b.setType(b.getType());
        if (b.getBlockData() instanceof Ageable a2) { a2.setAge(0); b.setBlockData(a2, false); }
    }
}
