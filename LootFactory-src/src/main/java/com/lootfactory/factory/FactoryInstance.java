package com.lootfactory.factory;

import org.bukkit.Location;
import java.util.UUID;

/**
 * Represents a single placed Factory of a player.
 * IMPORTANT: Prestige is stored PER INSTANCE here (not globally per player/type).
 * New factories (e.g., from the shop) should start with prestige = 0.
 */
public class FactoryInstance {

    public final UUID uuid;
    public final UUID owner;
    public final String typeId;

    public int level;
    public double xpSeconds;
    public transient double productionAccum;
    public Location location;

    /** Per-instance prestige (NOT per player/type). Defaults to 0 for new instances. */
    public int prestige;

    /** Full constructor including per-instance prestige. */
    public FactoryInstance(UUID uuid,
                           UUID owner,
                           String typeId,
                           int level,
                           double xpSeconds,
                           Location location,
                           int prestige) {
        this.uuid = uuid;
        this.owner = owner;
        this.typeId = typeId;
        this.level = level;
        this.xpSeconds = xpSeconds;
        this.location = location;
        this.productionAccum = 0d;
        this.prestige = Math.max(0, prestige);
    }

    /** Backward-compatible constructor: defaults prestige to 0. */
    public FactoryInstance(UUID uuid,
                           UUID owner,
                           String typeId,
                           int level,
                           double xpSeconds,
                           Location location) {
        this(uuid, owner, typeId, level, xpSeconds, location, 0);
    }

    /** Convenience: increase prestige (never below 0). */
    public void addPrestige(int amount) {
        this.prestige = Math.max(0, this.prestige + amount);
    }

    /** Reset typical stats after a prestige (keep location, uuid, owner, typeId). */
    public void resetAfterPrestige() {
        this.level = 1;
        this.xpSeconds = 0d;
        this.productionAccum = 0d;
    }

    @Override
    public String toString() {
        return "FactoryInstance{" +
                "uuid=" + uuid +
                ", owner=" + owner +
                ", typeId='" + typeId + '\'' +
                ", level=" + level +
                ", xpSeconds=" + xpSeconds +
                ", prestige=" + prestige +
                ", location=" + (location != null ? location.toString() : "null") +
                '}';
    }
}
