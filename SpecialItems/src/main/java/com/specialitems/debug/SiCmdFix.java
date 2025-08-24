package com.specialitems.debug;
import org.bukkit.command.*; import org.bukkit.entity.Player;
import org.bukkit.inventory.*; import org.bukkit.inventory.meta.ItemMeta;
public final class SiCmdFix implements CommandExecutor{
  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a){
    if(!(s instanceof Player p)){ s.sendMessage("Player only"); return true; }
    ItemStack it=p.getInventory().getItemInMainHand();
    if(it==null||it.getType().isAir()){ p.sendMessage("Hold an item."); return true; }
    ItemMeta m=it.getItemMeta(); Integer v=(m!=null?m.getCustomModelData():null);
    if(m!=null&&v!=null){ m.setCustomModelData(null); m.setCustomModelData(v); it.setItemMeta(m); p.sendMessage("CustomModelData normalized to integer: "+v); }
    else p.sendMessage("No CMD on this item.");
    return true;
  }
}
