package com.lootfactory.gui;

import com.lootfactory.factory.FactoryManager;
import com.lootfactory.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Main menu for factory-related features.
 */
public class FactoriesGUI {

    public static void open(Player p, FactoryManager manager){
        Inventory inv = Bukkit.createInventory(new MenuView(manager), 27, ChatColor.BLUE + "Factories");
        render(inv);
        p.openInventory(inv);
    }

    private static void render(Inventory inv){
        inv.clear();

        ItemStack shop = new ItemStack(Material.EMERALD);
        ItemMeta sm = shop.getItemMeta();
        sm.setDisplayName(Msg.color("&aFactory Shop"));
        sm.setLore(Arrays.asList(Msg.color("&7Buy new factories")));
        shop.setItemMeta(sm);
        inv.setItem(11, shop);

        ItemStack types = new ItemStack(Material.CHEST);
        ItemMeta tm = types.getItemMeta();
        tm.setDisplayName(Msg.color("&aAll Factory Types"));
        tm.setLore(Arrays.asList(Msg.color("&7View all available factories")));
        types.setItemMeta(tm);
        inv.setItem(13, types);

        ItemStack mine = new ItemStack(Material.FURNACE);
        ItemMeta mm = mine.getItemMeta();
        mm.setDisplayName(Msg.color("&aYour Factories"));
        mm.setLore(Arrays.asList(Msg.color("&7View your active factories")));
        mine.setItemMeta(mm);
        inv.setItem(15, mine);
    }

    public static class MenuView implements InventoryHolder {
        private final FactoryManager manager;
        public MenuView(FactoryManager manager){ this.manager = manager; }
        @Override public Inventory getInventory(){ return Bukkit.createInventory(this, 27); }
        public void onClick(InventoryClickEvent e){
            e.setCancelled(true);
            if(!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player)e.getWhoClicked();
            int slot = e.getRawSlot();
            if(slot == 11){
                ShopGUI.open(p, manager);
            } else if(slot == 13){
                FactoryTypesGUI.open(p, manager);
            } else if(slot == 15){
                FactoryListGUI.open(p, manager);
            }
        }
        public void onClose(InventoryCloseEvent e){ }
    }
}
