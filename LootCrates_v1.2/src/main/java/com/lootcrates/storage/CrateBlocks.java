package com.lootcrates.storage;

import com.lootcrates.LootCratesPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CrateBlocks {
    private final LootCratesPlugin plugin;
    private final File file;
    private final YamlConfiguration conf;

    private final Map<String, String> map = new HashMap<>();

    public CrateBlocks(LootCratesPlugin plugin){
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
        this.conf = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private String key(Location l){
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }

    public void bind(Location l, String crateId){
        map.put(key(l), crateId.toUpperCase(java.util.Locale.ROOT));
        save();
    }

    public void unbind(Location l){
        map.remove(key(l));
        save();
    }

    public String get(Location l){
        return map.get(key(l));
    }

    private void load(){
        map.clear();
        if (conf.isConfigurationSection("blocks")){
            for (String k: conf.getConfigurationSection("blocks").getKeys(false)){
                String crate = conf.getString("blocks."+k);
                map.put(k, crate);
            }
        }
    }

    private void save(){
        conf.set("blocks", null);
        for (Map.Entry<String, String> e: map.entrySet()){
            conf.set("blocks."+e.getKey(), e.getValue());
        }
        try { conf.save(file); } catch (IOException ignored){}
    }
}
