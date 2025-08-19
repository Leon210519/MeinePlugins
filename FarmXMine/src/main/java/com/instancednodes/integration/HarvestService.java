package com.instancednodes.integration;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Service encapsulating harvest logic so that respawn/instancing is handled
 * consistently and no world drops are produced.
 */
public interface HarvestService {

    /**
     * Check whether the given block represents a mature crop.
     */
    boolean isMatureCrop(Block block);

    /**
     * Harvest a single block. Implementations must handle respawn/instancing
     * and return the items and xp to be awarded.
     */
    HarvestResult harvestSingle(Player player, Block block, RegionType regionType, double yieldMultiplier);

    /**
     * Locate blocks affected by an AOE harvest.
     */
    List<Block> findAoeTargets(Block origin, RegionType regionType, int maxBlocks, int maxRadius);

    /** Simple holder for harvest results. */
    class HarvestResult {
        public final List<ItemStack> drops;
        public final int xp;

        public HarvestResult(List<ItemStack> drops, int xp) {
            this.drops = drops;
            this.xp = xp;
        }
    }
}

