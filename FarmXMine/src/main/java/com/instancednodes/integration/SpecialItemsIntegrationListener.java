package com.instancednodes.integration;

import com.instancednodes.util.Cfg;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Integrates FarmxMine harvesting with SpecialItems effects.
 */
public class SpecialItemsIntegrationListener implements Listener {

    private final RegionService regionService;
    private final SpecialItemsApi specialApi;
    private final HarvestService harvestService;

    public SpecialItemsIntegrationListener(RegionService regionService,
                                           SpecialItemsApi specialApi,
                                           HarvestService harvestService) {
        this.regionService = regionService;
        this.specialApi = specialApi;
        this.harvestService = harvestService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();

        RegionType rt = regionService.getRegionType(block.getLocation());
        if (rt == RegionType.NONE) return;

        if (!regionService.isWhitelisted(rt, block.getType())) {
            e.setCancelled(true);
            e.setDropItems(false);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Hier nicht erlaubt"));
            return;
        }

        e.setDropItems(false);

        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean special = specialApi.isSpecialItem(hand);
        Set<SpecialItemsApi.Effect> effects = special
                ? new HashSet<>(specialApi.getEffects(hand))
                : Collections.emptySet();
        double yieldMul = special ? specialApi.getYieldMultiplier(hand) : 1.0;

        if (rt == RegionType.FARM && Cfg.DISABLE_REPLANT_IN_FARM) {
            effects.remove(SpecialItemsApi.Effect.REPLANT);
        }

        if (special && effects.contains(SpecialItemsApi.Effect.HARVESTER)) {
            List<Block> targets = harvestService.findAoeTargets(block, rt, Cfg.HARVESTER_MAX_BLOCKS, Cfg.HARVESTER_MAX_RADIUS);
            for (Block t : targets) {
                if (rt == RegionType.FARM && !harvestService.isMatureCrop(t)) continue;
                HarvestService.HarvestResult r = harvestService.harvestSingle(player, t, rt, yieldMul);
                giveDirectToInventory(player, r.drops);
                addXp(player, r.xp);
            }
            e.setCancelled(true);
            return;
        }

        if (rt == RegionType.FARM && !harvestService.isMatureCrop(block)) {
            e.setCancelled(true);
            return;
        }

        HarvestService.HarvestResult r = harvestService.harvestSingle(player, block, rt, yieldMul);
        giveDirectToInventory(player, r.drops);
        addXp(player, r.xp);
        e.setCancelled(true);
    }

    private void giveDirectToInventory(Player player, List<ItemStack> drops) {
        if (!Cfg.DIRECT_TO_INVENTORY) {
            for (ItemStack it : drops) {
                player.getWorld().dropItemNaturally(player.getLocation(), it);
            }
            return;
        }
        Map<Integer, ItemStack> leftover = new HashMap<>();
        for (ItemStack it : drops) {
            leftover.putAll(player.getInventory().addItem(it));
        }
        for (ItemStack it : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), it);
        }
    }

    private void addXp(Player player, int xp) {
        player.giveExp(xp);
    }
}

