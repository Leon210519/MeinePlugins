package com.lootfactory.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {
  @EventHandler
  public void onClick(InventoryClickEvent e){
    InventoryHolder h = e.getView().getTopInventory().getHolder();
    if(h instanceof FactoryView) ((FactoryView)h).onClick(e);
    else if(h instanceof ShopView) ((ShopView)h).onClick(e);
    else if(h instanceof FactoryTypesGUI.TypesView) ((FactoryTypesGUI.TypesView)h).onClick(e);
    else if(h instanceof FactoriesGUI.MenuView) ((FactoriesGUI.MenuView)h).onClick(e);
    else if(h instanceof FactoryListGUI.ListView) ((FactoryListGUI.ListView)h).onClick(e);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e){
    InventoryHolder h = e.getInventory().getHolder();
    if(h instanceof FactoryView) ((FactoryView)h).onClose(e);
    else if(h instanceof ShopView) ((ShopView)h).onClose(e);
    else if(h instanceof FactoryTypesGUI.TypesView) ((FactoryTypesGUI.TypesView)h).onClose(e);
    else if(h instanceof FactoriesGUI.MenuView) ((FactoriesGUI.MenuView)h).onClose(e);
    else if(h instanceof FactoryListGUI.ListView) ((FactoryListGUI.ListView)h).onClose(e);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent e){
    InventoryHolder h = e.getView().getTopInventory().getHolder();
    if(h instanceof FactoryView || h instanceof ShopView ||
       h instanceof FactoryTypesGUI.TypesView || h instanceof FactoriesGUI.MenuView ||
       h instanceof FactoryListGUI.ListView){
      if(e.getRawSlots().stream().anyMatch(slot -> slot < e.getView().getTopInventory().getSize())){
        e.setCancelled(true);
      }
    }
  }
}
