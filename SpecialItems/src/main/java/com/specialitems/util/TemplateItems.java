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

import java.util.*;

public final class TemplateItems {

    public record TemplateItem(String id, ItemStack stack) {}

    private TemplateItems() {}

    private static final Map<String, TemplateItem> BY_MAT_NAME = new HashMap<>();

    public static java.util.List<TemplateItem> loadAll() {
        java.util.List<TemplateItem> list = new java.util.ArrayList<>();
        BY_MAT_NAME.clear();
        CustomModels.reset();
        var sec = Configs.templates.getConfigurationSection("templates");
        if (sec == null) return list;
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        Collections.sort(keys);
        for (String key : keys) {
            ConfigurationSection tsec = sec.getConfigurationSection(key);
            String tid = (tsec == null ? key : tsec.getString("id", key));
            ItemStack it = buildFrom(tid, tsec);
            if (it != null) {
                list.add(new TemplateItem(tid, it));
                ItemMeta meta = it.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String k = it.getType().name() + "|" + ChatColor.stripColor(meta.getDisplayName());
                    BY_MAT_NAME.put(k, new TemplateItem(tid, it));
                }
            }
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
            int explicit = t.getInt("custom_model_data", 0);
            int cmd = CustomModels.register(id, explicit);
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

    /**
     * Attempts to match the given item to a template based on material and
     * display name. If a match is found the template's custom model data is
     * applied to the item.
     *
     * @param item item to update
     * @return true if a template match was applied
     */
    public static boolean applyTemplateMeta(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String key = item.getType().name() + "|" + ChatColor.stripColor(meta.getDisplayName());
        TemplateItem tmpl = BY_MAT_NAME.get(key);
        if (tmpl == null) return false;
        ItemMeta tmeta = tmpl.stack().getItemMeta();
        if (tmeta == null || !tmeta.hasCustomModelData()) return false;
        meta.setCustomModelData(tmeta.getCustomModelData());
        item.setItemMeta(meta);
        return true;
    }
}