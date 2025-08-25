package com.specialitems2.effects.impl;

import com.specialitems2.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class Wither implements CustomEffect {
    @Override public String id() { return "wither"; }
    @Override public String displayName() { return "Wither"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_SWORD") || type.name().endsWith("_AXE"); }
    @Override
    public void onEntityDamage(Player player, ItemStack weapon, EntityDamageByEntityEvent e, int level) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        double chance = com.specialitems2.util.Configs.cfg.getDouble("effects.wither.chance", 0.25);
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            int dur = com.specialitems2.util.Configs.cfg.getInt("effects.wither.duration-ticks-per-level", 40) * level;
            int amp = Math.max(0, com.specialitems2.util.Configs.cfg.getInt("effects.wither.amplifier", 0));
            le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, dur, amp, true, true, true));
        }
    }
}
