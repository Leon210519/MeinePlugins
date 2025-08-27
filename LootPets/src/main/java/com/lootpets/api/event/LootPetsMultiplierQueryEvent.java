package com.lootpets.api.event;

import com.lootpets.api.EarningType;
import com.lootpets.service.StackingMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

/**
 * Fired when LootPets calculates a multiplier for a player.
 * Listeners may override the uncapped or capped multiplier via the provided setters.
 */
public class LootPetsMultiplierQueryEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final EarningType earningType;
    private double uncappedMultiplier;
    private double cappedMultiplier;
    private final StackingMode stackingMode;
    private final List<Contribution> breakdown;

    public LootPetsMultiplierQueryEvent(Player player,
                                        EarningType earningType,
                                        double uncappedMultiplier,
                                        double cappedMultiplier,
                                        StackingMode stackingMode,
                                        List<Contribution> breakdown) {
        this.player = player;
        this.earningType = earningType;
        this.uncappedMultiplier = uncappedMultiplier;
        this.cappedMultiplier = cappedMultiplier;
        this.stackingMode = stackingMode;
        this.breakdown = Collections.unmodifiableList(breakdown);
    }

    public Player getPlayer() {
        return player;
    }

    public EarningType getEarningType() {
        return earningType;
    }

    public double getUncappedMultiplier() {
        return uncappedMultiplier;
    }

    public void setUncappedMultiplier(double uncappedMultiplier) {
        this.uncappedMultiplier = uncappedMultiplier;
    }

    public double getCappedMultiplier() {
        return cappedMultiplier;
    }

    public void setCappedMultiplier(double cappedMultiplier) {
        this.cappedMultiplier = cappedMultiplier;
    }

    public StackingMode getStackingMode() {
        return stackingMode;
    }

    public List<Contribution> getBreakdown() {
        return breakdown;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Compact representation of a pet's contribution to the multiplier.
     */
    public record Contribution(String petId,
                               String rarityId,
                               int level,
                               int stars,
                               double weight,
                               double typedFactor) {}
}
