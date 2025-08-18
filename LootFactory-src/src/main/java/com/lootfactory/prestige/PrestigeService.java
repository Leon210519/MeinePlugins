package com.lootfactory.prestige;

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.integrations.LootCratesKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Central, single point to grant Prestige Keys.
 * - Prefer official crate-plugin keys via configured command / known aliases
 * - If that fails, fall back to our own PDC-tagged item (appearance can match the crate key)
 */
public final class PrestigeService {

    private PrestigeService() {}

    /** Give keys for a successful per-factory prestige. Exactly once. */
    public static void givePrestigeKeys(Player p) {
        try {
            LootFactoryPlugin plugin = LootFactoryPlugin.get();
            KeyItemService kis = plugin.prestigeKeys();

            int amount = 1;
            String crateId = "PRESTIGE";
            boolean debug = false;

            if (kis != null) {
                try { amount = Math.max(1, kis.getKeysPerPrestige()); } catch (Throwable ignored) {}
                try { crateId = kis.getConfiguredCrateId(); } catch (Throwable ignored) {}
            }
            try { debug = plugin.getConfig().getBoolean("prestige.rewards.key.debug", false); } catch (Throwable ignored) {}

            // Try to give the official crate-plugin key
            boolean ok = LootCratesKeys.giveKey(p, crateId, amount);
            if (ok) {
                if (debug) {
                    p.sendMessage(Component.text("[Prestige] Granted crate-plugin key: ", NamedTextColor.GRAY)
                            .append(Component.text(crateId, NamedTextColor.LIGHT_PURPLE))
                            .append(Component.text(" x" + amount, NamedTextColor.GRAY)));
                }
                return;
            }

            // Fallback: build our own item (appearance configured in KeyItemService / config.yml)
            ItemStack item = (kis != null) ? kis.createPrestigeKey(amount) : new org.bukkit.inventory.ItemStack(org.bukkit.Material.TRIPWIRE_HOOK, amount);
            p.getInventory().addItem(item);

            if (debug) {
                p.sendMessage(Component.text("[Prestige] Fallback item key granted (no crate key).", NamedTextColor.YELLOW));
            }
        } catch (Throwable t) {
            // Never break the prestige flow because of key delivery
            try {
                p.sendMessage(Component.text("[Prestige] Could not grant key. Contact an admin.", NamedTextColor.RED));
            } catch (Throwable ignored) {}
        }
    }
}
