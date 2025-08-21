package com.farmxmine.service;

import com.farmxmine.data.PlayerData;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LevelService {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final ArtifactService artifacts;
    private final BossbarService bossbars;
    private final double xpMine;
    private final double xpFarm;
    private final double xpCap;
    private final double base;
    private final double exponent;

    public LevelService(JavaPlugin plugin, StorageService storage, ArtifactService artifacts, BossbarService bossbars) {
        this.plugin = plugin;
        this.storage = storage;
        this.artifacts = artifacts;
        this.bossbars = bossbars;
        this.xpMine = plugin.getConfig().getDouble("leveling.xp_per_action.MINE_BLOCK_BREAK", 3.0);
        this.xpFarm = plugin.getConfig().getDouble("leveling.xp_per_action.FARM_CROP_BREAK", 1.0);
        this.xpCap = plugin.getConfig().getDouble("leveling.per_action_xp_cap", 1_000_000);
        this.base = plugin.getConfig().getDouble("leveling.level_curve.base", 50.0);
        this.exponent = plugin.getConfig().getDouble("leveling.level_curve.exponent", 1.35);
    }

    private double nextReq(int level) {
        return base * Math.pow(level, exponent);
    }

    public void addMineXp(Player player, int count) {
        PlayerData data = storage.get(player);
        double gained = Math.min(xpMine * count, xpCap);
        data.setMiningXp(data.getMiningXp() + gained);
        boolean levelled = false;
        while (data.getMiningXp() >= nextReq(data.getMiningLevel())) {
            data.setMiningXp(data.getMiningXp() - nextReq(data.getMiningLevel()));
            data.setMiningLevel(data.getMiningLevel() + 1);
            levelled = true;
        }
        bossbars.update(player, true, data.getMiningXp(), nextReq(data.getMiningLevel()), data.getMiningLevel());
        if (levelled) {
            player.sendMessage("Mining level up! Now level " + data.getMiningLevel());
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            artifacts.tryGrant(player, ArtifactService.Category.MINING);
        }
    }

    public void addFarmXp(Player player, int count) {
        PlayerData data = storage.get(player);
        double gained = Math.min(xpFarm * count, xpCap);
        data.setFarmingXp(data.getFarmingXp() + gained);
        boolean levelled = false;
        while (data.getFarmingXp() >= nextReq(data.getFarmingLevel())) {
            data.setFarmingXp(data.getFarmingXp() - nextReq(data.getFarmingLevel()));
            data.setFarmingLevel(data.getFarmingLevel() + 1);
            levelled = true;
        }
        bossbars.update(player, false, data.getFarmingXp(), nextReq(data.getFarmingLevel()), data.getFarmingLevel());
        if (levelled) {
            player.sendMessage("Farming level up! Now level " + data.getFarmingLevel());
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            artifacts.tryGrant(player, ArtifactService.Category.FARMING);
        }
    }
}
