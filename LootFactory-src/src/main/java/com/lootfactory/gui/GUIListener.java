package com.lootfactory.gui;
import org.bukkit.event.*; import org.bukkit.event.inventory.*;
public class GUIListener implements Listener {
  @EventHandler public void onClick(InventoryClickEvent e){ if(e.getView().getTopInventory().getHolder() instanceof FactoryView) ((FactoryView)e.getView().getTopInventory().getHolder()).onClick(e);
    else if(e.getView().getTopInventory().getHolder() instanceof ShopView) ((ShopView)e.getView().getTopInventory().getHolder()).onClick(e); }
  @EventHandler public void onClose(InventoryCloseEvent e){ if(e.getInventory().getHolder() instanceof FactoryView) ((FactoryView)e.getInventory().getHolder()).onClose(e);
    else if(e.getInventory().getHolder() instanceof ShopView) ((ShopView)e.getInventory().getHolder()).onClose(e);
    else if(e.getInventory().getHolder() instanceof FactoryTypesGUI.TypesView) ((FactoryTypesGUI.TypesView)e.getInventory().getHolder()).onClose(e); }
}
