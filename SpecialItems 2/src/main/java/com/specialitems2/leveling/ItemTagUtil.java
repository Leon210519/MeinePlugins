package com.specialitems2.leveling;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.enchantments.Enchantment;

import java.util.Locale;
import java.util.UUID;

/** Helper for storing and retrieving special item tags. */
public class ItemTagUtil {
    private final NamespacedKey uuidKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey xpKey;
    private final NamespacedKey yieldKey;

    public ItemTagUtil(Plugin plugin) {
        this.uuidKey = new NamespacedKey(plugin, "si.uuid");
        this.typeKey = new NamespacedKey(plugin, "si.type");
        this.levelKey = new NamespacedKey(plugin, "si.level");
        this.xpKey = new NamespacedKey(plugin, "si.xp");
        this.yieldKey = new NamespacedKey(plugin, "si.yield");
    }

    public boolean isTagged(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING);
    }

    public String getType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "";
        String t = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        return t == null ? "" : t;
    }

    public int getLevel(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 1;
        Integer lvl = meta.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return lvl == null ? 1 : lvl;
    }

    public double getXp(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0.0;
        Double xp = meta.getPersistentDataContainer().get(xpKey, PersistentDataType.DOUBLE);
        return xp == null ? 0.0 : xp;
    }

    public double getYield(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0.0;
        Double y = meta.getPersistentDataContainer().get(yieldKey, PersistentDataType.DOUBLE);
        return y == null ? 0.0 : y;
    }

    public void setLevel(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
    }

    public void setXp(ItemStack item, double xp) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(xpKey, PersistentDataType.DOUBLE, xp);
        item.setItemMeta(meta);
    }

    public void setYield(ItemStack item, double yield) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (yield <= 0) {
            meta.getPersistentDataContainer().remove(yieldKey);
        } else {
            meta.getPersistentDataContainer().set(yieldKey, PersistentDataType.DOUBLE, yield);
        }
        item.setItemMeta(meta);
    }

    /** Ensure the item is tagged and prepared for leveling. */
    public void init(ItemStack item, String type) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(uuidKey, PersistentDataType.STRING)) {
            pdc.set(uuidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        }
        pdc.set(typeKey, PersistentDataType.STRING, type.toLowerCase(Locale.ROOT));
        if (!pdc.has(levelKey, PersistentDataType.INTEGER)) {
            pdc.set(levelKey, PersistentDataType.INTEGER, 1);
        }
        if (!pdc.has(xpKey, PersistentDataType.DOUBLE)) {
            pdc.set(xpKey, PersistentDataType.DOUBLE, 0.0);
        }
        if (type.equalsIgnoreCase("pickaxe") || type.equalsIgnoreCase("hoe")) {
            double yield = getYield(item);
            pdc.set(yieldKey, PersistentDataType.DOUBLE, yield);
        } else {
            pdc.remove(yieldKey);
        }
        meta.setUnbreakable(true);
        try {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        } catch (Throwable ignored) {}
        try {
            if (!meta.hasEnchant(Enchantment.UNBREAKING)) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            }
        } catch (Throwable ignored) {}
        item.setItemMeta(meta);
    }
}
