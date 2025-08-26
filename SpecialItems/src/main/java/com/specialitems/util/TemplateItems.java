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

/** Loader and registries for item templates. */
public final class TemplateItems {

    public record TemplateItem(String id, ItemStack stack, int customModelData, Rarity rarity) {}

    private static final Map<String, TemplateItem> BY_ID = new HashMap<>();
    private static final Map<String, TemplateItem> BY_MAT_CMD = new HashMap<>();
    private static final Map<Rarity, TemplateItem> BY_RARITY = new EnumMap<>(Rarity.class);
    private static final Map<String, TemplateItem> BY_MAT_NAME = new HashMap<>();

    private static int loadedCount = 0;
    private static int skippedNonInt = 0;

    private TemplateItems() {}

    /** Loads all templates from configuration, rebuilding registries. */
    public static void loadAll() {
        BY_ID.clear();
        BY_MAT_CMD.clear();
        BY_RARITY.clear();
        BY_MAT_NAME.clear();
        loadedCount = 0;
        skippedNonInt = 0;

        ConfigurationSection root = Configs.templates.getConfigurationSection("templates");
        if (root == null) return;

        List<String> keys = new ArrayList<>(root.getKeys(false));
        Collections.sort(keys);
        Keys keysHelper = new Keys(SpecialItemsPlugin.getInstance());

        for (String key : keys) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            String id = sec.getString("id", key);

            Material mat = Material.matchMaterial(sec.getString("material", ""));
            if (mat == null) {
                Log.warn("Unknown material at templates." + key + ".material");
                continue;
            }

            Integer cmd = ItemUtil.readInt(sec, "custom_model_data");
            if (cmd == null) {
                skippedNonInt++;
                Log.warn("Invalid non-integer custom_model_data at templates." + key + ".custom_model_data; use plain integer (e.g. 9001). Skipped.");
                continue;
            }

            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("name", "&fSpecial Item")));
                List<String> lore = new ArrayList<>();
                for (String l : sec.getStringList("lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }

            ConfigurationSection ench = sec.getConfigurationSection("enchants");
            if (ench != null) {
                for (String eid : ench.getKeys(false)) {
                    int lvl = ench.getInt(eid, 1);
                    stack = ItemUtil.withEffect(stack, eid, lvl);
                }
            }

            ItemUtil.forceSetCustomModelData(stack, cmd);

            Rarity rarity = null;
            String rarStr = sec.getString("rarity");
            if (rarStr != null) {
                try {
                    rarity = Rarity.fromString(rarStr);
                    RarityUtil.set(stack, keysHelper, rarity);
                } catch (Throwable ignored) {}
            }

            try { Tagger.tagAsSpecial(SpecialItemsPlugin.getInstance(), stack, id); } catch (Throwable ignored) {}

            TemplateItem tmpl = new TemplateItem(id, stack, cmd, rarity);
            BY_ID.put(id, tmpl);
            BY_MAT_CMD.put(mat.name() + "#" + cmd, tmpl);

            ItemMeta metaCheck = stack.getItemMeta();
            if (metaCheck != null && metaCheck.hasDisplayName()) {
                String nameKey = mat.name() + "|" + ChatColor.stripColor(metaCheck.getDisplayName());
                BY_MAT_NAME.put(nameKey, tmpl);
            }

            if (rarity != null) {
                if (!BY_RARITY.containsKey(rarity)) {
                    BY_RARITY.put(rarity, tmpl);
                } else {
                    Log.info("Template '" + id + "' skipped for rarity " + rarity + " (one set per rarity)." );
                }
            }

            loadedCount++;
        }
    }

    /** Builds a TemplateItem from a configuration section without registering it. */
    public static TemplateItem buildFrom(String id, ConfigurationSection sec) {
        if (sec == null) return null;

        Material mat = Material.matchMaterial(sec.getString("material", ""));
        if (mat == null) {
            Log.warn("Unknown material for template " + id);
            return null;
        }

        Integer cmd = ItemUtil.readInt(sec, "custom_model_data");
        if (cmd == null) {
            Log.warn("Invalid non-integer custom_model_data at templates." + id + ".custom_model_data; use plain integer (e.g. 9001). Skipped.");
        }

        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("name", "&fSpecial Item")));
            List<String> lore = new ArrayList<>();
            for (String l : sec.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }

        ConfigurationSection ench = sec.getConfigurationSection("enchants");
        if (ench != null) {
            for (String eid : ench.getKeys(false)) {
                int lvl = ench.getInt(eid, 1);
                stack = ItemUtil.withEffect(stack, eid, lvl);
            }
        }

        if (cmd != null) {
            ItemUtil.forceSetCustomModelData(stack, cmd);
        }

        Rarity rarity = null;
        String rarStr = sec.getString("rarity");
        if (rarStr != null) {
            try {
                rarity = Rarity.fromString(rarStr);
                RarityUtil.set(stack, new Keys(SpecialItemsPlugin.getInstance()), rarity);
            } catch (Throwable ignored) {}
        }

        if (cmd == null) return null;
        try { Tagger.tagAsSpecial(SpecialItemsPlugin.getInstance(), stack, id); } catch (Throwable ignored) {}
        return new TemplateItem(id, stack, cmd, rarity);
    }

    public static Map<Rarity, TemplateItem> byRarity() {
        return Collections.unmodifiableMap(BY_RARITY);
    }

    public static Map<String, TemplateItem> byMatCmd() {
        return Collections.unmodifiableMap(BY_MAT_CMD);
    }

    public static Map<String, TemplateItem> byId() {
        return Collections.unmodifiableMap(BY_ID);
    }

    public static List<TemplateItem> getAll() {
        return new ArrayList<>(BY_ID.values());
    }

    public static int loadedCount() { return loadedCount; }

    public static int skippedNonIntCount() { return skippedNonInt; }

    /** Attempts to match the given item to a template and apply its CMD. */
    public static boolean applyTemplateMeta(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String key = item.getType().name() + "|" + ChatColor.stripColor(meta.getDisplayName());
        TemplateItem tmpl = BY_MAT_NAME.get(key);
        if (tmpl == null) return false;
        ItemUtil.forceSetCustomModelData(item, tmpl.customModelData());
        return true;
    }
}

