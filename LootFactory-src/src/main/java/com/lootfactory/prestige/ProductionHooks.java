package com.lootfactory.prestige;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hilfsfunktionen, um Produktionsgeschwindigkeit & Output um Prestige-Boni zu erweitern.
 *
 * Beispiel-Nutzung in deiner Produktionsschleife:
 *
 *   ProductionHooks.Result r = ProductionHooks.apply(prestigeManager, playerUUID, factoryType,
 *                                                    baseIntervalTicks, baseAmount);
 *   long newInterval = r.intervalTicks();
 *   double amount = r.amount();
 *   if (ProductionHooks.rollDoubleOutput(prestigeManager, prestigeLevel, null)) amount *= 2;
 *   // dann: Intervall setzen + produzieren
 */
public final class ProductionHooks {

    public static final class Result {
        private final long intervalTicks;
        private final double amount;
        public Result(long intervalTicks, double amount) {
            this.intervalTicks = intervalTicks;
            this.amount = amount;
        }
        public long intervalTicks() { return intervalTicks; }
        public double amount() { return amount; }
    }

    private ProductionHooks() {}

    /**
     * Wendet Produktionsmultiplikator, Speedbonus (mit Cap) an.
     * Doppel-Output wird separat via rollDoubleOutput(...) gewürfelt.
     */
    public static Result apply(PrestigeManager pm, UUID player, String factoryType, long baseIntervalTicks, double baseAmount) {
        int p = pm.getPrestige(player, factoryType);
        double prodMult = 1.0 + (p * pm.getProdBonusPerLevel());
        double amount = baseAmount * prodMult;

        long minTicks = pm.getMinTickIntervalTicks();
        double factor = Math.max(0.0, 1.0 - (p * pm.getSpeedBonusPerLevel()));
        long interval = Math.max(minTicks, Math.round(baseIntervalTicks * factor));

        return new Result(interval, amount);
        // Hinweis: Doppel-Output separat per rollDoubleOutput() prüfen
    }

    /** true = Doppel-Output auslösen. RNG darf null sein (nutzt dann ThreadLocalRandom). */
    public static boolean rollDoubleOutput(PrestigeManager pm, int prestige, Random rng) {
        if (rng == null) rng = ThreadLocalRandom.current();
        double chance = prestige * pm.getRareChancePerLevel();
        return rng.nextDouble() < chance;
    }
}
