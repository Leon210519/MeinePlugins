package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import com.lootpets.model.PetDefinition;
import com.lootpets.util.Colors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides static preview calculations for pet boosts.
 */
public class PreviewService {

    public record Range(double min, double max) {}

    private record Key(String petId, String rarityId, EarningType type) {}

    private record CacheEntry(Range range, long expire) {}

    private final LootPetsPlugin plugin;
    private final PetRegistry petRegistry;
    private final RarityRegistry rarityRegistry;
    private final Map<Key, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis = 30000L;
    private final List<EarningType> showTypes;

    public PreviewService(LootPetsPlugin plugin, PetRegistry petRegistry, RarityRegistry rarityRegistry) {
        this.plugin = plugin;
        this.petRegistry = petRegistry;
        this.rarityRegistry = rarityRegistry;
        List<EarningType> types = new ArrayList<>();
        for (String key : plugin.getConfig().getStringList("preview.show_types")) {
            EarningType.parse(key).ifPresent(types::add);
        }
        this.showTypes = Collections.unmodifiableList(types);
    }

    /**
     * Returns configured earning types to display in previews.
     */
    public List<EarningType> getShowTypes() {
        return showTypes;
    }

    /** Clears cached preview values. */
    public void clearAll() {
        cache.clear();
    }

    /**
     * Resolves a pet id case-insensitively.
     */
    public String resolvePetId(String input) {
        if (input == null) return null;
        String norm = input.trim();
        for (PetDefinition def : petRegistry.all()) {
            if (def.id().equalsIgnoreCase(norm)) {
                return def.id();
            }
        }
        return null;
    }

    /**
     * Resolves a rarity id case-insensitively.
     */
    public String resolveRarityId(String input) {
        if (input == null) return null;
        String norm = input.trim();
        for (String id : rarityRegistry.getRarities().keySet()) {
            if (id.equalsIgnoreCase(norm)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Computes the preview range for the given pet, rarity and earning type.
     * Returns {@code null} if inputs are unknown.
     */
    public Range getRange(String petId, String rarityId, EarningType type) {
        if (petId == null || rarityId == null || type == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        Key key = new Key(petId, rarityId, type);
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expire > now) {
            return entry.range;
        }
        Range range = compute(petId, rarityId, type);
        if (range != null) {
            cache.put(key, new CacheEntry(range, now + ttlMillis));
        }
        return range;
    }

    private Range compute(String petId, String rarityId, EarningType type) {
        PetDefinition def = petRegistry.byId(petId);
        if (def == null) {
            return null;
        }
        if (!rarityRegistry.getRarities().containsKey(rarityId)) {
            return null;
        }
        FileConfiguration cfg = plugin.getConfig();
        double perLevel = cfg.getDouble("boosts.per_level", 0.02);
        double starMult = cfg.getDouble("boosts.star_multiplier", 1.5);
        int maxStars = cfg.getInt("boosts.max_stars", 5);
        int levelBase = cfg.getInt("leveling_runtime.level_cap_base", 100);
        int levelExtra = cfg.getInt("leveling_runtime.level_cap_extra_per_star", 50);
        double rarityBase = 1.0;
        ConfigurationSection baseSec = cfg.getConfigurationSection("boosts.rarity_base_multipliers");
        if (baseSec != null) {
            rarityBase = baseSec.getDouble(rarityId, 1.0);
        }
        double weight = def.weights().getOrDefault(type.weightKey(), 1.0);
        if (weight < 0) weight = 0;
        if (weight > 1) weight = 1;

        double minBase = rarityBase;
        int maxLevel = levelBase + levelExtra * maxStars;
        double maxBase = rarityBase * (1 + perLevel * maxLevel) * Math.pow(starMult, maxStars);

        double minTyped = 1 + weight * (minBase - 1);
        double maxTyped = 1 + weight * (maxBase - 1);

        double cap = cfg.getDouble("preview.cap_multiplier", Double.MAX_VALUE);
        if (cap > 1) {
            minTyped = Math.min(minTyped, cap);
            maxTyped = Math.min(maxTyped, cap);
        }
        return new Range(minTyped, maxTyped);
    }

    /**
     * Formats the preview range using configuration settings.
     */
    public String formatRange(EarningType type, Range range) {
        if (range == null) return "";
        FileConfiguration cfg = plugin.getConfig();
        boolean percent = cfg.getBoolean("preview.format_percent", true);
        int percentDecimals = cfg.getInt("placeholders.format.percent_decimals", 0);
        int rawDecimals = cfg.getInt("placeholders.format.raw_decimals", 2);
        String template = Colors.color(cfg.getString("preview.template", "{min} - {max}"));
        String minStr;
        String maxStr;
        if (percent) {
            double a = (range.min - 1) * 100.0;
            double b = (range.max - 1) * 100.0;
            BigDecimal bdA = BigDecimal.valueOf(a).setScale(percentDecimals, RoundingMode.HALF_UP);
            BigDecimal bdB = BigDecimal.valueOf(b).setScale(percentDecimals, RoundingMode.HALF_UP);
            minStr = (a >= 0 ? "+" : "") + bdA.toPlainString() + "%";
            maxStr = (b >= 0 ? "+" : "") + bdB.toPlainString() + "%";
        } else {
            BigDecimal bdA = BigDecimal.valueOf(range.min).setScale(rawDecimals, RoundingMode.HALF_UP);
            BigDecimal bdB = BigDecimal.valueOf(range.max).setScale(rawDecimals, RoundingMode.HALF_UP);
            minStr = "×" + bdA.toPlainString();
            maxStr = "×" + bdB.toPlainString();
        }
        return template.replace("{type}", type.name().toLowerCase(Locale.ROOT))
                .replace("{min}", minStr)
                .replace("{max}", maxStr);
    }

    /**
     * Formats the raw preview range without symbols.
     */
    public String formatRawRange(Range range) {
        if (range == null) return "";
        int rawDecimals = plugin.getConfig().getInt("placeholders.format.raw_decimals", 2);
        BigDecimal bdA = BigDecimal.valueOf(range.min).setScale(rawDecimals, RoundingMode.HALF_UP);
        BigDecimal bdB = BigDecimal.valueOf(range.max).setScale(rawDecimals, RoundingMode.HALF_UP);
        return bdA.toPlainString() + "-" + bdB.toPlainString();
    }
}

