package com.specialitems.leveling;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/** Provides simple axis-aligned region checks. */
public class RegionService {
    private final Region mine;
    private final Region farm;

    public RegionService(ConfigurationSection sec) {
        this.mine = loadRegion(sec == null ? null : sec.getConfigurationSection("mine"));
        this.farm = loadRegion(sec == null ? null : sec.getConfigurationSection("farm"));
    }

    private Region loadRegion(ConfigurationSection s) {
        if (s == null) return null;
        String world = s.getString("world", "world");
        var minList = s.getDoubleList("min");
        var maxList = s.getDoubleList("max");
        if (minList.size() != 3 || maxList.size() != 3) return null;
        return new Region(world,
                minList.get(0), minList.get(1), minList.get(2),
                maxList.get(0), maxList.get(1), maxList.get(2));
    }

    public boolean inMine(Location loc) { return mine != null && mine.contains(loc); }
    public boolean inFarm(Location loc) { return farm != null && farm.contains(loc); }

    private static class Region {
        final String world;
        final double minX, minY, minZ;
        final double maxX, maxY, maxZ;
        Region(String world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.world = world;
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }
        boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().getName().equals(world)) return false;
            double x = loc.getX(), y = loc.getY(), z = loc.getZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }
}
