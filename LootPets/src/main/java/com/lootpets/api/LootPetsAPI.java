package com.lootpets.api;

import com.lootpets.service.BoostService;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.OptionalDouble;

/**
 * Public facade for interacting with LootPets.
 *
 * <p>The multipliers returned by this API are always {@code >= 1.0} and are capped
 * according to the active configuration. Results are cached briefly and automatically
 * invalidated on pet state changes.</p>
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
     * The value is guaranteed to be at least {@code 1.0}.
     */
    public static OptionalDouble getMultiplier(Player player, EarningType type) {
        if (boostService == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(boostService.getMultiplier(player, type));
    }

    /**
     * Applies the current boost multiplier to a base value. The result is never
     * less than the provided base value and will respect the configured caps.
     */
    public static BigDecimal apply(Player player, EarningType type, BigDecimal base) {
        if (boostService == null) {
            return base;
        }
        return boostService.apply(player, type, base);
    }
}
