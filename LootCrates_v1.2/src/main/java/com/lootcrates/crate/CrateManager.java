package com.lootcrates.crate;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class CrateManager {
    private final LootCratesPlugin plugin;
    private final Map<String, Crate> crates = new HashMap<>();
    private final Random rng = new Random();

    public CrateManager(LootCratesPlugin plugin){
        this.plugin = plugin;
        reload();
    }

    public void reload(){
        crates.clear();
        plugin.reloadConfig();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("crates");
        if (sec != null){
            for (String id : sec.getKeys(false)){
                Crate c = Crate.fromConfig(id, sec.getConfigurationSection(id));
                crates.put(c.id, c);
            }
        }
    }

    public Set<String> list(){ return crates.keySet(); }
    public Crate get(String id){ return crates.get(id.toUpperCase(Locale.ROOT)); }
    public Random rng(){ return rng; }

    public void giveKey(org.bukkit.entity.Player p, String crateId, int amount){
        Crate c = get(crateId);
        if (c == null){ p.sendMessage("§cUnknown crate: " + crateId); return; }
        org.bukkit.inventory.ItemStack key = c.key.createItem(amount);
        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left = p.getInventory().addItem(key);
        for (org.bukkit.inventory.ItemStack it : left.values()){
            p.getWorld().dropItem(p.getLocation(), it);
        }
        p.sendMessage("§aYou received §e" + amount + "§a " + c.display + " §akeys.");
    }
}
