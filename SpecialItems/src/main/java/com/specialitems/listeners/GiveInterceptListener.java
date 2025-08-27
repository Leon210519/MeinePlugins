package com.specialitems.listeners;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.util.ItemUtil;
import com.specialitems.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Objects;

/**
 * Normalizes newly given items so that CustomModelData is stored as an integer.
 */
public final class GiveInterceptListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg == null) return;
        msg = msg.trim();
        if (!msg.toLowerCase(Locale.ROOT).startsWith("/give ")) return;

        String[] parts = msg.split("\\s+");
        if (parts.length < 3) return; // /give <player> <item>

        Player target = Bukkit.getPlayerExact(parts[1]);
        if (target == null) return; // not on this server

        ItemStack[] before = target.getInventory().getContents().clone();

        Bukkit.getScheduler().runTaskLater(SpecialItemsPlugin.getInstance(), () -> {
            try {
                var inv = target.getInventory();
                ItemStack[] after = inv.getContents();
                for (int i = 0; i < after.length; i++) {
                    ItemStack item = after[i];
                    if (!Objects.equals(before[i], item)) {
                        try {
                            if (ItemUtil.normalizeCustomModelData(item)) {
                                inv.setItem(i, item);
                            }
                        } catch (Throwable t) {
                            Log.debug("CMD normalize failed after /give", t);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.debug("Failed to process /give normalization", t);
            }
        }, 1L);
    }
}

