package com.specialitems.gui;

import com.specialitems.util.TemplateItems;
import com.specialitems.util.Configs;
import com.specialitems.util.GuiItemUtil;
import com.specialitems.SpecialItemsPlugin;
import com.specialitems.leveling.Keys;
import com.specialitems.leveling.Rarity;
import com.specialitems.leveling.RarityUtil;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class TemplateGUI {

    public static final String TITLE = ChatColor.AQUA + "SpecialItems Templates";
    private static final int ROWS = 6;
    // Inventory has 6 rows: top row used for navigation, the rest for item rows.

    public static void open(Player p, int page) {
        if (!p.hasPermission("specialitems.admin")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', Configs.msg.getString("no-permission","&cNo permission.")));
            return;
        }
        List<TemplateItems.TemplateItem> all = TemplateItems.loadAll();
        Keys keys = new Keys(SpecialItemsPlugin.getInstance());

        // Group template items by rarity so each row can display a full set
        Map<Rarity, List<TemplateItems.TemplateItem>> byRarity = new EnumMap<>(Rarity.class);
        for (TemplateItems.TemplateItem t : all) {
            Rarity r = RarityUtil.get(t.stack(), keys);
            byRarity.computeIfAbsent(r, k -> new ArrayList<>()).add(t);
        }
        // Sort each rarity's items consistently (armor first, then tools)
        for (List<TemplateItems.TemplateItem> list : byRarity.values()) {
            list.sort(Comparator.comparing(t -> typeOrder(t.stack().getType())));
        }

        Rarity[] rarities = Rarity.values();
        int itemRows = ROWS - 1; // top row reserved for navigation
        int pages = Math.max(1, (int) Math.ceil(rarities.length / (double) itemRows));
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;
        Inventory inv = Bukkit.createInventory(p, ROWS * 9,
                TITLE + " §7(" + (page + 1) + "/" + pages + ")");

        int startRarity = page * itemRows;
        int endRarity = Math.min(rarities.length, startRarity + itemRows);
        for (int rIndex = startRarity; rIndex < endRarity; rIndex++) {
            Rarity rarity = rarities[rIndex];
            List<TemplateItems.TemplateItem> items = byRarity.getOrDefault(rarity, Collections.emptyList());

            int localIndex = rIndex - startRarity;
            int rowIndex = ROWS - 1 - localIndex; // bottom row -> first rarity in page
            int rowStart = rowIndex * 9;
            int slot = rowStart;
            for (TemplateItems.TemplateItem t : items) {
                if (slot >= rowStart + 9) break; // leave empty slots if fewer than 9 items
                ItemStack display = GuiItemUtil.forDisplay(SpecialItemsPlugin.getInstance(), t.stack());
                if (display == null) display = t.stack().clone();
                inv.setItem(slot++, display);
            }
        }

        // Navigation bar (top row)
        inv.setItem(0, GuiIcons.navPrev(page > 0));
        inv.setItem(4, GuiIcons.navClose());
        inv.setItem(8, GuiIcons.navNext(page < pages - 1));
        p.openInventory(inv);
    }

    private static int typeOrder(Material m) {
        String name = m.name();
        if (name.endsWith("_HELMET")) return 0;
        if (name.endsWith("_CHESTPLATE")) return 1;
        if (name.endsWith("_LEGGINGS")) return 2;
        if (name.endsWith("_BOOTS")) return 3;
        if (name.endsWith("_SWORD")) return 4;
        if (name.endsWith("_PICKAXE")) return 5;
        if (name.endsWith("_AXE")) return 6;
        if (name.endsWith("_HOE")) return 7;
        return 100;
    }
}
