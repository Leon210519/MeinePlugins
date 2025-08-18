package com.lootcrates.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.lootcrates.command.GUI;

public class CrateListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e){
        String t = e.getView().getTitle();
        if (t.startsWith("§8Preview:") || t.startsWith("§8Opening:")){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        String t = e.getView().getTitle();
        if (t.startsWith("§8Opening:")){
            GUI.handleCloseDuringRoll((Player) e.getPlayer());
        }
    }
}
