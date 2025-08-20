package com.specialitems.leveling;

import com.specialitems.util.TemplateItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
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

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.REDSTONE_ORE,
            Material.LAPIS_ORE, Material.COPPER_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART
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
        meta.setUnbreakable(true);
        try {
            meta.setEnchantmentGlintOverride(true);
        } catch (NoSuchMethodError err) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        item.setItemMeta(meta);
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
        return 0.0; // not used in this simplified system
    }

    public ToolClass detectToolClass(ItemStack it) {
        if (it == null) return ToolClass.OTHER;
        String n = it.getType().name();
        if (n.endsWith("_PICKAXE")) return ToolClass.PICKAXE;
        if (n.endsWith("_HOE")) return ToolClass.HOE;
        if (n.endsWith("_SWORD")) return ToolClass.SWORD;
        if (n.endsWith("_AXE")) return ToolClass.AXE;
        return ToolClass.OTHER;
    }

    // ---------------------------------------------------------------------
    // XP handling
    // ---------------------------------------------------------------------
    public void grantXp(Player player, ItemStack item, int amount, ToolClass clazz) {
        if (!isSpecialItem(item)) return;
        initItem(item);
        addXp(player, item, amount);
    }

    private void addXp(Player player, ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int level = getLevel(item);
        int xp = getXp(item) + amount;
        int needed = (int) LevelMath.neededXpFor(level);
        boolean leveled = false;
        while (xp >= needed) {
            xp -= needed;
            level++;
            needed = (int) LevelMath.neededXpFor(level);
            leveled = true;
        }
        pdc.set(keys.LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(keys.XP, PersistentDataType.INTEGER, xp);
        item.setItemMeta(meta);
        if (leveled && player != null) {
            boolean enchanted = maybeEnchant(item, level);
            String itemName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : item.getType().name();
            String msg = ChatColor.GOLD + itemName + ChatColor.GREEN + " reached level " + level;
            if (enchanted) msg += ChatColor.AQUA + " with a bonus enchant!";
            player.sendMessage(msg);
        }
    }

    private boolean maybeEnchant(ItemStack item, int level) {
        double chance = Math.min(1.0, 0.25 * level);
        if (random.nextDouble() > chance) return false;
        Enchantment ench = selectEnchant(item.getType());
        if (ench == null) return false;
        ItemMeta meta = item.getItemMeta();
        int current = meta.getEnchantLevel(ench);
        int newLevel = Math.min(current + 1, ench.getMaxLevel());
        if (newLevel <= current) return false;
        meta.addEnchant(ench, newLevel, true);
        item.setItemMeta(meta);
        return true;
    }

    private Enchantment selectEnchant(Material mat) {
        String n = mat.name();
        if (n.endsWith("_SWORD")) return Enchantment.SHARPNESS;
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS"))
            return Enchantment.PROTECTION;
        if (n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_HOE") || n.endsWith("_SHOVEL"))
            return Enchantment.EFFICIENCY;
        return Enchantment.UNBREAKING;
    }

    // ---------------------------------------------------------------------
    // Event hooks
    // ---------------------------------------------------------------------
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (!isSpecialItem(tool)) return;
        Material toolMat = tool.getType();
        Material block = e.getBlock().getType();
        if (isPickaxe(toolMat) && ORES.contains(block)) {
            grantXp(p, tool, XP_PER_ACTION, ToolClass.PICKAXE);
        } else if (isHoe(toolMat) && CROPS.contains(block) && isFullyGrown(e.getBlock())) {
            grantXp(p, tool, XP_PER_ACTION, ToolClass.HOE);
        }
    }

    public void onItemDamage(PlayerItemDamageEvent e) {
        if (isSpecialItem(e.getItem())) {
            e.setCancelled(true);
        }
    }

    private boolean isPickaxe(Material m) { return m.name().endsWith("_PICKAXE"); }
    private boolean isHoe(Material m) { return m.name().endsWith("_HOE"); }

    private boolean isFullyGrown(Block block) {
        if (!(block.getBlockData() instanceof Ageable age)) return false;
        return age.getAge() >= age.getMaximumAge();
    }
}
