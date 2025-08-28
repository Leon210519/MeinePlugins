package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Lightweight runtime debug/inspection facility. Debug messages are
 * throttled per category and emitted asynchronously to avoid blocking the
 * main server thread.
 */
public class DebugService {
    private final LootPetsPlugin plugin;
    private volatile boolean enabled;
    private final Map<String, Boolean> categories = new HashMap<>();
    private long throttleMillis;
    private final Map<String, Long> lastEmitted = new HashMap<>();
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(256);
    private long dropped = 0;
    private long lastDropNotice = 0;
    private BukkitTask worker;

    public DebugService(LootPetsPlugin plugin) {
        this.plugin = plugin;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("debug");
        enabled = sec != null && sec.getBoolean("enabled", false);
        throttleMillis = sec != null ? sec.getLong("throttle_millis", 750L) : 750L;
        ConfigurationSection catSec = sec != null ? sec.getConfigurationSection("categories") : null;
        List<String> defaults = Arrays.asList("gui","boosts","papi","storage","eggs","reload","validator");
        for (String c : defaults) {
            boolean val = catSec != null && catSec.getBoolean(c, false);
            categories.put(c.toLowerCase(Locale.ROOT), val);
        }
        startWorker();
    }

    private void startWorker() {
        worker = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            String msg;
            while ((msg = queue.poll()) != null) {
                plugin.getLogger().info(msg);
            }
            long now = System.currentTimeMillis();
            if (dropped > 0 && now - lastDropNotice > 60_000L) {
                plugin.getLogger().info("[debug] dropped " + dropped + " debug lines");
                dropped = 0;
                lastDropNotice = now;
            }
        }, 1L, 1L);
    }

    public void shutdown() {
        if (worker != null) {
            worker.cancel();
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean flag) { this.enabled = flag; }

    public boolean isCategoryEnabled(String cat) {
        return categories.getOrDefault(cat.toLowerCase(Locale.ROOT), false);
    }

    public void setCategory(String cat, boolean flag) {
        categories.put(cat.toLowerCase(Locale.ROOT), flag);
    }

    public Set<String> getKnownCategories() { return categories.keySet(); }

    public long getThrottleMillis() { return throttleMillis; }
    public void setThrottleMillis(long millis) { this.throttleMillis = Math.max(0, millis); }

    public void debug(String category, String message) {
        if (!enabled) return;
        String cat = category.toLowerCase(Locale.ROOT);
        if (!isCategoryEnabled(cat)) return;
        long now = System.currentTimeMillis();
        long last = lastEmitted.getOrDefault(cat + message, 0L);
        if (now - last < throttleMillis) return;
        lastEmitted.put(cat + message, now);
        String line = "[" + cat + "] " + message;
        if (!queue.offer(line)) {
            dropped++;
        }
    }
}
