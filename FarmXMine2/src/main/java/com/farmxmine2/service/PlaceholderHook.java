package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.PlayerStats;
import com.farmxmine2.model.TrackType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderHook extends PlaceholderExpansion {
    private final FarmXMine2Plugin plugin;

    public PlaceholderHook(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "farmxmine";
    }

    @Override
    public String getAuthor() {
        return "FarmXMine Team";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        PlayerStats ps = plugin.getLevelService().getStats(player.getUniqueId());
        return switch (identifier) {
            case "mine_level" -> String.valueOf(ps.getLevel(TrackType.MINE));
            case "mining_level" -> String.valueOf(ps.getLevel(TrackType.MINE));
            case "mine_xp" -> String.valueOf(ps.getXp(TrackType.MINE));
            case "farm_level" -> String.valueOf(ps.getLevel(TrackType.FARM));
            case "farming_level" -> String.valueOf(ps.getLevel(TrackType.FARM));
            case "farm_xp" -> String.valueOf(ps.getXp(TrackType.FARM));
            default -> null;
        };
    }
}
