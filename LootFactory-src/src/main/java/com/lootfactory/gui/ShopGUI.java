package com.lootfactory.gui;  
import com.lootfactory.LootFactoryPlugin; 
import com.lootfactory.factory.FactoryDef; 
import com.lootfactory.factory.FactoryManager; 
import com.lootfactory.util.Msg;
import org.bukkit.Bukkit; 
import org.bukkit.ChatColor; 
import org.bukkit.Material; 
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*; 
import org.bukkit.inventory.*; 
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class ShopGUI {
  public static void open(Player p, FactoryManager manager){
    Inventory inv = Bukkit.createInventory(new ShopView(manager), 27, ChatColor.DARK_PURPLE + "Factory Shop");
    render(inv, manager, p);
    p.openInventory(inv);
  }

  private static void render(Inventory inv, FactoryManager manager, Player viewer){
    inv.clear();

    double price = LootFactoryPlugin.get().getConfig().getDouble("shop.price", 2000d);
    String cur = LootFactoryPlugin.get().getConfig().getString("economy.currency_symbol", "$");

    // Buy 1 (slot 13)
    ItemStack buy1 = new ItemStack(Material.CHEST);
    ItemMeta m1 = buy1.getItemMeta();
    m1.setDisplayName(Msg.color("&aBuy Random Factory"));
    List<String> lore1 = new ArrayList<>();
    lore1.add(Msg.color("&7Price: &e" + cur + String.format("%.2f", price)));
    lore1.add(Msg.color("&8Legendary factories are very rare! &dInsane &8factories are extremely rare!"));
    m1.setLore(lore1);
    buy1.setItemMeta(m1);
    inv.setItem(13, buy1);

    // Buy 10 (slot 15)
    ItemStack buy10 = new ItemStack(Material.SHULKER_BOX);
    ItemMeta m10 = buy10.getItemMeta();
    m10.setDisplayName(Msg.color("&aBuy &e10 &aRandom Factories"));
    List<String> lore10 = new ArrayList<>();
    lore10.add(Msg.color("&7Price: &e" + cur + String.format("%.2f", price * 10)));
    lore10.add(Msg.color("&8Buys ten factories at 10x the single price."));
    m10.setLore(lore10);
    buy10.setItemMeta(m10);
    inv.setItem(15, buy10);

    // Back (slot 26)
    ItemStack back = new ItemStack(Material.ARROW);
    ItemMeta bm = back.getItemMeta();
    bm.setDisplayName(Msg.color("&cBack"));
    back.setItemMeta(bm);
    inv.setItem(26, back);
  }
}

class ShopView implements InventoryHolder {
  private final FactoryManager manager;
  public ShopView(FactoryManager manager){ this.manager = manager; }

  @Override public Inventory getInventory(){ return null; }

  public void onClick(InventoryClickEvent e){
    e.setCancelled(true);
    if(!(e.getWhoClicked() instanceof Player)) return;
    Player p = (Player)e.getWhoClicked();

    double price = LootFactoryPlugin.get().getConfig().getDouble("shop.price", 2000d);
    String cur = LootFactoryPlugin.get().getConfig().getString("economy.currency_symbol", "$");

    // Buy 1
    if(e.getSlot() == 13){
      if(!LootFactoryPlugin.get().eco().has(price, p)){
        p.sendMessage(Msg.prefix() + Msg.color("&cYou don't have enough money."));
        return;
      }
      LootFactoryPlugin.get().eco().withdraw(p, price);
      String id = manager.randomFactoryId();
      // Use prestige-aware creation: prestige fixed to 0 for shop items
      ItemStack item = manager.createFactoryItem(id, 1, 0d, 0);
      Map<Integer, ItemStack> left = p.getInventory().addItem(item);
      if(!left.isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), item);
      FactoryDef def = manager.getDef(id);
      p.sendMessage(Msg.prefix() + Msg.color("&aYou bought a random factory: &e" + (def != null ? def.display : id)));
      return; // keep GUI open
    }

    // Buy 10
    if(e.getSlot() == 15){
      double total = price * 10d;
      if(!LootFactoryPlugin.get().eco().has(total, p)){
        p.sendMessage(Msg.prefix() + Msg.color("&cYou need &e" + cur + String.format("%.2f", total) + " &cfor 10 factories."));
        return;
      }
      LootFactoryPlugin.get().eco().withdraw(p, total);

      for(int i = 0; i < 10; i++){
        String id = manager.randomFactoryId();
        ItemStack item = manager.createFactoryItem(id, 1, 0d, 0); // prestige 0
        Map<Integer, ItemStack> left = p.getInventory().addItem(item);
        if(!left.isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), item);
      }
      p.sendMessage(Msg.prefix() + Msg.color("&aYou bought &e10 &afactories for &e" + cur + String.format("%.2f", total) + "&a."));
      return; // keep GUI open
    }

    // Back button
    if(e.getSlot() == 26){
      FactoriesGUI.open(p, manager);
    }
  }

  public void onClose(InventoryCloseEvent e) {}
}
