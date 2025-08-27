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
    private static List<TemplateItem> ALL = new ArrayList<>();
    private static int SKIPPED_NON_INT = 0;
    private static final Map<Rarity, List<TemplateItem>> BY_RARITY = new EnumMap<>(Rarity.class);

    public static java.util.List<TemplateItem> loadAll() {
        java.util.List<TemplateItem> list = new java.util.ArrayList<>();
        BY_MAT_NAME.clear();
        BY_RARITY.clear();
        SKIPPED_NON_INT = 0;
        var sec = Configs.templates.getConfigurationSection("templates");
        if (sec == null) {
            ALL = list;
            return list;
        }
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
                try {
                    Rarity r = RarityUtil.get(it, new Keys(SpecialItemsPlugin.getInstance()));
                    if (r != null) BY_RARITY.computeIfAbsent(r, k -> new ArrayList<>()).add(tmpl);
                } catch (Throwable ignored) {}
            }
        }
        ALL = list;
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

        Integer cmd = readModelData(id, t);
        if (cmd == null) cmd = computeCmdFallback(mat, t.getString("rarity"));
        if (cmd != null) {
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                // Always write CMD as an integer via the Bukkit API
                it = ItemUtil.forceSetCustomModelDataBoth(it, cmd);
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

    private static Integer readModelData(String id, ConfigurationSection t) {
        if (t == null) return null;

        for (String path : new String[]{"custom_model_data", "model-data", "model_data"}) {
            if (!t.contains(path)) continue;
            Integer v = ItemUtil.readInt(t, path);
            if (v != null) return v;
            SKIPPED_NON_INT++;
            Log.warn("Template '" + id + "' has non-integer CMD at '" + path + "': " + t.get(path));
        }
        return null;
    }

    private static Integer computeCmdFallback(org.bukkit.Material mat, String rarityStr) {
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
        if (ALL.isEmpty()) loadAll();
        return new ArrayList<>(BY_RARITY.getOrDefault(rarity, Collections.emptyList()));
    }

    // --- Additional helpers used by other parts of the plugin ---

    public static List<TemplateItem> getAll() {
        return new ArrayList<>(ALL);
    }

    public static int loadedCount() {
        return ALL.size();
    }

    public static int skippedNonIntCount() {
        return SKIPPED_NON_INT;
    }

    public static Map<Rarity, List<TemplateItem>> byRarity() {
        return BY_RARITY;
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
        // Overwrite any existing CMD, removing legacy floating point values
        meta.setCustomModelData(null);
        item.setItemMeta(meta);
        // Ensure the raw item components/NBT tag are cleared as well
        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            var asNmsCopy = craft.getMethod("asNMSCopy", ItemStack.class);
            Object nms = asNmsCopy.invoke(null, item);
            // Strip the data-component based field introduced in 1.20+
            try {
                Class<?> comps = Class.forName("net.minecraft.world.item.component.DataComponents");
                Object type = comps.getField("CUSTOM_MODEL_DATA").get(null);
                var has = nms.getClass().getMethod("has", type.getClass());
                if ((Boolean) has.invoke(nms, type)) {
                    var remove = nms.getClass().getMethod("remove", type.getClass());
                    remove.invoke(nms, type);
                    var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                    ItemStack cleaned = (ItemStack) asBukkitCopy.invoke(null, nms);
                    item.setItemMeta(cleaned.getItemMeta());
                }
            } catch (Throwable ignored) {}
            // Legacy NBT tag fallback for older items
            var getTag = nms.getClass().getMethod("getTag");
            Object tag = getTag.invoke(nms);
            if (tag != null) {
                var contains = tag.getClass().getMethod("contains", String.class);
                if ((Boolean) contains.invoke(tag, "CustomModelData")) {
                    var remove = tag.getClass().getMethod("remove", String.class);
                    remove.invoke(tag, "CustomModelData");
                    var setTag = nms.getClass().getMethod("setTag", tag.getClass());
                    setTag.invoke(nms, tag);
                    var asBukkitCopy = craft.getMethod("asBukkitCopy", nms.getClass());
                    ItemStack cleaned = (ItemStack) asBukkitCopy.invoke(null, nms);
                    item.setItemMeta(cleaned.getItemMeta());
                }
            }
        } catch (Throwable ignored) {}
        // Finally apply the integer value from the template
        Integer val = (tmeta.hasCustomModelData() ? tmeta.getCustomModelData() : null);
        if (val == null) return false;
        item = ItemUtil.forceSetCustomModelDataBoth(item, val);
        return true;
    }
}
