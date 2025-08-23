package com.specialitems.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.specialitems.leveling.Rarity;

public final class GuiIcons {
    public static ItemStack navPrev(boolean enabled) {
        return simple(Material.ARROW, enabled ? ChatColor.YELLOW + "Previous Page" : ChatColor.DARK_GRAY + "Previous Page", null);
        }
    public static ItemStack navNext(boolean enabled) {
        return simple(Material.ARROW, enabled ? ChatColor.YELLOW + "Next Page" : ChatColor.DARK_GRAY + "Next Page", null);
        }
    public static ItemStack navClose() {
        return simple(Material.BARRIER, ChatColor.RED + "Close", null);
    }
    public static ItemStack navFiller() {
        return simple(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "", null);
    }

    private static final Map<Rarity, ItemStack> RARITY_ICONS = new EnumMap<>(Rarity.class);
    static {
        RARITY_ICONS.put(Rarity.COMMON, simple(Material.GRAY_DYE, ChatColor.GRAY + "Common", null));
        RARITY_ICONS.put(Rarity.UNCOMMON, simple(Material.GREEN_DYE, ChatColor.GREEN + "Uncommon", null));
        RARITY_ICONS.put(Rarity.RARE, simple(Material.LAPIS_LAZULI, ChatColor.BLUE + "Rare", null));
        RARITY_ICONS.put(Rarity.EPIC, simple(Material.PURPLE_DYE, ChatColor.LIGHT_PURPLE + "Epic", null));
        RARITY_ICONS.put(Rarity.LEGENDARY, simple(Material.GOLD_INGOT, ChatColor.GOLD + "Legendary", null));
        RARITY_ICONS.put(Rarity.STARFORGED, simple(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "StarForged", null));
    }

    public static ItemStack rarity(Rarity r) {
        ItemStack base = RARITY_ICONS.get(r);
        return base == null ? null : base.clone();
    }
    public static ItemStack simple(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null) m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }
}
