package com.farmxmine2.service;

import com.farmxmine2.model.BlockVec;
import com.farmxmine2.model.TrackType;
import com.farmxmine2.util.Materials;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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
    private final ConfigService config;
    private final LevelService levelService;
    private final CooldownService cooldownService;

    public HarvestService(ConfigService config, LevelService levelService, CooldownService cooldownService) {
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
        if (config.isMiningEnabled() && config.getMiningOres().contains(type)) {
            track = TrackType.MINE;
        } else if (config.isFarmingEnabled() && config.getFarmingCrops().contains(type)) {
            track = TrackType.FARM;
        }
        if (track == null) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (track == TrackType.MINE) {
            if (!Materials.isPickaxe(tool.getType())) return;
            if (Materials.pickaxeLevel(tool.getType()) < Materials.requiredLevel(type)) return;
        } else {
            if (!Materials.isHoe(tool.getType())) return;
            if (block.getBlockData() instanceof Ageable age && age.getAge() < age.getMaximumAge()) return;
        }

        UUID uuid = player.getUniqueId();
        BlockVec key = BlockVec.of(block);
        if (cooldownService.isCooling(uuid, key)) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }

        long endMs = System.currentTimeMillis() + config.getRespawnSeconds() * 1000L;
        cooldownService.start(uuid, key, endMs);

        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        Collection<ItemStack> drops = block.getDrops(tool, player);
        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
            for (ItemStack l : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), l);
            }
        }

        if (track == TrackType.MINE) {
            player.sendBlockChange(block.getLocation(), Bukkit.createBlockData(Material.STONE));
        } else {
            player.sendBlockChange(block.getLocation(), Bukkit.createBlockData(Material.AIR));
        }

        levelService.addXp(player, track);
    }
}
