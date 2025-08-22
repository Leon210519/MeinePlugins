package com.farmxmine2.model;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Set;

public class Region {
    private final String world;
    private final Vec3i min;
    private final Vec3i max;
    private final Set<Material> allowed;
    private final String requireTool; // suffix like PICKAXE or HOE
    private final Material defaultType;

    public Region(String world, Vec3i min, Vec3i max, Set<Material> allowed, String requireTool, Material defaultType) {
        this.world = world;
        this.min = min;
        this.max = max;
        this.allowed = allowed;
        this.requireTool = requireTool;
        this.defaultType = defaultType;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equalsIgnoreCase(world)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= min.x() && x <= max.x() &&
               y >= min.y() && y <= max.y() &&
               z >= min.z() && z <= max.z();
    }

    public boolean isAllowed(Material mat) {
        return allowed.contains(mat);
    }

    public String getWorld() { return world; }
    public Vec3i getMin() { return min; }
    public Vec3i getMax() { return max; }
    public Set<Material> getAllowed() { return allowed; }
    public String getRequireTool() { return requireTool; }
    public Material getDefaultType() { return defaultType; }
}
