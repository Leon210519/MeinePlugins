package com.lootforge.armorskins;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public enum ArmorSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET;

    public ItemStack getItem(Player player) {
        switch (this) {
            case HEAD:
                return player.getInventory().getHelmet();
            case CHEST:
                return player.getInventory().getChestplate();
            case LEGS:
                return player.getInventory().getLeggings();
            case FEET:
                return player.getInventory().getBoots();
            default:
                return null;
        }
    }

    public static ArmorSlot match(String name) {
        try {
            return ArmorSlot.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
