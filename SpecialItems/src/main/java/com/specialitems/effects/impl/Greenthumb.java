package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import com.specialitems.util.Configs;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class Greenthumb implements CustomEffect {
    @Override public String id() { return "greenthumb"; }
    @Override public String displayName() { return "Greenthumb"; }
    @Override public int maxLevel() { return 5; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_HOE"); }
    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        if (!Configs.effectEnabled(id())) return;
        Block b = e.getBlock();
        if (!(b.getBlockData() instanceof Ageable age)) return;
        if (age.getAge() < age.getMaximumAge()) return;
        double chance = Configs.effectDouble(id(), "bonus_per_level", 0.15) * level;
        if (ThreadLocalRandom.current().nextDouble() > chance) return;
        Material crop = b.getType();
        Material main = mainDrop(crop);
        ItemStack bonus = new ItemStack(main, 1);
        boolean direct = Configs.cfg.getBoolean("farmxmine.direct_to_inventory", true);
        boolean voidOverflow = Configs.cfg.getBoolean("inventory.void_overflow", true);
        if (direct) {
            var leftover = player.getInventory().addItem(bonus);
            if (!voidOverflow) leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        } else {
            player.getWorld().dropItemNaturally(b.getLocation(), bonus);
        }
        if (Configs.cfg.getBoolean("effects.greenthumb.duplicate_seeds", false)) {
            Material seed = seedDrop(crop);
            if (seed != null) {
                ItemStack seedIt = new ItemStack(seed, 1);
                if (direct) {
                    var leftover = player.getInventory().addItem(seedIt);
                    if (!voidOverflow) leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
                } else {
                    player.getWorld().dropItemNaturally(b.getLocation(), seedIt);
                }
            }
        }
    }
    private Material mainDrop(Material crop) {
        switch (crop) {
            case WHEAT: return Material.WHEAT;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT;
            default: return crop;
        }
    }
    private Material seedDrop(Material crop) {
        switch (crop) {
            case WHEAT: return Material.WHEAT_SEEDS;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT_SEEDS;
            default: return null;
        }
    }
}
