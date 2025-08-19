package com.instancednodes.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class Cfg {

    public static RegionRect MINE;
    public static RegionRect FARM;

    public static String MINE_defaultMat;
    public static String FARM_defaultMat;

    public static int RESPAWN_SECONDS;
    public static long TARGET_HARVESTS;
    public static double EXPONENT;

    public static boolean PLAY_SOUNDS;
    public static boolean REQUIRE_PERMS;
      public static boolean OVERRIDE_CANCELLED;

    public static boolean INTEGRATE_SPECIALITEMS;
    public static boolean DISABLE_REPLANT_IN_FARM;
      public static boolean DIRECT_TO_INVENTORY;
      public static boolean VOID_OVERFLOW;
    public static int HARVESTER_MAX_BLOCKS;
    public static int HARVESTER_MAX_RADIUS;

    public static String[] VEIN_LORE;
    public static int VEIN_MAX_BLOCKS;

    public static Set<Material> FARM_CROPS;

    public static void load(Plugin plugin) {
        plugin.saveDefaultConfig();
        RESPAWN_SECONDS = plugin.getConfig().getInt("respawn_seconds", 30);
        TARGET_HARVESTS = plugin.getConfig().getLong("target_harvests", 3000000L);
        EXPONENT = plugin.getConfig().getDouble("exponent", 1.6);

        INTEGRATE_SPECIALITEMS = plugin.getConfig().getBoolean("farmxmine.integrate_specialitems", true);
        DISABLE_REPLANT_IN_FARM = plugin.getConfig().getBoolean("farmxmine.disable_replant_in_farm", true);
          DIRECT_TO_INVENTORY = plugin.getConfig().getBoolean("farmxmine.direct_to_inventory", true);
          VOID_OVERFLOW = plugin.getConfig().getBoolean("inventory.void_overflow", true);
        HARVESTER_MAX_BLOCKS = plugin.getConfig().getInt("harvester.max_blocks", 32);
        HARVESTER_MAX_RADIUS = plugin.getConfig().getInt("harvester.max_radius", 5);

        PLAY_SOUNDS = plugin.getConfig().getBoolean("options.play_sounds", false);
        REQUIRE_PERMS = plugin.getConfig().getBoolean("options.require_permissions", false);
        OVERRIDE_CANCELLED = plugin.getConfig().getBoolean("options.override_cancelled", true);

        VEIN_MAX_BLOCKS = plugin.getConfig().getInt("compat.veinminer.max_blocks", 64);
        java.util.List<String> lore = plugin.getConfig().getStringList("compat.veinminer.detect_lore");
        VEIN_LORE = lore.toArray(new String[0]);

        ConfigurationSection rs = plugin.getConfig().getConfigurationSection("regions");
        if (rs == null) throw new IllegalStateException("Missing 'regions' in config.yml");

        ConfigurationSection mine = rs.getConfigurationSection("mine");
        ConfigurationSection farm = rs.getConfigurationSection("farm");
        if (mine == null || farm == null) throw new IllegalStateException("Missing regions.mine or regions.farm in config.yml");

        MINE = fromSection(mine);
        FARM = fromSection(farm);
        MINE_defaultMat = mine.getString("default_mat", "COAL_ORE");
        FARM_defaultMat = farm.getString("default_mat", "WHEAT");

        java.util.List<String> crops = plugin.getConfig().getStringList("farm.crops");
        FARM_CROPS = new HashSet<>();
        for (String c : crops) {
            Material m = Material.matchMaterial(c);
            if (m != null) {
                FARM_CROPS.add(m);
            }
        }
    }

    private static RegionRect fromSection(ConfigurationSection s) {
        String world = s.getString("world", "world");
        ConfigurationSection p1 = s.getConfigurationSection("pos1");
        ConfigurationSection p2 = s.getConfigurationSection("pos2");
        int x1 = p1 != null ? p1.getInt("x") : 0;
        int y1 = p1 != null ? p1.getInt("y") : 0;
        int z1 = p1 != null ? p1.getInt("z") : 0;
        int x2 = p2 != null ? p2.getInt("x") : 0;
        int y2 = p2 != null ? p2.getInt("y") : 0;
        int z2 = p2 != null ? p2.getInt("z") : 0;
        return new RegionRect(world, x1,y1,z1, x2,y2,z2);
    }

    public static class RegionRect {
        public final String world;
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public RegionRect(String world, int x1,int y1,int z1, int x2,int y2,int z2) {
            this.world = world;
            this.minX = Math.min(x1, x2);
            this.minY = Math.min(y1, y2);
            this.minZ = Math.min(z1, z2);
            this.maxX = Math.max(x1, x2);
            this.maxY = Math.max(y1, y2);
            this.maxZ = Math.max(z1, z2);
        }
        public boolean contains(Location loc) {
            World w = loc.getWorld();
            if (w == null) return false;
            if (!w.getName().equalsIgnoreCase(world)) return false;
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return x>=minX && x<=maxX && y>=minY && y<=maxY && z>=minZ && z<=maxZ;
        }
    }
}
