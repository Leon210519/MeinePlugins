package com.specialitems.leveling;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

public final class HarvestUtil {
    private HarvestUtil(){}

    public static boolean isMatureCrop(Block b) {
        if (b == null) return false;
        if (!(b.getBlockData() instanceof Ageable age)) return false;
        return age.getAge() >= age.getMaximumAge();
    }

    public static boolean isCrop(Material m) {
        return switch (m) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART,
                 TORCHFLOWER_CROP, SWEET_BERRY_BUSH, COCOA -> true;
            default -> false;
        };
    }
}
