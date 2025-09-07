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
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class TemplateItems {

    public record TemplateItem(String id, ItemStack stack, Integer customModelData) {}

    private TemplateItems() {}

    private static final Map<String, TemplateItem> BY_MAT_NAME = new HashMap<>();
    private static List<TemplateItem> ALL = new ArrayList<>();
    private static int INVALID_CMD = 0;
    private static final Map<Rarity, List<TemplateItem>> BY_RARITY = new EnumMap<>(Rarity.class);
    private static final Map<String, String> IA_ARMOR = Map.of(
            "legendary_chest", "lootforge:omega_chestplate",
            "legendary_helm", "lootforge:omega_helmet",
            "legendary_legs", "lootforge:omega_leggings",
            "legendary_boots", "lootforge:omega_boots",
            "epic_chest", "lootforge:mythic_chestplate",
            "epic_helm", "lootforge:mythic_helmet",
            "epic_legs", "lootforge:mythic_leggings",
            "epic_boots", "lootforge:mythic_boots"
    );

    public static java.util.List<TemplateItem> loadAll() {
        java.util.List<TemplateItem> list = new java.util.ArrayList<>();
        BY_MAT_NAME.clear();
        BY_RARITY.clear();
        INVALID_CMD = 0;
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
                it = ItemUtil.forceSetCustomModelData(it, cmd);
            }
        } else {
            Log.warn("Template '" + id + "' did not provide CustomModelData");
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

        String iaId = IA_ARMOR.get(id);
        if (iaId != null) {
            it = swapToIA(it, iaId);
        }

        return new TemplateItem(id, it, cmd);
    }

    private static ItemStack swapToIA(ItemStack vanilla, String iaId) {
        CustomStack cs = CustomStack.getInstance(iaId);
        if (cs == null) return vanilla;
        ItemStack out = cs.getItemStack().clone();
        ItemMeta dst = out.getItemMeta();
        ItemMeta src = vanilla.getItemMeta();
        dst = copyMeta(src, dst);
        if (src instanceof Damageable s && dst instanceof Damageable d) {
            d.setDamage(s.getDamage());
        }
        if (src != null && src.hasCustomModelData()) {
            try { dst.setCustomModelData(src.getCustomModelData()); } catch (Throwable ignored) {}
        }
        out.setItemMeta(dst);
        out.setAmount(vanilla.getAmount());
        return out;
    }

    private static ItemMeta copyMeta(ItemMeta src, ItemMeta dst) {
        if (src == null || dst == null) return dst;
        if (src.hasDisplayName()) dst.setDisplayName(src.getDisplayName());
        if (src.hasLore()) dst.setLore(src.getLore());
        src.getEnchants().forEach((e, l) -> dst.addEnchant(e, l, true));
        dst.setUnbreakable(src.isUnbreakable());
        for (ItemFlag f : src.getItemFlags()) dst.addItemFlags(f);
        var s = src.getPersistentDataContainer();
        var d = dst.getPersistentDataContainer();
        for (var key : s.getKeys()) {
            if (s.has(key, PersistentDataType.STRING))
                d.set(key, PersistentDataType.STRING, s.get(key, PersistentDataType.STRING));
            else if (s.has(key, PersistentDataType.INTEGER))
                d.set(key, PersistentDataType.INTEGER, s.get(key, PersistentDataType.INTEGER));
            else if (s.has(key, PersistentDataType.LONG))
                d.set(key, PersistentDataType.LONG, s.get(key, PersistentDataType.LONG));
            else if (s.has(key, PersistentDataType.DOUBLE))
                d.set(key, PersistentDataType.DOUBLE, s.get(key, PersistentDataType.DOUBLE));
        }
        return dst;
    }

    private static Integer readModelData(String id, ConfigurationSection t) {
        if (t == null) return null;

        for (String path : new String[]{"custom_model_data", "model-data", "model_data"}) {
            if (!t.contains(path)) continue;
            if (t.isInt(path)) {
                return t.getInt(path);
            }
            INVALID_CMD++;
            Log.warn("Template '" + id + "' has invalid CMD (must be int) at '" + path + "'");
            return null;
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

    public static int normalizedNonIntCount() {
        return INVALID_CMD;
    }

    public static Map<Rarity, List<TemplateItem>> byRarity() {
        return BY_RARITY;
    }

    /**
     * Attempts to match the given item to a template based on material and
     * display name. If a match is found the template's custom model data is
     * applied to the item. If the template lacks a CMD the item is left
     * unchanged and a warning is logged.
     *
     * @param item item to update
     * @return true if a template match was applied
     */
    public static boolean applyTemplateMeta(ItemStack item) {
        TemplateItem tmpl = match(item);
        if (tmpl == null) return false;
        ItemMeta tmeta = tmpl.stack().getItemMeta();
        if (tmeta == null || !tmeta.hasCustomModelData()) {
            Log.warn("Template '" + tmpl.id() + "' did not provide CustomModelData");
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        meta.setCustomModelData(tmeta.getCustomModelData());
        item.setItemMeta(meta);
        return true;
    }

    /**
     * Returns the template matching the item's material and stripped display
     * name without modifying the item.
     */
    public static TemplateItem match(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        String key = item.getType().name() + "|" + ChatColor.stripColor(meta.getDisplayName());
        return BY_MAT_NAME.get(key);
    }

}
