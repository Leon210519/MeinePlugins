package com.focusnpc.transform;

import com.focusnpc.FocusNPCPlugin;
import com.focusnpc.npc.NpcType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class BlockTransformer {
    private final FocusNPCPlugin plugin;
    private int radius;
    private int maxPerTick;
    private boolean respectWorldGuard;
    private Set<String> allowedWorlds = new HashSet<>();

    // WorldGuard reflection
    private boolean wgAvailable = false;
    private Object wgPlugin;
    private Object regionContainer;
    private Method wrapPlayer;
    private Method createQuery;
    private Method adaptLocation;
    private Method testState;
    private Object buildFlag;

    private static final Set<Material> CROP_TYPES = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART
    );

    private static final Set<Material> ORE_TYPES = EnumSet.of(
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    public BlockTransformer(FocusNPCPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.radius = plugin.getConfig().getInt("transform.radius", 100);
        this.maxPerTick = plugin.getConfig().getInt("transform.max_blocks_per_tick", 500);
        this.respectWorldGuard = plugin.getConfig().getBoolean("transform.respect_worldguard", true);
        this.allowedWorlds = new HashSet<>(plugin.getConfig().getStringList("transform.allowed_worlds"));
        setupWorldGuard();
    }

    private void setupWorldGuard() {
        wgAvailable = false;
        if (!respectWorldGuard || !Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return;
        try {
            wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
            Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            wrapPlayer = wgPluginClass.getMethod("wrapPlayer", Player.class);

            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wg = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wg);
            regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> containerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
            createQuery = containerClass.getMethod("createQuery");
            Class<?> queryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Class<?> weLoc = Class.forName("com.sk89q.worldedit.util.Location");
            Class<?> localPlayer = Class.forName("com.sk89q.worldguard.LocalPlayer");
            Class<?> stateFlag = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            testState = queryClass.getMethod("testState", weLoc, localPlayer, stateFlag);

            Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            adaptLocation = adapterClass.getMethod("adapt", Location.class);
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            Field build = flagsClass.getField("BUILD");
            buildFlag = build.get(null);
            wgAvailable = true;
        } catch (Exception ignored) {
            wgAvailable = false;
        }
    }

    public void transform(Player player, NpcType type, Material target) {
        World world = player.getWorld();
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(world.getName())) {
            return;
        }
        Location loc = player.getLocation();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        int radiusSq = radius * radius;
        List<Block> blocks = new ArrayList<>();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    int dx = x - cx;
                    int dy = y - cy;
                    int dz = z - cz;
                    if (dx*dx + dy*dy + dz*dz > radiusSq) continue;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    Block block = world.getBlockAt(x, y, z);
                    if (type == NpcType.FARMER) {
                        if (isMatureCrop(block)) {
                            blocks.add(block);
                        }
                    } else {
                        if (ORE_TYPES.contains(block.getType())) {
                            blocks.add(block);
                        }
                    }
                }
            }
        }
        if (blocks.isEmpty()) {
            player.sendMessage("§eNo blocks to transform.");
            return;
        }
        new BukkitRunnable() {
            int index = 0;
            int changed = 0;
            @Override
            public void run() {
                int processed = 0;
                while (index < blocks.size() && processed < maxPerTick) {
                    Block b = blocks.get(index++);
                    if (!canModify(player, b)) continue;
                    if (type == NpcType.FARMER) {
                        b.setType(target);
                        BlockData data = b.getBlockData();
                        if (data instanceof Ageable age) {
                            age.setAge(age.getMaximumAge());
                            b.setBlockData(age);
                        }
                    } else {
                        b.setType(target);
                    }
                    changed++;
                    processed++;
                }
                if (index >= blocks.size()) {
                    player.sendMessage("§aReplaced " + changed + " blocks.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean canModify(Player player, Block block) {
        if (!respectWorldGuard || !wgAvailable) return true;
        try {
            Object query = createQuery.invoke(regionContainer);
            Object localPlayer = wrapPlayer.invoke(wgPlugin, player);
            Object loc = adaptLocation.invoke(null, block.getLocation());
            return (boolean) testState.invoke(query, loc, localPlayer, buildFlag);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isMatureCrop(Block block) {
        if (!CROP_TYPES.contains(block.getType())) return false;
        BlockData data = block.getBlockData();
        if (data instanceof Ageable age) {
            return age.getAge() == age.getMaximumAge();
        }
        return false;
    }
}
