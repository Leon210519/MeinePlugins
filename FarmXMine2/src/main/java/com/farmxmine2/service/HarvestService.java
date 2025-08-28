package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.BlockKey;
import com.farmxmine2.model.TrackType;
import com.farmxmine2.util.ItemUtil;
import com.farmxmine2.util.Materials;
import com.farmxmine2.util.Visuals;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles mining and farming harvesting while preventing real-world block changes. */
public class HarvestService {
    private static final Map<Material, Material> MAIN_DROPS = Map.of(
            Material.WHEAT, Material.WHEAT,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.BEETROOTS, Material.BEETROOT,
            Material.NETHER_WART, Material.NETHER_WART
    );

    private final FarmXMine2Plugin plugin;
    private final ConfigService config;
    private final LevelService levelService;
    private final CooldownService cooldownService;
    private final Map<UUID, Set<BlockKey>> inflight = new ConcurrentHashMap<>();

    public HarvestService(FarmXMine2Plugin plugin, ConfigService config, LevelService levelService, CooldownService cooldownService) {
        this.plugin = plugin;
        this.config = config;
        this.levelService = levelService;
        this.cooldownService = cooldownService;
    }

    public void handleBlockBreak(Player p, Block b, BlockBreakEvent e) {
        if (b.getWorld() == null || !config.isWorldAllowed(b.getWorld().getName())) {
            return;
        }

        Material type = b.getType();
        boolean isOre = config.isMiningEnabled() && config.getMiningOres().contains(type);
        boolean isCrop = config.isFarmingEnabled() && config.getFarmingCrops().contains(type) && Materials.isMature(b);
        if (!isOre && !isCrop) {
            return;
        }

        // capture references
        Location loc = b.getLocation();
        Material mat = b.getType();
        ItemStack tool = p.getInventory().getItemInMainHand();
        BlockKey key = BlockKey.of(b);
        UUID id = p.getUniqueId();

        // tool gating before cooldown start
        if (isOre) {
            String req = config.getMiningRequireTool();
            if (req != null && !req.equalsIgnoreCase("NONE") && (tool == null || !tool.getType().name().endsWith("_" + req))) {
                String msg = plugin.color(plugin.getMessages().getString("wrong-tool", "&cYou need a %tool%"))
                        .replace("%tool%", req.toLowerCase());
                p.sendMessage(msg);
                e.setCancelled(true);
                e.setDropItems(false);
                e.setExpToDrop(0);
                return;
            }
        } else if (isCrop) {
            String req = config.getFarmingRequireTool();
            if (req != null && !req.equalsIgnoreCase("NONE") && (tool == null || !tool.getType().name().endsWith("_" + req))) {
                String msg = plugin.color(plugin.getMessages().getString("wrong-tool", "&cYou need a %tool%"))
                        .replace("%tool%", req.toLowerCase());
                p.sendMessage(msg);
                e.setCancelled(true);
                e.setDropItems(false);
                e.setExpToDrop(0);
                return;
            }
        }

        if (isOre) {
            handleOreBreak(p, b, e, loc, mat, tool, key, id);
            return;
        }

        // Farming logic (unchanged)
        if (cooldownService.isCooling(key)) {
            sendAirVisual(p, loc);
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }

        Set<BlockKey> inflightSet = inflight.computeIfAbsent(id, u -> ConcurrentHashMap.newKeySet());
        if (!inflightSet.add(key)) {
            sendAirVisual(p, loc);
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }

        // determine drops before starting cooldown
        Collection<ItemStack> drops = mainProduceOnly(b, tool, p);
        if (drops.isEmpty()) {
            String msg = plugin.color(plugin.getMessages().getString("invalid-harvest", "&cYour tool cannot harvest this block."));
            p.sendMessage(msg);
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            inflightSet.remove(key);
            if (inflightSet.isEmpty()) {
                inflight.remove(id);
            }
            return;
        }

        BlockData snapshot = b.getBlockData();

        try {
            // cancel vanilla break and start cooldown
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            long endMs = System.currentTimeMillis() + config.getRespawnSeconds() * 1000L;
            cooldownService.start(key, endMs);

            b.setType(Material.AIR, false);
            BlockData air = Bukkit.createBlockData(Material.AIR);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(id)) {
                    sendStoneVisual(online, loc);
                } else {
                    online.sendBlockChange(loc, air);
                }
            }

            // drops already determined before cooldown
            ItemUtil.giveAll(p, drops);

            // add XP only on success
            levelService.addXp(p, TrackType.FARM);

