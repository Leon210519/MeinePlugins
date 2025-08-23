package com.farmxmine2.util;

import org.bukkit.Material;

import java.util.Map;

public final class Materials {
    private Materials() {}

    static final Map<Material, Material> MAIN_PRODUCE = Map.of(
            Material.WHEAT, Material.WHEAT,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.BEETROOTS, Material.BEETROOT
    );

    public static boolean isCrop(Material m) {
        return MAIN_PRODUCE.containsKey(m);
    }

    public static Material mainProduceOf(Material crop) {
        return MAIN_PRODUCE.get(crop);
    }

    public static boolean isHoe(Material mat) {
        return mat != null && mat.name().endsWith("_HOE");
    }

    public static boolean isPickaxe(Material mat) {
        return mat != null && mat.name().endsWith("_PICKAXE");
    }

    public static int pickaxeLevel(Material mat) {
        return switch (mat) {
            case WOODEN_PICKAXE, GOLDEN_PICKAXE -> 0;
            case STONE_PICKAXE -> 1;
            case IRON_PICKAXE -> 2;
            case DIAMOND_PICKAXE -> 3;
            case NETHERITE_PICKAXE -> 4;
            default -> -1;
        };
    }

    public static int requiredLevel(Material ore) {
        return switch (ore) {
            case COPPER_ORE, DEEPSLATE_COPPER_ORE, IRON_ORE, DEEPSLATE_IRON_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> 1;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                    DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> 2;
            default -> 0; // coal, nether quartz etc.
        };
    }
}
