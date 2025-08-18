package com.lootfactory.gui;

import com.lootfactory.factory.FactoryDef;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            ItemStack it = new ItemStack(def.material != null ? def.material : Material.CHEST);
            ItemMeta im = it.getItemMeta();
            String name = (def.display != null && !def.display.isEmpty()) ? def.display : def.id;
            im.setDisplayName(Msg.color("&e" + name + " &7[" + ChatColor.GRAY + def.rarity.name() + "&7]"));
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7Type: &f" + def.id));
            lore.add(Msg.color("&7Rarity: &f" + def.rarity.name()));
            lore.add(Msg.color("&7Base: &f" + def.baseAmount + " &7per &f" + def.baseIntervalSec + "s"));
            if (def.yieldBonusPct != 0 || def.speedBonusPct != 0){
                lore.add(Msg.color("&7Perks: &a+" + (int)def.yieldBonusPct + "% Yield &7/ &a+" + (int)def.speedBonusPct + "% Speed"));
            }
            im.setLore(lore);
            im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            it.setItemMeta(im);
            inv.setItem(slot++, it);
        }
    }

    /** InventoryHolder for the types view (read-only). */
    public static class TypesView implements InventoryHolder {
        private final FactoryManager manager;
        public TypesView(FactoryManager manager){ this.manager = manager; }
        @Override public Inventory getInventory(){ return Bukkit.createInventory(this, 54); }
        public void onClick(InventoryClickEvent e){
            // Make it strictly read-only in the top inventory
            if (e.getClickedInventory() != null && e.getView().getTopInventory().equals(e.getClickedInventory())){
                e.setCancelled(true);
            }
        }
        public void onClose(InventoryCloseEvent e){
            // nothing for now
        }
    }
}
