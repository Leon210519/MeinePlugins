package com.lootcrates.crate;

import com.specialitems.util.Configs;
import com.specialitems.util.TemplateItems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.lootcrates.util.Color.cc;

public class Reward {
    public enum Type { MONEY_XP, ITEM, COMMAND, KEY, SPECIAL_ITEM }
    public final String id;
    public final int weight;
    public final Type type;

    // Data
    public double money;
    public int xp;
    public ItemStack item;
    public int itemAmount = 1;
    public List<String> commands;
    public String keyCrate;
    public int keyAmount = 1;

    // Display for GUI
    public ItemStack display;

    public Reward(String id, int weight, Type type){ this.id=id; this.weight=weight; this.type=type; }

    public static Reward fromConfig(ConfigurationSection sec){
        String id = sec.getString("id", "unknown");
        int weight = sec.getInt("weight", 1);
        Type type = Type.valueOf(sec.getString("type", "ITEM"));
        Reward r = new Reward(id, weight, type);

        if (type == Type.MONEY_XP){
            r.money = sec.getDouble("money", 0.0);
            r.xp = sec.getInt("xp", 0);
        } else if (type == Type.ITEM){
            r.item = readItem(sec.getConfigurationSection("item"));
            r.itemAmount = sec.getInt("amount", 1);
            if (r.item != null) r.item.setAmount(Math.max(1, r.itemAmount));
        } else if (type == Type.COMMAND){
            r.commands = sec.getStringList("commands");
        } else if (type == Type.KEY){
            ConfigurationSection k = sec.getConfigurationSection("key");
            if (k != null){
                r.keyCrate = k.getString("crate");
                r.keyAmount = k.getInt("amount", 1);
            }
        } else if (type == Type.SPECIAL_ITEM) {
            String tid = sec.getString("template");
            r.item = TemplateItems.buildFrom(tid, Configs.templates.getConfigurationSection("templates." + tid));
            r.itemAmount = sec.getInt("amount", 1);
            if (r.item != null) r.item.setAmount(Math.max(1, r.itemAmount));
            r.display = r.item != null ? r.item.clone() : new ItemStack(Material.PAPER);
        }

        if (type != Type.SPECIAL_ITEM) {
            ConfigurationSection d = sec.getConfigurationSection("display");
            r.display = d != null ? readItem(d) : new ItemStack(Material.PAPER);
        }
        return r;
    }

    public static ItemStack readItem(ConfigurationSection sec){
        if (sec == null) return new ItemStack(Material.PAPER);
        Material mat = Material.matchMaterial(sec.getString("material", "PAPER"));
        ItemStack it = new ItemStack(Objects.requireNonNullElse(mat, Material.PAPER));
        ItemMeta meta = it.getItemMeta();
        if (meta != null){
            if (sec.isString("name")) meta.setDisplayName(cc(sec.getString("name")));
            if (sec.isList("lore")){
                List<String> lore = new ArrayList<>();
                for (String line: sec.getStringList("lore")) lore.add(cc(line));
                meta.setLore(lore);
            }
            if (sec.isInt("custom_model_data")) meta.setCustomModelData(sec.getInt("custom_model_data"));
            if (sec.isList("enchantments")){
                for (String s: sec.getStringList("enchantments")){
                    String[] p = s.split(":");
                    try {
                        meta.addEnchant(org.bukkit.enchantments.Enchantment.getByName(p[0]), Integer.parseInt(p[1]), true);
                    } catch (Exception ignored){}
                }
            }
            if (sec.isList("flags")){
                for (String f: sec.getStringList("flags")){
                    try { meta.addItemFlags(ItemFlag.valueOf(f)); } catch (Exception ignored){}
                }
            }
            it.setItemMeta(meta);
        }
        return it;
    }
}
