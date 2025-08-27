package com.lootpets.model;

public record OwnedPetState(
        String rarity,
        int level,
        int stars,
        int evolveProgress
) {
}

