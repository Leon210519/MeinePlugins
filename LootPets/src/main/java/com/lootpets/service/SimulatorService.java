package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import com.lootpets.model.PetDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/**
 * Diagnostic "what-if" simulator for admins and designers.
 * All computations mirror {@link BoostService} formulas but avoid
 * any player state and side effects.
 */
public class SimulatorService {

    public record PetSpec(String petId, String rarityId, int level, int stars) {}

    public record ComboResult(List<BoostBreakdown.PetContribution> contributions,
                              BigDecimal uncapped, BigDecimal capped) {}

    public record SweepPoint(int level, int stars,
                             BigDecimal typed, BigDecimal uncapped, BigDecimal capped) {}

    private record Config(BigDecimal perLevel,
                          BigDecimal starMult,
                          int maxStars,
                          StackingMode stackingMode,
                          Map<String, Double> rarityBase,
                          BigDecimal cap,
                          int levelBase,
                          int levelExtra) {}

    private final LootPetsPlugin plugin;
    private final PetRegistry petRegistry;
    private final RarityRegistry rarityRegistry;

    public SimulatorService(LootPetsPlugin plugin, PetRegistry petRegistry, RarityRegistry rarityRegistry) {
        this.plugin = plugin;
        this.petRegistry = petRegistry;
        this.rarityRegistry = rarityRegistry;
    }

