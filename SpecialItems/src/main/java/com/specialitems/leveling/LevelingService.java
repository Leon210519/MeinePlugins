package com.specialitems.leveling;

import com.specialitems.util.TemplateItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Minimal leveling service implementing item-bound leveling for SpecialItems.
 * Items are considered special only when they have custom model data.
 */
public class LevelingService {

    private final Keys keys;
    private final Random random = new Random();

    private static final int XP_PER_ACTION = 1;

    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.SWEET_BERRY_BUSH,
            Material.COCOA,
            Material.PUMPKIN,
            Material.MELON,
            Material.PUMPKIN_STEM,
            Material.ATTACHED_PUMPKIN_STEM,
            Material.MELON_STEM,
            Material.ATTACHED_MELON_STEM,
            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP
    );

    public LevelingService(JavaPlugin plugin) {
        this.keys = new Keys(plugin);
    }

    // ---------------------------------------------------------------------
    // Identification
    // ---------------------------------------------------------------------
    public boolean isSpecialItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.hasCustomModelData()) {
            if (!TemplateItems.applyTemplateMeta(item)) {
                return false;
            }
            initItem(item); // finalize newly matched template
            meta = item.getItemMeta();
        }
        return meta != null && meta.hasCustomModelData();
    }

    // ---------------------------------------------------------------------
    // Initialization
    // ---------------------------------------------------------------------
    public void initItem(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.SI_ID, PersistentDataType.STRING)) {
            pdc.set(keys.SI_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
        }
        if (!pdc.has(keys.LEVEL, PersistentDataType.INTEGER)) {
            pdc.set(keys.LEVEL, PersistentDataType.INTEGER, 1);
        }
        if (!pdc.has(keys.XP, PersistentDataType.INTEGER)) {
            pdc.set(keys.XP, PersistentDataType.INTEGER, 0);
        }
        ToolClass clazz = detectToolClass(item);
        if (clazz == ToolClass.HOE) {
            if (!pdc.has(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE)) {
                pdc.set(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE, 0.0);
            }
        } else {
            pdc.remove(keys.BONUS_YIELD_PCT);
        }
        meta.setUnbreakable(true);
        try {
            meta.setEnchantmentGlintOverride(true);
        } catch (NoSuchMethodError err) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        item.setItemMeta(meta);
        try {
            com.specialitems.util.LoreRenderer.updateItemLore(item);
        } catch (Throwable ignored) {}
    }

    // ---------------------------------------------------------------------
    // Basic getters
    // ---------------------------------------------------------------------
    public int getLevel(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return 1;
        Integer lvl = meta.getPersistentDataContainer().get(keys.LEVEL, PersistentDataType.INTEGER);
        return lvl == null ? 1 : lvl;
    }

    public int getXp(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return 0;
        Integer xp = meta.getPersistentDataContainer().get(keys.XP, PersistentDataType.INTEGER);
        return xp == null ? 0 : xp;
    }

    public double getBonusYieldPct(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) return 0.0;
        Double val = meta.getPersistentDataContainer().get(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE);
        return val == null ? 0.0 : val;
    }

    public ToolClass detectToolClass(ItemStack it) {
        if (it == null) return ToolClass.OTHER;
        String n = it.getType().name();
        if (n.endsWith("_PICKAXE")) return ToolClass.PICKAXE;
        if (n.endsWith("_HOE")) return ToolClass.HOE;
        if (n.endsWith("_SWORD")) return ToolClass.SWORD;
        if (n.endsWith("_AXE")) return ToolClass.AXE;
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS"))
            return ToolClass.ARMOR;
        return ToolClass.OTHER;
    }

    // ---------------------------------------------------------------------
    // XP handling
    // ---------------------------------------------------------------------
    public void grantXp(Player player, ItemStack item, int amount, ToolClass clazz) {
        if (!isSpecialItem(item)) return;
        initItem(item);
        addXp(player, item, amount, clazz);
    }

    private void addXp(Player player, ItemStack item, int amount, ToolClass clazz) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int level = getLevel(item);
        int xp = getXp(item) + amount;
        int needed = (int) LevelMath.neededXpFor(level);
        boolean leveled = false;
        boolean reward = false;
        while (xp >= needed) {
            xp -= needed;
            level++;
            needed = (int) LevelMath.neededXpFor(level);
            leveled = true;
            reward |= handleLevelReward(item, clazz); // apply reward per level gained
        }
        pdc.set(keys.LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(keys.XP, PersistentDataType.INTEGER, xp);
        item.setItemMeta(meta);
        try {
            com.specialitems.util.LoreRenderer.updateItemLore(item);
        } catch (Throwable ignored) {}
        if (leveled && player != null) {
            String itemName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : item.getType().name();
            String msg = ChatColor.GOLD + itemName + ChatColor.GREEN + " reached level " + level;
            if (reward) msg += ChatColor.AQUA + " with a bonus enchant!";
            player.sendMessage(msg);
        }
    }

    private boolean handleLevelReward(ItemStack item, ToolClass clazz) {
        if (clazz == ToolClass.HOE) {
            increaseHoeYield(item);
            return false;
        }
        return maybeEnchant(item, clazz);
    }

    private boolean maybeEnchant(ItemStack item, ToolClass clazz) {
        if (random.nextDouble() >= 0.01) return false;
        Enchantment ench;
        switch (clazz) {
            case SWORD, AXE -> ench = Enchantment.SHARPNESS;
            case PICKAXE -> ench = Enchantment.FORTUNE;
            case ARMOR -> ench = Enchantment.PROTECTION;
            default -> {
                return false;
            }
        }
        ItemMeta meta = item.getItemMeta();
        int current = meta.getEnchantLevel(ench);
        int newLevel = Math.min(current + 1, ench.getMaxLevel());
        if (newLevel <= current) return false;
        meta.addEnchant(ench, newLevel, true);
        item.setItemMeta(meta);
        return true;
    }

    private void increaseHoeYield(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        double current = 0.0;
        Double stored = pdc.get(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE);
        if (stored != null) current = stored;
        current += 2.0;
        pdc.set(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE, current);
        item.setItemMeta(meta);
    }

    // ---------------------------------------------------------------------
    // Event hooks
    // ---------------------------------------------------------------------
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (!isSpecialItem(tool)) return;
        ToolClass clazz = detectToolClass(tool);
        if (clazz == ToolClass.PICKAXE) {
            grantXp(p, tool, XP_PER_ACTION, ToolClass.PICKAXE);
        } else if (clazz == ToolClass.HOE && isValidCrop(e.getBlock())) {
            grantXp(p, tool, XP_PER_ACTION, ToolClass.HOE);
        }
    }

    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!isSpecialItem(weapon)) return;
        ToolClass clazz = detectToolClass(weapon);
        if (clazz == ToolClass.SWORD || clazz == ToolClass.AXE) {
            grantXp(killer, weapon, XP_PER_ACTION, clazz);
        }
    }

    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) return;
        for (ItemStack armor : p.getInventory().getArmorContents()) {
            if (armor != null) {
                grantXp(p, armor, XP_PER_ACTION, ToolClass.ARMOR);
            }
        }
    }

    public void onItemDamage(PlayerItemDamageEvent e) {
        if (isSpecialItem(e.getItem())) {
            e.setCancelled(true);
        }
    }

    private boolean isValidCrop(Block block) {
        Material type = block.getType();
        if (!CROPS.contains(type)) return false;
        if (block.getBlockData() instanceof Ageable age) {
            return age.getAge() >= age.getMaximumAge();
        }
        return true;
    }
}
