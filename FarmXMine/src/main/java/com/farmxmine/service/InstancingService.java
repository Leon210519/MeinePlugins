package com.farmxmine.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * InstancingService
 *
 * - Per-Player "Fake Break/Harvest": Der Block verschwindet nur für den Spieler.
 * - Respawn nach konfigurierbaren Sekunden (respawn_seconds, Default 20s).
 * - Nur Overworld (Environment.NORMAL).
 * - Ores/Crops werden aus den Konfig-Listen "ores" und "crops" gelesen.
 * - Respektiert das Flag "override_cancelled".
 * - Sauberes Cleanup bei Logout.
 */
public class InstancingService implements Listener {

    private final JavaPlugin plugin;
    private final LevelService level;

    // Vein/Harvest Multiplikator-Erkennung
    private final List<String> veinLore;
    private final int veinMax;
    private final int harvestMax;
    private final int generalMax;

    // Drop-Ziel
    private final boolean directToInv;
    private final boolean voidOverflow;

    // Konfig-Listen
    private final Set<Material> ores;
    private final Set<Material> crops;

    private final boolean overrideCancelled;
    private final long respawnTicks;

    // Für per-Player versteckte Blockpositionen (nur optisch verborgen)
    private final Map<UUID, Set<Location>> hidden = new HashMap<>();

    // Felder zu Schutz-Bypasses wurden entfernt (WorldGuard etc. wird respektiert, außer override_cancelled=true)

    public InstancingService(JavaPlugin plugin, LevelService level) {
        this.plugin = plugin;
        this.level = level;

        this.veinLore = plugin.getConfig().getStringList("compat.veinminer.detect_lore");
        this.veinMax = plugin.getConfig().getInt("compat.veinminer.max_blocks", 64);
        this.harvestMax = plugin.getConfig().getInt("compat.harvester.max_blocks", 64);
        this.generalMax = plugin.getConfig().getInt("general.max-broken-blocks", 64);

        this.directToInv = plugin.getConfig().getBoolean("farmxmine.direct_to_inventory", true);
        this.voidOverflow = plugin.getConfig().getBoolean("inventory.void_overflow", true);

        this.ores = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("ores")) {
            try { this.ores.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }

        this.crops = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("crops")) {
            try { this.crops.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }

        this.overrideCancelled = plugin.getConfig().getBoolean("override_cancelled", false);
        this.respawnTicks = plugin.getConfig().getInt("respawn_seconds", 20) * 20L;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Falls ein anderer Plugin-Schutz gecancelt hat und wir das respektieren sollen → raus
        if (event.isCancelled() && !overrideCancelled) return;

        // Nur Overworld
        if (block.getWorld().getEnvironment() != World.Environment.NORMAL) return;

        // Optional: Deaktivierte Welten
        String worldName = block.getWorld().getName();
        if (plugin.getConfig().getStringList("general.disabled-worlds").contains(worldName)) return;

        Material type = block.getType();
        boolean mining = ores.contains(type);
        boolean farming = !mining && crops.contains(type) && isMatureCrop(block);
        if (!mining && !farming) return;

        // Doppelverarbeitung auf gleicher Position verhindern
        Location loc = block.getLocation().toBlockLocation();
        Set<Location> set = hidden.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (set.contains(loc)) {
            event.setCancelled(true);
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }

        // Tool-Check für Mining (Pickaxe)
        ItemStack tool = player.getInventory().getItemInMainHand();
        String toolName = tool.getType().name();
        if (mining && !toolName.endsWith("_PICKAXE")) return;

        // Verarbeiten
        handle(event, player, block, loc, set, mining);
    }

