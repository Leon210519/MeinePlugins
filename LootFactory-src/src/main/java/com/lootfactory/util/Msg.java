package com.lootfactory.util;
import com.lootfactory.LootFactoryPlugin; import org.bukkit.ChatColor;
public class Msg {
  public static String color(String s){ return ChatColor.translateAlternateColorCodes('&', s); }
  public static String prefix(){ String p=LootFactoryPlugin.get().getConfig().getString("messages.prefix", null); if(p!=null) return color(p);
    return color(LootFactoryPlugin.get().getConfig().getString("prefix","&6[&eLootFactory&6]&r ")); }
}
