package com.lootpets.api;

import com.lootpets.service.BoostService;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.OptionalDouble;

/**
 * Public facade for interacting with LootPets.
 */
public final class LootPetsAPI {

    private static BoostService boostService;

    private LootPetsAPI() {
    }

    // Internal initialization from plugin
    public static void init(BoostService service) {
        boostService = service;
    }

    public static void shutdown() {
        boostService = null;
    }

    /**
     * Access to the boost service for advanced operations.
     */
    public static BoostService boosts() {
        return boostService;
    }

    /**
     * Returns the current multiplier for the given player and earning type.
     */
    public static OptionalDouble getMultiplier(Player player, EarningType type) {
        if (boostService == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(boostService.getMultiplier(player, type));
    }

    /**
     * Applies the current boost multiplier to a base value.
     */
    public static BigDecimal apply(Player player, EarningType type, BigDecimal base) {
        if (boostService == null) {
            return base;
        }
        return boostService.apply(player, type, base);
    }
}
