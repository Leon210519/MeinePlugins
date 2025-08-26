package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.Effects;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
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

    // ---- Integer config reader (canonical) ----
    public static Integer readInt(ConfigurationSection sec, String path) {
        if (sec == null || path == null) return null;
        if (sec.isInt(path)) return sec.getInt(path);

        if (sec.isString(path)) {
            String s = sec.getString(path);
            if (s != null && s.matches("^-?\\d+$")) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null; // caller must log a precise error
    }

    // ---- Force set CMD on an item (canonical) ----
    public static ItemStack forceSetCustomModelData(ItemStack item, int cmd) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setCustomModelData(Integer.valueOf(cmd));
        item.setItemMeta(meta);

        // Also write legacy NBT tag for resource pack lookups and to purge float values.
        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            Object nms = craft.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
            var getOrCreateTag = nms.getClass().getMethod("getOrCreateTag");
            Object tag = getOrCreateTag.invoke(nms);
            try {
                tag.getClass().getMethod("putInt", String.class, int.class)
                        .invoke(tag, "CustomModelData", cmd);
            } catch (NoSuchMethodException ex) {
                try {
                    tag.getClass().getMethod("setInt", String.class, int.class)
                            .invoke(tag, "CustomModelData", cmd);
                } catch (NoSuchMethodException ignored) {}
            }
            var setTag = nms.getClass().getMethod("setTag", tag.getClass());
            setTag.invoke(nms, tag);
            var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
            ItemStack withTag = (ItemStack) asBukkitCopy.invoke(null, nms);
            item.setItemMeta(withTag.getItemMeta());
        } catch (Throwable ignored) {}


        return item;
    }

    // ---- Read CMD from item ----
    public static Integer getCustomModelData(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getCustomModelData(); // returns Integer
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