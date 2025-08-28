package com.lootfactory.factory;

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.util.Cfg;
import com.lootfactory.util.Items;
import com.lootfactory.util.Msg;
import com.lootfactory.prestige.PrestigeStars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import com.lootpets.api.LootPetsAPI;
import com.lootpets.api.EarningType;
import java.math.BigDecimal;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FactoryManager {
    private final LootFactoryPlugin plugin;
    private final Map<String, FactoryDef> defs = new HashMap<>();

    private final Map<Location, FactoryInstance> byLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Set<FactoryInstance>> byOwner = new ConcurrentHashMap<>();
    private BukkitTask ticker;

    private final NamespacedKey keyType, keyLevel, keyXP, keyPrestige;

    private final File dataFile;
    private YamlConfiguration data;

    public FactoryManager(LootFactoryPlugin plugin) {
        this.plugin = plugin;
        this.keyType = new NamespacedKey(plugin, "factory_type");
        this.keyLevel = new NamespacedKey(plugin, "factory_level");
        this.keyXP = new NamespacedKey(plugin, "factory_xp");
        this.keyPrestige = new NamespacedKey(plugin, "factory_prestige");
        this.dataFile = new File(plugin.getDataFolder(), "factories.yml");
    }

    public void load() {
        plugin.reloadConfig();
        loadDefs();
        try {
            if (!dataFile.exists()) {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            }
        } catch (Exception ignore) {}

        data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = data.getConfigurationSection("factories");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection fs = sec.getConfigurationSection(id);
                try {
                    UUID uuid = UUID.fromString(id);
                    UUID owner = UUID.fromString(Objects.requireNonNull(fs.getString("owner")));
                    String typeId = fs.getString("type");
                    int level = fs.getInt("level", 1);
                    double xp = fs.getDouble("xp", 0d);
                    int prestige = fs.getInt("prestige", 0);
                    String world = fs.getString("world");
                    int x = fs.getInt("x"), y = fs.getInt("y"), z = fs.getInt("z");
                    Location loc = (world != null && Bukkit.getWorld(world) != null)
                            ? new Location(Bukkit.getWorld(world), x, y, z) : null;

                    FactoryInstance fi = new FactoryInstance(uuid, owner, typeId, level, xp, loc, prestige);
                    if (loc != null) byLocation.put(loc, fi);
                    byOwner.computeIfAbsent(owner, k -> new HashSet<>()).add(fi);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed loading factory " + id + ": " + ex.getMessage());
                }
            }
        }
    }

    private void loadDefs() {
        defs.clear();
        FileConfiguration c = plugin.getConfig();
        ConfigurationSection fs = c.getConfigurationSection("factories");
        if (fs == null) return;

        for (String id : fs.getKeys(false)) {
            ConfigurationSection d = fs.getConfigurationSection(id);
            if (d == null) continue;
            String display = d.getString("display");
            Material mat = Material.matchMaterial(Objects.requireNonNull(d.getString("material")));
            double baseAmt = d.getDouble("base_amount");
            double baseInt = d.getDouble("base_interval_seconds");
            FactoryRarity rarity = FactoryRarity.valueOf(d.getString("rarity"));
            double yb = d.getDouble("yield_bonus_pct", 0d);
            double sb = d.getDouble("speed_bonus_pct", 0d);
            FactoryDef def = new FactoryDef(id, display, mat, baseAmt, baseInt, rarity, yb, sb);
            defs.put(id, def);
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection sec = out.createSection("factories");

        for (FactoryInstance fi : allFactories()) {
            ConfigurationSection fs = sec.createSection(fi.uuid.toString());
            fs.set("owner", fi.owner.toString());
            fs.set("type", fi.typeId);
            fs.set("level", fi.level);
            fs.set("xp", fi.xpSeconds);
            fs.set("prestige", fi.prestige);
            if (fi.location != null) {
                fs.set("world", fi.location.getWorld().getName());
                fs.set("x", fi.location.getBlockX());
                fs.set("y", fi.location.getBlockY());
                fs.set("z", fi.location.getBlockZ());
            }
        }
        try { out.save(dataFile); } catch (IOException ignored) {}
    }

    public void startTicker() {
        stopTicker();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stopTicker() { if (ticker != null) ticker.cancel(); }

    private void tick() {
        double seconds = 1.0;
        double xpPerLevel = plugin.getConfig().getDouble("xp.seconds_per_level", 300d);
        double yMul = plugin.getConfig().getDouble("leveling.yield_multiplier_per_level", 1.5);
        double sMul = plugin.getConfig().getDouble("leveling.speed_multiplier_per_level", 1.5);
        String cur = plugin.getConfig().getString("economy.currency_symbol", "$");

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID u = p.getUniqueId();
            Set<FactoryInstance> set = byOwner.getOrDefault(u, Collections.emptySet());
            for (FactoryInstance fi : set) {
                if (fi.location == null) continue;
                FactoryDef def = defs.get(fi.typeId);
                if (def == null) continue;

                double yield = def.baseAmount * Math.pow(yMul, fi.level - 1);
                double interval = def.baseIntervalSec / Math.pow(sMul, fi.level - 1);

                // instance prestige
                double prestigeMul = 1.0 + Math.max(0, fi.prestige);
                yield *= prestigeMul;

                yield *= (1.0 + def.yieldBonusPct / 100.0);
                interval /= (1.0 + def.speedBonusPct / 100.0);

                double minInterval = plugin.getConfig().getDouble("leveling.min_interval_seconds", 1.0);
                if (interval < minInterval) interval = minInterval;

                fi.productionAccum += seconds;
                while (fi.productionAccum >= interval) {
                    fi.productionAccum -= interval;
                    BigDecimal boosted = LootPetsAPI.apply(p, EarningType.EARNINGS_LOOTFACTORY, BigDecimal.valueOf(yield));
                    LootFactoryPlugin.get().eco().deposit(p, boosted.doubleValue());
                    p.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(
                            Msg.color("&a+" + cur + String.format("%.2f", boosted.doubleValue()) + " &7from &e" + def.display)
                        )
                    );
                }

                fi.xpSeconds += seconds;
                if (fi.xpSeconds >= xpPerLevel) {
                    fi.xpSeconds -= xpPerLevel;
                    fi.level += 1;
                    p.sendMessage(Msg.prefix() + Msg.color("&aYour &e" + def.display + " &ahas reached &eLevel " + fi.level + "&a!"));
                }
            }
        }
    }

    public int calcMaxSlots(UUID player) {
        int base = plugin.getConfig().getInt("slots.base", 5);
        int bonus = byOwner.getOrDefault(player, Collections.emptySet()).stream()
                .filter(fi -> fi.location != null)
                .mapToInt(fi -> fi.level / 100)
                .sum();
        return base + bonus;
    }

    public int countPlaced(UUID player) { return (int) byOwner.getOrDefault(player, Collections.emptySet()).stream().filter(fi -> fi.location != null).count(); }
    public boolean canPlace(UUID player) { return countPlaced(player) < calcMaxSlots(player); }

    public Collection<FactoryInstance> getFactories(UUID owner) {
        return Collections.unmodifiableCollection(byOwner.getOrDefault(owner, Collections.emptySet()));
    }

    /* ====== Item creation / reading ====== */

    public ItemStack createFactoryItem(String typeId, int level, double xpSeconds, int prestige) {
        FactoryDef def = defs.get(typeId);
        if (def == null) {
            def = new FactoryDef(typeId, typeId, Material.BLAST_FURNACE, 5.0, 10.0, FactoryRarity.COMMON, 0.0, 0.0);
        }

        Material mat = (def.material != null ? def.material : Material.BLAST_FURNACE);
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();

        // Name in rarity color + stars for THIS instance prestige
        String col = rarityColor(def.rarity);
        String nameWithStars = PrestigeStars.withStarsLegacy(def.display, Math.max(0, prestige));
        meta.setDisplayName(Msg.color(col + nameWithStars));

        // Lore
        List<String> lore = new ArrayList<>();
        lore.add(Msg.color("&7Rarity: " + col + pretty(def.rarity)));
        lore.add(Msg.color("&7Level: &a" + level));
        lore.add(Msg.color("&7Prestige: &e" + Math.max(0, prestige)));
        lore.add(Msg.color("&7Perks: &a+" + (int) def.yieldBonusPct + "% Yield &7/ &a+" + (int) def.speedBonusPct + "% Speed"));
        meta.setLore(lore);

        // Glint (visual only)
        try {
            meta.setEnchantmentGlintOverride(true);
        } catch (Throwable t) {
            is.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyType, PersistentDataType.STRING, typeId);
        pdc.set(keyLevel, PersistentDataType.INTEGER, level);
        pdc.set(keyXP, PersistentDataType.DOUBLE, xpSeconds);
        pdc.set(keyPrestige, PersistentDataType.INTEGER, Math.max(0, prestige));

        is.setItemMeta(meta);
        return is;
    }

    // Backwards-compat: delegate to prestige=0 and DO NOT add owner-based stars.
    public ItemStack createFactoryItem(UUID owner, String typeId, int level, double xpSeconds) {
        return createFactoryItem(typeId, level, xpSeconds, 0);
    }

    public ItemStack createFactoryItem(String typeId, int level, double xpSeconds) {
        return createFactoryItem(typeId, level, xpSeconds, 0);
    }

    public boolean isFactoryItem(ItemStack stack) { return Items.hasPdc(stack, keyType, PersistentDataType.STRING); }
    public String getItemType(ItemStack stack) { return Items.getPdc(stack, keyType, PersistentDataType.STRING); }
    public int getItemLevel(ItemStack stack) { Integer v = Items.getPdc(stack, keyLevel, PersistentDataType.INTEGER); return v == null ? 1 : v; }
    public double getItemXP(ItemStack stack) { Double v = Items.getPdc(stack, keyXP, PersistentDataType.DOUBLE); return v == null ? 0d : v; }
    public int getItemPrestige(ItemStack stack) { Integer v = Items.getPdc(stack, keyPrestige, PersistentDataType.INTEGER); return v == null ? 0 : v; }

    public void placeFactory(Player p, Location loc, String typeId, int level, double xpSeconds, int prestige) {
        FactoryInstance fi = new FactoryInstance(UUID.randomUUID(), p.getUniqueId(), typeId, level, xpSeconds, loc, Math.max(0, prestige));
        byLocation.put(loc, fi);
        byOwner.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>()).add(fi);
    }

    public void placeFactory(Player p, Location loc, String typeId, int level, double xpSeconds) {
        placeFactory(p, loc, typeId, level, xpSeconds, 0);
    }

    public void placeFactoryFromItem(Player p, Location loc, ItemStack stack) {
        String typeId = getItemType(stack);
        if (typeId == null) return;
        int level = getItemLevel(stack);
        double xp = getItemXP(stack);
        int prestige = getItemPrestige(stack);
        placeFactory(p, loc, typeId, level, xp, prestige);
    }

    public FactoryInstance getAt(Location loc) { return byLocation.get(loc); }
    public FactoryInstance removeAt(Location loc) {
        FactoryInstance fi = byLocation.remove(loc);
        if (fi != null) {
            Set<FactoryInstance> set = byOwner.get(fi.owner);
            if (set != null) set.remove(fi);
        }
        return fi;
    }

    public FactoryDef getDef(String id) { return defs.get(id); }
    public Collection<FactoryDef> getAllDefs() { return defs.values(); }
    public Collection<FactoryInstance> allFactories() {
        Set<FactoryInstance> out = new HashSet<>();
        for (Set<FactoryInstance> s : byOwner.values()) out.addAll(s);
        return out;
    }

    public String randomFactoryId() {
        Map<FactoryRarity, Double> weights = Cfg.getRarityWeights(plugin.getConfig());
        Map<FactoryRarity, List<FactoryDef>> byR = new HashMap<>();
        for (FactoryDef d : getAllDefs()) byR.computeIfAbsent(d.rarity, k -> new ArrayList<>()).add(d);

        double r = Math.random(), sum = 0;
        FactoryRarity chosen = FactoryRarity.COMMON;
        for (FactoryRarity fr : FactoryRarity.values()) {
            double w = weights.getOrDefault(fr, 0.0);
            sum += w;
            if (r <= sum) { chosen = fr; break; }
        }
        List<FactoryDef> pick = byR.getOrDefault(chosen, new ArrayList<>(getAllDefs()));
        if (pick.isEmpty()) pick = new ArrayList<>(getAllDefs());
        FactoryDef def = pick.get(new Random().nextInt(pick.size()));
        return def.id;
    }

    private static String rarityColor(FactoryRarity r) {
        if (r == null) return "&7";
        switch (r) {
            case COMMON: return "&7";
            case UNCOMMON: return "&a";
            case RARE: return "&9";
            case EPIC: return "&5";
            case LEGENDARY: return "&6";
            case INSANE: return "&d";
            default: return "&f";
        }
    }

    private static String pretty(FactoryRarity r) {
        if (r == null) return "Common";
        String n = r.name().toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
