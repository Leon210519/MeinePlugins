package com.lootpets.model;

import org.bukkit.Material;

import java.util.Map;

public record PetDefinition(
        String id,
        String displayName,
        Material iconMaterial,
        Integer iconCustomModelData,
        Map<String, Double> weights
) {}
