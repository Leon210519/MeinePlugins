package com.lootfactory.factory;

import org.bukkit.Material;

/**
 * Definition of a factory type. Loaded from config.
 */
public class FactoryDef {
    public final String id;
    public final String display;
    public final Material material;
    public final double baseAmount;
    public final double baseIntervalSec;
    public final FactoryRarity rarity;       // COMMON / UNCOMMON / RARE / EPIC / LEGENDARY / INSANE
    public final double yieldBonusPct;       // e.g. 20.0 for +20% yield
    public final double speedBonusPct;       // e.g. 15.0 for +15% speed

    public FactoryDef(String id,
                      String display,
                      Material material,
                      double baseAmount,
                      double baseIntervalSec,
                      FactoryRarity rarity,
                      double yieldBonusPct,
                      double speedBonusPct) {
        this.id = id;
        this.display = display;
        this.material = material;
        this.baseAmount = baseAmount;
        this.baseIntervalSec = baseIntervalSec;
        this.rarity = rarity;
        this.yieldBonusPct = yieldBonusPct;
        this.speedBonusPct = speedBonusPct;
    }
}
