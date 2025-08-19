package com.lootfactory.gui;

import com.lootfactory.factory.FactoryDef;
import com.lootfactory.factory.FactoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only GUI that lists all available factory types.
 */
public class FactoryTypesGUI {

    public static void open(Player p, FactoryManager manager){
        String title = ChatColor.AQUA + "Factory Types";
        Inventory inv = Bukkit.createInventory(new TypesView(manager), 54, title);
        render(inv, manager);
        p.openInventory(inv);
    }

    private static void render(Inventory inv, FactoryManager manager){
        inv.clear();
        // Sort by rarity weight (ordinal) then display/name
        List<FactoryDef> defs = new ArrayList<>(manager.getAllDefs());
        defs.sort(Comparator
                .comparing((FactoryDef d) -> d.rarity.ordinal())
                .thenComparing(d -> d.display != null ? d.display : d.id, String.CASE_INSENSITIVE_ORDER));

        int slot = 0;
        for (FactoryDef def : defs){
            if (slot >= inv.getSize()) break;
            ItemStack it = manager.createFactoryItem(def.id, 1, 0d, 0);
            inv.setItem(slot++, it);
        }
    }

    /** InventoryHolder for the types view (read-only). */
    public static class TypesView implements InventoryHolder {
        private final FactoryManager manager;
        public TypesView(FactoryManager manager){ this.manager = manager; }
        @Override public Inventory getInventory(){ return Bukkit.createInventory(this, 54); }
        public void onClick(InventoryClickEvent e){
            if (e.getView().getTopInventory().equals(e.getClickedInventory())
                    || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
            }
        }
        public void onClose(InventoryCloseEvent e){
            // nothing for now
        }
    }
}
