package com.lootfactory.gui;

import com.lootfactory.factory.FactoryDef;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.factory.FactoryRarity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

        Map<FactoryRarity, List<FactoryDef>> grouped = new EnumMap<>(FactoryRarity.class);
        for (FactoryDef def : manager.getAllDefs()) {
            grouped.computeIfAbsent(def.rarity, r -> new ArrayList<>()).add(def);
        }

        for (FactoryRarity r : FactoryRarity.values()) {
            List<FactoryDef> list = grouped.getOrDefault(r, Collections.emptyList());
            list.sort(Comparator.comparing(d -> d.display != null ? d.display : d.id, String.CASE_INSENSITIVE_ORDER));

            int start = r.ordinal() * 9 + (9 - list.size()) / 2;
            for (FactoryDef def : list) {
                if (start >= (r.ordinal() + 1) * 9) break;
                ItemStack it = manager.createFactoryItem(def.id, 1, 0d, 0);
                ItemMeta meta = it.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Type:"));
                    meta.setLore(lore);
                    it.setItemMeta(meta);
                }
                inv.setItem(start++, it);
            }
        }
    }

    /** InventoryHolder for the types view (read-only). */
    public static class TypesView implements InventoryHolder {
        private final FactoryManager manager;
        public TypesView(FactoryManager manager){ this.manager = manager; }
        @Override public Inventory getInventory(){ return Bukkit.createInventory(this, 54); }
        public void onClick(InventoryClickEvent e){
            if (e.getView().getTopInventory().equals(e.getClickedInventory())
                    || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || e.getAction() == InventoryAction.HOTBAR_SWAP
                    || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
                    || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
            }
        }
        public void onClose(InventoryCloseEvent e){
            // nothing for now
        }
    }
}
