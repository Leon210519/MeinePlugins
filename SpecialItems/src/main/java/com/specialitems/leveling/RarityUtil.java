package com.specialitems.leveling;

import com.specialitems.util.SkinService;
import org.bukkit.inventory.EquipmentSlot;
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

        EquipmentSlot slot = detectSlot(it);
        SkinService.applyForRarity(it, rarity, slot);
    }

    private static EquipmentSlot detectSlot(ItemStack it) {
        String n = it.getType().name();
        if (n.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (n.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (n.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (n.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return null;
    }
}
