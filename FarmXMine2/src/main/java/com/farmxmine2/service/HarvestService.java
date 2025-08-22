package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.Region;
import com.farmxmine2.model.TrackType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HarvestService {
    private final FarmXMine2Plugin plugin;
    private final RegionService regionService;
    private final LevelService levelService;
    private final BossBarService bossBarService;
    private final VeinMinerCompat veinMiner;
    private final Map<UUID, Map<Block, StoredBlock>> views = new ConcurrentHashMap<>();

    private record StoredBlock(BlockData data, BukkitTask task) {}

    public HarvestService(FarmXMine2Plugin plugin, RegionService regionService, LevelService levelService, BossBarService bossBarService, VeinMinerCompat veinMiner) {
        this.plugin = plugin;
        this.regionService = regionService;
        this.levelService = levelService;
        this.bossBarService = bossBarService;
        this.veinMiner = veinMiner;
    }

    public void handle(Player player, Block block) {
        TrackType track = null;
        Region mine = regionService.getRegion(TrackType.MINE);
        Region farm = regionService.getRegion(TrackType.FARM);
        if (mine.contains(block.getLocation())) track = TrackType.MINE;
        else if (farm.contains(block.getLocation())) track = TrackType.FARM;
        if (track == null) return;
        if (plugin.getConfig().getBoolean("general.require_permissions")) {
            if (track == TrackType.MINE && !player.hasPermission("farmxmine.mine")) return;
            if (track == TrackType.FARM && !player.hasPermission("farmxmine.farm")) return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        Region region = regionService.getRegion(track);
        if (region.getRequireTool() != null && !region.getRequireTool().isEmpty()) {
            if (tool == null || !tool.getType().name().endsWith("_" + region.getRequireTool())) return;
        }
        if (track == TrackType.FARM) {
            BlockData data = block.getBlockData();
            if (data instanceof Ageable age) {
                if (age.getAge() != age.getMaximumAge()) return;
            }
        }
        if (track == TrackType.MINE && veinMiner.hasVeinMiner(tool)) {
            Set<Block> blocks = veinMiner.collect(block, region);
            for (Block b : blocks) {
                processBlock(player, b, region, track, tool);
            }
        } else {
            processBlock(player, block, region, track, tool);
        }
    }

    private void processBlock(Player player, Block block, Region region, TrackType track, ItemStack tool) {
        if (!region.isAllowed(block.getType())) return;
        BlockData original = block.getBlockData();
        Collection<ItemStack> drops = block.getDrops(tool, player);
        for (ItemStack drop : drops) {
            player.getInventory().addItem(drop);
        }
        player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
        long delay = plugin.getConfig().getInt("general.respawn_seconds") * 20L;
        Map<Block, StoredBlock> playerMap = views.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendBlockChange(block.getLocation(), original);
            playerMap.remove(block);
        }, delay);
        playerMap.put(block, new StoredBlock(original, task));
        levelService.addXp(player, track);
    }

    public void clear(Player player) {
        Map<Block, StoredBlock> map = views.remove(player.getUniqueId());
        if (map != null) {
            for (Map.Entry<Block, StoredBlock> e : map.entrySet()) {
                e.getValue().task.cancel();
                player.sendBlockChange(e.getKey().getLocation(), e.getValue().data);
            }
        }
        bossBarService.remove(player);
    }

    public void clearAll() {
        for (UUID uuid : views.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) clear(player);
        }
    }
}