            // restoration after cooldown
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    loc.getBlock().setBlockData(snapshot, false);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendBlockChange(loc, snapshot);
                    }
                } finally {
                    cooldownService.end(key);
                }
            }, config.getRespawnSeconds() * 20L);
        } finally {
            inflightSet.remove(key);
            if (inflightSet.isEmpty()) {
                inflight.remove(id);
            }
        }
    }

    private void handleOreBreak(Player p, Block b, BlockBreakEvent e, Location loc, Material mat, ItemStack tool, BlockKey key, UUID id) {
        // per-player cooldown gate
        if (cooldownService.isCooling(id, key)) {
            sendStoneVisual(p, loc);
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }

        Set<BlockKey> inflightSet = inflight.computeIfAbsent(id, u -> ConcurrentHashMap.newKeySet());
        if (!inflightSet.add(key)) {
            sendStoneVisual(p, loc);
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }

        Collection<ItemStack> drops;
        boolean hasPickaxe = Materials.hasPickaxe(tool);
        boolean mineable = Materials.isMineableByPickaxe(mat);
        boolean correctTool = hasPickaxe && mineable;
        drops = correctTool ? b.getDrops(tool, p) : Collections.emptyList();
        if (drops.isEmpty()) {
            String msg = plugin.color(plugin.getMessages().getString("invalid-harvest", "&cYour tool cannot harvest this block."));
            p.sendMessage(msg);
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            inflightSet.remove(key);
            if (inflightSet.isEmpty()) {
                inflight.remove(id);
            }
            return;
        }

        BlockData snapshot = b.getBlockData();

        try {
            // cancel vanilla break and start cooldown
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            long endMs = System.currentTimeMillis() + config.getRespawnSeconds() * 1000L;
            cooldownService.start(id, key, endMs);

            sendStoneVisual(p, loc);

            // drops already determined before cooldown
            ItemUtil.giveAll(p, drops);

            // add XP only on success
            levelService.addXp(p, TrackType.MINE);

            // restoration after cooldown
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    Visuals.show(plugin, p, loc, snapshot);
                } finally {
                    cooldownService.end(id, key);
                }
            }, config.getRespawnSeconds() * 20L);
        } finally {
            inflightSet.remove(key);
            if (inflightSet.isEmpty()) {
                inflight.remove(id);
            }
        }
    }

    public void handleBlockDamage(Player p, Block b, BlockDamageEvent e) {
        if (b.getWorld() == null || !config.isWorldAllowed(b.getWorld().getName())) {
            return;
        }
        Material type = b.getType();
        boolean isOre = config.isMiningEnabled() && config.getMiningOres().contains(type);
        boolean isCrop = config.isFarmingEnabled() && config.getFarmingCrops().contains(type) && Materials.isMature(b);
        if (!isOre && !isCrop) {
            return;
        }
        BlockKey key = BlockKey.of(b);
        if (isOre) {
            if (cooldownService.isCooling(p.getUniqueId(), key)) {
                e.setCancelled(true);
                sendStoneVisual(p, b.getLocation());
            }
        } else if (isCrop && cooldownService.isCooling(key)) {
            e.setCancelled(true);
            sendAirVisual(p, b.getLocation());
        }
    }

    public void handleLeftClick(Player p, Block b, PlayerInteractEvent e) {
        if (b.getWorld() == null || !config.isWorldAllowed(b.getWorld().getName())) {
            return;
        }
        Material type = b.getType();
        boolean isOre = config.isMiningEnabled() && config.getMiningOres().contains(type);
        boolean isCrop = config.isFarmingEnabled() && config.getFarmingCrops().contains(type) && Materials.isMature(b);
        if (!isOre && !isCrop) {
            return;
        }
        BlockKey key = BlockKey.of(b);
        if (isOre) {
            if (cooldownService.isCooling(p.getUniqueId(), key)) {
                e.setCancelled(true);
                sendStoneVisual(p, b.getLocation());
            }
        } else if (isCrop && cooldownService.isCooling(key)) {
            e.setCancelled(true);
            sendAirVisual(p, b.getLocation());
        }
    }

    private Collection<ItemStack> mainProduceOnly(Block block, ItemStack tool, Player player) {
        Material main = MAIN_DROPS.getOrDefault(block.getType(), block.getType());
        Collection<ItemStack> all = block.getDrops(tool, player);
        if (all.isEmpty()) return all;
        Collection<ItemStack> filtered = new ArrayList<>();
        for (ItemStack drop : all) {
            if (drop.getType() == main) {
                filtered.add(drop);
            }
        }
        return filtered;
    }

    private void sendAirVisual(Player player, Location loc) {
        Visuals.show(plugin, player, loc, Material.AIR);
    }

    private void sendStoneVisual(Player player, Location loc) {
        Visuals.show(plugin, player, loc, Material.STONE);
    }
}
