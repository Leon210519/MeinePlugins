package com.farmxmine.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class InstancingService implements Listener {
    private final JavaPlugin plugin;
    private final LevelService level;
    private final int respawnSeconds;
    private final List<String> veinLore;
    private final int veinMax;
    private final int harvestMax;
    private final int generalMax;
    private final boolean directToInv;
    private final boolean voidOverflow;

    public InstancingService(JavaPlugin plugin, LevelService level) {
        this.plugin = plugin;
        this.level = level;
        this.respawnSeconds = plugin.getConfig().getInt("respawn_seconds", 30);
        this.veinLore = plugin.getConfig().getStringList("compat.veinminer.detect_lore");
        this.veinMax = plugin.getConfig().getInt("compat.veinminer.max_blocks", 64);
        this.harvestMax = plugin.getConfig().getInt("compat.harvester.max_blocks", 64);
        this.generalMax = plugin.getConfig().getInt("general.max-broken-blocks", 64);
        this.directToInv = plugin.getConfig().getBoolean("farmxmine.direct_to_inventory", true);
        this.voidOverflow = plugin.getConfig().getBoolean("inventory.void_overflow", true);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String world = block.getWorld().getName();
        if (plugin.getConfig().getStringList("general.disabled-worlds").contains(world)) return;
        Material type = block.getType();
        boolean mining = isOre(type);
        boolean farming = !mining && isMatureCrop(block);
        if (!mining && !farming) return;

        if (event.isCancelled()) {
            if (!plugin.getConfig().getBoolean("general.override_cancelled", true)) return;
            int count = computeCount(player, mining);
            if (mining) {
                level.addMineXp(player, count);
            } else {
                level.addFarmXp(player, count);
            }
            return;
        }

        if (mining) {
            handle(event, player, block, true);
        } else {
            handle(event, player, block, false);
        }
    }

    private boolean isMatureCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable age) {
            return age.getAge() >= age.getMaximumAge();
        }
        return false;
    }

    private void handle(BlockBreakEvent event, Player player, Block block, boolean mining) {
        event.setCancelled(true);
        int count = computeCount(player, mining);
        ItemStack tool = player.getInventory().getItemInMainHand();
        for (ItemStack drop : block.getDrops(tool, player)) {
            drop.setAmount(drop.getAmount() * count);
            giveDrop(player, block, drop);
        }
        if (mining) {
            level.addMineXp(player, count);
            sendBlockChange(player, block, Material.AIR.createBlockData());
        } else {
            level.addFarmXp(player, count);
            BlockData replanted = block.getBlockData().clone();
            if (replanted instanceof Ageable age) { age.setAge(0); }
            sendBlockChange(player, block, replanted);
        }
        BlockData original = block.getBlockData();
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendBlockChange(block.getLocation(), original), respawnSeconds * 20L);
    }

    private int computeCount(Player player, boolean mining) {
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean multi = false;
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            for (String line : item.getItemMeta().getLore()) {
                for (String tag : veinLore) {
                    if (line.contains(tag)) multi = true;
                }
            }
        }
        if (!multi) return 1;
        int max = mining ? veinMax : harvestMax;
        return Math.min(max, generalMax);
    }

    private void giveDrop(Player player, Block block, ItemStack drop) {
        if (directToInv) {
            var overflow = player.getInventory().addItem(drop);
            if (!overflow.isEmpty() && !voidOverflow) {
                overflow.values().forEach(it -> block.getWorld().dropItemNaturally(block.getLocation(), it));
            }
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
    }

    private boolean isOre(Material type) {
        String n = type.name();
        return n.endsWith("_ORE") || n.equals("ANCIENT_DEBRIS");
    }

    private void sendBlockChange(Player player, Block block, BlockData data) {
        player.sendBlockChange(block.getLocation(), data);
    }
}
