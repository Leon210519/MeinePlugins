package com.specialitems2.listeners;

import com.specialitems2.SpecialItems2Plugin;
import com.specialitems2.bin.Bin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class BinListener implements Listener {
    private final SpecialItems2Plugin plugin;

    public BinListener(SpecialItems2Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(PlayerItemBreakEvent e) {
        ItemStack it = e.getBrokenItem();
        if (plugin.leveling().isSpecialItem(it)) {
            Bin.store(it);
            e.getPlayer().sendMessage(ChatColor.YELLOW + "Your special item was moved to the bin.");
        }
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent e) {
        ItemStack it = e.getEntity().getItemStack();
        if (plugin.leveling().isSpecialItem(it)) {
            Bin.store(it);
        }
    }
}
