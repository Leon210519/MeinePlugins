package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import com.specialitems.util.Configs;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class DoubleDamage implements CustomEffect {
    @Override public String id() { return "double_damage"; }
    @Override public String displayName() { return "Double Damage"; }
    @Override public int maxLevel() { return 5; }
    @Override public boolean supports(Material type) {
        String n = type.name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE");
    }
    @Override
    public void onEntityDamage(Player player, ItemStack weapon, EntityDamageByEntityEvent e, int level) {
        if (!Configs.effectEnabled(id())) return;
        double chance = Configs.effectDouble(id(), "chance_per_level", 0.20) * level;
        if (ThreadLocalRandom.current().nextDouble() > chance) return;
        double mult = Math.max(1.0, Configs.effectDouble(id(), "max_multiplier", 2.0));
        e.setDamage(e.getDamage() * mult);
    }
}
