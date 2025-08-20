package com.farmxmine.service;

import com.farmxmine.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ArtifactService {
    public enum Category { MINING, FARMING }

    private final JavaPlugin plugin;
    private final StorageService storage;
    private final Random random = new Random();
    private final int totalArtifacts;
    private final double dropChance;
    private final double baseMultiplier;
    private final boolean multiplicative;

    public ArtifactService(JavaPlugin plugin, StorageService storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.totalArtifacts = plugin.getConfig().getInt("artifacts.total_unique", 100);
        this.dropChance = plugin.getConfig().getDouble("artifacts.drop_chance_on_levelup_pct", 4.5) / 100.0;
        this.baseMultiplier = plugin.getConfig().getDouble("artifacts.base_multiplier_common_pct", 1.0) / 100.0;
        this.multiplicative = plugin.getConfig().getString("artifacts.apply_mode", "multiplicative").equalsIgnoreCase("multiplicative");
    }

    public void tryGrant(Player player, Category category) {
        if (!plugin.getConfig().getBoolean("artifacts.enabled", true)) return;
        if (random.nextDouble() > dropChance) return;
        PlayerData data = storage.get(player);
        List<Integer> available = new ArrayList<>();
        int start = category == Category.MINING ? 0 : totalArtifacts / 2;
        int end = category == Category.MINING ? totalArtifacts / 2 : totalArtifacts;
        for (int i = start; i < end; i++) {
            if (!data.getArtifacts().contains(i)) available.add(i);
        }
        if (available.isEmpty()) return;
        int chosen = available.get(random.nextInt(available.size()));
        data.getArtifacts().add(chosen);
        player.sendMessage("§aYou found an artifact! (#" + chosen + ")");
    }

    public double getMultiplier(Player player) {
        PlayerData data = storage.get(player);
        int count = data.getArtifacts().size();
        if (multiplicative) {
            double mul = 1.0;
            for (int i = 0; i < count; i++) {
                mul *= (1.0 + baseMultiplier);
            }
            return mul;
        } else {
            return 1.0 + baseMultiplier * count;
        }
    }

    public Set<Integer> getArtifacts(Player player) {
        return storage.get(player).getArtifacts();
    }
}
