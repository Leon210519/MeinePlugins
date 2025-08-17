package com.specialitems.gui;

import com.specialitems.bin.Bin;
import com.specialitems.util.Configs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BinGUI {
    public static final String TITLE = ChatColor.DARK_RED + "Special Item Bin";
    private static final int SIZE = 6 * 9;

    private BinGUI() {}

    public static void open(Player p) {
        if (!p.hasPermission("specialitems.admin")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', Configs.msg.getString("no-permission","&cNo permission.")));
            return;
        }
        Inventory inv = Bukkit.createInventory(p, SIZE, TITLE);
        List<ItemStack> items = Bin.getItems();
        int i = 0;
        for (ItemStack it : items) {
            if (i >= SIZE - 1) break;
            inv.setItem(i++, it.clone());
        }
        inv.setItem(SIZE - 1, GuiIcons.navClose());
        p.openInventory(inv);
    }
}
