package com.specialitems2.leveling;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class EnchantUtil {
    private EnchantUtil(){}

    public static void addOrIncrease(ItemStack item, Enchantment ench, int delta, boolean unsafe) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        int current = meta.getEnchantLevel(ench);
        int target = Math.max(0, current + delta);
        if (unsafe) {
            item.addUnsafeEnchantment(ench, target);
        } else {
            int max = ench.getMaxLevel();
            target = Math.min(target, max);
            if (target <= 0) meta.removeEnchant(ench);
            else meta.addEnchant(ench, target, false);
            item.setItemMeta(meta);
        }
    }
}
