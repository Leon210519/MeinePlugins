package com.lootfactory.integrations;

import com.lootfactory.LootFactoryPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Grants official crate keys by dispatching the crate plugin's console commands.
 * Uses the configurable custom command first, then tries common fallbacks.
 */
public final class LootCratesKeys {

    private LootCratesKeys() {}

    public static boolean giveKey(Player p, String crateId, int amount) {
        if (p == null || crateId == null || amount <= 0) return false;

        // 1) Try custom command from config (highest priority)
        try {
            String cmdTpl = LootFactoryPlugin.get().getConfig()
                    .getString("prestige.rewards.key.customCommand", "")
                    .trim();
            if (!cmdTpl.isEmpty()) {
                String cmd = cmdTpl
                        .replace("{player}", p.getName())
                        .replace("{crate}", crateId)
                        .replace("{amount}", String.valueOf(amount));
                if (dispatch(cmd)) return true;
            }
        } catch (Throwable ignored) {}

        // 2) Try common aliases (LootCrates, CrazyCrates, ExcellentCrates)
        String player = p.getName();
        String[] candidates = new String[]{
                "lootcrates key give " + player + " " + crateId + " " + amount,   // LootCrates (full alias)
                "lc key give " + player + " " + crateId + " " + amount,           // LootCrates (short alias)
                "cc give physical " + player + " " + crateId + " " + amount,      // CrazyCrates (physical item key)
                "excellentcrates key give " + player + " " + crateId + " " + amount // ExcellentCrates
        };
        for (String c : candidates) {
            if (dispatch(c)) return true;
        }
        return false;
    }

    private static boolean dispatch(String command) {
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
