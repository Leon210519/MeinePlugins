package com.specialitems.leveling;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class LevelingListener implements Listener {
    private final LevelingService svc;
    public LevelingListener(LevelingService svc) { this.svc = svc; }

    private ItemStack hand(Player p) { return p.getInventory().getItem(EquipmentSlot.HAND); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack held = hand(p);
        if (!svc.isSpecialItem(held)) return;

        var clazz = svc.detectToolClass(held);
        Block b = e.getBlock();
        Material m = b.getType();

        switch (clazz) {
            case PICKAXE -> {
                double add;
                if (m.name().endsWith("_ORE")) {
                    // ensure pickaxe per ore = 4x hoe harvest XP
                    add = 4.0 * svc.xpHoeHarvest;
                } else {
                    // base blocks mining
                    add = switch (m) {
                        case STONE, COBBLESTONE, DEEPSLATE, NETHERRACK, END_STONE -> svc.xpPickaxeBlock;
                        default -> 0.0;
                    };
                }
                if (add > 0) svc.grantXp(held, add, clazz);
            }
            case HOE -> {
                if (HarvestUtil.isCrop(m) && HarvestUtil.isMatureCrop(b)) {
                    svc.grantXp(held, svc.xpHoeHarvest, clazz);
                }
            }
            default -> {}
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        ItemStack held = killer.getInventory().getItemInMainHand();
        if (!svc.isSpecialItem(held)) return;

        var clazz = svc.detectToolClass(held);
        if (clazz != ToolClass.SWORD) return;

        double add = isBoss(dead.getType()) ? svc.xpSwordBossKill : svc.xpSwordKill;
        svc.grantXp(held, add, clazz);
    }

    private boolean isBoss(EntityType t) {
        return switch (t) {
            case WITHER, ENDER_DRAGON, ELDER_GUARDIAN, WARDEN -> true;
            default -> false;
        };
    }
}
