package com.farmxmine.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
    private final ArtifactService artifacts;

    // Vein/Harvest Multiplikator-Erkennung
    private final List<String> veinLore;
    private final int veinMax;
    private final int harvestMax;
    private final int generalMax;

    // Konfig-Listen
    private final Set<Material> ores;
    private final Set<Material> crops;
    private final Region miningRegion;
    private final Region farmingRegion;

    private final boolean overrideCancelled;
    private final long respawnTicks;

    // Für per-Player versteckte Blockpositionen (nur optisch verborgen)
    private final Map<UUID, Set<Location>> hidden = new HashMap<>();

    // Felder zu Schutz-Bypasses wurden entfernt (WorldGuard etc. wird respektiert, außer override_cancelled=true)

    public InstancingService(JavaPlugin plugin, LevelService level, ArtifactService artifacts) {
        this.plugin = plugin;
        this.level = level;
        this.artifacts = artifacts;

        this.veinLore = plugin.getConfig().getStringList("compat.veinminer.detect_lore");
        this.veinMax = plugin.getConfig().getInt("compat.veinminer.max_blocks", 64);
        this.harvestMax = plugin.getConfig().getInt("compat.harvester.max_blocks", 64);
        this.generalMax = plugin.getConfig().getInt("general.max-broken-blocks", 64);

        this.ores = loadMaterials("regions.mine.ores", "mine.ores", "ores");
        if (this.ores.isEmpty()) this.ores.add(Material.COAL_ORE);
        this.crops = loadMaterials("regions.farm.crops", "farm.crops", "crops");
        if (this.crops.isEmpty()) this.crops.add(Material.WHEAT);

        this.miningRegion = parseRegion("regions.mine.bounds", "mine.bounds");
        this.farmingRegion = parseRegion("regions.farm.bounds", "farm.bounds");

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

        Location loc = block.getLocation().toBlockLocation();
        Material type = block.getType();
        boolean mining = ores.contains(type);
        boolean farming = !mining && crops.contains(type) && isMatureCrop(block);
        if (!mining && !farming) return;

        if (mining && miningRegion != null && !miningRegion.contains(loc)) return;
        if (farming && farmingRegion != null && !farmingRegion.contains(loc)) return;

        int baseXp = event.getExpToDrop();
        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);

        // Doppelverarbeitung auf gleicher Position verhindern
        Set<Location> set = hidden.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (set.contains(loc)) return;

        // Tool-Checks
        ItemStack tool = player.getInventory().getItemInMainHand();
        String toolName = tool.getType().name();
        if (mining && !toolName.endsWith("_PICKAXE")) return;
        if (farming && !toolName.endsWith("_HOE")) return;

        // Verarbeiten
        handle(player, block, loc, set, mining, baseXp);
    }

    private boolean isMatureCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable age) {
            return age.getAge() >= age.getMaximumAge();
        }
        return false;
    }

    private void handle(Player player, Block block, Location loc, Set<Location> set, boolean mining, int baseXp) {
        int count = computeCount(player, mining);
        int xpFromEvent = baseXp * count;

        ItemStack tool = player.getInventory().getItemInMainHand();

        // Drops simulieren (inkl. Fortune/Verzauberungen via getDrops)
        double multi = artifacts.getMultiplier(player, mining ? ArtifactService.Category.MINING : ArtifactService.Category.FARMING);
        List<ItemStack> drops = collectDrops(block, tool, player, mining, count, multi);
        for (ItemStack drop : drops) {
            giveDrop(player, drop);
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
        BlockData replacement = mining ? Material.STONE.createBlockData() : Material.AIR.createBlockData();

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

    private void giveDrop(Player player, ItemStack drop) {
        if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) return;
        player.getInventory().addItem(drop);
    }

    private void sendBlockChange(Player player, Block block, BlockData data) {
        player.sendBlockChange(block.getLocation(), data);
    }

    private List<ItemStack> collectDrops(Block block, ItemStack tool, Player player, boolean mining, int count, double multi) {
        List<ItemStack> drops = new ArrayList<>();
        boolean smelt = mining && hasAutoSmelt(tool);

        for (ItemStack original : block.getDrops(tool, player)) {
            if (original == null || original.getType() == Material.AIR) continue;

            Material outType = original.getType();
            if (smelt) {
                outType = SMELTS.getOrDefault(outType, outType);
            }

            int amt = (int) Math.round(Math.max(1, original.getAmount()) * Math.max(1, count) * multi);
            ItemStack drop = new ItemStack(outType, Math.max(1, amt));
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

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        hidden.values().forEach(s -> s.removeIf(loc -> {
            World w = loc.getWorld();
            if (w == null || w != event.getWorld()) return false;
            return (loc.getBlockX() >> 4) == event.getChunk().getX() && (loc.getBlockZ() >> 4) == event.getChunk().getZ();
        }));
    }

    private Set<Material> loadMaterials(String... paths) {
        Set<Material> set = new HashSet<>();
        for (String p : paths) {
            for (String s : plugin.getConfig().getStringList(p)) {
                try { set.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
            }
        }
        return set;
    }

    private Region parseRegion(String... paths) {
        for (String p : paths) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection(p);
            if (sec == null) continue;
            String world = sec.getString("world", null);
            List<Integer> minList = sec.getIntegerList("min");
            List<Integer> maxList = sec.getIntegerList("max");
            if (minList.size() == 3 && maxList.size() == 3) {
                return new Region(world,
                        Math.min(minList.get(0), maxList.get(0)),
                        Math.min(minList.get(1), maxList.get(1)),
                        Math.min(minList.get(2), maxList.get(2)),
                        Math.max(minList.get(0), maxList.get(0)),
                        Math.max(minList.get(1), maxList.get(1)),
                        Math.max(minList.get(2), maxList.get(2)));
            }
            ConfigurationSection minSec = sec.getConfigurationSection("min");
            ConfigurationSection maxSec = sec.getConfigurationSection("max");
            if (minSec != null && maxSec != null) {
                int minX = minSec.getInt("x");
                int minY = minSec.getInt("y");
                int minZ = minSec.getInt("z");
                int maxX = maxSec.getInt("x");
                int maxY = maxSec.getInt("y");
                int maxZ = maxSec.getInt("z");
                return new Region(world,
                        Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
                        Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));
            }
        }
        return null;
    }

    private record Region(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean contains(Location loc) {
            World w = loc.getWorld();
            if (world != null && (w == null || !w.getName().equals(world))) return false;
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }
}
