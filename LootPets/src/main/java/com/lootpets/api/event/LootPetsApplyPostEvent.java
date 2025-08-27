package com.lootpets.api.event;

import com.lootpets.api.EarningType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;

/**
 * Fired after LootPets applies a multiplier to a value.
 * Informational only.
 */
public class LootPetsApplyPostEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final EarningType earningType;
    private final BigDecimal baseAmount;
    private final double finalMultiplier;
    private final BigDecimal finalAmount;

    public LootPetsApplyPostEvent(Player player,
                                  EarningType earningType,
                                  BigDecimal baseAmount,
                                  double finalMultiplier,
                                  BigDecimal finalAmount) {
        this.player = player;
        this.earningType = earningType;
        this.baseAmount = baseAmount;
        this.finalMultiplier = finalMultiplier;
        this.finalAmount = finalAmount;
    }

    public Player getPlayer() {
        return player;
    }

    public EarningType getEarningType() {
        return earningType;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public double getFinalMultiplier() {
        return finalMultiplier;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
