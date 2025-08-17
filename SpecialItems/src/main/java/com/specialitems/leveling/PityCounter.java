package com.specialitems.leveling;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PityCounter {
    private PityCounter(){}

    public static double get(ItemStack it, Keys keys) {
        if (it == null) return 0.0;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return 0.0;
        Double v = meta.getPersistentDataContainer().get(keys.PITY, PersistentDataType.DOUBLE);
        return v == null ? 0.0 : Math.max(0.0, Math.min(1.0, v));
    }

    public static void set(ItemStack it, Keys keys, double v) {
        if (it == null) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(keys.PITY, PersistentDataType.DOUBLE, Math.max(0.0, Math.min(1.0, v)));
        it.setItemMeta(meta);
    }

    public static double add(ItemStack it, Keys keys, double delta) {
        double v = get(it, keys) + delta;
        set(it, keys, v);
        return get(it, keys);
    }

    public static void reset(ItemStack it, Keys keys) {
        set(it, keys, 0.0);
    }
}
