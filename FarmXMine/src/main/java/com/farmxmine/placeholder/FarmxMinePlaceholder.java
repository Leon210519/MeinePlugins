package com.farmxmine.placeholder;

import com.farmxmine.data.PlayerData;
import com.farmxmine.service.ArtifactService;
import com.farmxmine.service.BossbarService;
import com.farmxmine.service.StorageService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class FarmxMinePlaceholder extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final StorageService storage;
    private final ArtifactService artifacts;
    private final BossbarService bossbars;

    public FarmxMinePlaceholder(JavaPlugin plugin, StorageService storage, ArtifactService artifacts, BossbarService bossbars) {
        this.plugin = plugin;
        this.storage = storage;
        this.artifacts = artifacts;
        this.bossbars = bossbars;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "fxm";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        PlayerData data = storage.get(player);
        return switch (params) {
            case "mining_level" -> String.valueOf(data.getMiningLevel());
            case "farming_level" -> String.valueOf(data.getFarmingLevel());
            case "eco_multiplier" -> {
                double mul = Math.max(
                        artifacts.getMultiplier(player, ArtifactService.Category.MINING),
                        artifacts.getMultiplier(player, ArtifactService.Category.FARMING));
                yield String.format("%.2f", mul);
            }
            case "bossbar_active" -> bossbars.isActive(player) ? "true" : "false";
            case "artifacts_owned" -> String.valueOf(data.getArtifacts().size());
            default -> "";
        };
    }
}
