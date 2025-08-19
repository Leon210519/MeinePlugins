package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.leveling.Keys;
import com.specialitems.leveling.Rarity;
import com.specialitems.leveling.RarityUtil;
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
        for (String key : sec.getKeys(false)) {
            ConfigurationSection tsec = sec.getConfigurationSection(key);
            String tid = (tsec == null ? key : tsec.getString("id", key));
            ItemStack it = buildFrom(tid, tsec);
            if (it != null) list.add(new TemplateItem(tid, it));
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

        String rar = t.getString("rarity");
        if (rar != null) {
            try {
                Rarity r = Rarity.fromString(rar);
                RarityUtil.set(it, new Keys(SpecialItemsPlugin.getInstance()), r);
            } catch (Throwable ignored) {}
        }

        return it;
    }
}