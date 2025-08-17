package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Gears implements CustomEffect {
    @Override public String id() { return "gears"; }
    @Override public String displayName() { return "Gears"; }
    @Override public int maxLevel() { return 3; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_BOOTS"); }
    @Override
    public void onTick(Player player, ItemStack item, int level) {
        int amp = Math.max(0, com.specialitems.util.Configs.cfg.getInt("effects.gears.speed-amplifier-per-level", 0) + (level-1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, amp, true, false, false));
    }
}
