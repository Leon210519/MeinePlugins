package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import com.lootpets.api.event.LootPetsApplyPostEvent;
import com.lootpets.api.event.LootPetsApplyPreEvent;
import com.lootpets.api.event.LootPetsMultiplierQueryEvent;
import com.lootpets.util.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates pet boost multipliers with caching and event hooks.
 */
public class BoostService {

    private final LootPetsPlugin plugin;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private final RarityRegistry rarityRegistry;
    private final RuleService ruleService;
    private final AuditService auditService;
    private final Map<UUID, EnumMap<EarningType, CacheEntry>> cache = new ConcurrentHashMap<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();
    private final Set<UUID> applyLogged = ConcurrentHashMap.newKeySet();
    private final long ttlMillis = 4000L;

    public BoostService(LootPetsPlugin plugin,
                        PetService petService,
                        PetRegistry petRegistry,
                        RarityRegistry rarityRegistry,
                        RuleService ruleService,
                        AuditService auditService) {
        this.plugin = plugin;
        this.petService = petService;
        this.petRegistry = petRegistry;
        this.rarityRegistry = rarityRegistry;
        this.ruleService = ruleService;
        this.auditService = auditService;
    }

    private record CacheEntry(double value, long expire, BoostBreakdown breakdown) {}

    /**
     * Returns the current multiplier for the player and type.
     */
    public double getMultiplier(Player player, EarningType type) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        EnumMap<EarningType, CacheEntry> map = cache.computeIfAbsent(uuid, u -> new EnumMap<>(EarningType.class));
        CacheEntry entry = map.get(type);
        if (entry != null && entry.expire > now) {
            return entry.value;
        }
        BoostBreakdown breakdown = compute(player, type);
        double val = breakdown.finalMultiplier().doubleValue();
        map.put(type, new CacheEntry(val, now + ttlMillis, breakdown));
        return val;
    }

    /**
     * Applies the multiplier to the given base value.
     */
    public BigDecimal apply(Player player, EarningType type, BigDecimal base) {
        if (!ruleService.canApply(player)) {
            if (applyLogged.add(player.getUniqueId())) {
                DebugLogger.debug(plugin, "boosts", "apply denied for " + player.getName());
            }
            LootPetsApplyPostEvent post = new LootPetsApplyPostEvent(player, type, base, 1.0, base);
            callEvent(post);
            return base;
        }
        double mult = getMultiplier(player, type);
        LootPetsApplyPreEvent pre = new LootPetsApplyPreEvent(player, type, base, mult);
        callEvent(pre);
        if (pre.isCancelled()) {
            LootPetsApplyPostEvent post = new LootPetsApplyPostEvent(player, type, base, 1.0, base);
            callEvent(post);
            auditService.log(player, type, base, 1.0, base, null);
            return base;
        }
        double finalMult = pre.getMultiplier();
        BigDecimal result = base.multiply(BigDecimal.valueOf(finalMult));
        LootPetsApplyPostEvent post = new LootPetsApplyPostEvent(player, type, base, finalMult, result);
        callEvent(post);
        BoostBreakdown bd = null;
        if (auditService.isEnabled() && auditService.isIncludeBreakdown()) {
            bd = getBreakdown(player, type);
        }
        auditService.log(player, type, base, finalMult, result, bd);
        return result;
    }

    /**
     * Returns a detailed breakdown of the current boost computation.
     */
    public BoostBreakdown getBreakdown(Player player, EarningType type) {
        getMultiplier(player, type); // ensure cache
        EnumMap<EarningType, CacheEntry> map = cache.get(player.getUniqueId());
        if (map == null) {
            return new BoostBreakdown(List.of(), StackingMode.ADDITIVE, BigDecimal.ONE, BigDecimal.ONE);
        }
        CacheEntry entry = map.get(type);
        if (entry == null) {
            return new BoostBreakdown(List.of(), StackingMode.ADDITIVE, BigDecimal.ONE, BigDecimal.ONE);
        }
        return entry.breakdown;
    }

    /**
     * Invalidates cached multipliers for the given player.
     */
    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }

    /** Clears all cached data. */
    public void clearAll() {
        cache.clear();
        applyLogged.clear();
    }

    private BoostBreakdown compute(Player player, EarningType type) {
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
        ConfigurationSection baseSec = cfg.getConfigurationSection("boosts.rarity_base_multipliers");
        Map<String, Double> rarityBase = new HashMap<>();
        if (baseSec != null) {
            for (String key : baseSec.getKeys(false)) {
                rarityBase.put(key, baseSec.getDouble(key));
            }
        }
        BigDecimal cap = BigDecimal.valueOf(cfg.getDouble("caps.global_multiplier_max", 6.0));
        int levelBase = cfg.getInt("leveling_runtime.level_cap_base", 100);
        int levelExtra = cfg.getInt("leveling_runtime.level_cap_extra_per_star", 50);

        List<String> active = petService.getActivePetIds(player.getUniqueId(), Integer.MAX_VALUE);
        Map<String, OwnedPetState> owned = petService.getOwnedPets(player.getUniqueId());

        List<BoostBreakdown.PetContribution> contributions = new ArrayList<>();
        List<LootPetsMultiplierQueryEvent.Contribution> evContrib = new ArrayList<>();
        BigDecimal add = BigDecimal.ZERO;
        BigDecimal mul = BigDecimal.ONE;

        for (String petId : active) {
            OwnedPetState state = owned.get(petId);
            if (state == null) {
                warnOnce("Missing pet state for id " + petId);
                continue;
            }
            PetDefinition def = petRegistry.byId(petId);
            if (def == null) {
                warnOnce("Missing pet definition for id " + petId);
                continue;
            }
            String rarityId = state.rarity();
            if (rarityId == null || !rarityRegistry.getRarities().containsKey(rarityId)) {
                warnOnce("Pet " + petId + " has unknown rarity " + rarityId);
                continue;
            }
            int stars = Math.max(0, Math.min(state.stars(), maxStars));
            int capLevel = levelBase + levelExtra * stars;
            int level = Math.max(0, Math.min(state.level(), capLevel));

            BigDecimal rarityBaseVal = BigDecimal.valueOf(rarityBase.getOrDefault(rarityId, 1.0));
            BigDecimal base = rarityBaseVal
                    .multiply(BigDecimal.ONE.add(perLevel.multiply(BigDecimal.valueOf(level))))
                    .multiply(starMult.pow(stars, MathContext.DECIMAL64));

            double weight = def.weights().getOrDefault(type.weightKey(), 1.0);
            if (weight < 0) {
                weight = 0;
            }
            if (weight > 1) {
                weight = 1;
            }
            BigDecimal typed = BigDecimal.ONE.add(BigDecimal.valueOf(weight).multiply(base.subtract(BigDecimal.ONE)));

            contributions.add(new BoostBreakdown.PetContribution(petId, rarityId, level, stars, weight, typed));
            evContrib.add(new LootPetsMultiplierQueryEvent.Contribution(petId, rarityId, level, stars, weight, typed.doubleValue()));

            if (stackingMode == StackingMode.ADDITIVE) {
                add = add.add(typed.subtract(BigDecimal.ONE));
            } else {
                mul = mul.multiply(typed, MathContext.DECIMAL64);
            }
        }

        BigDecimal uncapped = stackingMode == StackingMode.ADDITIVE ? BigDecimal.ONE.add(add) : mul;
        if (uncapped.compareTo(BigDecimal.ONE) < 0) {
            uncapped = BigDecimal.ONE;
        }
        BigDecimal finalVal = uncapped.min(cap);
        if (finalVal.compareTo(BigDecimal.ONE) < 0) {
            finalVal = BigDecimal.ONE;
        }

        LootPetsMultiplierQueryEvent event = new LootPetsMultiplierQueryEvent(player, type,
                uncapped.doubleValue(), finalVal.doubleValue(), stackingMode, evContrib);
        callEvent(event);
        uncapped = BigDecimal.valueOf(event.getUncappedMultiplier());
        finalVal = BigDecimal.valueOf(event.getCappedMultiplier());
        if (uncapped.compareTo(BigDecimal.ONE) < 0) {
            uncapped = BigDecimal.ONE;
        }
        if (finalVal.compareTo(BigDecimal.ONE) < 0) {
            finalVal = BigDecimal.ONE;
        }

        return new BoostBreakdown(Collections.unmodifiableList(contributions), stackingMode, uncapped, finalVal);
    }

    private void callEvent(Event e) {
        try {
            Bukkit.getPluginManager().callEvent(e);
        } catch (Exception ex) {
            plugin.getLogger().warning("Event error: " + ex.getMessage());
        }
    }

    private void warnOnce(String msg) {
        if (warned.add(msg)) {
            plugin.getLogger().warning(msg);
        }
    }
}
