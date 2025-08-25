package com.specialitems2.integration;

import com.instancednodes.integration.RegionType;
import com.instancednodes.integration.SpecialItemsApi;
import com.specialitems2.leveling.LevelingService;
import com.specialitems2.leveling.ToolClass;
import com.specialitems2.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class SpecialItemsBridge implements SpecialItemsApi {

    private final LevelingService leveling;

    public SpecialItemsBridge(LevelingService leveling) {
        this.leveling = leveling;
    }

    @Override
    public boolean isSpecialItem(ItemStack item) {
        return leveling.isSpecialItem(item);
    }

    @Override
    public Set<Effect> getEffects(ItemStack item) {
        EnumSet<Effect> set = EnumSet.noneOf(Effect.class);
        if (ItemUtil.getEffectLevel(item, "harvester") > 0 || ItemUtil.getEffectLevel(item, "veinminer") > 0) {
            set.add(Effect.HARVESTER);
        }
        if (ItemUtil.getEffectLevel(item, "replant") > 0) {
            set.add(Effect.REPLANT);
        }
        if (ItemUtil.getToolYieldBonus(item) > 0) {
            set.add(Effect.YIELD_MULTIPLIER);
        }
        return set;
    }

    @Override
    public double getYieldMultiplier(ItemStack item) {
        return 1.0 + ItemUtil.getToolYieldBonus(item);
    }

    @Override
    public void grantHarvestXp(Player player, ItemStack item, RegionType regionType, int amount) {
        ToolClass clazz = regionType == RegionType.MINE ? ToolClass.PICKAXE : ToolClass.HOE;
        leveling.grantXp(player, item, amount, clazz);
    }
}
