package com.specialitems.listeners;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.CustomEffect;
import com.specialitems.effects.Effects;
import com.specialitems.util.Configs;
import com.specialitems.util.ItemUtil;
import com.specialitems.effects.impl.AutoSmelt;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class BlockListener implements Listener {

    private static Set<String> idsFromItem(ItemStack item) {
        Set<String> ids = new LinkedHashSet<>();
        if (item == null || item.getType() == Material.AIR) return ids;
        ItemMeta m = item.getItemMeta();
        if (m == null) return ids;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (pdc == null) return ids;
        for (NamespacedKey key : pdc.getKeys()) {
            String k = key.getKey();
            if (k != null && k.startsWith("ench_")) {
                ids.add(k.substring("ench_".length()));
            }
        }
        return ids;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p == null || p.getGameMode() == GameMode.CREATIVE) return;
        if (Configs.cfg.getStringList("general.disabled-worlds").contains(p.getWorld().getName())) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

        if (handleSpecialHarvest(p, tool, e)) return;

        if (Effects.size() == 0) {
            try { Effects.registerDefaults(); } catch (Throwable ignored) {}
        }

        Set<String> candidates = new LinkedHashSet<>(Effects.ids());
        candidates.addAll(idsFromItem(tool));

        for (String id : candidates) {
            CustomEffect eff = Effects.get(id);
            if (eff == null) continue;
            if (!eff.supports(tool.getType())) continue;
            int level = ItemUtil.getEffectLevel(tool, id);
            if (level <= 0) continue;
            eff.onBlockBreak(p, tool, e, Math.min(level, eff.maxLevel()));
        }
    }

    private boolean handleSpecialHarvest(Player p, ItemStack tool, BlockBreakEvent e) {
        Block b = e.getBlock();
        Material type = b.getType();
        boolean isCrop = b.getBlockData() instanceof Ageable;
        boolean isOre = type.name().endsWith("_ORE");
        if (!isCrop && !isOre) return false;

        if (isCrop) {
            Ageable age = (Ageable) b.getBlockData();
            if (age.getAge() < age.getMaximumAge()) {
                e.setCancelled(true);
                return true;
            }
        }

        e.setDropItems(false);
        Collection<ItemStack> drops = isCrop ? b.getDrops(new ItemStack(Material.WOODEN_HOE))
                : b.getDrops(new ItemStack(Material.WOODEN_PICKAXE));
        int enchLevel = ItemUtil.getEffectLevel(tool, isCrop ? "harvester" : "veinminer");
        double scale = Configs.cfg.getDouble(isCrop ? "specialitems.harvester_scale" : "specialitems.vein_scale", 0.0);
        double enchMul = 1.0 + scale * enchLevel;
        double yieldBonus = 1.0 + ItemUtil.getToolYieldBonus(tool);
        boolean minFloor = Configs.cfg.getBoolean("specialitems.min_total_floor", true);
        boolean smelt = ItemUtil.getEffectLevel(tool, "autosmelt") > 0;
        boolean direct = Configs.cfg.getBoolean("farmxmine.direct_to_inventory", true);

        for (ItemStack drop : drops) {
            ItemStack give = drop.clone();
            if (smelt) {
                Material out = AutoSmelt.SMELTS.get(give.getType());
                if (out != null) give.setType(out);
            }
            int base = give.getAmount();
            int total = (int) Math.floor(base * enchMul * yieldBonus);
            if (minFloor && base > 0 && total < 1) total = 1;
            give.setAmount(total);
            if (direct) {
                var leftover = p.getInventory().addItem(give);
                for (ItemStack it : leftover.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), it);
                }
            } else {
                p.getWorld().dropItemNaturally(b.getLocation(), give);
            }
        }
        return true;
    }
}