package com.lootcrates.crate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.lootcrates.util.Color.cc;

public class Crate {
    public final String id;
    public final String display;
    public final KeyDef key;
    public final String openMethod; // GUI or BLOCK
    public final List<Reward> rewards = new ArrayList<>();

    public Crate(String id, String display, KeyDef key, String openMethod){
        this.id=id; this.display=display; this.key=key; this.openMethod=openMethod;
    }

    public static class KeyDef {
        public final String display;
        public final Material material;
        public final Integer cmd; // CustomModelData

        public KeyDef(String display, Material material, Integer cmd){
            this.display=display; this.material=material; this.cmd=cmd;
        }

        public ItemStack createItem(int amount){
            ItemStack it = new ItemStack(material, amount);
            ItemMeta m = it.getItemMeta();
            if (m != null){
                m.setDisplayName(cc(display));
                if (cmd != null) m.setCustomModelData(cmd);
                it.setItemMeta(m);
            }
            return it;
        }
    }

    @SuppressWarnings("unchecked")
    public static Crate fromConfig(String id, ConfigurationSection sec){
        String display = sec.getString("display", id);
        ConfigurationSection key = sec.getConfigurationSection("key");
        KeyDef kd = new KeyDef(
                key.getString("display", "&eKey"),
                org.bukkit.Material.matchMaterial(key.getString("material", "TRIPWIRE_HOOK")),
                key.isInt("custom_model_data") ? key.getInt("custom_model_data") : null
        );
        String method = sec.getString("open-method", "BLOCK");
        Crate c = new Crate(id.toUpperCase(Locale.ROOT), display, kd, method);
        if (sec.isList("rewards")){
            List<Map<?, ?>> list = sec.getMapList("rewards");
            for (Map<?, ?> map : list){
                try {
                    MemoryConfiguration tmp = new MemoryConfiguration();
                    ConfigurationSection rSec = tmp.createSection("reward", map);
                    c.rewards.add(Reward.fromConfig(rSec));
                } catch (Exception ex){
                    Bukkit.getLogger().warning("Failed to load reward in crate " + id + ": " + ex.getMessage());
                }
            }
        } else if (sec.isConfigurationSection("rewards")) {
            ConfigurationSection r = sec.getConfigurationSection("rewards");
            for (String k : r.getKeys(false)){
                c.rewards.add(Reward.fromConfig(r.getConfigurationSection(k)));
            }
        }
        return c;
    }

    public Reward roll(Random rng){
        int total = rewards.stream().mapToInt(r -> r.weight).sum();
        int pick = rng.nextInt(Math.max(total, 1)) + 1;
        int cum = 0;
        for (Reward r : rewards){
            cum += r.weight;
            if (pick <= cum) return r;
        }
        return rewards.get(0);
    }
}
