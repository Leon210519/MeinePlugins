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

    public void addXp(Player p, Kind kind) { addXp(p, kind, -1); }

    public void addXp(Player p, Kind kind, int amount) {
        int add = amount > 0 ? amount : (kind == Kind.MINE ? xpMine : xpFarm);
        BossBar bar = bars.computeIfAbsent(p.getUniqueId(), k -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));
        bar.setColor(kind == Kind.MINE ? BarColor.BLUE : BarColor.GREEN);
        bar.setTitle((kind == Kind.MINE ? "Mining XP" : "Farming XP") + " +" + add);
        double progress = bar.getProgress();
        progress += Math.min(1.0, add / 20.0);
        if (progress > 1.0) progress = 0.0;
        bar.setProgress(progress);
        bar.addPlayer(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> bar.removePlayer(p), 40L);
    }
}
