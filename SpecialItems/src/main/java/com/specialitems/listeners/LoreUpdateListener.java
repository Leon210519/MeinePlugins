package com.specialitems.listeners;

import com.specialitems.leveling.Keys;
import com.specialitems.leveling.LevelingService;
import com.specialitems.util.ItemLoreService;
import com.specialitems.SpecialItemsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Refreshes lore displays for special items on common inventory interactions. */
public class LoreUpdateListener implements Listener {
    private final Keys keys;

    public LoreUpdateListener(LevelingService svc) {
        this.keys = new Keys(SpecialItemsPlugin.getInstance());
    }

    private boolean isSpecial(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(keys.SI_ID, PersistentDataType.STRING);
    }

    private void refreshSlot(Inventory inv, int slot, ItemStack item) {
        if (!isSpecial(item)) return;
        ItemLoreService.renderLore(item, SpecialItemsPlugin.getInstance());
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
        if (isSpecial(current)) {
            ItemLoreService.renderLore(current, SpecialItemsPlugin.getInstance());
            e.setCurrentItem(current);
        }
        ItemStack cursor = e.getCursor();
        if (isSpecial(cursor)) {
            ItemLoreService.renderLore(cursor, SpecialItemsPlugin.getInstance());
            e.setCursor(cursor);
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        int slot = e.getNewSlot();
        ItemStack item = p.getInventory().getItem(slot);
        if (isSpecial(item)) {
            ItemLoreService.renderLore(item, SpecialItemsPlugin.getInstance());
            p.getInventory().setItem(slot, item);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        ItemStack main = e.getMainHandItem();
        if (isSpecial(main)) {
            ItemLoreService.renderLore(main, SpecialItemsPlugin.getInstance());
            e.setMainHandItem(main);
        }
        ItemStack off = e.getOffHandItem();
        if (isSpecial(off)) {
            ItemLoreService.renderLore(off, SpecialItemsPlugin.getInstance());
            e.setOffHandItem(off);
        }
    }
}
