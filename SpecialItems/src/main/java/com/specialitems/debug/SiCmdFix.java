package com.specialitems.debug;

import com.specialitems.util.ItemUtil;
import com.specialitems.util.TemplateItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SiCmdFix implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Player only"); return true; }
        ItemStack it = p.getInventory().getItemInMainHand();
        if (it == null || it.getType().isAir()) { p.sendMessage("Hold an item."); return true; }

        ItemMeta m = it.getItemMeta();
        Integer val = null;

        // Read raw item components/NBT and remove any existing CustomModelData entry
        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            var asNmsCopy = craft.getMethod("asNMSCopy", ItemStack.class);
            Object nms = asNmsCopy.invoke(null, it);
            // New data-component based storage introduced in 1.20+ uses namespaced keys
            // such as "minecraft:custom_model_data".  Check for this first and strip it.
            try {
                var getComponents = nms.getClass().getMethod("getComponents");
                Object comps = getComponents.invoke(nms);
                if (comps != null) {
                    var contains = comps.getClass().getMethod("contains", String.class);
                    String key = "minecraft:custom_model_data";
                    if ((Boolean) contains.invoke(comps, key)) {
                        var get = comps.getClass().getMethod("get", String.class);
                        Object raw = get.invoke(comps, key);
                        double dbl;
                        try { dbl = (double) raw.getClass().getMethod("asDouble").invoke(raw); }
                        catch (NoSuchMethodException ex) { dbl = Double.parseDouble(raw.toString()); }
                        val = (int) Math.round(dbl);
                        var remove = comps.getClass().getMethod("remove", String.class);
                        remove.invoke(comps, key);
                        var setComponents = nms.getClass().getMethod("setComponents", comps.getClass());
                        setComponents.invoke(nms, comps);
                        var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                        ItemStack cleaned = (ItemStack) asBukkitCopy.invoke(null, nms);
                        m = cleaned.getItemMeta();
                        it.setItemMeta(m);
                    }
                }
            } catch (Throwable ignored) {}

            // Legacy NBT tag fallback for older items
            if (val == null) {
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
                        val = ItemUtil.toInt(str);
                        var remove = tag.getClass().getMethod("remove", String.class);
                        remove.invoke(tag, "CustomModelData");
                        var setTag = nms.getClass().getMethod("setTag", tag.getClass());
                        setTag.invoke(nms, tag);
                        var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                        ItemStack cleaned = (ItemStack) asBukkitCopy.invoke(null, nms);
                        m = cleaned.getItemMeta();
                        it.setItemMeta(m);
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (m != null && val != null) {
            m.setCustomModelData(val);
            it.setItemMeta(m);
            p.getInventory().setItemInMainHand(it);
            p.sendMessage("CustomModelData normalized to integer: " + val);
        } else if (TemplateItems.applyTemplateMeta(it)) {
            p.getInventory().setItemInMainHand(it);
            p.sendMessage("CustomModelData applied from template.");
        } else {
            p.sendMessage("No CMD on this item.");
        }
        return true;
    }
}
