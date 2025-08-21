package com.farmxmine.service;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossbarService implements Listener {
    // service to manage player bossbars
    private final JavaPlugin plugin;
    private final Map<UUID, BossInfo> bars = new HashMap<>();
    private final BossBar.Color color;
    private final BossBar.Overlay style;
    private final int timeout;
    private final String titleMining;
    private final String titleFarming;

    private static class BossInfo {
        BossBar bar;
        int task;
    }

    public boolean isActive(Player player) {
        BossInfo info = bars.get(player.getUniqueId());
        return info != null && info.bar != null;
    }

    public BossbarService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.color = BossBar.Color.valueOf(plugin.getConfig().getString("bossbar.color", "BLUE"));
        this.style = BossBar.Overlay.valueOf(plugin.getConfig().getString("bossbar.style", "SOLID"));
        this.timeout = plugin.getConfig().getInt("bossbar.timeout_active_seconds", 30);
        this.titleMining = plugin.getConfig().getString("bossbar.title_mining", "Mining");
        this.titleFarming = plugin.getConfig().getString("bossbar.title_farming", "Farming");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void update(Player player, boolean mining, double xp, double next, int level) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) return;
        UUID id = player.getUniqueId();
        BossInfo info = bars.computeIfAbsent(id, k -> new BossInfo());
        if (info.bar == null) {
            info.bar = BossBar.bossBar(net.kyori.adventure.text.Component.text(""), 0f, color, style);
            player.showBossBar(info.bar);
        }
        String title = mining ? titleMining : titleFarming;
        info.bar.name(net.kyori.adventure.text.Component.text(title + " L" + level + " " + (int)xp + "/" + (int)next));
        info.bar.progress((float) (xp / next));
        if (info.task != 0) Bukkit.getScheduler().cancelTask(info.task);
        info.task = Bukkit.getScheduler().runTaskLater(plugin, () -> hide(player), timeout * 20L).getTaskId();
    }

    public void hide(Player player) {
        BossInfo info = bars.get(player.getUniqueId());
        if (info != null && info.bar != null) {
            player.hideBossBar(info.bar);
            info.bar = null;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BossInfo info = bars.remove(event.getPlayer().getUniqueId());
        if (info != null && info.bar != null) {
            event.getPlayer().hideBossBar(info.bar);
        }
    }
}
