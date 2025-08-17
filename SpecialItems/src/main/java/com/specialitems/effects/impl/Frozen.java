package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

public class Frozen implements CustomEffect {
    @Override public String id() { return "frozen"; }
    @Override public String displayName() { return "Frozen"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_SWORD") || type.name().endsWith("_AXE"); }
    @Override
    public void onEntityDamage(Player player, ItemStack weapon, EntityDamageByEntityEvent e, int level) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        double chance = com.specialitems.util.Configs.cfg.getDouble("effects.frozen.chance", 0.25);
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            int dur = com.specialitems.util.Configs.cfg.getInt("effects.frozen.duration-ticks-per-level", 40) * level;
            int amp = Math.max(0, com.specialitems.util.Configs.cfg.getInt("effects.frozen.amplifier", 0));
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, amp, true, true, true));
        }
    }
}
