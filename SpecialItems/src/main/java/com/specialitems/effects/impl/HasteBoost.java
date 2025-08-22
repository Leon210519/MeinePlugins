package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import com.specialitems.util.Configs;
import com.specialitems.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HasteBoost implements CustomEffect {
    @Override public String id() { return "haste_boost"; }
    @Override public String displayName() { return "Haste Boost"; }
    @Override public int maxLevel() { return 5; }
    @Override public boolean supports(Material type) { return type.name().endsWith("_PICKAXE"); }
    @Override
    public void onTick(Player player, ItemStack item, int level) {
        if (!Configs.effectEnabled(id())) return;
        int held = ItemUtil.getEffectLevel(item, id());
        if (item != null && item.getType().name().endsWith("_PICKAXE") && held > 0) {
            int amp = held - 1;
            int refresh = Math.max(1, Configs.effectInt(id(), "refresh_ticks", 20));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, refresh * 2, amp, true, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.HASTE);
        }
    }
    @Override
    public void onItemHeld(Player player, ItemStack item, int level) {
        onTick(player, item, level);
    }
}
