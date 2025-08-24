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

    public record TemplateItem(String id, ItemStack stack, Integer customModelData) {}

    private TemplateItems() {}

    private static final Map<String, TemplateItem> BY_MAT_NAME = new HashMap<>();

    public static java.util.List<TemplateItem> loadAll() {
        java.util.List<TemplateItem> list = new java.util.ArrayList<>();
        BY_MAT_NAME.clear();
        var sec = Configs.templates.getConfigurationSection("templates");
        if (sec == null) return list;
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        Collections.sort(keys);
        for (String key : keys) {
            ConfigurationSection tsec = sec.getConfigurationSection(key);
            String tid = (tsec == null ? key : tsec.getString("id", key));
            TemplateItem tmpl = buildFrom(tid, tsec);
            if (tmpl != null) {
                list.add(tmpl);
                ItemStack it = tmpl.stack();
                ItemMeta meta = it.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String k = it.getType().name() + "|" + ChatColor.stripColor(meta.getDisplayName());
                    BY_MAT_NAME.put(k, tmpl);
                }
            }
        }
        return list;
    }

    public static TemplateItem buildFrom(String id, ConfigurationSection t) {
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

        Integer cmd = readCmd(t);
        if (cmd == null) {
            cmd = computeCmdFallback(mat, t.getString("rarity"));
        }
        if (cmd != null) {
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                m.setCustomModelData(null); // normalize any previous wrong type
                m.setCustomModelData(cmd);  // REQUIRED for resource-pack overrides
                it.setItemMeta(m);
            }
        }

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

        return new TemplateItem(id, it, cmd);
    }

    private static Integer readCmd(ConfigurationSection t) {
        if (t == null) return null;
        if (t.isInt("custom_model_data")) return t.getInt("custom_model_data");
        if (t.isInt("model-data")) return t.getInt("model-data");
        if (t.isInt("model_data")) return t.getInt("model_data");
        return null;
    }

    private static Integer computeCmdFallback(Material mat, String rarityStr) {
        if (mat == null || rarityStr == null) return null;
        String m = mat.name();
        int base;
        if (m.endsWith("_SWORD")) base = 1000;
        else if (m.endsWith("_PICKAXE")) base = 1100;
        else if (m.endsWith("_HOE")) base = 1200;
        else if (m.endsWith("_AXE")) base = 1300;
        else if (m.endsWith("_HELMET")) base = 2000;
        else if (m.endsWith("_CHESTPLATE")) base = 2100;
        else if (m.endsWith("_LEGGINGS")) base = 2200;
        else if (m.endsWith("_BOOTS")) base = 2300;
        else return null;

        String r = rarityStr.toUpperCase(java.util.Locale.ROOT);
        int off =
            r.equals("COMMON") ? 1 :
            r.equals("UNCOMMON") ? 2 :
            r.equals("RARE") ? 3 :
            r.equals("EPIC") ? 4 :
            r.equals("LEGENDARY") ? 5 :
            r.equals("STARFORGED") ? 6 : 0;

        return (off == 0 ? null : base + off);
    }

    public static List<TemplateItem> getByRarity(Rarity rarity) {
        List<TemplateItem> result = new ArrayList<>();
        for (TemplateItem t : loadAll()) {
            Rarity r = RarityUtil.get(t.stack(), new Keys(SpecialItemsPlugin.getInstance()));
            if (r == rarity) result.add(t);
        }
        return result;
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