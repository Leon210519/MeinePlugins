package com.specialitems2.leveling;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class RarityUtil {
    private RarityUtil(){}

    public static Rarity get(ItemStack it, Keys keys) {
        if (it == null) return Rarity.COMMON;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return Rarity.COMMON;
        var pdc = meta.getPersistentDataContainer();
        String raw = pdc.get(keys.RARITY, PersistentDataType.STRING);
        return Rarity.fromString(raw);
    }

    public static void set(ItemStack it, Keys keys, Rarity rarity) {
        if (it == null) return;
        var meta = it.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(keys.RARITY, PersistentDataType.STRING, rarity.name());
        it.setItemMeta(meta);
    }
}
