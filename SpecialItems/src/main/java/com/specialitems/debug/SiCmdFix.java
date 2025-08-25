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
            // Use the 1.20+ data component system if present
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
                    double dbl = ((Number) raw).doubleValue();
                    val = (int) Math.round(dbl);
                    var remove = nms.getClass().getMethod("remove", type.getClass());
                    remove.invoke(nms, type);
                    var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                    ItemStack cleaned = (ItemStack) asBukkitCopy.invoke(null, nms);
                    m = cleaned.getItemMeta();
                    it.setItemMeta(m);
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
