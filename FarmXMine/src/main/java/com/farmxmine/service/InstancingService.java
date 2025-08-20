package com.farmxmine.service;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
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
    private final boolean wgBreakOverride;
    private final List<String> wgWorlds;
    private final String wgPermission;

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
        this.wgBreakOverride = plugin.getConfig().getBoolean("general.worldguard_break_override", true);
        this.wgWorlds = plugin.getConfig().getStringList("general.allowed_worlds");
        this.wgPermission = plugin.getConfig().getString("general.required_permission", "farmxmine.override.break");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
public void onBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    Block block = event.getBlock();

    String worldName = block.getWorld().getName();
    if (plugin.getConfig().getStringList("general.disabled-worlds").contains(worldName)) return;

    Material type = block.getType();
    boolean mining = isOre(type);
    boolean farming = !mining && isMatureCrop(block);
    if (!mining && !farming) return;

    ItemStack tool = player.getInventory().getItemInMainHand();
    String toolName = tool.getType().name();
    if (mining && !toolName.endsWith("_PICKAXE")) return;
    if (farming && !toolName.endsWith("_HOE")) return;

    if (event.isCancelled()) {
        if (!wgBreakOverride) return;
        if (!wgWorlds.contains(worldName)) return;
        if (wgPermission != null && !wgPermission.isEmpty() && !player.hasPermission(wgPermission)) return;

        int count = computeCount(player, mining);
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack drop : block.getDrops(tool, player)) {
            ItemStack copy = drop.clone();
            copy.setAmount(copy.getAmount() * count);
            drops.add(copy);
        }

        event.setCancelled(false);
        event.setDropItems(false);
        event.setExpToDrop(0);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() != Material.AIR) {
                block.setType(Material.AIR);
                block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, type);
            }
            for (ItemStack drop : drops) {
                giveDrop(player, block, drop);
            }
            if (mining) {
                level.addMineXp(player, count);
            } else {
                level.addFarmXp(player, count);
            }
        });
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
