package com.instancednodes.placeholder;

import com.instancednodes.InstancedNodesPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for FarmXMine (InstancedNodes).
 */
public class FarmXMinePlaceholders extends PlaceholderExpansion {

    private final InstancedNodesPlugin plugin;

    public FarmXMinePlaceholders(InstancedNodesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "farmxmine";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        UUID uid = player.getUniqueId();
        if (params.equalsIgnoreCase("level")) {
            return String.valueOf(plugin.level().getLevel(uid));
        }
        if (params.equalsIgnoreCase("prestige")) {
            return String.valueOf(plugin.level().getPrestige(uid));
        }
        if (params.equalsIgnoreCase("xp")) {
            return String.valueOf((int) plugin.data().getXp(uid));
        }
        return null;
    }
}
