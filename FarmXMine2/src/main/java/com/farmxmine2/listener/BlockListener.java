package com.farmxmine2.listener;

import com.farmxmine2.service.ConfigService;
import com.farmxmine2.service.HarvestService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

/** Routes block-related events into the harvest service while respecting cancelled events. */
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(BlockDamageEvent event) {
        if (event.isCancelled() && !configService.isOverrideCancelled()) {
            return;
        }
        harvestService.handleBlockDamage(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (event.isCancelled() && !configService.isOverrideCancelled()) {
            return;
        }
        harvestService.handleLeftClick(event.getPlayer(), event.getClickedBlock(), event);
    }
}
