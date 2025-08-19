package com.lootfactory.gui;

import com.lootfactory.factory.FactoryInstance;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI listing a player's active factories.
 */
public class FactoryListGUI {

    public static void open(Player p, FactoryManager manager, UUID owner){
        Inventory inv = Bukkit.createInventory(new ListView(manager, owner), 54, ChatColor.GREEN + "Your Factories");
        render(inv, manager, owner);
        p.openInventory(inv);
    }

    public static void open(Player p, FactoryManager manager){
        open(p, manager, p.getUniqueId());
    }

    private static void render(Inventory inv, FactoryManager manager, UUID owner){
        inv.clear();
        int slot = 0;
        List<FactoryInstance> list = new ArrayList<>(manager.getFactories(owner));
        for(FactoryInstance fi : list){
            if(slot >= inv.getSize() - 1) break;
            ItemStack it = manager.createFactoryItem(fi.typeId, fi.level, fi.xpSeconds, fi.prestige);
            inv.setItem(slot++, it);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName(Msg.color("&cBack"));
        back.setItemMeta(bm);
        inv.setItem(inv.getSize() - 1, back);
    }

    public static class ListView implements InventoryHolder {
        private final FactoryManager manager;
        private final List<FactoryInstance> list;
        public ListView(FactoryManager manager, UUID owner){
            this.manager = manager;
            this.list = new ArrayList<>(manager.getFactories(owner));
        }
        @Override public Inventory getInventory(){ return Bukkit.createInventory(this, 54); }
        public void onClick(InventoryClickEvent e){
            if(e.getView().getTopInventory().equals(e.getClickedInventory())
                    || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || e.getAction() == InventoryAction.HOTBAR_SWAP
                    || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
                    || e.getAction() == InventoryAction.COLLECT_TO_CURSOR){
                e.setCancelled(true);
                if(e.getWhoClicked() instanceof Player){
                    Player p = (Player)e.getWhoClicked();
                    int slot = e.getRawSlot();
                    int last = e.getView().getTopInventory().getSize() - 1;
                    if(slot == last){
                        FactoriesGUI.open(p, manager);
                    } else if(slot >= 0 && slot < list.size()){
                        FactoryInstance fi = list.get(slot);
                        FactoryGUI.open(p, manager, fi);
                    }
                }
            }
        }
        public void onClose(InventoryCloseEvent e){ }
    }
}
