package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.CustomEffect;
import com.specialitems.effects.Effects;
import com.specialitems.leveling.Keys;
import com.specialitems.leveling.Rarity;
import com.specialitems.leveling.RarityUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/** Utility to build GUI display items for special items. */
public final class GuiItemUtil {
    private GuiItemUtil() {}

    private static boolean hasSpecialPdc(ItemStack it) {
        if (it == null) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (pdc == null) return false;
        for (NamespacedKey key : pdc.getKeys()) {
            String k = key.getKey();
            if (k != null && k.startsWith("ench_")) return true;
        }
        return false;
    }

    private static String prettyVanilla(Enchantment e) {
        String key = e.getKey().getKey().replace('_', ' ');
        if (key.isEmpty()) return "Unknown";
        return key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
    }

    /**
     * Build a display item suitable for GUI views. Returns {@code null} if the item is not a special item.
     */
    public static ItemStack forDisplay(SpecialItemsPlugin plugin, ItemStack it) {
        if (plugin == null || it == null || it.getType() == Material.AIR) return null;
        var svc = plugin.leveling();
        boolean special = svc.isSpecialItem(it) || hasSpecialPdc(it);
        if (!special) return null;

        Rarity rarity = RarityUtil.get(it, new Keys(plugin));

        ItemStack display = it.clone();
        com.specialitems.util.LoreRenderer.updateItemLore(display);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            try { meta.removeItemFlags(ItemFlag.values()); } catch (Throwable ignored) {}
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();

            String harvest = null;
            for (int i = 0; i < lore.size(); i++) {
                String s = ChatColor.stripColor(lore.get(i));
                if (s.startsWith("Harvest Bonus:")) {
                    harvest = lore.remove(i);
                    break;
                }
            }

            lore.add(ChatColor.GOLD + "Rarity: " + rarity.displayName());
            if (harvest != null) lore.add(harvest);

            Map<Enchantment, Integer> enchants = it.getEnchantments();
            if (!enchants.isEmpty()) {
                lore.add(ChatColor.GRAY + " ");
                lore.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Vanilla Enchants:");
                for (Map.Entry<Enchantment, Integer> en : enchants.entrySet()) {
                    lore.add(ChatColor.BLUE + " - " + prettyVanilla(en.getKey()) + " " + ItemUtil.roman(en.getValue()));
                }
            }

            List<String> specialLines = new ArrayList<>();
            for (String id : Effects.ids()) {
                int elv = 0;
                try { elv = ItemUtil.getEffectLevel(it, id); } catch (Throwable ignored) {}
                if (elv > 0) {
                    CustomEffect ce = Effects.get(id);
                    String name = (ce != null ? ce.displayName() : id);
                    if (!name.isEmpty()) name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
                    specialLines.add(ChatColor.LIGHT_PURPLE + " - " + name + " " + ItemUtil.roman(elv));
                }
            }

            if (specialLines.isEmpty()) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc != null) {
                    for (NamespacedKey key : pdc.getKeys()) {
                        String k = key.getKey();
                        if (k != null && k.startsWith("ench_")) {
                            String effectId = k.substring("ench_".length());
                            Integer lv = pdc.get(key, PersistentDataType.INTEGER);
                            int effLevel = (lv == null ? 0 : lv);
                            if (effLevel > 0) {
                                CustomEffect ce = Effects.get(effectId);
                                String name = (ce != null ? ce.displayName() : effectId);
                                if (!name.isEmpty()) name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
                                specialLines.add(ChatColor.LIGHT_PURPLE + " - " + name + " " + ItemUtil.roman(effLevel));
                            }
                        }
                    }
                }
            }

            if (!specialLines.isEmpty()) {
                lore.add(ChatColor.GRAY + " ");
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Special Enchants:");
                lore.addAll(specialLines);
            }

            lore.add(ChatColor.GRAY + " ");
            lore.add(ChatColor.GOLD + "Unbreakable");
            meta.setLore(lore);
            Integer cmd = ItemUtil.getCustomModelData(it);
            if (cmd != null) {
                meta.setCustomModelData(cmd);
            }
            display.setItemMeta(meta);
        }
        return display;
    }
}
