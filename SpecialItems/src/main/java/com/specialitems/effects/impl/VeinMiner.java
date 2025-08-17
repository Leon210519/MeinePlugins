package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class VeinMiner implements CustomEffect {
    @Override public String id() { return "veinminer"; }
    @Override public String displayName() { return "Vein Miner"; }
    @Override public int maxLevel() { return 5; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_PICKAXE"); }

    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        Set<Material> ores = new HashSet<>();
        for (String s : com.specialitems.util.Configs.cfg.getStringList("effects.veinminer.ores")) {
            Material m = Material.matchMaterial(s); if (m != null) ores.add(m);
        }
        Block start = e.getBlock();
        if (!ores.contains(start.getType())) return;
        int max = com.specialitems.util.Configs.cfg.getInt("general.max-broken-blocks", 128);
        int limit = Math.min(max, 16 + level * 32);
        bfsBreak(player, tool, start, ores, limit);
    }
    private void bfsBreak(Player player, ItemStack tool, Block start, Set<Material> match, int limit) {
        Queue<Block> q = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        q.add(start); visited.add(start);
        int broken = 0;
        while (!q.isEmpty() && broken < limit) {
            Block b = q.poll();
            if (!match.contains(b.getType())) continue;
            if (!b.equals(start)) b.breakNaturally(tool, true);
            broken++;
            for (int dx=-1; dx<=1; dx++) for (int dy=-1; dy<=1; dy++) for (int dz=-1; dz<=1; dz++) {
                if (dx==0 && dy==0 && dz==0) continue;
                Block nb = b.getRelative(dx, dy, dz);
                if (!visited.contains(nb) && match.contains(nb.getType())) { visited.add(nb); q.add(nb); }
            }
        }
    }
}
