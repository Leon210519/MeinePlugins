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
import org.bukkit.block.data.Ageable;
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

        // earliest cooldown and inflight gates
        if (cooldownService.isCooling(id, key)) {
            if (isCrop) {
                sendAirVisual(p, loc);
            } else {
                sendStoneVisual(p, loc);
            }
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }

        Set<BlockKey> inflightSet = inflight.computeIfAbsent(id, u -> ConcurrentHashMap.newKeySet());
        if (!inflightSet.add(key)) {
            if (isCrop) {
                sendAirVisual(p, loc);
            } else {
                sendStoneVisual(p, loc);
            }
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }

        // determine drops before starting cooldown
        Collection<ItemStack> drops;
        if (isOre) {
            boolean hasPickaxe = Materials.hasPickaxe(tool);
            boolean mineable = Materials.isMineableByPickaxe(mat);
            boolean correctTool = hasPickaxe && mineable;
            drops = correctTool ? b.getDrops(tool, p) : Collections.emptyList();
        } else {
            drops = mainProduceOnly(b, tool, p);
        }
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

        try {
            // cancel vanilla break and start cooldown
            e.setCancelled(true);
            e.setDropItems(false);
            e.setExpToDrop(0);
            long endMs = System.currentTimeMillis() + config.getRespawnSeconds() * 1000L;
            cooldownService.start(id, key, endMs);

            if (isCrop) {
                sendAirVisual(p, loc);
            } else {
                sendStoneVisual(p, loc);
            }

            // drops already determined before cooldown
            ItemUtil.giveAll(p, drops);

            // add XP only on success
            if (isOre) {
                levelService.addXp(p, TrackType.MINE);
            } else if (isCrop) {
                levelService.addXp(p, TrackType.FARM);
            }

            // restoration after cooldown
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (!p.isOnline()) {
                        return;
                    }
                    BlockData restore = loc.getWorld().getBlockAt(loc).getBlockData();
                    if (isCrop) {
                        if (!(restore instanceof Ageable) || ((Ageable) restore).getAge() < ((Ageable) restore).getMaximumAge()) {
                            Ageable full = (Ageable) Bukkit.createBlockData(mat);
                            full.setAge(full.getMaximumAge());
                            restore = full;
                        }
                    }
                    p.sendBlockChange(loc, restore);
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
        if (cooldownService.isCooling(p.getUniqueId(), key)) {
            e.setCancelled(true);
            if (isCrop) {
                sendAirVisual(p, b.getLocation());
            } else {
                sendStoneVisual(p, b.getLocation());
            }
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
        if (cooldownService.isCooling(p.getUniqueId(), key)) {
            e.setCancelled(true);
            if (isCrop) {
                sendAirVisual(p, b.getLocation());
            } else {
                sendStoneVisual(p, b.getLocation());
            }
        }
    }

    private Collection<ItemStack> mainProduceOnly(Block block, ItemStack tool, Player player) {
        Material main = block.getType();
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
