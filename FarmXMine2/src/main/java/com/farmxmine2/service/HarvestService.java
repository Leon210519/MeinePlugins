package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.BlockKey;
import com.farmxmine2.model.TrackType;
import com.farmxmine2.util.Materials;
import com.farmxmine2.util.Visuals;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Handles mining and farming harvesting while preventing real-world block changes.
 */
public class HarvestService {
    private final FarmXMine2Plugin plugin;
    private final ConfigService config;
    private final LevelService levelService;
    private final CooldownService cooldownService;

    public HarvestService(FarmXMine2Plugin plugin, ConfigService config, LevelService levelService, CooldownService cooldownService) {
        this.plugin = plugin;
        this.config = config;
        this.levelService = levelService;
        this.cooldownService = cooldownService;
    }

    public void handleBlockBreak(Player player, Block block, BlockBreakEvent event) {
        if (block.getWorld() == null || !block.getWorld().getName().equalsIgnoreCase(config.getMainWorld())) {
            return;
        }

        Material type = block.getType();
        TrackType track = null;
        if (config.isMiningEnabled() && Materials.isOre(type, config.getMiningOres())) {
            track = TrackType.MINE;
        } else if (config.isFarmingEnabled() && Materials.isCrop(type, config.getFarmingCrops()) && Materials.isMature(block)) {
            track = TrackType.FARM;
        }
        if (track == null) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (track == TrackType.MINE) {
            if (!Materials.isPickaxe(tool.getType())) return;
            if (Materials.pickaxeLevel(tool.getType()) < Materials.requiredLevel(type)) return;
        } else {
            if (!Materials.isHoe(tool.getType())) return;
        }

        UUID uuid = player.getUniqueId();
        BlockKey key = BlockKey.of(block);
        if (cooldownService.isCooling(uuid, key)) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }

        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        long endMs = System.currentTimeMillis() + config.getRespawnSeconds() * 1000L;
        cooldownService.start(uuid, key, endMs);

        if (track == TrackType.MINE) {
            Visuals.show(plugin, player, block.getLocation(), Material.STONE);
        } else {
            Visuals.show(plugin, player, block.getLocation(), Material.AIR);
        }

        Collection<ItemStack> drops = block.getDrops(tool, player);
        for (ItemStack drop : drops) {
            if (drop == null) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
            for (ItemStack l : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), l);
            }
        }

        levelService.addXp(player, track);
    }
}
