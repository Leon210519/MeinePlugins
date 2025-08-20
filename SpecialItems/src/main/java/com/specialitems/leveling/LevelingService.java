package com.specialitems.leveling;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import com.specialitems.util.ItemUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Handles XP, levels, yield and enchants for special items. */
public class LevelingService {
    private final JavaPlugin plugin;
    private final ItemTagUtil tags;
    private final RegionService regions;
    private final Random rng = new Random();

    // config values
    private final double xpMine, xpFarm, xpMelee, xpRanged, xpTaken;
    private final int xpCap;
    private final double yieldPerLevel, yieldMax;
    private final double enchantChance;
    private final List<Enchantment> enchantPool = new ArrayList<>();
    private final int enchantLevelMin, enchantLevelMax;
    private final String loreLevelFmt, loreXpFmt, loreYieldMineFmt, loreYieldFarmFmt;
    private final String msgLevelUp, msgEnchant;
    private final List<String> veinLore;
    private final int veinMax, harvesterMax;

    private final Map<java.util.UUID, Integer> pendingMulti = new HashMap<>();

    public LevelingService(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.xpMine = cfg.getDouble("leveling.xp_per_action.MINE_BLOCK_BREAK", 3.0);
        this.xpFarm = cfg.getDouble("leveling.xp_per_action.FARM_CROP_BREAK", 1.0);
        this.xpMelee = cfg.getDouble("leveling.xp_per_action.MELEE_HIT", 2.0);
        this.xpRanged = cfg.getDouble("leveling.xp_per_action.RANGED_HIT", 2.0);
        this.xpTaken = cfg.getDouble("leveling.xp_per_action.TAKEN_HIT", 1.0);
        this.xpCap = cfg.getInt("leveling.per_action_xp_cap", 200);

        this.yieldPerLevel = cfg.getDouble("leveling.yield.per_level_pct", 2.0);
        this.yieldMax = cfg.getDouble("leveling.yield.max_pct", 200.0);

        this.enchantChance = cfg.getDouble("leveling.enchants.chance_pct", 1.0) / 100.0;
        this.enchantLevelMin = cfg.getInt("leveling.enchants.level_min", 1);
        this.enchantLevelMax = cfg.getInt("leveling.enchants.level_max", 10);
        for (String id : cfg.getStringList("leveling.enchants.pool")) {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(id.toLowerCase(Locale.ROOT)));
            if (ench == null) ench = Enchantment.getByName(id.toUpperCase(Locale.ROOT));
            if (ench != null) enchantPool.add(ench);
        }

        this.loreLevelFmt = cfg.getString("leveling.display.lore.level", "Level: %LEVEL%");
        this.loreXpFmt = cfg.getString("leveling.display.lore.xp", "XP: %XP%/%NEED%");
        this.loreYieldMineFmt = cfg.getString("leveling.display.lore.yield_mine", "Yield: +%YIELD%% (mine)");
        this.loreYieldFarmFmt = cfg.getString("leveling.display.lore.yield_farm", "Yield: +%YIELD%% (farm)");
        this.msgLevelUp = cfg.getString("leveling.messages.level_up", "%ITEM% leveled up to %LEVEL%");
        this.msgEnchant = cfg.getString("leveling.messages.extra_enchant", "%ITEM% gained %ENCHANT% %LEVEL%");

