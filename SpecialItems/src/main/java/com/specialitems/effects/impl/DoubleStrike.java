package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class DoubleStrike implements CustomEffect {
    @Override public String id() { return "doublestrike"; }
    @Override public String displayName() { return "Double Strike"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_SWORD") || type.name().endsWith("_AXE"); }
    @Override
    public void onEntityDamage(Player player, ItemStack weapon, EntityDamageByEntityEvent e, int level) {
        double chance = com.specialitems.util.Configs.cfg.getDouble("effects.doublestrike.chance-per-level", 0.15) * level;
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            if (e.getEntity() instanceof LivingEntity le) le.damage(e.getFinalDamage(), player);
        }
    }
}
