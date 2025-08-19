package com.instancednodes.nodes;

import com.instancednodes.InstancedNodesPlugin;
import com.instancednodes.util.Cfg;
import com.instancednodes.util.Mathf;
import com.instancednodes.util.Msg;
import com.instancednodes.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class NodeManager implements Listener {

    private static NodeManager INSTANCE;
    public static NodeManager getInstance(){ return INSTANCE; }

    private final InstancedNodesPlugin plugin;
    private final Map<BlockVector, RespawnInfo> respawns = new HashMap<>();
    private final Map<UUID, Map<BlockVector, Long>> cooldowns = new HashMap<>();

    public NodeManager(InstancedNodesPlugin plugin) { this.plugin = plugin; INSTANCE = this; }

    private boolean isOre(Material m) {
        if (m == null) return false;
        String n = m.name();
        return n.endsWith("_ORE") || m == Material.NETHER_QUARTZ_ORE;
    }

    private boolean sameOreFamily(Material a, Material b) {
        if (a == b) return true;
        if (!isOre(a) || !isOre(b)) return false;
        String na = a.name().replace("DEEPSLATE_", "");
        String nb = b.name().replace("DEEPSLATE_", "");
        return na.equals(nb);
    }

    // FARM: handle left-click harvest even if other plugins cancel the event
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        Location loc = b.getLocation();
        boolean isMine = Cfg.MINE.contains(loc);
        boolean isFarm = !isMine && Cfg.FARM.contains(loc);
        if (!isFarm) return;
        if (Cfg.REQUIRE_PERMS && !e.getPlayer().hasPermission("instancednodes.farm")) return;

        boolean processed = processFarmHarvest(e.getPlayer(), b);
        if (processed) {
            e.setCancelled(true);
        }
    }

    // MINE: process even if cancelled by other plugins (e.g. WorldGuard)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Location loc = b.getLocation();
        boolean isMine = Cfg.MINE.contains(loc);
        boolean isFarm = !isMine && Cfg.FARM.contains(loc);
        if (!isMine && !isFarm) return;

        if (isMine) {
            if (Cfg.REQUIRE_PERMS && !e.getPlayer().hasPermission("instancednodes.mine")) return;

            // If not an ore -> block it cleanly
            if (!isOre(b.getType())) {
                e.setCancelled(true);
                e.setDropItems(false);
                e.setExpToDrop(0);
                e.getPlayer().sendMessage(Msg.get("mine_only_ores"));
                Log.d("Blocked non-ore: " + b.getType() + " at " + Log.loc(loc));
                return;
            }

            // Veinminer detection via lore (fallback for implementations that don't fire break events for every block)
            if (hasVeinminerLore(e.getPlayer().getInventory().getItem(EquipmentSlot.HAND))) {
                int processed = processVein(purify(e.getPlayer()), b);
                e.setCancelled(true);
                e.setDropItems(false);
                e.setExpToDrop(0);
                Log.d("Veinminer processed " + processed + " blocks starting at " + Log.loc(loc));
                return;
            }

            boolean processed = processMineHarvest(e.getPlayer(), e);
            if (processed) {
                e.setCancelled(true);
                e.setDropItems(false);
                e.setExpToDrop(0);
            }
        } else {
            boolean processed = processFarmHarvest(e.getPlayer(), e.getBlock());
            if (processed) {
                e.setCancelled(true);
                e.setDropItems(false);
                e.setExpToDrop(0);
            }
        }
    }

    // Public entry for other plugins
    public boolean processMineBlockExternal(Player p, Block b) {
        if (p == null || b == null) return false;
        if (!Cfg.MINE.contains(b.getLocation())) return false;
        if (!isOre(b.getType())) return false;
        return processMineFinal(p, b);
    }

    private boolean processFarmHarvest(Player p, Block b) {
        Location loc = b.getLocation();
        if (!Cfg.FARM.contains(loc)) return false;
        if (!hasTool(p, "HOE")) { p.sendMessage(Msg.get("harvest_blocked_tool").replace("%tool%", "hoe")); return false; }

        UUID uidSel = p.getUniqueId();
        String selName = InstancedNodesPlugin.get().data().getSelection(uidSel, "crop", Cfg.FARM_defaultMat);
        Material selected = Material.matchMaterial(selName);
        if (selected == null || !Cfg.FARM_CROPS.contains(selected)) return false;
        if (!isSelectedCropBlock(b.getType(), selected)) return false;
        if (!isMatureCrop(b)) { p.sendMessage(Msg.get("crop_not_mature")); return false; }

        UUID uid = p.getUniqueId();
        BlockVector key = new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Map<BlockVector, Long> map = cooldowns.computeIfAbsent(uid, k -> new HashMap<>());
        long now = System.currentTimeMillis();
        Long until = map.get(key);
        if (until != null && until > now) {
            if (Cfg.PLAY_SOUNDS) p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            return true;
        }

        long harvests = InstancedNodesPlugin.get().data().incrementHarvest(uid, selected.name());
        int yield = Mathf.yieldFor(harvests, Cfg.TARGET_HARVESTS, Cfg.EXPONENT);
        yield = applyYieldBonuses(p, p.getInventory().getItem(EquipmentSlot.HAND), yield);

        ItemStack drop = new ItemStack(materialToDrop(selected), yield);
        Map<Integer, ItemStack> left = p.getInventory().addItem(drop);
        if (!left.isEmpty()) for (ItemStack it : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), it);

        InstancedNodesPlugin.get().level().addXp(p, com.instancednodes.leveling.LevelManager.Kind.FARM);

        p.sendBlockChange(loc, Bukkit.createBlockData(Material.AIR));
        if (Cfg.PLAY_SOUNDS) p.playSound(loc, Sound.BLOCK_STONE_BREAK, 0.7f, 1.2f);
        map.put(key, now + Cfg.RESPAWN_SECONDS * 1000L);

        Bukkit.getScheduler().runTaskLater(InstancedNodesPlugin.get(), () -> {
            if (p.isOnline()) {
                p.sendBlockChange(loc, fullyGrownData(selected));
            }
        }, Cfg.RESPAWN_SECONDS * 20L);

        Log.d("FARM harvest by " + p.getName() + " at " + Log.loc(loc) + " yield=" + yield + " sel=" + selected);
        return true;
    }

    private boolean processMineHarvest(Player p, BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!Cfg.MINE.contains(b.getLocation())) return false;
        if (!hasTool(p, "PICKAXE")) { p.sendMessage(Msg.get("harvest_blocked_tool").replace("%tool%", "pickaxe")); return false; }
        if (!isOre(b.getType())) return false;
        return processMineFinal(p, b);
    }

    private boolean processMineFinal(Player p, Block b) {
        Location loc = b.getLocation();
        BlockVector key = new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RespawnInfo existing = respawns.get(key);
        long now = System.currentTimeMillis();
        if (existing != null && existing.until > now) {
            p.sendMessage(Msg.get("ore_regenerating"));
            return true;
        }

        UUID uid = p.getUniqueId();
        Material oreType = b.getType();

        long harvests = InstancedNodesPlugin.get().data().incrementHarvest(uid, oreType.name());
        int yield = Mathf.yieldFor(harvests, Cfg.TARGET_HARVESTS, Cfg.EXPONENT);
        yield = applyYieldBonuses(p, p.getInventory().getItem(EquipmentSlot.HAND), yield);
        ItemStack drop = new ItemStack(materialToDrop(oreType), yield);
        Map<Integer, ItemStack> left = p.getInventory().addItem(drop);
        if (!left.isEmpty()) for (ItemStack it : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), it);

        int baseMine = InstancedNodesPlugin.get().getConfig().getInt("leveling.xp_per_harvest.mine", 3);
        double penalty = InstancedNodesPlugin.get().getConfig().getDouble("leveling.mine_efficiency_penalty_per_level", 0.5);
        int minXp = InstancedNodesPlugin.get().getConfig().getInt("leveling.mine_min_xp", 1);
        int eff = 0;
        ItemStack hand = p.getInventory().getItem(EquipmentSlot.HAND);
        if (hand != null) eff = hand.getEnchantmentLevel(Enchantment.EFFICIENCY);
        int addXp = (int)Math.round(baseMine - eff * penalty);
        if (addXp < minXp) addXp = minXp;
        InstancedNodesPlugin.get().level().addXp(p, com.instancednodes.leveling.LevelManager.Kind.MINE, addXp);

        Material base = baseForOre(oreType);
        long until = System.currentTimeMillis() + Cfg.RESPAWN_SECONDS * 1000L;
        respawns.put(key, new RespawnInfo(oreType, base, until));

        // Change the world block regardless of cancel state
        Bukkit.getScheduler().runTask(InstancedNodesPlugin.get(), () -> b.setType(base, false));
        Bukkit.getScheduler().runTaskLater(InstancedNodesPlugin.get(), () -> {
            RespawnInfo info = respawns.get(key);
            if (info != null && System.currentTimeMillis() >= info.until) {
                b.setType(oreType, false);
                respawns.remove(key);
            }
        }, Cfg.RESPAWN_SECONDS * 20L);

        if (Cfg.PLAY_SOUNDS) p.playSound(loc, Sound.BLOCK_STONE_BREAK, 0.7f, 1.2f);
        Log.d("MINE harvest by " + p.getName() + " at " + Log.loc(loc) + " yield=" + yield + " ore=" + oreType);
        return true;
    }

    private int processVein(Player p, Block start) {
        Material target = start.getType();
        if (!isOre(target)) return 0;
        int max = Math.max(1, Cfg.VEIN_MAX_BLOCKS);
        Set<BlockVector> visited = new HashSet<>();
        ArrayDeque<Block> q = new ArrayDeque<>();
        q.add(start);
        int count = 0;
        while (!q.isEmpty() && count < max) {
            Block b = q.poll();
            if (!Cfg.MINE.contains(b.getLocation())) continue;
            if (!sameOreFamily(b.getType(), target)) continue;
            BlockVector v = new BlockVector(b.getX(), b.getY(), b.getZ());
            if (!visited.add(v)) continue;
            processMineFinal(p, b);
            count++;

            // 6-neighborhood
            q.add(b.getRelative( 1, 0, 0));
            q.add(b.getRelative(-1, 0, 0));
            q.add(b.getRelative( 0, 1, 0));
            q.add(b.getRelative( 0,-1, 0));
            q.add(b.getRelative( 0, 0, 1));
            q.add(b.getRelative( 0, 0,-1));
        }
        return count;
    }

    private boolean hasVeinminerLore(ItemStack hand) {
        if (hand == null) return false;
        ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        java.util.List<String> lore = meta.getLore();
        if (lore == null) return false;
        outer: for (String detect : Cfg.VEIN_LORE) {
            if (detect == null || detect.isEmpty()) continue;
            for (String line : lore) {
                if (line != null && line.toLowerCase().contains(detect.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Material baseForOre(Material ore) {
        String n = ore.name();
        if (n.startsWith("DEEPSLATE_")) return Material.DEEPSLATE;
        if (n.contains("NETHER") || ore == Material.NETHER_QUARTZ_ORE) return Material.NETHERRACK;
        return Material.STONE;
    }

    private boolean hasTool(Player p, String type) {
        ItemStack it = p.getInventory().getItem(EquipmentSlot.HAND);
        if (it == null) return false;
        String name = it.getType().name();
        if ("PICKAXE".equals(type)) return name.endsWith("_PICKAXE");
        if ("HOE".equals(type))     return name.endsWith("_HOE");
        return true;
    }

    private boolean isSelectedCropBlock(Material blockType, Material selected) {
        return blockType == selected && Cfg.FARM_CROPS.contains(blockType);
    }

    private boolean isMatureCrop(Block b) {
        BlockData data = b.getBlockData();
        if (data instanceof Ageable) {
            Ageable ag = (Ageable) data;
            return ag.getAge() >= ag.getMaximumAge();
        }
        return true;
    }

    private BlockData fullyGrownData(Material crop) {
        BlockData data = Bukkit.createBlockData(crop);
        if (data instanceof Ageable) {
            Ageable ag = (Ageable) data;
            ag.setAge(ag.getMaximumAge());
            return ag;
        }
        return data;
    }

    private Material materialToDrop(Material selected) {
        switch (selected) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE: return Material.COAL;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE: return Material.RAW_IRON;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE: return Material.RAW_GOLD;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE: return Material.RAW_COPPER;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE: return Material.REDSTONE;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE: return Material.LAPIS_LAZULI;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE: return Material.DIAMOND;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE: return Material.EMERALD;
            case NETHER_QUARTZ_ORE: return Material.QUARTZ;
            case WHEAT: return Material.WHEAT;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOT;
            case CHERRY_SAPLING: return Material.CHERRY_SAPLING;
            case ROSE_BUSH: return Material.ROSE_BUSH;
            case TUBE_CORAL: return Material.TUBE_CORAL;
            case FIRE_CORAL: return Material.FIRE_CORAL;
            default: return selected;
        }
    }

    private int applyYieldBonuses(Player p, ItemStack tool, int base) {
        double mult = InstancedNodesPlugin.get().level().getIncomeMultiplier(p.getUniqueId());

        if (tool != null) {
            int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
            if (fortune > 0) {
                mult *= 1.0 + InstancedNodesPlugin.get().getRandom().nextInt(fortune + 1);
            }
            Plugin spec = Bukkit.getPluginManager().getPlugin("SpecialItems");
            if (spec != null) {
                ItemMeta meta = tool.getItemMeta();
                if (meta != null) {
                    NamespacedKey key = new NamespacedKey(spec, "si_bonus_yield_pct");
                    Double pct = meta.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
                    if (pct != null) mult *= 1.0 + (pct / 100.0);
                }
            }
        }

        int result = (int)Math.round(base * mult);
        return result < 1 ? 1 : result;
    }

    private static class BlockVector {
        final int x, y, z;
        BlockVector(int x, int y, int z){ this.x=x; this.y=y; this.z=z; }
        @Override public boolean equals(Object o){ if(!(o instanceof BlockVector)) return false; BlockVector b=(BlockVector)o; return x==b.x && y==b.y && z==b.z; }
        @Override public int hashCode(){ return java.util.Objects.hash(x,y,z); }
    }

    private static class RespawnInfo {
        final Material ore;
        final Material base;
        final long until;
        RespawnInfo(Material ore, Material base, long until){ this.ore=ore; this.base=base; this.until=until; }
    }

    private Player purify(Player p){ return p; } // placeholder for future context purification if needed
}
