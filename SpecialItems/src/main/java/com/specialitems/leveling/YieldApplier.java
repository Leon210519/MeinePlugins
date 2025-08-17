package com.specialitems.leveling;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class YieldApplier implements Listener {
    private final LevelingService svc;
    public YieldApplier(LevelingService svc) { this.svc = svc; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        var p = e.getPlayer();
        if (p == null) return;
        ItemStack held = p.getInventory().getItem(EquipmentSlot.HAND);
        if (!svc.isSpecialItem(held)) return;
        if (svc.detectToolClass(held) != ToolClass.HOE) return;

        double bonusPct = svc.getBonusYieldPct(held);
        if (bonusPct <= 0) return;

        double mult = 1.0 + (bonusPct / 100.0);
        e.getItems().forEach(item -> {
            var stack = item.getItemStack();
            int newAmount = (int) Math.floor(stack.getAmount() * mult);
            stack.setAmount(Math.max(1, newAmount));
        });
    }
}
