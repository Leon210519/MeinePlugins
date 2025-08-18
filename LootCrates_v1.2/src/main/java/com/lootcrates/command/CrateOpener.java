package com.lootcrates.command;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.crate.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CrateOpener {
    public static void giveReward(LootCratesPlugin plugin, Player p, Crate c, Reward r){
        switch (r.type){
            case MONEY_XP -> {
                if (r.money > 0){ plugin.economy().depositPlayer(p, r.money); }
                if (r.xp > 0){ p.giveExp(r.xp); }
            }
            case ITEM, SPECIAL_ITEM -> {
                java.util.Map<Integer, org.bukkit.inventory.ItemStack> left = p.getInventory().addItem(r.item.clone());
                for (org.bukkit.inventory.ItemStack it : left.values()) p.getWorld().dropItem(p.getLocation(), it);
            }
            case COMMAND -> {
                if (r.commands != null){
                    for (String cmd : r.commands){
                        String run = cmd.replace("{player}", p.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
                    }
                }
            }
            case KEY -> {
                plugin.crates().giveKey(p, r.keyCrate, r.keyAmount);
            }
        }
    }
}
