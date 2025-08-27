package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.ChatColor;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RarityRegistry {

    public record Rarity(String id, String displayName, String color) {}

    private final Map<String, Rarity> rarities;
    private final boolean fallback;

    public RarityRegistry(LootPetsPlugin plugin) {
        Map<String, Rarity> map = new LinkedHashMap<>();
        boolean fb = false;
        try {
            Class<?> clazz = Class.forName("com.specialitems.leveling.Rarity");
            Object[] constants = clazz.getEnumConstants();
            for (Object constant : constants) {
                Enum<?> e = (Enum<?>) constant;
                String id = e.name();
                String name = resolveDisplayName(constant);
                String color = resolveColor(constant);
                map.put(id, new Rarity(id, name, color));
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to import rarities from SpecialItems, using fallback.");
            map.put("COMMON", new Rarity("COMMON", "Common", "§f"));
            fb = true;
        }
        this.rarities = Collections.unmodifiableMap(map);
        this.fallback = fb;
    }

    public Map<String, Rarity> getRarities() {
        return rarities;
    }

    public boolean isFallback() {
        return fallback;
    }

    public int size() {
        return rarities.size();
    }

    private String resolveDisplayName(Object constant) {
        for (String name : new String[]{"displayName", "getDisplayName", "toString"}) {
            try {
                Method m = constant.getClass().getMethod(name);
                Object val = m.invoke(constant);
                if (val instanceof String s) {
                    return s;
                }
            } catch (Exception ignored) {
            }
        }
        return ((Enum<?>) constant).name();
    }

    private String resolveColor(Object constant) {
        for (String name : new String[]{"color", "getColor", "chatColor", "getChatColor"}) {
            try {
                Method m = constant.getClass().getMethod(name);
                Object val = m.invoke(constant);
                if (val instanceof ChatColor cc) {
                    return "§" + cc.getChar();
                }
                if (val instanceof String s) {
                    return s;
                }
            } catch (Exception ignored) {
            }
        }
        return "§f";
    }
}
