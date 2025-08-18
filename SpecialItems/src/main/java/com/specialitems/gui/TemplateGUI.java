package com.specialitems.gui;

import com.specialitems.util.TemplateItems;
import com.specialitems.util.Configs;
import com.specialitems.util.GuiItemUtil;
import com.specialitems.SpecialItemsPlugin;
import com.specialitems.leveling.Keys;
import com.specialitems.leveling.RarityUtil;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;

public final class TemplateGUI {

    public static final String TITLE = ChatColor.AQUA + "SpecialItems Templates";
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = 45;

    public static void open(Player p, int page) {
        if (!p.hasPermission("specialitems.admin")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', Configs.msg.getString("no-permission","&cNo permission.")));
            return;
        }
        List<TemplateItems.TemplateItem> all = TemplateItems.loadAll();
        Keys keys = new Keys(SpecialItemsPlugin.getInstance());
        all.sort(Comparator
                .comparing((TemplateItems.TemplateItem t) -> RarityUtil.get(t.stack(), keys).ordinal())
                .thenComparing(t -> typeOrder(t.stack().getType())));
        int pages = Math.max(1, (int)Math.ceil(all.size() / (double)PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;
        Inventory inv = Bukkit.createInventory(p, ROWS*9, TITLE + " §7(" + (page+1) + "/" + pages + ")");
        int start = page * PAGE_SIZE;
        int end = Math.min(all.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            ItemStack display = GuiItemUtil.forDisplay(SpecialItemsPlugin.getInstance(), all.get(i).stack());
            if (display == null) display = all.get(i).stack().clone();
            inv.setItem(slot++, display);
        }
        // nav bar
        inv.setItem(45, GuiIcons.navPrev(page > 0));
        inv.setItem(49, GuiIcons.navClose());
        inv.setItem(53, GuiIcons.navNext(page < pages - 1));
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
