package com.specialitems.listeners;

import com.specialitems.leveling.LevelingService;
import com.specialitems.util.LoreRenderer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Refreshes lore displays for special items on common inventory interactions. */
public class LoreUpdateListener implements Listener {
    private final LevelingService svc;

    public LoreUpdateListener(LevelingService svc) {
        this.svc = svc;
    }

    private void refreshSlot(Inventory inv, int slot, ItemStack item) {
        if (item == null) return;
        if (!svc.isSpecialItem(item)) return;
        LoreRenderer.updateItemLore(item);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        Inventory top = e.getInventory();
        ItemStack[] items = top.getContents();
        for (int i = 0; i < items.length; i++) {
            refreshSlot(top, i, items[i]);
        }
        Inventory bottom = e.getPlayer().getInventory();
        ItemStack[] bItems = bottom.getContents();
        for (int i = 0; i < bItems.length; i++) {
            refreshSlot(bottom, i, bItems[i]);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        ItemStack current = e.getCurrentItem();
        if (current != null && svc.isSpecialItem(current)) {
            LoreRenderer.updateItemLore(current);
            e.setCurrentItem(current);
        }
        ItemStack cursor = e.getCursor();
        if (cursor != null && svc.isSpecialItem(cursor)) {
            LoreRenderer.updateItemLore(cursor);
            e.setCursor(cursor);
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        int slot = e.getNewSlot();
        ItemStack item = p.getInventory().getItem(slot);
        if (item != null && svc.isSpecialItem(item)) {
            LoreRenderer.updateItemLore(item);
            p.getInventory().setItem(slot, item);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        ItemStack main = e.getMainHandItem();
        if (main != null && svc.isSpecialItem(main)) {
            LoreRenderer.updateItemLore(main);
            e.setMainHandItem(main);
        }
        ItemStack off = e.getOffHandItem();
        if (off != null && svc.isSpecialItem(off)) {
            LoreRenderer.updateItemLore(off);
            e.setOffHandItem(off);
        }
    }
}
