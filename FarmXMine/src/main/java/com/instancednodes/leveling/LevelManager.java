package com.instancednodes.leveling;

import com.instancednodes.InstancedNodesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple player leveling and prestige handler.
 * XP is gathered from farming and mining and stored in the plugin DataManager.
 */
public class LevelManager {

    public enum Kind { MINE, FARM }

    private final InstancedNodesPlugin plugin;
    private final int xpMine;
    private final int xpFarm;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public LevelManager(InstancedNodesPlugin plugin) {
        this.plugin = plugin;
        this.xpMine = plugin.getConfig().getInt("leveling.xp_per_harvest.mine", 3);
        this.xpFarm = plugin.getConfig().getInt("leveling.xp_per_harvest.farm", 1);
    }

    private int levelCap(int prestige) {
        return 50 + prestige * 10; // gradually increasing requirement
    }

    private int xpNeededFor(int level) {
        return (int) Math.round(100 * Math.pow(1.15, level - 1));
    }

    public int getLevel(UUID uid) { return plugin.data().getLevel(uid); }
    public int getPrestige(UUID uid) { return plugin.data().getPrestige(uid); }
    public double getIncomeMultiplier(UUID uid) { return 1.0 + getPrestige(uid); }

    /**
     * Adds XP of the given kind to a player and handles level ups.
     */
    public void addXp(Player p, Kind kind) { addXp(p, kind, -1); }

    public void addXp(Player p, Kind kind, int amount) {
        int add = amount > 0 ? amount : (kind == Kind.MINE ? xpMine : xpFarm);
        UUID uid = p.getUniqueId();
        double xp = plugin.data().getXp(uid) + add;
        int level = plugin.data().getLevel(uid);
        int needed = xpNeededFor(level);
        while (xp >= needed && level < levelCap(getPrestige(uid))) {
            xp -= needed;
            level++;
            p.sendMessage("§aLevel up! §7Level " + level);
            needed = xpNeededFor(level);
        }
        plugin.data().setXp(uid, xp);
        plugin.data().setLevel(uid, level);

        BossBar bar = bars.computeIfAbsent(uid, k -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));
        bar.setColor(kind == Kind.MINE ? BarColor.BLUE : BarColor.GREEN);
        bar.setTitle((kind == Kind.MINE ? "Mining" : "Farming") + " XP +" + add);
        double progress = Math.min(1.0, xp / needed);
        bar.setProgress(progress);
        bar.addPlayer(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> bar.removePlayer(p), 40L);
    }

    /**
     * Attempts to prestige the player. Returns true on success.
     */
    public boolean prestige(Player p) {
        UUID uid = p.getUniqueId();
        int prestige = getPrestige(uid);
        if (getLevel(uid) < levelCap(prestige)) {
            return false; // not high enough level
        }
        prestige++;
        plugin.data().setPrestige(uid, prestige);
        plugin.data().setLevel(uid, 1);
        plugin.data().setXp(uid, 0);
        p.sendMessage("§6Prestige! §7You are now prestige " + prestige + ". Income multiplier x" + getIncomeMultiplier(uid));
        return true;
    }
}
