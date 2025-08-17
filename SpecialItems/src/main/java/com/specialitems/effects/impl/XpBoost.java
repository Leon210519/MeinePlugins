package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class XpBoost implements CustomEffect {
    @Override public String id() { return "xp_boost"; }
    @Override public String displayName() { return "XP Boost"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) { return true; }
    @Override
    public void onEntityKill(Player player, ItemStack weapon, EntityDeathEvent e, int level) {
        double multi = 1.0 + (com.specialitems.util.Configs.cfg.getDouble("effects.xp_boost.multiplier-per-level", 0.25) * level);
        int xp = e.getDroppedExp();
        e.setDroppedExp((int)Math.round(xp * multi));
    }
}
