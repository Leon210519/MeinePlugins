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

    /** Converts various object representations of numbers or numeric strings
     *  to a strict integer (no fractional part). */
    public static Integer readInt(ConfigurationSection sec, String path) {
        if (sec == null || path == null) return null;
        Object raw = sec.get(path);
        return toInt(raw);
    }

    /** Strict: akzeptiert nur ganze Zahlen (oder x.0) */
    public static Integer toInt(Object raw) {
    if (raw == null) return null;
    try {
        if (raw instanceof Number n) {
            double d = n.doubleValue();
            long l = Math.round(d);
            if (Math.abs(d - l) < 1e-9) {
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
            }
            return null; // hatte Nachkommastellen
        }
        if (raw instanceof String s) {
            s = s.trim();
            if (s.matches("^-?\\d+$")) return Integer.parseInt(s);                // "1102"
            if (s.matches("^-?\\d+\\.0+[fFdD]?$"))                                // "1102.0"
                return Integer.parseInt(s.substring(0, s.indexOf('.')));
            return null; // z.B. "1102.5" -> invalid
        }
     } catch (Throwable ignored) {}
     return null;
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
    /**
     * Sets the CustomModelData value using the Bukkit API only.
     */
    public static ItemStack forceSetCustomModelData(ItemStack item, int cmd) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Reads the CustomModelData value from the item via {@link ItemMeta}.
     */
    public static Integer getCustomModelData(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        return (meta != null && meta.hasCustomModelData()) ? meta.getCustomModelData() : null;
    }

    /**
     * Normalizes the CustomModelData of the given item.
     * <ul>
     *     <li>If no CMD is present but a template match exists, the template's
     *     value is applied.</li>
     *     <li>If a floating point or root-level NBT CMD is found, it is
     *     rewritten to an integer via {@link ItemMeta}.</li>
     * </ul>
     *
     * @param item item to normalize
     * @return {@code true} if the item was modified
     */
    public static boolean normalizeCustomModelData(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        ItemMeta beforeMeta = item.getItemMeta();
        if (beforeMeta == null) return false;

        // If CMD missing, try to apply template
        if (!beforeMeta.hasCustomModelData()) {
            TemplateItems.applyTemplateMeta(item);
            beforeMeta = item.getItemMeta();
        }

        Integer cmd = beforeMeta.hasCustomModelData() ? beforeMeta.getCustomModelData() : null;
        boolean changed = false;

        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            Object nms = craft.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);

            // Remove data component entry if present (1.20+)
            try {
                Class<?> comps = Class.forName("net.minecraft.world.item.component.DataComponents");
                Object type = comps.getField("CUSTOM_MODEL_DATA").get(null);
                var has = nms.getClass().getMethod("has", type.getClass());
                if ((Boolean) has.invoke(nms, type)) {
                    var get = nms.getClass().getMethod("get", type.getClass());
                    Object val = get.invoke(nms, type);
                    if (cmd == null && val instanceof Integer v) cmd = v;
                    var remove = nms.getClass().getMethod("remove", type.getClass());
                    remove.invoke(nms, type);
                    changed = true;
                }
            } catch (Throwable ignored) {}

            // Legacy NBT tag
            var getTag = nms.getClass().getMethod("getTag");
            Object tag = getTag.invoke(nms);
            if (tag != null) {
                var contains = tag.getClass().getMethod("contains", String.class);
                if ((Boolean) contains.invoke(tag, "CustomModelData")) {
                    var get = tag.getClass().getMethod("get", String.class);
                    Object raw = get.invoke(tag, "CustomModelData");
                    String str;
                    try { str = (String) raw.getClass().getMethod("asString").invoke(raw); }
                    catch (NoSuchMethodException ex) { str = raw.toString(); }
                    try {
                        int val = (int) Math.floor(Double.parseDouble(str));
                        if (cmd == null || cmd != val) cmd = val;
                    } catch (NumberFormatException ignored) {}
                    var remove = tag.getClass().getMethod("remove", String.class);
                    remove.invoke(tag, "CustomModelData");
                    var setTag = nms.getClass().getMethod("setTag", tag.getClass());
                    setTag.invoke(nms, tag);
                    changed = true;
                }
            }

            if (changed) {
                ItemStack cleaned = (ItemStack) craft.getMethod("asBukkitCopy", nms.getClass()).invoke(null, nms);
                ItemMeta cleanedMeta = cleaned.getItemMeta();
                if (cleanedMeta != null) item.setItemMeta(cleanedMeta);
            }
        } catch (Throwable ignored) {}

        if (cmd != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != cmd) {
                if (meta == null) return changed;
                meta.setCustomModelData(cmd);
                item.setItemMeta(meta);
                changed = true;
            }
        }

        return changed;
    }

}