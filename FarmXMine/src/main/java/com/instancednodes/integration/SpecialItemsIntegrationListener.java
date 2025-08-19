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
        if (e.isCancelled()) return;
        Block block = e.getBlock();
        Player player = e.getPlayer();

        RegionType rt = regionService.getRegionType(block.getLocation());
        if (rt == RegionType.NONE) return;

        boolean bypass = player.hasPermission("farmxmine.bypass");
        boolean whitelisted = regionService.isWhitelisted(rt, block.getType());
        if (!whitelisted) {
            if (!bypass) {
                e.setCancelled(true);
                e.setDropItems(false);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Hier nicht erlaubt"));
            }
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        String toolName = hand.getType().name();
        boolean isHoe = toolName.endsWith("_HOE");
        boolean isPickaxe = toolName.endsWith("_PICKAXE");

        if (rt == RegionType.FARM) {
            if (!isHoe || !harvestService.isMatureCrop(block)) {
                return;
            }
        } else if (rt == RegionType.MINE) {
            if (!isPickaxe) {
                return;
            }
        }

        e.setDropItems(false);

        boolean special = specialApi.isSpecialItem(hand);
        Set<SpecialItemsApi.Effect> effects = special
                ? new HashSet<>(specialApi.getEffects(hand))
                : Collections.emptySet();
        double yieldMul = special ? specialApi.getYieldMultiplier(hand) : 1.0;

        if (rt == RegionType.FARM && Cfg.DISABLE_REPLANT_IN_FARM) {
            effects.remove(SpecialItemsApi.Effect.REPLANT);
        }

        int blocksProcessed = 0;
        if (special && effects.contains(SpecialItemsApi.Effect.HARVESTER)) {
            List<Block> targets = harvestService.findAoeTargets(block, rt, Cfg.HARVESTER_MAX_BLOCKS, Cfg.HARVESTER_MAX_RADIUS);
            for (Block t : targets) {
                if (!regionService.isWhitelisted(rt, t.getType())) continue;
                if (rt == RegionType.FARM && !harvestService.isMatureCrop(t)) continue;
                HarvestService.HarvestResult r = harvestService.harvestSingle(player, t, rt, yieldMul);
                if (rt == RegionType.FARM) filterFarmDrops(t.getType(), r.drops);
                giveDirectToInventory(player, r.drops);
                addXp(player, r.xp);
                blocksProcessed++;
            }
        } else {
            HarvestService.HarvestResult r = harvestService.harvestSingle(player, block, rt, yieldMul);
            if (rt == RegionType.FARM) filterFarmDrops(block.getType(), r.drops);
            giveDirectToInventory(player, r.drops);
            addXp(player, r.xp);
            blocksProcessed = 1;
        }

        if (blocksProcessed > 0) {
            int xp = (rt == RegionType.FARM ? Cfg.XP_FARM : Cfg.XP_MINE) * blocksProcessed;
            specialApi.grantHarvestXp(player, hand, rt, xp);
        }
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
        if (!Cfg.VOID_OVERFLOW) {
            for (ItemStack it : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), it);
            }
        }
    }

    private void addXp(Player player, int xp) {
        player.giveExp(xp);
    }

    private void filterFarmDrops(org.bukkit.Material blockType, List<ItemStack> drops) {
        org.bukkit.Material main = switch (blockType) {
            case WHEAT -> org.bukkit.Material.WHEAT;
            case BEETROOTS -> org.bukkit.Material.BEETROOT;
            case CARROTS -> org.bukkit.Material.CARROT;
            case POTATOES -> org.bukkit.Material.POTATO;
            default -> null;
        };
        if (main == null) return;
        drops.removeIf(it -> it.getType() != main);
    }
}

