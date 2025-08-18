package com.lootfactory.placeholder;

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.factory.FactoryManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LootFactoryPlaceholders extends PlaceholderExpansion {

    private final LootFactoryPlugin plugin;

    public LootFactoryPlaceholders(LootFactoryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "lootfactory"; }
    @Override public @NotNull String getAuthor() { return String.join(", ", plugin.getDescription().getAuthors()); }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) return "";
        FactoryManager m = plugin.factories();
        int current = m.countPlaced(player.getUniqueId());
        int max = m.calcMaxSlots(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "slots_current": return String.valueOf(current);
            case "slots_max":     return String.valueOf(max);
            case "slots":         return current + "/" + max;
            default:              return null; // unbekannt -> PAPI probiert andere Expansions
        }
    }
}
