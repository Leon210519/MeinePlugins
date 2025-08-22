package com.specialitems.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

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
