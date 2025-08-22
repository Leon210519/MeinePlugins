package com.specialitems.effects;

import java.util.*;

public final class Effects {
    private static final Map<String, CustomEffect> REG = new LinkedHashMap<>();

    static {
        // Ensure defaults are registered even if the plugin loader misses the explicit call.
        try {
            if (REG.isEmpty()) {
                registerDefaults();
            }
        } catch (Throwable ignored) {}
    }

    public static void register(CustomEffect e) { REG.put(e.id(), e); }
    public static CustomEffect get(String id) { return REG.get(id); }
    public static Set<String> ids() { return REG.keySet(); }
    public static int size() { return REG.size(); }

    public static void registerDefaults() {
        register(new com.specialitems.effects.impl.AutoSmelt());
        register(new com.specialitems.effects.impl.Telekinesis());
        register(new com.specialitems.effects.impl.VeinMiner());
        register(new com.specialitems.effects.impl.AreaMiner());
        register(new com.specialitems.effects.impl.Lifesteal());
        register(new com.specialitems.effects.impl.Wither());
        register(new com.specialitems.effects.impl.Frozen());
        register(new com.specialitems.effects.impl.DoubleStrike());
        register(new com.specialitems.effects.impl.Gears());
        register(new com.specialitems.effects.impl.HasteTouch());
        register(new com.specialitems.effects.impl.NightVision());
        register(new com.specialitems.effects.impl.Magnet());
        register(new com.specialitems.effects.impl.Harvester());
        register(new com.specialitems.effects.impl.XpBoost());
        register(new com.specialitems.effects.impl.DoubleDamage());
        register(new com.specialitems.effects.impl.HasteBoost());
        register(new com.specialitems.effects.impl.AbsorptionShield());
        register(new com.specialitems.effects.impl.Greenthumb());
    }
}