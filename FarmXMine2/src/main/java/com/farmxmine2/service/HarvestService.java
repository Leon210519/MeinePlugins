package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.BlockKey;
import com.farmxmine2.model.TrackType;
import com.farmxmine2.util.ItemUtil;
import com.farmxmine2.util.Materials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.UUID;

/** Handles mining and farming harvesting while preventing real-world block changes. */
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

    public void handleBlockBreak(Player p, Block b, BlockBreakEvent e) {
        if (b.getWorld() == null || !b.getWorld().getName().equalsIgnoreCase(config.getMainWorld())) {
            return;
        }

        Material type = b.getType();
        boolean isOre = config.isMiningEnabled() && Materials.isOre(type, config.getMiningOres());
        boolean isCrop = config.isFarmingEnabled() && Materials.isCrop(type, config.getFarmingCrops()) && Materials.isMature(b);
        if (!isOre && !isCrop) {
            return;
        }

        // capture references
        Location loc = b.getLocation();
        Material mat = b.getType();
        ItemStack tool = p.getInventory().getItemInMainHand();

        // cancel vanilla break
        e.setCancelled(true);
        e.setDropItems(false);
        e.setExpToDrop(0);

        BlockKey key = BlockKey.of(b);
        UUID id = p.getUniqueId();
        if (cooldownService.isCooling(id, key)) {
            return;
        }
        long endMs = System.currentTimeMillis() + config.getRespawnSeconds() * 1000L;
        cooldownService.start(id, key, endMs);

        // compute drops independent of visuals
        Collection<ItemStack> drops = b.getDrops(tool, p);
        if (!drops.isEmpty()) {
            ItemUtil.giveAll(p, drops);
        }

        // add XP only on success
        if (isOre && !drops.isEmpty()) {
            levelService.addXp(p, TrackType.MINE);
        } else if (isCrop && !drops.isEmpty()) {
            levelService.addXp(p, TrackType.FARM);
        }

        // visuals
        if (isOre) {
            BlockData stone = Bukkit.createBlockData(Material.STONE);
            p.sendBlockChange(loc, stone);
            Bukkit.getScheduler().runTask(plugin, () -> p.sendBlockChange(loc, stone));
        } else {
            BlockData air = Bukkit.createBlockData(Material.AIR);
            p.sendBlockChange(loc, air);
            Bukkit.getScheduler().runTask(plugin, () -> p.sendBlockChange(loc, air));
        }

        // restoration after cooldown
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) {
                cooldownService.end(id, key);
                return;
            }
            BlockData restore = loc.getWorld().getBlockAt(loc).getBlockData();
            if (isCrop) {
                BlockData d = restore;
                if (d instanceof Ageable a && a.getAge() < a.getMaximumAge()) {
                    Ageable full = (Ageable) Bukkit.createBlockData(mat);
                    full.setAge(full.getMaximumAge());
                    restore = full;
                }
            }
            p.sendBlockChange(loc, restore);
            cooldownService.end(id, key);
        }, config.getRespawnSeconds() * 20L);
    }
}
