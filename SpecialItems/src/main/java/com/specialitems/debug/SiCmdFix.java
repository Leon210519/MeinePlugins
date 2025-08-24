package com.specialitems.debug;
import org.bukkit.command.*; import org.bukkit.entity.Player;
import org.bukkit.inventory.*; import org.bukkit.inventory.meta.ItemMeta; import java.util.Map; import com.specialitems.util.TemplateItems;
public final class SiCmdFix implements CommandExecutor{
  @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a){
    if(!(s instanceof Player p)){ s.sendMessage("Player only"); return true; }
    ItemStack it=p.getInventory().getItemInMainHand();
    if(it==null||it.getType().isAir()){ p.sendMessage("Hold an item."); return true; }
    ItemMeta m=it.getItemMeta(); Map<String,Object> metaMap=(m!=null?m.serialize():null); Object raw=(metaMap!=null?metaMap.get("custom-model-data"):null);
    if(m!=null&&raw instanceof Number){ int v=((Number)raw).intValue(); m.setCustomModelData(null); m.setCustomModelData(v); it.setItemMeta(m); p.getInventory().setItemInMainHand(it); p.sendMessage("CustomModelData normalized to integer: "+v); }
    else if(TemplateItems.applyTemplateMeta(it)){ p.getInventory().setItemInMainHand(it); p.sendMessage("CustomModelData applied from template."); }
    else p.sendMessage("No CMD on this item.");
    return true;
  }
}
