package com.instancednodes.integration;

import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Abstraction over the SpecialItems plugin.
 */
public interface SpecialItemsApi {

    /** Effects a special item can provide. */
    enum Effect {
        HARVESTER,
        YIELD_MULTIPLIER,
        REPLANT
    }

    /**
     * Whether the provided item stack is managed by SpecialItems.
     */
    boolean isSpecialItem(ItemStack item);

    /**
     * Retrieve the set of effects active on the item.
     */
    Set<Effect> getEffects(ItemStack item);

    /**
     * Get the yield multiplier the item applies. Returns 1.0 if none.
     */
    double getYieldMultiplier(ItemStack item);
}

