package com.specialitems.listeners;

import com.specialitems.util.CmdFixer;
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
        ItemStack normCurrent = CmdFixer.normalize(current);
        if (normCurrent != current) e.setCurrentItem(normCurrent);

        ItemStack cursor = e.getCursor();
        ItemStack normCursor = CmdFixer.normalize(cursor);
        if (normCursor != cursor) e.setCursor(normCursor);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItem(e.getNewSlot());
        ItemStack norm = CmdFixer.normalize(item);
        if (norm != item) p.getInventory().setItem(e.getNewSlot(), norm);
    }
}

