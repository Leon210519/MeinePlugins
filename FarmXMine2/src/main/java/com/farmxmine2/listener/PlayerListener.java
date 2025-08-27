package com.farmxmine2.listener;

import com.farmxmine2.service.CooldownService;
import com.farmxmine2.service.LevelService;
import com.farmxmine2.service.StorageService;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final StorageService storage;
    private final LevelService levelService;
    private final CooldownService cooldownService;

    public PlayerListener(StorageService storage, LevelService levelService, CooldownService cooldownService) {
        this.storage = storage;
        this.levelService = levelService;
        this.cooldownService = cooldownService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        storage.load(id, stats -> levelService.loadStats(id, stats));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        storage.save(p.getUniqueId(), levelService.getStats(p.getUniqueId()));
        levelService.remove(p.getUniqueId());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        cooldownService.clear(chunk);
    }
}
