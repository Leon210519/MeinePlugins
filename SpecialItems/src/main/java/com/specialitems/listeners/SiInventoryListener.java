package com.specialitems.listeners;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.util.InventorySyncUtil;
import com.specialitems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.logging.Logger;

/** Handles inventory interactions for SpecialItems to avoid client desync. */
public final class SiInventoryListener implements Listener {
    private static final Logger LOG = Logger.getLogger("SpecialItems/InventorySync");
    private final SpecialItemsPlugin plugin;

    public SiInventoryListener(SpecialItemsPlugin plugin) {
        this.plugin = plugin;
    }

    private void log(Player p, int slot, String action, ItemStack cursor) {
        if (!plugin.isDebug()) return;
        String cur = cursor == null ? "null" : cursor.getType().name();
        LOG.info(p.getName() + " slot=" + slot + " action=" + action +
                " cursor=" + cur + " usedNextTick=true updateInvCalled=" + InventorySyncUtil.DEBUG_FORCE_UPDATE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType().isAir()) return;
        ItemStack clone = current.clone();
        if (!ItemUtil.normalizeCustomModelData(clone)) return; // nothing to do

        int slot = e.getSlot();
        InventoryAction act = e.getAction();
        ItemStack cursor = e.getCursor();
        e.setCancelled(true);

        switch (act) {
            case MOVE_TO_OTHER_INVENTORY -> { // shift-click
                Inventory clicked = e.getClickedInventory();
                if (clicked == p.getInventory()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Inventory top = e.getView().getTopInventory();
                        Map<Integer, ItemStack> leftover = top.addItem(clone);
                        ItemStack remain = leftover.isEmpty() ? null : leftover.values().iterator().next();
                        p.getInventory().setItem(slot, remain);
                        InventorySyncUtil.updateIfNeeded(p);
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        clicked.setItem(slot, null);
                        var leftover = p.getInventory().addItem(clone);
                        leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
                        InventorySyncUtil.updateIfNeeded(p);
                    });
                }
                InventorySyncUtil.clearCursorNextTick(plugin, p);
                log(p, slot, act.name(), cursor);
            }
            case SWAP_WITH_CURSOR -> {
                ItemStack cursorClone = cursor == null ? null : cursor.clone();
                if (cursorClone != null) ItemUtil.normalizeCustomModelData(cursorClone);
                InventorySyncUtil.setSlotNextTick(plugin, p, slot, cursorClone);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    p.setItemOnCursor(clone);
                    InventorySyncUtil.updateIfNeeded(p);
                });
                log(p, slot, act.name(), cursor);
            }
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                int hotbar = e.getHotbarButton();
                ItemStack hot = p.getInventory().getItem(hotbar);
                ItemStack hotClone = hot == null ? null : hot.clone();
                if (hotClone != null) ItemUtil.normalizeCustomModelData(hotClone);
                InventorySyncUtil.setSlotNextTick(plugin, p, slot, hotClone);
                InventorySyncUtil.setSlotNextTick(plugin, p, hotbar, clone);
                InventorySyncUtil.clearCursorNextTick(plugin, p);
                log(p, slot, act.name(), cursor);
            }
            default -> {
                InventorySyncUtil.setSlotNextTick(plugin, p, slot, clone);
                InventorySyncUtil.clearCursorNextTick(plugin, p);
                log(p, slot, act.name(), cursor);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        boolean changed = false;
        for (Map.Entry<Integer, ItemStack> ent : e.getNewItems().entrySet()) {
            ItemStack clone = ent.getValue().clone();
            if (ItemUtil.normalizeCustomModelData(clone)) {
                changed = true;
                int slot = ent.getKey();
                InventorySyncUtil.setSlotNextTick(plugin, p, slot, clone);
                log(p, slot, "DRAG", e.getCursor());
            }
        }
        if (changed) {
            e.setCancelled(true);
            InventorySyncUtil.clearCursorNextTick(plugin, p);
        }
    }
}
