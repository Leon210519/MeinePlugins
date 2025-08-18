package com.lootfactory.prestige;

import java.util.UUID;
import org.bukkit.Location;

/**
 * Adapter interface to your Factory API.
 * Now supports fetching a factory by exact block location (per-instance).
 */
public interface FactoryGateway {

    /**
     * Get the player's factory by a type identifier (e.g., "IRON").
     * May return null if not found.
     */
    FactoryHandle getFactory(UUID owner, String factoryType);

    /**
     * Get the player's factory by its exact location.
     * This ensures we only target the specific factory instance the GUI was opened for.
     */
    FactoryHandle getFactoryByLocation(UUID owner, Location location);

    interface FactoryHandle {
        String getType();                   // e.g., "IRON"
        int getLevel();                     // current factory level
        void resetToLevel(int level, int keepStoragePercent, boolean keepCosmetics);
    }
}
