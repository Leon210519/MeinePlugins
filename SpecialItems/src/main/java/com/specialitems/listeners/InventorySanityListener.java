package com.specialitems.listeners;

import com.specialitems.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/** Normalizes items during inventory interactions. */
public final class InventorySanityListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        ItemStack current = e.getCurrentItem();
        if (ItemUtil.normalizeCustomModelData(current)) {
            e.setCurrentItem(current);
        }

        ItemStack cursor = e.getCursor();
        if (ItemUtil.normalizeCustomModelData(cursor)) {
            e.setCursor(cursor);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItem(e.getNewSlot());
        if (ItemUtil.normalizeCustomModelData(item)) {
            p.getInventory().setItem(e.getNewSlot(), item);
        }
    }
}

