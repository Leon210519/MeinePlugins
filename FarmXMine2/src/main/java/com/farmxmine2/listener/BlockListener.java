package com.farmxmine2.listener;

import com.farmxmine2.service.HarvestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockListener implements Listener {
    private final HarvestService harvestService;

    public BlockListener(HarvestService harvestService) {
        this.harvestService = harvestService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        harvestService.handleBlockBreak(event.getPlayer(), event.getBlock(), event);
    }
}
