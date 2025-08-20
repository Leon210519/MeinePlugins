package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.Effects;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {}

    public static ItemStack withEffect(ItemStack item, String effectId, int level) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        var pdc = meta.getPersistentDataContainer();
        var key = new NamespacedKey(SpecialItemsPlugin.getInstance(), "ench_" + effectId);
        pdc.set(key, PersistentDataType.INTEGER, Math.max(1, level));

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        String display = effectId;
        try {
            var ce = Effects.get(effectId);
            if (ce != null && ce.displayName() != null && !ce.displayName().isEmpty()) {
                display = ce.displayName();
            }
        } catch (Throwable ignored) {}
        removeLoreLinePrefix(lore, ChatColor.GRAY + display);
        lore.add(ChatColor.GRAY + display + " " + roman(level));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static int getEffectLevel(ItemStack item, String effectId) {
        if (item == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer lvl = meta.getPersistentDataContainer().get(
                new NamespacedKey(SpecialItemsPlugin.getInstance(), "ench_" + effectId),
                PersistentDataType.INTEGER);
        return (lvl == null ? 0 : Math.max(0, lvl));
    }

    public static double getToolYieldBonus(ItemStack item) {
        if (item == null) return 0.0;
        try {
            double pct = SpecialItemsPlugin.getInstance().leveling().getBonusYieldPct(item);
            return pct / 100.0;
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    public static void removeLoreLinePrefix(List<String> lore, String prefix) {
        if (lore == null || prefix == null) return;
        String sp = ChatColor.stripColor(prefix);
        lore.removeIf(s -> ChatColor.stripColor(s).startsWith(sp));
    }

    public static String roman(int n) {
        String[] r = {"","I","II","III","IV","V","VI","VII","VIII","IX","X"};
        if (n >= 0 && n < r.length) return r[n];
        StringBuilder sb = new StringBuilder();
        while (n >= 10) { sb.append("X"); n -= 10; }
        if (n >= 9) { sb.append("IX"); n -= 9; }
        if (n >= 5) { sb.append("V"); n -= 5; }
        if (n >= 4) { sb.append("IV"); n -= 4; }
        while (n >= 1) { sb.append("I"); n -= 1; }
        return sb.toString();
    }
}