        this.tags = new ItemTagUtil(plugin);
        this.regions = new RegionService(cfg.getConfigurationSection("leveling.regions"));
        this.veinLore = new ArrayList<>();
        for (String s : cfg.getStringList("compat.veinminer.detect_lore")) {
            veinLore.add(s.toLowerCase(Locale.ROOT));
        }
        this.veinMax = Math.max(1, cfg.getInt("compat.veinminer.max_blocks", 64));
        this.harvesterMax = Math.max(1, cfg.getInt("compat.harvester.max_blocks", 64));
    }

    // --- Initialization ---
    public void initItem(ItemStack item) {
        String type = detectType(item.getType());
        tags.init(item, type);
        if (type.equals("pickaxe") || type.equals("hoe")) {
            double yield = Math.min(tags.getLevel(item) * yieldPerLevel, yieldMax);
            tags.setYield(item, yield);
        }
        updateLore(item);
    }

    public boolean isSpecialItem(ItemStack item) {
        return item != null && tags.isTagged(item);
    }

    // Simple getters used by commands/utilities
    public int getLevel(ItemStack item) { return tags.getLevel(item); }
    public double getXp(ItemStack item) { return tags.getXp(item); }
    public double getBonusYieldPct(ItemStack item) { return tags.getYield(item); }
    public ToolClass detectToolClass(ItemStack it) {
        if (it == null) return ToolClass.OTHER;
        String t = detectType(it.getType());
        return switch (t) {
            case "pickaxe" -> ToolClass.PICKAXE;
            case "hoe" -> ToolClass.HOE;
            case "sword" -> ToolClass.SWORD;
            case "axe" -> ToolClass.AXE;
            default -> ToolClass.OTHER;
        };
    }

    /** External API for granting XP. */
    public void grantXp(Player player, ItemStack item, double amount, ToolClass clazz) {
        boolean allowEnchant = clazz == ToolClass.SWORD || clazz == ToolClass.AXE || clazz == ToolClass.OTHER;
        addXp(player, item, amount, allowEnchant);
    }

    private String detectType(Material m) {
        String n = m.name().toLowerCase(Locale.ROOT);
        if (n.endsWith("_pickaxe")) return "pickaxe";
        if (n.endsWith("_hoe")) return "hoe";
        if (n.endsWith("_sword")) return "sword";
        if (n.endsWith("_axe")) return "axe";
        if (n.endsWith("_helmet") || n.endsWith("_chestplate") || n.endsWith("_leggings") || n.endsWith("_boots")) return "armor";
        return "other";
    }

    private double neededXp(int level) {
        return 50.0 * Math.pow(level, 1.35);
    }

    private String fmt(double v) {
        return String.format("%.0f", v);
    }

    private String itemName(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return item == null ? "item" : item.getType().name();
    }

    private boolean hasVeinLore(ItemStack it) {
        if (veinLore.isEmpty() || it == null) return false;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return false;
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        for (String line : lore) {
            String s = ChatColor.stripColor(line).toLowerCase(Locale.ROOT);
            for (String n : veinLore) {
                if (s.contains(n)) return true;
            }
        }
        return false;
    }

    private static final BlockFace[] FACES3D = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    private static final BlockFace[] FACES2D = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private int countVeinBlocks(Block start) {
        Material type = start.getType();
        Set<Block> visited = new HashSet<>();
        Deque<Block> q = new ArrayDeque<>();
        q.add(start);
        while (!q.isEmpty() && visited.size() < veinMax) {
            Block b = q.poll();
            if (visited.contains(b)) continue;
            if (!regions.inMine(b.getLocation())) continue;
            if (b.getType() != type) continue;
            visited.add(b);
            for (BlockFace f : FACES3D) {
                Block nb = b.getRelative(f);
                if (!visited.contains(nb) && nb.getType() == type) {
                    q.add(nb);
                }
            }
        }
        return visited.size();
    }

    private int countHarvestCrops(Block start) {
        Material type = start.getType();
        Set<Block> visited = new HashSet<>();
        Deque<Block> q = new ArrayDeque<>();
        q.add(start);
        while (!q.isEmpty() && visited.size() < harvesterMax) {
            Block b = q.poll();
            if (visited.contains(b)) continue;
            if (!regions.inFarm(b.getLocation())) continue;
            if (b.getType() != type || !HarvestUtil.isMatureCrop(b)) continue;
            visited.add(b);
            for (BlockFace f : FACES2D) {
                Block nb = b.getRelative(f);
                if (!visited.contains(nb) && nb.getType() == type) {
                    q.add(nb);
                }
            }
        }
        return visited.size();
    }

    // --- Event handling ---
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (!isSpecialItem(tool)) return;
        String type = tags.getType(tool);
        if ("pickaxe".equals(type)) {
            if (!regions.inMine(e.getBlock().getLocation())) return;
            int count = 1;
            if (ItemUtil.getEffectLevel(tool, "veinminer") > 0 || hasVeinLore(tool)) {
                count = Math.max(1, countVeinBlocks(e.getBlock()));
            }
            double xp = Math.min(xpMine * count, xpCap);
            addXp(p, tool, xp, false);
            if (count > 1) pendingMulti.put(p.getUniqueId(), count); else pendingMulti.remove(p.getUniqueId());
        } else if ("hoe".equals(type)) {
            if (!regions.inFarm(e.getBlock().getLocation())) return;
            Block b = e.getBlock();
            if (!(b.getBlockData() instanceof Ageable age) || age.getAge() < age.getMaximumAge()) return;
            int count = 1;
            if (ItemUtil.getEffectLevel(tool, "harvester") > 0) {
                count = Math.max(1, countHarvestCrops(b));
            }
            double xp = Math.min(xpFarm * count, xpCap);
            addXp(p, tool, xp, false);
            if (count > 1) pendingMulti.put(p.getUniqueId(), count); else pendingMulti.remove(p.getUniqueId());
        }
    }

    public void onBlockDrop(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (!isSpecialItem(tool)) return;
        String type = tags.getType(tool);
        int count = pendingMulti.getOrDefault(p.getUniqueId(), 1);
        pendingMulti.remove(p.getUniqueId());

        double yield = tags.getYield(tool);
        boolean inRegion;
        if ("pickaxe".equals(type)) {
            inRegion = regions.inMine(e.getBlock().getLocation());
        } else if ("hoe".equals(type)) {
            inRegion = regions.inFarm(e.getBlock().getLocation()) &&
                    e.getBlockState().getBlockData() instanceof Ageable age && age.getAge() >= age.getMaximumAge();
        } else {
            return;
        }
        for (Item item : e.getItems()) {
            ItemStack drop = item.getItemStack();
            int base = drop.getAmount();
            double amount = base * count;
            if (yield > 0 && inRegion) {
                amount = amount * (1.0 + yield / 100.0);
            }
            drop.setAmount((int) Math.round(amount));
            item.setItemStack(drop);
        }
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        ItemStack weapon = p.getInventory().getItemInMainHand();
        if (!isSpecialItem(weapon)) return;
        String type = tags.getType(weapon);
        if ("sword".equals(type) || "axe".equals(type)) {
            addXp(p, weapon, xpMelee, true);
        }
    }

    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getFinalDamage() <= 0) return;
        for (ItemStack armor : p.getInventory().getArmorContents()) {
            if (armor == null) continue;
            if (!isSpecialItem(armor)) continue;
            if (!"armor".equals(tags.getType(armor))) continue;
            addXp(p, armor, xpTaken, true);
        }
    }

    // --- XP / Lore / Enchant helpers ---
    private void addXp(Player holder, ItemStack item, double amount, boolean allowEnchant) {
        if (amount <= 0) return;
        // ensure basic tags and properties are present
        tags.init(item, tags.getType(item));
        int level = tags.getLevel(item);
        double xp = tags.getXp(item) + amount;
        boolean leveled = false;
        while (level < 100) {
            double need = neededXp(level);
            if (xp < need) break;
            xp -= need;
            level++;
            leveled = true;
            String type = tags.getType(item);
            if ("pickaxe".equals(type) || "hoe".equals(type)) {
                double y = Math.min(level * yieldPerLevel, yieldMax);
                tags.setYield(item, y);
            }
        }
        tags.setLevel(item, level);
        tags.setXp(item, xp);
        updateLore(item);
        if (leveled) {
            holder.sendMessage(msgLevelUp.replace("%ITEM%", itemName(item)).replace("%LEVEL%", String.valueOf(level)));
        }
        if (allowEnchant) {
            maybeAddEnchant(holder, item);
        }
    }

    private void maybeAddEnchant(Player holder, ItemStack item) {
        if (enchantPool.isEmpty()) return;
        if (rng.nextDouble() >= enchantChance) return;
        Enchantment ench = enchantPool.get(rng.nextInt(enchantPool.size()));
        int lvl = rng.nextInt(Math.max(1, enchantLevelMax - enchantLevelMin + 1)) + enchantLevelMin;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.addEnchant(ench, lvl, true);
        item.setItemMeta(meta);
        holder.sendMessage(msgEnchant.replace("%ITEM%", itemName(item))
                .replace("%ENCHANT%", ench.getKey().getKey())
                .replace("%LEVEL%", String.valueOf(lvl)));
    }

    public void updateLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        int level = tags.getLevel(item);
        double xp = tags.getXp(item);
        double need = neededXp(level);
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        String type = tags.getType(item);
        String lvlLine = color(loreLevelFmt.replace("%LEVEL%", String.valueOf(level)));
        String xpLine = color(loreXpFmt.replace("%XP%", fmt(xp)).replace("%NEED%", fmt(need)));
        String yieldLine = null;
        if ("pickaxe".equals(type)) {
            yieldLine = color(loreYieldMineFmt.replace("%YIELD%", fmt(tags.getYield(item))));
        } else if ("hoe".equals(type)) {
            yieldLine = color(loreYieldFarmFmt.replace("%YIELD%", fmt(tags.getYield(item))));
        }
        String finalYield = yieldLine;
        lore.removeIf(s -> ChatColor.stripColor(s).startsWith("Level:")
                || ChatColor.stripColor(s).startsWith("XP:")
                || ChatColor.stripColor(s).startsWith("Yield:"));
        List<String> add = new ArrayList<>();
        add.add(lvlLine);
        add.add(xpLine);
        if (finalYield != null) add.add(finalYield);
        lore.addAll(0, add);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
