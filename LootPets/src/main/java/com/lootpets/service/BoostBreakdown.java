package com.lootpets.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed view of a player's boost computation.
 */
public record BoostBreakdown(
        List<PetContribution> contributions,
        StackingMode stackingMode,
        BigDecimal uncappedResult,
        BigDecimal finalMultiplier
) {
    /**
     * Per-pet contribution information.
     */
    public record PetContribution(
            String petId,
            String rarityId,
            int level,
            int stars,
            double weight,
            BigDecimal typedFactor
    ) {
    }
}
