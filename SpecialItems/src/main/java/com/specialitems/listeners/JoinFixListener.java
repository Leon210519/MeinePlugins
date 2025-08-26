package com.specialitems.listeners;

import com.specialitems.util.CmdFixer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/** Normalizes player inventories on join to guarantee integer CMD values. */
public final class JoinFixListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        var inv = p.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            ItemStack norm = CmdFixer.normalize(item);
            if (norm != item) inv.setItem(i, norm);
        }

        var ec = p.getEnderChest();
        for (int i = 0; i < ec.getSize(); i++) {
            ItemStack item = ec.getItem(i);
            ItemStack norm = CmdFixer.normalize(item);
            if (norm != item) ec.setItem(i, norm);
        }
    }
}

