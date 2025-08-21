package com.farmxmine.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InstancingService implements Listener {
    private final JavaPlugin plugin;
    private final LevelService level;
    private final List<String> veinLore;
    private final int veinMax;
    private final int harvestMax;
    private final int generalMax;
    private final boolean directToInv;
    private final boolean voidOverflow;
    private final Set<Material> ores;
    private final Set<Material> crops;
    private final boolean overrideCancelled;
    private final long respawnTicks;
    private final Map<UUID, Set<Location>> hidden = new HashMap<>();
    // Fields related to bypassing protections have been removed

    public InstancingService(JavaPlugin plugin, LevelService level) {
        this.plugin = plugin;
        this.level = level;
        this.veinLore = plugin.getConfig().getStringList("compat.veinminer.detect_lore");
        this.veinMax = plugin.getConfig().getInt("compat.veinminer.max_blocks", 64);
        this.harvestMax = plugin.getConfig().getInt("compat.harvester.max_blocks", 64);
        this.generalMax = plugin.getConfig().getInt("general.max-broken-blocks", 64);
        this.directToInv = plugin.getConfig().getBoolean("farmxmine.direct_to_inventory", true);
        this.voidOverflow = plugin.getConfig().getBoolean("inventory.void_overflow", true);
        this.ores = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("ores")) {
            try { this.ores.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
        this.crops = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("crops")) {
            try { this.crops.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }
        this.overrideCancelled = plugin.getConfig().getBoolean("override_cancelled", false);
        this.respawnTicks = plugin.getConfig().getInt("respawn_seconds", 20) * 20L;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (event.isCancelled() && !overrideCancelled) return;
        if (block.getWorld().getEnvironment() != World.Environment.NORMAL) return;

        String worldName = block.getWorld().getName();
        if (plugin.getConfig().getStringList("general.disabled-worlds").contains(worldName)) return;

        Material type = block.getType();
        boolean mining = ores.contains(type);
        boolean farming = !mining && crops.contains(type) && isMatureCrop(block);
        if (!mining && !farming) return;

        Location loc = block.getLocation().toBlockLocation();
        Set<Location> set = hidden.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (set.contains(loc)) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        String toolName = tool.getType().name();
        if (mining && !toolName.endsWith("_PICKAXE")) return;

        if (mining) {
            handle(event, player, block, loc, set, true);
        } else {
            handle(event, player, block, loc, set, false);
        }
    }


    private boolean isMatureCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable age) {
            return age.getAge() >= age.getMaximumAge();
        }
        return false;
    }

    private void handle(BlockBreakEvent event, Player player, Block block, Location loc, Set<Location> set, boolean mining) {
        int xp = event.getExpToDrop();
        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);
        int count = computeCount(player, mining);
        ItemStack tool = player.getInventory().getItemInMainHand();
        List<ItemStack> drops = collectDrops(block, tool, player, mining, count);
        for (ItemStack drop : drops) {
            giveDrop(player, block, drop);
        }
        if (mining) {
            level.addMineXp(player, count);
        } else {
            level.addFarmXp(player, count);
        }
        player.giveExp(xp);
        BlockData replacement;
        if (mining) {
            boolean deepslate = block.getType().name().startsWith("DEEPSLATE_");
            replacement = (deepslate ? Material.DEEPSLATE : Material.STONE).createBlockData();
        } else {
            replacement = Material.AIR.createBlockData();
        }
        set.add(loc);
        sendBlockChange(player, block, replacement);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            set.remove(loc);
            if (!player.isOnline()) return;
            World world = loc.getWorld();
            if (world == null) return;
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            if (!world.isChunkLoaded(cx, cz)) return;
            player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
        }, respawnTicks);
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

    private void sendBlockChange(Player player, Block block, BlockData data) {
        player.sendBlockChange(block.getLocation(), data);
    }

    private List<ItemStack> collectDrops(Block block, ItemStack tool, Player player, boolean mining, int count) {
        List<ItemStack> drops = new ArrayList<>();
        boolean smelt = mining && hasAutoSmelt(tool);
        for (ItemStack drop : block.getDrops(tool, player)) {
            Material type = drop.getType();
            if (smelt) {
                type = SMELTS.getOrDefault(type, type);
                drop = new ItemStack(type, drop.getAmount());
            }
            drop.setAmount(drop.getAmount() * count);
            drops.add(drop);
        }
        return drops;
    }

    private boolean hasAutoSmelt(ItemStack tool) {
        if (!tool.hasItemMeta() || !tool.getItemMeta().hasLore()) return false;
        for (String line : tool.getItemMeta().getLore()) {
            if (line.toLowerCase().contains("autosmelt")) return true;
        }
        return false;
    }

    private static final Map<Material, Material> SMELTS = Map.ofEntries(
            Map.entry(Material.RAW_IRON, Material.IRON_INGOT),
            Map.entry(Material.RAW_GOLD, Material.GOLD_INGOT),
            Map.entry(Material.RAW_COPPER, Material.COPPER_INGOT),
            Map.entry(Material.GOLD_NUGGET, Material.GOLD_INGOT),
            Map.entry(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP)
    );

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hidden.remove(event.getPlayer().getUniqueId());
    }
}
