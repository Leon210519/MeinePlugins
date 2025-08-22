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
    // Inventory has 6 rows: right-most column used for navigation.

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

        List<Rarity> rarityList = new ArrayList<>(Arrays.asList(Rarity.values()));
        rarityList.remove(Rarity.STARFORGED); // top row dedicated to STARFORGED showcase
        int itemRows = ROWS - 1; // one row (top) used for showcase
        int pages = Math.max(1, (int) Math.ceil(rarityList.size() / (double) itemRows));
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;
        Inventory inv = Bukkit.createInventory(p, ROWS * 9,
                TITLE + " §7(" + (page + 1) + "/" + pages + ")");
        // Showcase STARFORGED items in the top row
        List<TemplateItems.TemplateItem> starItems = byRarity.getOrDefault(Rarity.STARFORGED, Collections.emptyList());
        starItems.sort(Comparator.comparing(t -> typeOrder(t.stack().getType())));
        int showcaseSlot = 0;
        for (TemplateItems.TemplateItem t : starItems) {
            if (showcaseSlot >= 8) break; // slot 8 reserved for filler/nav column
            ItemStack display = GuiItemUtil.forDisplay(SpecialItemsPlugin.getInstance(), t.stack());
            if (display == null) display = t.stack().clone();
            inv.setItem(showcaseSlot++, display);
        }
        inv.setItem(8, GuiIcons.navFiller());

        int startRarity = page * itemRows;
        int endRarity = Math.min(rarityList.size(), startRarity + itemRows);
        for (int rIndex = startRarity; rIndex < endRarity; rIndex++) {
            Rarity rarity = rarityList.get(rIndex);
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

        // Navigation column (right side)
        inv.setItem(35, GuiIcons.navNext(page < pages - 1));
        inv.setItem(44, GuiIcons.navPrev(page > 0));
        inv.setItem(53, GuiIcons.navClose());
        inv.setItem(17, GuiIcons.navFiller());
        inv.setItem(26, GuiIcons.navFiller());
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
