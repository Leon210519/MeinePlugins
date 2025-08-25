package com.specialitems2.util;

import com.specialitems2.cmd.CmdRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Utility to normalize custom model data to integer values within the whitelist. */
public final class CustomModelDataUtil {
    private CustomModelDataUtil() {}

    public static void normalize(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        Integer val = null;
        if (meta != null && meta.hasCustomModelData()) {
            val = meta.getCustomModelData();
            meta.setCustomModelData(null);
            item.setItemMeta(meta);
        }
        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            var asNmsCopy = craft.getMethod("asNMSCopy", ItemStack.class);
            Object nms = asNmsCopy.invoke(null, item);
            try {
                Class<?> comps = Class.forName("net.minecraft.world.item.component.DataComponents");
                Object type = comps.getField("CUSTOM_MODEL_DATA").get(null);
                var has = nms.getClass().getMethod("has", type.getClass());
                if ((Boolean) has.invoke(nms, type)) {
                    var get = nms.getClass().getMethod("get", type.getClass());
                    Object comp = get.invoke(nms, type);
                    Object raw;
                    try { raw = comp.getClass().getMethod("intValue").invoke(comp); }
                    catch (NoSuchMethodException ex) { raw = comp.getClass().getMethod("value").invoke(comp); }
                    val = ((Number) raw).intValue();
                    var remove = nms.getClass().getMethod("remove", type.getClass());
                    remove.invoke(nms, type);
                    var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                    ItemStack cleaned = (ItemStack) asBukkitCopy.invoke(null, nms);
                    meta = cleaned.getItemMeta();
                    item.setItemMeta(meta);
                }
            } catch (Throwable ignored) {}
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
                    try { val = Integer.parseInt(str.split("[.]")[0]); } catch (Exception ignored) {}
                    var remove = tag.getClass().getMethod("remove", String.class);
                    remove.invoke(tag, "CustomModelData");
                    var setTag = nms.getClass().getMethod("setTag", tag.getClass());
                    setTag.invoke(nms, tag);
                    var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                    ItemStack cleaned = (ItemStack) asBukkitCopy.invoke(null, nms);
                    meta = cleaned.getItemMeta();
                    item.setItemMeta(meta);
                }
            }
        } catch (Throwable ignored) {}
        Integer allowed = CmdRegistry.clamp(val);
        if (allowed != null) {
            meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(allowed);
                item.setItemMeta(meta);
                try {
                    Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
                    var asNmsCopy = craft.getMethod("asNMSCopy", ItemStack.class);
                    Object nms = asNmsCopy.invoke(null, item);
                    var getOrCreateTag = nms.getClass().getMethod("getOrCreateTag");
                    Object tag = getOrCreateTag.invoke(nms);
                    try {
                        tag.getClass().getMethod("putInt", String.class, int.class).invoke(tag, "CustomModelData", allowed);
                    } catch (NoSuchMethodException ex) {
                        try {
                            tag.getClass().getMethod("setInt", String.class, int.class).invoke(tag, "CustomModelData", allowed);
                        } catch (NoSuchMethodException ignored) {}
                    }
                    var setTag = nms.getClass().getMethod("setTag", tag.getClass());
                    setTag.invoke(nms, tag);
                    var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                    ItemStack withTag = (ItemStack) asBukkitCopy.invoke(null, nms);
                    item.setItemMeta(withTag.getItemMeta());
                } catch (Throwable ignored) {}
            }
        }
    }
}
