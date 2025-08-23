package com.farmxmine2.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

/** Simple key representing a block position in a world. */
public record BlockKey(String world, int x, int y, int z) {
    public static BlockKey of(Block block) {
        Location loc = block.getLocation();
        return new BlockKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
}
