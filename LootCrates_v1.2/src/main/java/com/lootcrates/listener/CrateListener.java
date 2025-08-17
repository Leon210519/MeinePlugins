package com.lootcrates.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CrateListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e){
        String t = e.getView().getTitle();
        if (t.startsWith("§8Preview:") || t.startsWith("§8Opening:")){
            e.setCancelled(true);
        }
    }
}
