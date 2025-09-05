package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.util.Materials;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigService {
    private final FarmXMine2Plugin plugin;
    private int respawnSeconds;
    /** Allowed world names in lowercase */
    private Set<String> allowedWorlds;
    private boolean overrideCancelled;
    private boolean miningEnabled;
    private String miningRequireTool;
    private Set<Material> miningOres;
    private boolean farmingEnabled;
    private String farmingRequireTool;
    private Set<Material> farmingCrops;
    private int mineXpPer;
    private int farmXpPer;
    private int baseXpPerLevel;
    private int xpGrowthPerLevel;
    private double artifactDropChance;
    private double artifactEcoBoost;

    public ConfigService(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        FileConfiguration cfg = plugin.getConfig();
        respawnSeconds = cfg.getInt("general.respawn_seconds");
        allowedWorlds = cfg.getStringList("general.allowed_worlds").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (allowedWorlds.isEmpty()) {
            String single = cfg.getString("general.main_world", "world");
            allowedWorlds = new java.util.HashSet<>();
            allowedWorlds.add(single.toLowerCase());
        }
        overrideCancelled = cfg.getBoolean("general.override_cancelled");
        miningEnabled = cfg.getBoolean("mining.enabled");
        miningRequireTool = cfg.getString("mining.require_tool", "PICKAXE");
        miningOres = cfg.getStringList("mining.ores").stream()
                .map(s -> {
                    try { return Material.valueOf(s); } catch (IllegalArgumentException ex) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class)));
        if (miningOres.isEmpty()) {
            miningOres = Materials.allOres();
        }
        farmingEnabled = cfg.getBoolean("farming.enabled");
        farmingRequireTool = cfg.getString("farming.require_tool", "HOE");
        farmingCrops = cfg.getStringList("farming.crops").stream()
                .map(s -> {
                    try { return Material.valueOf(s); } catch (IllegalArgumentException ex) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class)));
        if (farmingCrops.isEmpty()) {
            farmingCrops = Materials.allCrops();
        }
        mineXpPer = cfg.getInt("leveling.xp_per_harvest.mine");
        farmXpPer = cfg.getInt("leveling.xp_per_harvest.farm");
        baseXpPerLevel = cfg.getInt("leveling.base_xp_per_level");
        xpGrowthPerLevel = cfg.getInt("leveling.xp_growth_per_level");
        artifactDropChance = cfg.getDouble("artifact.drop_chance");
        artifactEcoBoost = cfg.getDouble("artifact.eco_boost");
    }

    public int getRespawnSeconds() { return respawnSeconds; }
    public Set<String> getAllowedWorlds() { return allowedWorlds; }
    public boolean isWorldAllowed(String worldName) {
        return allowedWorlds.contains(worldName.toLowerCase());
    }
    public boolean isOverrideCancelled() { return overrideCancelled; }
    public boolean isMiningEnabled() { return miningEnabled; }
    public String getMiningRequireTool() { return miningRequireTool; }
    public Set<Material> getMiningOres() { return miningOres; }
    public boolean isFarmingEnabled() { return farmingEnabled; }
    public String getFarmingRequireTool() { return farmingRequireTool; }
    public Set<Material> getFarmingCrops() { return farmingCrops; }
    public int getMineXpPer() { return mineXpPer; }
    public int getFarmXpPer() { return farmXpPer; }
    public int getBaseXpPerLevel() { return baseXpPerLevel; }
    public int getXpGrowthPerLevel() { return xpGrowthPerLevel; }
    public double getArtifactDropChance() { return artifactDropChance; }
    public double getArtifactEcoBoost() { return artifactEcoBoost; }
}
