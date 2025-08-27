package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.TrackType;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarService {
    private final FarmXMine2Plugin plugin;
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> hideTasks = new ConcurrentHashMap<>();

    public BossBarService(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
    }

    public void update(Player player, TrackType type) {
        UUID id = player.getUniqueId();
        BossBar bar = bars.computeIfAbsent(id, k -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));
        int level = plugin.getLevelService().getStats(id).getLevel(type);
        int xp = plugin.getLevelService().getStats(id).getXp(type);
        int needed = plugin.getLevelService().xpNeeded(level);
        bar.setTitle((type == TrackType.MINE ? "Mine" : "Farm") + " Level " + level);
        bar.setColor(type == TrackType.MINE ? BarColor.BLUE : BarColor.GREEN);
        bar.setProgress(needed == 0 ? 0 : Math.min(1.0, (double) xp / needed));
        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
        scheduleHide(player);
    }

    private void scheduleHide(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = hideTasks.remove(id);
        if (task != null) task.cancel();
        hideTasks.put(id, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BossBar bar = bars.get(id);
            if (bar != null) bar.removePlayer(player);
        }, 20L * 30));
    }

    public void remove(Player player) {
        UUID id = player.getUniqueId();
        BossBar bar = bars.remove(id);
        if (bar != null) bar.removeAll();
        BukkitTask task = hideTasks.remove(id);
        if (task != null) task.cancel();
    }
}
