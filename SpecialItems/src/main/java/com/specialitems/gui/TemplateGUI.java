package com.specialitems.gui;

import com.specialitems.util.TemplateItems;
import com.specialitems.util.Configs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
        int pages = Math.max(1, (int)Math.ceil(all.size() / (double)PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;
        Inventory inv = Bukkit.createInventory(p, ROWS*9, TITLE + " §7(" + (page+1) + "/" + pages + ")");
        int start = page * PAGE_SIZE;
        int end = Math.min(all.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inv.setItem(slot++, all.get(i).stack().clone());
        }
        // nav bar
        inv.setItem(45, GuiIcons.navPrev(page > 0));
        inv.setItem(49, GuiIcons.navClose());
        inv.setItem(53, GuiIcons.navNext(page < pages - 1));
        p.openInventory(inv);
    }
}
