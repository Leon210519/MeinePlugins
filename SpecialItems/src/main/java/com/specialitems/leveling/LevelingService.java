package com.specialitems.leveling;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Random;

public final class LevelingService {

    private final Plugin plugin;
    private final Keys keys;
    private final Random rng = new Random();

    // Defaults (configure later if needed)
    public double xpPickaxeBlock = 1.0;
    public double xpPickaxeOre = 3.0;
    public double xpPickaxeDeepslateOre = 6.0;
    public double xpSwordKill = 5.0;
    public double xpSwordBossKill = 25.0;
    public double xpHoeHarvest = 2.0;

    public double baseProcChance = 0.10; // 10%
    public double pityIncrement = 0.02;  // +2% per miss
    public boolean allowOverCapPickaxe = false;
    public boolean allowOverCapSword = false;

    public LevelingService(Plugin plugin) {
        this.plugin = plugin;
        this.keys = new Keys(plugin);
    }

    public Keys keys() { return keys; }

    public boolean isSpecialItem(ItemStack it) {
        if (it == null) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keys.SI_ID, PersistentDataType.STRING);
    }

    public ToolClass detectToolClass(ItemStack it) {
        if (it == null) return ToolClass.OTHER;
        Material m = it.getType();
        String n = m.name().toUpperCase(Locale.ROOT);
        if (n.endsWith("_PICKAXE")) return ToolClass.PICKAXE;
        if (n.endsWith("_SWORD")) return ToolClass.SWORD;
        if (n.endsWith("_HOE")) return ToolClass.HOE;
        return ToolClass.OTHER;
    }

    public int getLevel(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return 1;
        Integer lvl = meta.getPersistentDataContainer().get(keys.LEVEL, PersistentDataType.INTEGER);
        return (lvl == null || lvl < 1) ? 1 : Math.min(lvl, 100);
    }

    public double getXp(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return 0.0;
        Double xp = meta.getPersistentDataContainer().get(keys.XP, PersistentDataType.DOUBLE);
        return xp == null ? 0.0 : xp;
    }

    public double getBonusYieldPct(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return 0.0;
        Double v = meta.getPersistentDataContainer().get(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE);
        return v == null ? 0.0 : v;
    }

    public void setBonusYieldPct(ItemStack it, double pct) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE, Math.max(0.0, pct));
        it.setItemMeta(meta);
    }

    public void ensureInit(ItemStack it) {
        if (it == null) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.LEVEL, PersistentDataType.INTEGER)) pdc.set(keys.LEVEL, PersistentDataType.INTEGER, 1);
        if (!pdc.has(keys.XP, PersistentDataType.DOUBLE)) pdc.set(keys.XP, PersistentDataType.DOUBLE, 0.0);
        it.setItemMeta(meta);
    }

    private double applyRarityXpMult(ItemStack it, double base) {
        Rarity r = RarityUtil.get(it, keys);
        return base * r.xpMultiplier;
    }

    public void grantXp(ItemStack it, double amount, ToolClass clazz) {
        if (it == null || amount <= 0) return;
        ensureInit(it);

        int level = getLevel(it);
        if (level >= 100) return;

        // apply rarity multiplier
        amount = applyRarityXpMult(it, amount);

        double xp = getXp(it) + amount;
        double need = LevelMath.neededXpFor(level);

        while (level < 100 && xp >= need) {
            xp -= need;
            level++;

            // Proc = base + pity
            double pity = PityCounter.get(it, keys);
            double chance = Math.min(1.0, baseProcChance + pity);
            boolean success = rng.nextDouble() < chance;

            if (success) {
                switch (clazz) {
                    case PICKAXE -> EnchantUtil.addOrIncrease(it, Enchantment.EFFICIENCY, 1, allowOverCapPickaxe);
                    case SWORD   -> EnchantUtil.addOrIncrease(it, Enchantment.SHARPNESS, 1, allowOverCapSword);
                    case HOE     -> setBonusYieldPct(it, getBonusYieldPct(it) + 10.0);
                    default -> {}
                }
                PityCounter.reset(it, keys);
            } else {
                PityCounter.add(it, keys, pityIncrement);
            }

            need = LevelMath.neededXpFor(level);
        }

        // persist
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(keys.XP, PersistentDataType.DOUBLE, Math.max(0.0, xp));
        it.setItemMeta(meta);
    }
}