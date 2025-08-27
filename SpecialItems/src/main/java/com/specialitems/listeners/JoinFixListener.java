package com.specialitems.listeners;

import com.specialitems.util.ItemUtil;
import com.specialitems.util.Log;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/** Normalizes player inventories on join to guarantee integer CMD values. */
public final class JoinFixListener implements Listener {

    private static long lastWarn = 0L;

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        var inv = p.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            try {
                if (ItemUtil.normalizeCustomModelData(item)) {
                    inv.setItem(i, item);
                }
            } catch (Throwable t) {
                rateLimitWarn("inv", t);
            }
        }

        var ec = p.getEnderChest();
        for (int i = 0; i < ec.getSize(); i++) {
            ItemStack item = ec.getItem(i);
            try {
                if (ItemUtil.normalizeCustomModelData(item)) {
                    ec.setItem(i, item);
                }
            } catch (Throwable t) {
                rateLimitWarn("ec", t);
            }
        }
    }

    private static void rateLimitWarn(String where, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastWarn > 10000L) {
            lastWarn = now;
            Log.warn("CMD normalize failed in " + where + ": " + t.getMessage());
        }
    }
}

