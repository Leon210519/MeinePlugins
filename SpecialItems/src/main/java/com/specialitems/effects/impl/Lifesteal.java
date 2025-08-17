package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class Lifesteal implements CustomEffect {
    @Override public String id() { return "lifesteal"; }
    @Override public String displayName() { return "Lifesteal"; }
    @Override public int maxLevel() { return 5; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_SWORD") || type.name().endsWith("_AXE"); }
    @Override
    public void onEntityDamage(Player player, ItemStack weapon, EntityDamageByEntityEvent e, int level) {
        double pct = com.specialitems.util.Configs.cfg.getDouble("effects.lifesteal.heal-percentage-per-level", 0.05);
        double heal = Math.max(0.0, e.getFinalDamage() * pct * level);
        double max = player.getMaxHealth();
        player.setHealth(Math.min(max, player.getHealth() + heal));
    }
}
