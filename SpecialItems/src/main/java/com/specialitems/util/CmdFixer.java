package com.specialitems.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Utility to normalize CustomModelData values on items. */
public final class CmdFixer {

    private CmdFixer() {}

    /**
     * Ensures the item's CustomModelData is stored as a strict integer via the
     * Bukkit API. If the item has no meta or CMD, it is returned unchanged.
     */
    public static ItemStack normalize(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        Integer cmd = null;
        if (meta != null && meta.hasCustomModelData()) {
            cmd = meta.getCustomModelData();
        } else {
            // Attempt to read legacy float tag
            try {
                Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
                Object nms = craft.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
                Object tag = nms.getClass().getMethod("getTag").invoke(nms);
                if (tag != null) {
                    var contains = tag.getClass().getMethod("contains", String.class);
                    if ((Boolean) contains.invoke(tag, "CustomModelData")) {
                        var get = tag.getClass().getMethod("get", String.class);
                        Object raw = get.invoke(tag, "CustomModelData");
                        String str;
                        try { str = (String) raw.getClass().getMethod("asString").invoke(raw); }
                        catch (NoSuchMethodException ex) { str = raw.toString(); }
                        try { cmd = Integer.parseInt(str.split("[.]")[0]); } catch (Exception ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (cmd != null) {
            ItemUtil.forceSetCustomModelData(item, cmd);
        }
        return item;
    }
}

