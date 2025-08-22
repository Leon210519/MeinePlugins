package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.Region;
import com.farmxmine2.model.TrackType;
import com.farmxmine2.model.Vec3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionService {
    private final FarmXMine2Plugin plugin;
    private final Map<TrackType, Region> regions = new EnumMap<>(TrackType.class);

    public RegionService(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        regions.clear();
        ConfigurationSection mine = plugin.getConfig().getConfigurationSection("regions.mine");
        ConfigurationSection farm = plugin.getConfig().getConfigurationSection("regions.farm");
        regions.put(TrackType.MINE, parseRegion(mine, true));
        regions.put(TrackType.FARM, parseRegion(farm, false));
    }

    private Region parseRegion(ConfigurationSection sec, boolean mine) {
        String world = sec.getString("world");
        Vec3i min = parseVec(sec.getConfigurationSection("min"));
        Vec3i max = parseVec(sec.getConfigurationSection("max"));
        Set<Material> allowed = sec.getStringList(mine ? "ores" : "crops").stream()
                .map(Material::valueOf).collect(Collectors.toSet());
        String require = sec.getString("require_tool");
        Material def = Material.valueOf(sec.getString(mine ? "default_ore" : "default_crop"));
        return new Region(world, min, max, allowed, require, def);
    }

    private Vec3i parseVec(ConfigurationSection section) {
        return new Vec3i(section.getInt("x"), section.getInt("y"), section.getInt("z"));
    }

    public Region getRegion(TrackType type) {
        return regions.get(type);
    }

    /**
     * Determine which configured track a location falls into.
     * Only the main world "world" is considered valid.
     *
     * @param loc location to test
     * @return matching TrackType or null if none
     */
    public TrackType getKind(Location loc) {
        if (loc.getWorld() == null || !"world".equalsIgnoreCase(loc.getWorld().getName())) {
            return null;
        }
        for (Map.Entry<TrackType, Region> entry : regions.entrySet()) {
            if (entry.getValue().contains(loc)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
