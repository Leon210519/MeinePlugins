package com.specialitems2.effects.impl;

import com.specialitems2.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightVision implements CustomEffect {
    @Override public String id() { return "night_vision"; }
    @Override public String displayName() { return "Night Vision"; }
    @Override public int maxLevel() { return 1; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_HELMET"); }
    @Override
    public void onTick(Player player, ItemStack item, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false, false));
    }
}
