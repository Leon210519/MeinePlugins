package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import com.specialitems.util.Configs;
import com.specialitems.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AbsorptionShield implements CustomEffect {
    @Override public String id() { return "absorption_shield"; }
    @Override public String displayName() { return "Absorption Shield"; }
    @Override public int maxLevel() { return 4; }
    @Override public boolean supports(Material type) {
        String n = type.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }
    @Override
    public void onTick(Player player, ItemStack item, int level) {
        if (!Configs.effectEnabled(id())) return;
        int total = 0;
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor != null) {
            for (ItemStack a : armor) total += ItemUtil.getEffectLevel(a, id());
        }
        if (total <= 0) {
            player.removePotionEffect(PotionEffectType.ABSORPTION);
            return;
        }
        int maxAmp = Math.max(0, Configs.effectInt(id(), "max_amplifier", 4));
        int refresh = Math.max(1, Configs.effectInt(id(), "refresh_ticks", 60));
        int amp = Math.min(maxAmp, total - 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, refresh + 20, amp, true, false, false));
    }
}
