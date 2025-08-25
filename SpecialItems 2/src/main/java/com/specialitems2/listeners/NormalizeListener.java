package com.specialitems2.listeners;

import com.specialitems2.util.CustomModelDataUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class NormalizeListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        CustomModelDataUtil.normalize(cursor);
        ItemStack current = e.getCurrentItem();
        CustomModelDataUtil.normalize(current);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        for (ItemStack it : e.getNewItems().values()) {
            CustomModelDataUtil.normalize(it);
        }
        CustomModelDataUtil.normalize(e.getCursor());
    }
}
