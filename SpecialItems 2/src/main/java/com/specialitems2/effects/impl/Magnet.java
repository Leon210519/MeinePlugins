package com.specialitems2.effects.impl;

import com.specialitems2.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Magnet implements CustomEffect {
    @Override public String id() { return "magnet"; }
    @Override public String displayName() { return "Magnet"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_BOOTS") || type.name().endsWith("_CHESTPLATE"); }
    @Override
    public void onTick(Player player, ItemStack item, int level) {
        int per = Math.max(1, com.specialitems2.util.Configs.cfg.getInt("effects.magnet.range-per-level", 3));
        int max = Math.max(per, com.specialitems2.util.Configs.cfg.getInt("effects.magnet.max-range", 10));
        double range = Math.min(max, per * level);
        for (Item ent : player.getWorld().getNearbyEntitiesByType(Item.class, player.getLocation(), range)) {
            Vector vel = player.getLocation().toVector().subtract(ent.getLocation().toVector()).normalize().multiply(0.5);
            ent.setVelocity(vel);
        }
    }
}
