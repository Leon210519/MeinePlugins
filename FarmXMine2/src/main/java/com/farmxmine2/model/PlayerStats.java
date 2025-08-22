package com.farmxmine2.model;

import java.util.UUID;

public class PlayerStats {
    private final UUID uuid;
    private int mineLevel;
    private int mineXp;
    private int farmLevel;
    private int farmXp;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public int getMineLevel() { return mineLevel; }
    public int getMineXp() { return mineXp; }
    public int getFarmLevel() { return farmLevel; }
    public int getFarmXp() { return farmXp; }

    public void addXp(TrackType type, int amount) {
        if (type == TrackType.MINE) {
            mineXp += amount;
        } else {
            farmXp += amount;
        }
    }

    public int getLevel(TrackType type) {
        return type == TrackType.MINE ? mineLevel : farmLevel;
    }

    public int getXp(TrackType type) {
        return type == TrackType.MINE ? mineXp : farmXp;
    }

    public void setLevel(TrackType type, int level) {
        if (type == TrackType.MINE) mineLevel = level; else farmLevel = level;
    }

    public void setXp(TrackType type, int xp) {
        if (type == TrackType.MINE) mineXp = xp; else farmXp = xp;
    }
}
