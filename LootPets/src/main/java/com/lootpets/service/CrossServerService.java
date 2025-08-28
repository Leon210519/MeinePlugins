package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.storage.SqlStorageAdapter;
import com.lootpets.storage.StorageAdapter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simplified cross-server coordination service. Provides lightweight polling
 * and optional heartbeat support when using SQL storage.
 */
public class CrossServerService {
    private final LootPetsPlugin plugin;
    private final PetService petService;
    private final StorageAdapter storage;
    private final SqlStorageAdapter sql;
    private final boolean enabled;
    private final int pollInterval;
    private final int pollJitter;
    private final int maxBatch;
    private final long skewTolerance;
    private final boolean hbEnabled;
    private final int hbInterval;
    private final String serverId;
    private final long bootTime;
    private int pollTask = -1;
    private int hbTask = -1;
    private final AtomicLong invalidations = new AtomicLong();
    private final Random random = new Random();

    public CrossServerService(LootPetsPlugin plugin, PetService petService, StorageAdapter storage) {
        this.plugin = plugin;
        this.petService = petService;
        this.storage = storage;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("cross_server");
        SqlStorageAdapter sqlTmp = storage instanceof SqlStorageAdapter s ? s : null;
        boolean active = false;
        int pi = 5, pj = 400, mb = 200; long skew = 2000; boolean hb = false; int hbi = 15; String sid = "local";
        if (sec != null) {
            pi = sec.getInt("poll_interval_seconds", 5);
            pj = sec.getInt("poll_jitter_millis", 400);
            mb = sec.getInt("max_batch_players", 200);
            skew = sec.getLong("clock_skew_tolerance_millis", 2000);
            ConfigurationSection hbSec = sec.getConfigurationSection("heartbeat");
            hb = hbSec != null && hbSec.getBoolean("enabled", true);
            hbi = hbSec != null ? hbSec.getInt("interval_seconds", 15) : 15;
            sid = hbSec != null ? hbSec.getString("server_id", "AUTO") : "AUTO";
            if ("AUTO".equalsIgnoreCase(sid)) {
                sid = plugin.getServer().getName() + "-" + UUID.randomUUID();
            }
            active = sec.getBoolean("enabled", false) && sqlTmp != null && sqlTmp.getProvider() != SqlStorageAdapter.Provider.SQLITE;
        }
        this.sql = sqlTmp;
        this.enabled = active;
        this.pollInterval = pi;
        this.pollJitter = pj;
        this.maxBatch = mb;
        this.skewTolerance = skew;
        this.hbEnabled = hb;
        this.hbInterval = hbi;
        this.serverId = sid;
        this.bootTime = System.currentTimeMillis();
        if (!enabled) {
            plugin.getLogger().info("Cross-server mode inactive on YAML storage");
            return;
        }
        startPoller();
        if (hbEnabled) startHeartbeat();
        plugin.getLogger().info("Cross-server mode enabled, server_id=" + serverId);
    }

    private void startPoller() {
        long ticks = pollInterval * 20L;
        pollTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runPoll, ticks, ticks).getTaskId();
    }

    private void startHeartbeat() {
        long ticks = hbInterval * 20L;
        hbTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                storage.heartbeat(serverId, plugin.getServer().getName(), bootTime);
            } catch (Exception ignored) {}
        }, ticks, ticks).getTaskId();
    }

    private void runPoll() {
        try {
            Thread.sleep(random.nextInt(pollJitter + 1));
        } catch (InterruptedException ignored) {}
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;
        List<UUID> batch = new ArrayList<>();
        for (int i = 0; i < online.size() && batch.size() < maxBatch; i++) {
            batch.add(online.get(i).getUniqueId());
        }
        Map<UUID, long[]> remote;
        try {
            remote = storage.fetchVersions(batch);
        } catch (Exception e) {
            plugin.getLogger().warning("xserver poll failed: " + e.getMessage());
            return;
        }
        for (UUID u : batch) {
            long[] local = petService.getVersion(u);
            long[] db = remote.get(u);
            if (db != null && (db[0] > local[0] || db[1] - local[1] > skewTolerance)) {
                invalidations.incrementAndGet();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> petService.reload(u));
            }
        }
    }

    public String info() {
        if (!enabled) return "disabled";
        return "server_id=" + serverId + ", poll=" + pollInterval + "s, invalidations=" + invalidations.get();
    }

    public void resync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> petService.reload(uuid));
    }

    public void touch(UUID uuid) {
        if (!enabled) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                storage.touch(uuid);
            } catch (Exception ignored) {}
        });
    }

    public long getInvalidations() {
        return invalidations.get();
    }

    public void shutdown() {
        if (pollTask != -1) Bukkit.getScheduler().cancelTask(pollTask);
        if (hbTask != -1) Bukkit.getScheduler().cancelTask(hbTask);
    }
}
