package com.farmxmine2.util;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/** Utility helpers for material classification. */
public final class Materials {
    private static final Set<Material> ORE_ALLOWLIST = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART
    );

    private Materials() {}

    public static boolean isOre(Material mat) {
        return ORE_ALLOWLIST.contains(mat);
    }

    public static boolean isCrop(Material mat) {
        return CROPS.contains(mat);
    }

    public static boolean isMature(Block block) {
        if (!(block.getBlockData() instanceof Ageable age)) return false;
        return age.getAge() == age.getMaximumAge();
    }

    public static boolean hasPickaxe(ItemStack tool) {
        return tool != null && tool.getType().name().endsWith("_PICKAXE");
    }

    public static boolean isMineableByPickaxe(Material mat) {
        return Tag.MINEABLE_PICKAXE.isTagged(mat) || ORE_ALLOWLIST.contains(mat);
    }
}

