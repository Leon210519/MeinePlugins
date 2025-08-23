package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
        // Handled in CombatListener to apply deterministic scaling.
    }
}
