package com.lootpets.model;

import org.jetbrains.annotations.Nullable;

/**
 * Mutable-free data holder for a player's progress on a specific pet.
 */
public class OwnedPetState {
    private final String rarityId;
    private final int level;
    private final int xp;
    private final int stars;
    private final int evolveProgress;
    @Nullable
    private final String suffix;

    public OwnedPetState(String rarityId, int level, int xp, int stars, int evolveProgress, @Nullable String suffix) {
        this.rarityId = rarityId;
        this.level = level;
        this.xp = xp;
        this.stars = stars;
        this.evolveProgress = evolveProgress;
        this.suffix = suffix;
    }

    public String getRarityId() {
        return rarityId;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getStars() {
        return stars;
    }

    public int getEvolveProgress() {
        return evolveProgress;
    }

    @Nullable
    public String getSuffix() {
        return suffix;
    }

    // Backwards compatibility helpers
    public String rarity() {
        return rarityId;
    }

    public int level() {
        return level;
    }

    public int xp() {
        return xp;
    }

    public int stars() {
        return stars;
    }

    public int evolveProgress() {
        return evolveProgress;
    }

    public String suffix() {
        return suffix;
    }

    @Override
    public String toString() {
        return "OwnedPetState{" +
                "rarityId='" + rarityId + '\'' +
                ", level=" + level +
                ", xp=" + xp +
                ", stars=" + stars +
                ", evolveProgress=" + evolveProgress +
                ", suffix='" + suffix + '\'' +
                '}';
    }
}
