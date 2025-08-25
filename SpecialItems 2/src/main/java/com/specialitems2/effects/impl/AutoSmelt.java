package com.specialitems2.effects.impl;

import com.specialitems2.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AutoSmelt implements CustomEffect {
    @Override public String id() { return "autosmelt"; }
    @Override public String displayName() { return "AutoSmelt"; }
    @Override public int maxLevel() { return 1; }
    @Override public boolean supports(Material type) {
        return type.name().endsWith("_PICKAXE") || type.name().endsWith("_AXE") || type.name().endsWith("_SHOVEL");
    }
    public static final Map<Material, Material> SMELTS = new HashMap<>();
    static {
        SMELTS.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELTS.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELTS.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELTS.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELTS.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELTS.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELTS.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELTS.put(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET);
        SMELTS.put(Material.SAND, Material.GLASS);
        SMELTS.put(Material.COBBLESTONE, Material.STONE);
        SMELTS.put(Material.COBBLED_DEEPSLATE, Material.DEEPSLATE);
        SMELTS.put(Material.CLAY, Material.TERRACOTTA);
        SMELTS.put(Material.KELP, Material.DRIED_KELP);
        SMELTS.put(Material.CACTUS, Material.GREEN_DYE);
    }
    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        // Nothing here; Telekinesis handles converting drops if both present.
    }
}
