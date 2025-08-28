package com.lootpets.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

public record PetDefinition(
        String id,
        String displayName,
        Material iconMaterial,
        Integer iconCustomModelData,
        Map<String, Double> weights
) {
    @Override
    public Material iconMaterial() {
        return iconMaterial;
    }

    public OptionalInt iconCmd() {
        return iconCustomModelData == null ? OptionalInt.empty() : OptionalInt.of(iconCustomModelData);
    }

    public Map<String, Object> icon() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", iconMaterial.name());
        if (iconCustomModelData != null) {
            map.put("custom_model_data", iconCustomModelData);
        }
        return Collections.unmodifiableMap(map);
    }
}
