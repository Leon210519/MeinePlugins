package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.Region;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class VeinMinerCompat {
    private final FarmXMine2Plugin plugin;
    private final List<String> detectLore;
    private final int maxBlocks;

    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    public VeinMinerCompat(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("compat.veinminer");
        detectLore = sec.getStringList("detect_lore");
        maxBlocks = sec.getInt("max_blocks");
    }

    public boolean hasVeinMiner(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            for (String d : detectLore) {
                if (stripped.equalsIgnoreCase(d)) return true;
            }
        }
        return false;
    }

    public Set<Block> collect(Block start, Region region) {
        Set<Block> result = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        Material target = start.getType();
        queue.add(start);
        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block b = queue.poll();
            if (result.contains(b)) continue;
            if (!region.isAllowed(b.getType())) continue;
            if (b.getType() != target) continue;
            result.add(b);
            for (BlockFace face : FACES) {
                Block n = b.getRelative(face);
                if (!result.contains(n) && n.getType() == target) {
                    queue.add(n);
                }
            }
        }
        return result;
    }
}
