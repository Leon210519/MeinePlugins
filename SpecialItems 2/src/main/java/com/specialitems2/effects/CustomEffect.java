package com.specialitems2.effects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public interface CustomEffect {
    String id();
    String displayName();
    int maxLevel();
    boolean supports(Material itemType);

    default void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {}
    default void onEntityDamage(Player player, ItemStack weapon, EntityDamageByEntityEvent e, int level) {}
    default void onEntityKill(Player player, ItemStack weapon, EntityDeathEvent e, int level) {}
    default void onTick(Player player, ItemStack item, int level) {}
    default void onItemHeld(Player player, ItemStack item, int level) {}
}
