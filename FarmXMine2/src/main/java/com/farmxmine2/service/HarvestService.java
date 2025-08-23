package com.farmxmine2.service;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.BlockPosKey;
import com.farmxmine2.model.TrackType;
import com.farmxmine2.util.Materials;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
    private final ConfigService config;
    private final LevelService levelService;
    private final Map<UUID, Map<BlockPosKey, StoredBlock>> views = new ConcurrentHashMap<>();

    private record StoredBlock(BlockData data, BukkitTask task) {}

    public HarvestService(FarmXMine2Plugin plugin, ConfigService config, LevelService levelService) {
        this.plugin = plugin;
        this.config = config;
        this.levelService = levelService;
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
            BlockData data = block.getBlockData();
            if (data instanceof Ageable age && age.getAge() != age.getMaximumAge()) return;
        }

        BlockPosKey key = BlockPosKey.of(block);
        Map<BlockPosKey, StoredBlock> playerMap = views.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        if (playerMap.containsKey(key)) return; // cooldown

        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        BlockData original = block.getBlockData();
        Collection<ItemStack> drops = block.getDrops(tool, player);
        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
            for (ItemStack l : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), l);
            }
        }

        BlockData fake = track == TrackType.MINE ? Material.STONE.createBlockData() : Material.AIR.createBlockData();
        player.sendBlockChange(block.getLocation(), fake);
        long delay = config.getRespawnSeconds() * 20L;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendBlockChange(block.getLocation(), original);
            Map<BlockPosKey, StoredBlock> m = views.get(player.getUniqueId());
            if (m != null) m.remove(key);
        }, delay);
        playerMap.put(key, new StoredBlock(original, task));

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