    private boolean isMatureCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable age) {
            return age.getAge() >= age.getMaximumAge();
        }
        return false;
    }

    private void handle(BlockBreakEvent event, Player player, Block block, Location loc, Set<Location> set, boolean mining) {
        int count = computeCount(player, mining);
        int xpFromEvent = event.getExpToDrop() * count;

        // Serverzustand nicht ändern → abbrechen & eigene Drops/XP geben
        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        ItemStack tool = player.getInventory().getItemInMainHand();

        // Drops simulieren (inkl. Fortune/Verzauberungen via getDrops)
        List<ItemStack> drops = collectDrops(block, tool, player, mining, count);
        for (ItemStack drop : drops) {
            giveDrop(player, block, drop);
        }

        // XP/Leveling
        if (mining) {
            level.addMineXp(player, count);
        } else {
            level.addFarmXp(player, count);
        }
        if (xpFromEvent > 0) {
            player.giveExp(xpFromEvent);
        }

        // Spieler-Client: Block optisch ersetzen (Fake Break)
        BlockData replacement = Material.AIR.createBlockData();

        set.add(loc);
        // send after server has confirmed cancellation so client doesn't get overwritten
        Bukkit.getScheduler().runTask(plugin, () -> sendBlockChange(player, block, replacement));

        // Respawn nach Delay: Ursprungs-Blockzustand wieder anzeigen
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            set.remove(loc);
            if (!player.isOnline()) return;
            World world = loc.getWorld();
            if (world == null) return;

            // Chunk muss geladen sein
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            if (!world.isChunkLoaded(cx, cz)) return;

            // Zeige wieder den echten Blockzustand des Servers (falls er sich zwischenzeitlich geändert hat, sehen wir den neuen)
            player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
        }, respawnTicks);
    }

    private int computeCount(Player player, boolean mining) {
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean multi = false;
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    for (String tag : veinLore) {
                        if (line != null && tag != null && line.contains(tag)) {
                            multi = true;
                            break;
                        }
                    }
                    if (multi) break;
                }
            }
        }
        if (!multi) return 1;
        int max = mining ? veinMax : harvestMax;
        return Math.min(max, generalMax);
    }

    private void giveDrop(Player player, Block block, ItemStack drop) {
        if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) return;

        if (directToInv) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
            if (!overflow.isEmpty() && !voidOverflow) {
                overflow.values().forEach(it -> block.getWorld().dropItemNaturally(block.getLocation(), it));
            }
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
    }

    private void sendBlockChange(Player player, Block block, BlockData data) {
        player.sendBlockChange(block.getLocation(), data);
    }

    private List<ItemStack> collectDrops(Block block, ItemStack tool, Player player, boolean mining, int count) {
        List<ItemStack> drops = new ArrayList<>();
        boolean smelt = mining && hasAutoSmelt(tool);

        for (ItemStack original : block.getDrops(tool, player)) {
            if (original == null || original.getType() == Material.AIR) continue;

            Material outType = original.getType();
            if (smelt) {
                outType = SMELTS.getOrDefault(outType, outType);
            }

            ItemStack drop = new ItemStack(outType, Math.max(1, original.getAmount()) * Math.max(1, count));
            drops.add(drop);
        }
        return drops;
    }

    private boolean hasAutoSmelt(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta() || !tool.getItemMeta().hasLore()) return false;
        List<String> lore = tool.getItemMeta().getLore();
        if (lore == null) return false;
        for (String line : lore) {
            if (line != null && line.toLowerCase().contains("autosmelt")) return true;
        }
        return false;
    }

    // Achtung: Map.ofEntries ist ab Java 9 verfügbar — Paper 1.21 nutzt i. d. R. Java 21 → okay.
    private static final Map<Material, Material> SMELTS = Map.ofEntries(
            Map.entry(Material.RAW_IRON, Material.IRON_INGOT),
            Map.entry(Material.RAW_GOLD, Material.GOLD_INGOT),
            Map.entry(Material.RAW_COPPER, Material.COPPER_INGOT),
            Map.entry(Material.GOLD_NUGGET, Material.GOLD_INGOT),
            Map.entry(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP)
    );

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hidden.remove(event.getPlayer().getUniqueId());
    }
}
