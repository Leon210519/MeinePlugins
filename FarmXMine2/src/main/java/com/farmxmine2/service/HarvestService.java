package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.BlockPosKey;
import com.farmxmine2.model.Region;
import com.farmxmine2.model.TrackType;
import org.bukkit.Chunk;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
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
    private final Map<UUID, Map<BlockPosKey, StoredBlock>> views = new ConcurrentHashMap<>();

    private record StoredBlock(BlockData data, BukkitTask task) {}

    public HarvestService(FarmXMine2Plugin plugin, RegionService regionService, LevelService levelService, BossBarService bossBarService, VeinMinerCompat veinMiner) {
        this.plugin = plugin;
        this.regionService = regionService;
        this.levelService = levelService;
        this.bossBarService = bossBarService;
        this.veinMiner = veinMiner;
    }

    public void handleBlockBreak(Player player, Block block, BlockBreakEvent event) {
        TrackType track = regionService.getKind(block.getLocation());
        if (track == null) return;
        if (plugin.getConfig().getBoolean("general.require_permissions")) {
            if (track == TrackType.MINE && !player.hasPermission("farmxmine.mine")) return;
            if (track == TrackType.FARM && !player.hasPermission("farmxmine.farm")) return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        Region region = regionService.getRegion(track);
        String req = region.getRequireTool();
        if (req != null && !req.isEmpty()) {
            if (tool == null || !tool.getType().name().endsWith("_" + req)) return;
        }
        BlockData data = block.getBlockData();
        if (track == TrackType.FARM && data instanceof Ageable age && age.getAge() != age.getMaximumAge()) {
            return;
        }
        if (track == TrackType.MINE && veinMiner.hasVeinMiner(tool)) {
            Set<Block> blocks = veinMiner.collect(block, region);
            for (Block b : blocks) {
                processBlock(player, b, region, track, tool, event);
            }
        } else {
            processBlock(player, block, region, track, tool, event);
        }
    }

    private void processBlock(Player player, Block block, Region region, TrackType track, ItemStack tool, BlockBreakEvent event) {
        if (!region.isAllowed(block.getType())) return;
        event.setCancelled(true);
        BlockData original = block.getBlockData();
        final BlockData restore;
        if (track == TrackType.FARM && original instanceof Ageable age) {
            Ageable clone = (Ageable) age.clone();
            clone.setAge(clone.getMaximumAge());
            restore = clone;
        } else {
            restore = original;
        }
        Collection<ItemStack> drops = block.getDrops(tool, player);
        for (ItemStack drop : drops) {
            player.getInventory().addItem(drop);
        }
        player.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
        long delay = plugin.getConfig().getInt("general.respawn_seconds") * 20L;
        Map<BlockPosKey, StoredBlock> playerMap = views.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        BlockPosKey key = BlockPosKey.of(block);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendBlockChange(key.toLocation(), restore);
            playerMap.remove(key);
        }, delay);
        playerMap.put(key, new StoredBlock(restore, task));
        levelService.addXp(player, track);
    }

    public void clear(Player player) {
        Map<BlockPosKey, StoredBlock> map = views.remove(player.getUniqueId());
        if (map != null) {
            for (Map.Entry<BlockPosKey, StoredBlock> e : map.entrySet()) {
                e.getValue().task.cancel();
                player.sendBlockChange(e.getKey().toLocation(), e.getValue().data);
            }
        }
        bossBarService.remove(player);
    }

    public void clear(Player player, Chunk chunk) {
        Map<BlockPosKey, StoredBlock> map = views.get(player.getUniqueId());
        if (map == null) return;
        Iterator<Map.Entry<BlockPosKey, StoredBlock>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPosKey, StoredBlock> e = it.next();
            BlockPosKey key = e.getKey();
            if (key.world().equalsIgnoreCase(chunk.getWorld().getName()) &&
                    (key.x() >> 4) == chunk.getX() && (key.z() >> 4) == chunk.getZ()) {
                e.getValue().task.cancel();
                player.sendBlockChange(key.toLocation(), e.getValue().data);
                it.remove();
            }
        }
    }

    public void clearAll() {
        for (UUID uuid : views.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) clear(player);
        }
    }
}
