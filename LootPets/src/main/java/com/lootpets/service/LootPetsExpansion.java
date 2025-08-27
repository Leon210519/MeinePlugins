package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PetDefinition;
import com.lootpets.util.Colors;
import com.lootpets.service.BoostBreakdown;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlaceholderAPI expansion for LootPets.
 */
public class LootPetsExpansion extends PlaceholderExpansion {
    private final LootPetsPlugin plugin;
    private final BoostService boostService;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private final SlotService slotService;
    private final RarityRegistry rarityRegistry;
    private final Map<UUID, Map<String, CacheEntry>> cache = new ConcurrentHashMap<>();
    private final long ttlMillis = 3000L;

    private final int percentDecimals;
    private final int rawDecimals;
    private final String starSymbol;
    private final String listDelimiter;
    private final boolean colorizeNames;

    private record CacheEntry(String value, long expire) {}

    public LootPetsExpansion(LootPetsPlugin plugin) {
        this.plugin = plugin;
        this.boostService = plugin.getBoostService();
        this.petService = plugin.getPetService();
        this.petRegistry = plugin.getPetRegistry();
        this.slotService = plugin.getSlotService();
        this.rarityRegistry = plugin.getRarityRegistry();
        FileConfiguration cfg = plugin.getConfig();
        this.percentDecimals = cfg.getInt("placeholders.format.percent_decimals", 0);
        this.rawDecimals = cfg.getInt("placeholders.format.raw_decimals", 2);
        this.starSymbol = Colors.color(cfg.getString("placeholders.format.star_symbol", "★"));
        this.listDelimiter = Colors.color(cfg.getString("placeholders.format.list_delimiter", ", "));
        this.colorizeNames = cfg.getBoolean("placeholders.format.colorize_names", true);
    }

    @Override
    public String getIdentifier() {
        return "lootpets";
    }

    @Override
    public String getAuthor() {
        return "LootPets";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }

    public void clearAll() {
        cache.clear();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || identifier == null) {
            return "";
        }
        long now = System.currentTimeMillis();
        Map<String, CacheEntry> map = cache.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        CacheEntry entry = map.get(identifier);
        if (entry != null && entry.expire > now) {
            return entry.value;
        }
        String value = compute(player, identifier);
        map.put(identifier, new CacheEntry(value, now + ttlMillis));
        return value;
    }

    private String compute(Player player, String identifier) {
        try {
            if (identifier.startsWith("boost_percent_")) {
                String t = identifier.substring("boost_percent_".length());
                Optional<EarningType> type = EarningType.parse(t);
                if (type.isEmpty()) return "";
                double mult = boostService.getMultiplier(player, type.get());
                double percent = (mult - 1.0D) * 100.0D;
                BigDecimal bd = BigDecimal.valueOf(percent).setScale(percentDecimals, RoundingMode.HALF_UP);
                return (percent >= 0 ? "+" : "") + bd.toPlainString() + "%";
            }
            if (identifier.startsWith("boost_raw_")) {
                String t = identifier.substring("boost_raw_".length());
                Optional<EarningType> type = EarningType.parse(t);
                if (type.isEmpty()) return "";
                double mult = boostService.getMultiplier(player, type.get());
                BigDecimal bd = BigDecimal.valueOf(mult).setScale(rawDecimals, RoundingMode.HALF_UP);
                return bd.toPlainString();
            }
            switch (identifier) {
                case "slots" -> {
                    int max = slotService.getMaxSlots(player);
                    int active = petService.getActivePetIds(player.getUniqueId(), max).size();
                    return active + "/" + max;
                }
                case "slots_max" -> {
                    return String.valueOf(slotService.getMaxSlots(player));
                }
                case "active_count" -> {
                    int count = petService.getActivePetIds(player.getUniqueId(), Integer.MAX_VALUE).size();
                    return String.valueOf(count);
                }
                case "active_list" -> {
                    List<String> active = petService.getActivePetIds(player.getUniqueId(), Integer.MAX_VALUE);
                    Map<String, OwnedPetState> owned = petService.getOwnedPets(player.getUniqueId());
                    List<String> parts = new ArrayList<>();
                    for (String id : active) {
                        OwnedPetState st = owned.get(id);
                        PetDefinition def = petRegistry.byId(id);
                        if (st == null || def == null) continue;
                        String name = def.displayName();
                        if (colorizeNames) {
                            RarityRegistry.Rarity rar = rarityRegistry.getRarities().get(st.rarity());
                            if (rar != null) {
                                name = rar.color() + name;
                            }
                        }
                        parts.add(name + starSymbol + st.stars() + " L" + st.level());
                    }
                    return String.join(listDelimiter, parts);
                }
            }
            if (identifier.startsWith("top_pet_")) {
                String t = identifier.substring("top_pet_".length());
                Optional<EarningType> type = EarningType.parse(t);
                if (type.isEmpty()) return "";
                BoostBreakdown bd = boostService.getBreakdown(player, type.get());
                BoostBreakdown.PetContribution best = null;
                for (BoostBreakdown.PetContribution pc : bd.contributions()) {
                    if (best == null || pc.typedFactor().compareTo(best.typedFactor()) > 0) {
                        best = pc;
                    }
                }
                if (best == null) return "";
                PetDefinition def = petRegistry.byId(best.petId());
                if (def == null) return "";
                String name = def.displayName();
                if (colorizeNames) {
                    RarityRegistry.Rarity rar = rarityRegistry.getRarities().get(best.rarityId());
                    if (rar != null) {
                        name = rar.color() + name;
                    }
                }
                return name + starSymbol + best.stars() + " L" + best.level();
            }
            if (identifier.startsWith("prestige_")) {
                String id = identifier.substring("prestige_".length());
                OwnedPetState st = petService.getOwnedPets(player.getUniqueId()).get(id);
                if (st == null) return "";
                return String.valueOf(st.stars());
            }
            if (identifier.startsWith("evolve_")) {
                String id = identifier.substring("evolve_".length());
                OwnedPetState st = petService.getOwnedPets(player.getUniqueId()).get(id);
                if (st == null) return "";
                return st.evolveProgress() + "/5";
            }
            if (identifier.startsWith("levelcap_")) {
                String id = identifier.substring("levelcap_".length());
                OwnedPetState st = petService.getOwnedPets(player.getUniqueId()).get(id);
                if (st == null) return "";
                int base = plugin.getConfig().getInt("leveling_runtime.level_cap_base", 100);
                int extra = plugin.getConfig().getInt("leveling_runtime.level_cap_extra_per_star", 50);
                int cap = base + extra * st.stars();
                return String.valueOf(cap);
            }
            if (identifier.startsWith("level_")) {
                String id = identifier.substring("level_".length());
                OwnedPetState st = petService.getOwnedPets(player.getUniqueId()).get(id);
                if (st == null) return "";
                return String.valueOf(st.level());
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