    private Config readConfig() {
        FileConfiguration cfg = plugin.getConfig();
        BigDecimal perLevel = BigDecimal.valueOf(cfg.getDouble("boosts.per_level", 0.02));
        BigDecimal starMult = BigDecimal.valueOf(cfg.getDouble("boosts.star_multiplier", 1.5));
        int maxStars = cfg.getInt("boosts.max_stars", 5);
        StackingMode stackingMode;
        try {
            stackingMode = StackingMode.valueOf(cfg.getString("boosts.stacking_mode", "ADDITIVE").toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            stackingMode = StackingMode.ADDITIVE;
        }
        Map<String, Double> rarityBase = new HashMap<>();
        ConfigurationSection baseSec = cfg.getConfigurationSection("boosts.rarity_base_multipliers");
        if (baseSec != null) {
            for (String key : baseSec.getKeys(false)) {
                rarityBase.put(key, baseSec.getDouble(key));
            }
        }
        BigDecimal cap = BigDecimal.valueOf(cfg.getDouble("caps.global_multiplier_max", 6.0));
        int levelBase = cfg.getInt("leveling_runtime.level_cap_base", 100);
        int levelExtra = cfg.getInt("leveling_runtime.level_cap_extra_per_star", 50);
        return new Config(perLevel, starMult, maxStars, stackingMode, rarityBase, cap, levelBase, levelExtra);
    }

    /** Compute for a single pet spec. */
    public ComboResult simulateOne(PetSpec spec, EarningType type) {
        return simulate(List.of(spec), type);
    }

    /** Compute for a combination of pet specs. */
    public ComboResult simulate(List<PetSpec> specs, EarningType type) {
        Config cfg = readConfig();
        List<BoostBreakdown.PetContribution> contrib = new ArrayList<>();
        BigDecimal add = BigDecimal.ZERO;
        BigDecimal mul = BigDecimal.ONE;
        for (PetSpec spec : specs) {
            PetDefinition def = petRegistry.byId(spec.petId());
            if (def == null) {
                continue;
            }
            int stars = Math.max(0, Math.min(spec.stars(), cfg.maxStars));
            int capLevel = cfg.levelBase + cfg.levelExtra * stars;
            int level = Math.max(0, Math.min(spec.level(), capLevel));
            BigDecimal rarityBaseVal = BigDecimal.valueOf(cfg.rarityBase.getOrDefault(spec.rarityId(), 1.0));
            BigDecimal base = rarityBaseVal
                    .multiply(BigDecimal.ONE.add(cfg.perLevel.multiply(BigDecimal.valueOf(level))))
                    .multiply(cfg.starMult.pow(stars, MathContext.DECIMAL64));
            double weight = def.weights().getOrDefault(type.weightKey(), 1.0);
            if (weight < 0) weight = 0;
            if (weight > 1) weight = 1;
            BigDecimal typed = BigDecimal.ONE.add(BigDecimal.valueOf(weight).multiply(base.subtract(BigDecimal.ONE)));
            contrib.add(new BoostBreakdown.PetContribution(spec.petId(), spec.rarityId(), level, stars, weight, typed));
            if (cfg.stackingMode == StackingMode.ADDITIVE) {
                add = add.add(typed.subtract(BigDecimal.ONE));
            } else {
                mul = mul.multiply(typed, MathContext.DECIMAL64);
            }
        }
        BigDecimal uncapped = cfg.stackingMode == StackingMode.ADDITIVE ? BigDecimal.ONE.add(add) : mul;
        if (uncapped.compareTo(BigDecimal.ONE) < 0) {
            uncapped = BigDecimal.ONE;
        }
        BigDecimal capped = uncapped.min(cfg.cap);
        if (capped.compareTo(BigDecimal.ONE) < 0) {
            capped = BigDecimal.ONE;
        }
        return new ComboResult(Collections.unmodifiableList(contrib), uncapped, capped);
    }

    /** Runs a grid sweep across levels and stars for the given pet. */
    public List<SweepPoint> sweep(String petId, String rarityId, EarningType type,
                                  int levelFrom, int levelTo, int levelStep,
                                  int starFrom, int starTo, int starStep,
                                  int maxPoints) {
        Config cfg = readConfig();
        PetDefinition def = petRegistry.byId(petId);
        if (def == null) {
            return List.of();
        }
        List<SweepPoint> points = new ArrayList<>();
        outer:
        for (int s = starFrom; s <= starTo; s += starStep) {
            int stars = Math.max(0, Math.min(s, cfg.maxStars));
            int capLevel = cfg.levelBase + cfg.levelExtra * stars;
            for (int l = levelFrom; l <= levelTo; l += levelStep) {
                int level = Math.max(0, Math.min(l, capLevel));
                BigDecimal rarityBaseVal = BigDecimal.valueOf(cfg.rarityBase.getOrDefault(rarityId, 1.0));
                BigDecimal base = rarityBaseVal
                        .multiply(BigDecimal.ONE.add(cfg.perLevel.multiply(BigDecimal.valueOf(level))))
                        .multiply(cfg.starMult.pow(stars, MathContext.DECIMAL64));
                double weight = def.weights().getOrDefault(type.weightKey(), 1.0);
                if (weight < 0) weight = 0;
                if (weight > 1) weight = 1;
                BigDecimal typed = BigDecimal.ONE.add(BigDecimal.valueOf(weight).multiply(base.subtract(BigDecimal.ONE)));
                BigDecimal uncapped = typed;
                BigDecimal capped = uncapped.min(cfg.cap);
                if (capped.compareTo(BigDecimal.ONE) < 0) capped = BigDecimal.ONE;
                points.add(new SweepPoint(level, stars, typed, uncapped, capped));
                if (points.size() >= maxPoints) {
                    break outer;
                }
            }
        }
        return points;
    }

    /** Simple numeric search for per-level constant. */
    public OptionalDouble tunePerLevel(String petId, String rarityId, EarningType type,
                                       double target, int level, int stars,
                                       int steps, double min, double max) {
        Config cfg = readConfig();
        PetDefinition def = petRegistry.byId(petId);
        if (def == null) {
            return OptionalDouble.empty();
        }
        double bestVal = cfg.perLevel.doubleValue();
        double bestErr = Double.MAX_VALUE;
        for (int i = 0; i <= steps; i++) {
            double cand = min + (max - min) * i / steps;
            BigDecimal rarityBaseVal = BigDecimal.valueOf(cfg.rarityBase.getOrDefault(rarityId, 1.0));
            BigDecimal base = rarityBaseVal
                    .multiply(BigDecimal.ONE.add(BigDecimal.valueOf(cand).multiply(BigDecimal.valueOf(level))))
                    .multiply(cfg.starMult.pow(stars, MathContext.DECIMAL64));
            double weight = def.weights().getOrDefault(type.weightKey(), 1.0);
            if (weight < 0) weight = 0;
            if (weight > 1) weight = 1;
            BigDecimal typed = BigDecimal.ONE.add(BigDecimal.valueOf(weight).multiply(base.subtract(BigDecimal.ONE)));
            BigDecimal finalVal = typed.min(cfg.cap);
            double val = finalVal.doubleValue();
            double err = Math.abs(val - target);
            if (err < bestErr) {
                bestErr = err;
                bestVal = cand;
            }
        }
        return OptionalDouble.of(bestVal);
    }

    /** Simple numeric search for star multiplier. */
    public OptionalDouble tuneStarMultiplier(String petId, String rarityId, EarningType type,
                                             double target, int level, int stars,
                                             int steps, double min, double max) {
        Config cfg = readConfig();
        PetDefinition def = petRegistry.byId(petId);
        if (def == null) {
            return OptionalDouble.empty();
        }
        double bestVal = cfg.starMult.doubleValue();
        double bestErr = Double.MAX_VALUE;
        for (int i = 0; i <= steps; i++) {
            double cand = min + (max - min) * i / steps;
            BigDecimal rarityBaseVal = BigDecimal.valueOf(cfg.rarityBase.getOrDefault(rarityId, 1.0));
            BigDecimal base = rarityBaseVal
                    .multiply(BigDecimal.ONE.add(cfg.perLevel.multiply(BigDecimal.valueOf(level))))
                    .multiply(BigDecimal.valueOf(cand).pow(stars, MathContext.DECIMAL64));
            double weight = def.weights().getOrDefault(type.weightKey(), 1.0);
            if (weight < 0) weight = 0;
            if (weight > 1) weight = 1;
            BigDecimal typed = BigDecimal.ONE.add(BigDecimal.valueOf(weight).multiply(base.subtract(BigDecimal.ONE)));
            BigDecimal finalVal = typed.min(cfg.cap);
            double val = finalVal.doubleValue();
            double err = Math.abs(val - target);
            if (err < bestErr) {
                bestErr = err;
                bestVal = cand;
            }
        }
        return OptionalDouble.of(bestVal);
    }
}
