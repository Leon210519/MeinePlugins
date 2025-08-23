package com.farmxmine2.listener;

import com.farmxmine2.service.ConfigService;
import com.farmxmine2.service.HarvestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/** Routes block break events into the harvest service while respecting cancelled events. */
public class BlockListener implements Listener {
    private final HarvestService harvestService;
    private final ConfigService configService;

    public BlockListener(HarvestService harvestService, ConfigService configService) {
        this.harvestService = harvestService;
        this.configService = configService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled() && !configService.isOverrideCancelled()) {
            return;
        }
        harvestService.handleBlockBreak(event.getPlayer(), event.getBlock(), event);
    }
}
