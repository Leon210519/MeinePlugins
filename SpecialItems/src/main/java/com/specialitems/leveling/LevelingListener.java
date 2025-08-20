package com.specialitems.leveling;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

/** Simple bridge between Bukkit events and the LevelingService. */
public class LevelingListener implements Listener {
    private final LevelingService service;
    public LevelingListener(LevelingService service) { this.service = service; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) { service.onBlockBreak(e); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) { service.onItemDamage(e); }
}
