package com.lootfactory.util;
import org.bukkit.inventory.ItemStack; import org.bukkit.inventory.meta.ItemMeta; import org.bukkit.persistence.PersistentDataContainer; import org.bukkit.persistence.PersistentDataType;
import java.util.function.Consumer;
public class Items {
  public static void editMeta(ItemStack is, Consumer<ItemMeta> editor){ ItemMeta meta=is.getItemMeta(); if(meta==null) return; editor.accept(meta); is.setItemMeta(meta); }
  public static <T,Z> Z getPdc(ItemStack is, org.bukkit.NamespacedKey key, PersistentDataType<T,Z> type){
    if(is==null || !is.hasItemMeta()) return null; PersistentDataContainer pdc=is.getItemMeta().getPersistentDataContainer();
    if(!pdc.has(key,type)) return null; return pdc.get(key,type);
  }
  public static <T,Z> boolean hasPdc(ItemStack is, org.bukkit.NamespacedKey key, PersistentDataType<T,Z> type){
    if(is==null || !is.hasItemMeta()) return false; return is.getItemMeta().getPersistentDataContainer().has(key,type);
  }
}
