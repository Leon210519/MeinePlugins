package com.instancednodes.integration;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Service for resolving region types and material whitelist checks.
 */
public interface RegionService {

    /**
     * Determine the {@link RegionType} for a given location.
     */
    RegionType getRegionType(Location location);

    /**
     * Whether a material is allowed to be harvested in a region.
     */
    boolean isWhitelisted(RegionType type, Material material);
}

