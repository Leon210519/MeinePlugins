package com.focusnpc.placeholder;

import com.focusnpc.FocusNPCPlugin;
import com.focusnpc.util.TextUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;

public class FocusPlaceholders extends PlaceholderExpansion {
    private final FocusNPCPlugin plugin;

    public FocusPlaceholders(FocusNPCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "focusnpc";
    }

    @Override
    public String getAuthor() {
        return "FocusNPC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        if (params.equalsIgnoreCase("farm_target")) {
            Material mat = plugin.getPlayerData().getFarmFocus(player.getUniqueId());
            return TextUtil.format(mat);
        }
        if (params.equalsIgnoreCase("mine_target")) {
            Material mat = plugin.getPlayerData().getMineFocus(player.getUniqueId());
            return TextUtil.format(mat);
        }
        return null;
    }
}
