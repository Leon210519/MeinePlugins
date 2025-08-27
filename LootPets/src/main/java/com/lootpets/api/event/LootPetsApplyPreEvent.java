package com.lootpets.api.event;

import com.lootpets.api.EarningType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;

/**
 * Fired before LootPets applies a multiplier to a value.
 */
public class LootPetsApplyPreEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final EarningType earningType;
    private final BigDecimal baseAmount;
    private double multiplier;
    private boolean cancelled;

    public LootPetsApplyPreEvent(Player player, EarningType earningType, BigDecimal baseAmount, double multiplier) {
        this.player = player;
        this.earningType = earningType;
        this.baseAmount = baseAmount;
        this.multiplier = multiplier;
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

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
