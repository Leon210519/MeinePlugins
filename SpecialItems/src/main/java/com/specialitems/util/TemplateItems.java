package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class TemplateItems {

    public record TemplateItem(String id, ItemStack stack) {}

    private TemplateItems() {}

    public static java.util.List<TemplateItem> loadAll() {
        java.util.List<TemplateItem> list = new java.util.ArrayList<>();
        var sec = Configs.templates.getConfigurationSection("templates");
        if (sec == null) return list;
        for (String id : sec.getKeys(false)) {
            ItemStack it = buildFrom(id, sec.getConfigurationSection(id));
            if (it != null) list.add(new TemplateItem(id, it));
        }
        return list;
    }

    public static ItemStack buildFrom(String id, ConfigurationSection t) {
        if (t == null) return null;
        Material mat = Material.matchMaterial(t.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', t.getString("name","&fSpecial Item")));
            List<String> lore = new ArrayList<>();
            for (String l : t.getStringList("lore")) lore.add(ChatColor.translateAlternateColorCodes('&', l));

            meta.setLore(lore);
            it.setItemMeta(meta);
        }

        var enchSec = t.getConfigurationSection("enchants");
        if (enchSec != null) {
            for (String eid : enchSec.getKeys(false)) {
                int lvl = enchSec.getInt(eid, 1);
                it = ItemUtil.withEffect(it, eid, lvl);
            }
        }

        try {
            int cmd = CustomModels.cmdFor(id, t.getString("name"));
            if (cmd > 0) {
                ItemMeta m = it.getItemMeta();
                if (m != null) {
                    m.setCustomModelData(cmd);
                    it.setItemMeta(m);
                }
            }
        } catch (Throwable ignored) {}

        try {
            Tagger.tagAsSpecial(SpecialItemsPlugin.getInstance(), it, id);
        } catch (Throwable ignored) {}

        return it;
    }
}