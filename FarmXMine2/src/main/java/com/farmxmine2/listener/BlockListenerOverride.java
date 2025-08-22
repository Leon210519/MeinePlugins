package com.farmxmine2.listener;

import com.farmxmine2.service.HarvestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockListenerOverride implements Listener {
    private final HarvestService harvestService;

    public BlockListenerOverride(HarvestService harvestService) {
        this.harvestService = harvestService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        harvestService.handleBlockBreak(event.getPlayer(), event.getBlock(), event);
    }
}
