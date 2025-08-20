package com.farmxmine.data;

import java.util.HashSet;
import java.util.Set;

public class PlayerData {
    private int miningLevel = 1;
    private double miningXp = 0.0;
    private int farmingLevel = 1;
    private double farmingXp = 0.0;
    private final Set<Integer> artifacts = new HashSet<>();

    public int getMiningLevel() { return miningLevel; }
    public void setMiningLevel(int level) { this.miningLevel = level; }
    public double getMiningXp() { return miningXp; }
    public void setMiningXp(double xp) { this.miningXp = xp; }
    public int getFarmingLevel() { return farmingLevel; }
    public void setFarmingLevel(int level) { this.farmingLevel = level; }
    public double getFarmingXp() { return farmingXp; }
    public void setFarmingXp(double xp) { this.farmingXp = xp; }
    public Set<Integer> getArtifacts() { return artifacts; }
}
