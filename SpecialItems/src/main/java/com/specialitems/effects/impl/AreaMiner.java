package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class AreaMiner implements CustomEffect {
    @Override public String id() { return "areaminer"; }
    @Override public String displayName() { return "Area Miner"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) {
        return type.name().endsWith("_PICKAXE") || type.name().endsWith("_SHOVEL") || type.name().endsWith("_AXE");
    }
    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        int radius = Math.min(com.specialitems.util.Configs.cfg.getInt("effects.areaminer.radius", 1) + (level-1), 3);
        int max = com.specialitems.util.Configs.cfg.getInt("general.max-broken-blocks", 128);
        int broken = 0;
        Block origin = e.getBlock();
        for (int dx=-radius; dx<=radius; dx++) for (int dy=-radius; dy<=radius; dy++) for (int dz=-radius; dz<=radius; dz++) {
            Block b = origin.getRelative(dx, dy, dz);
            if (b.equals(origin)) continue;
            if (broken >= max) return;
            if (tool.getType().name().endsWith("_PICKAXE")
                    && (b.getType().name().endsWith("_ORE") || b.getType().name().contains("STONE") || b.getType().name().contains("DEEPSLATE"))) {
                b.breakNaturally(tool, true); broken++;
            } else if (tool.getType().name().endsWith("_AXE")
                    && (b.getType().name().contains("LOG") || b.getType().name().contains("PLANKS") || b.getType().name().contains("WOOD"))) {
                b.breakNaturally(tool, true); broken++;
            } else if (tool.getType().name().endsWith("_SHOVEL")
                    && (b.getType().name().contains("DIRT") || b.getType().name().contains("SAND") || b.getType().name().contains("GRAVEL") || b.getType().name().contains("CLAY"))) {
                b.breakNaturally(tool, true); broken++;
            }
        }
    }
}
