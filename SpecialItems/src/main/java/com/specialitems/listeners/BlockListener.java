package com.specialitems.listeners;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.CustomEffect;
import com.specialitems.effects.Effects;
import com.specialitems.util.Configs;
import com.specialitems.util.ItemUtil;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p == null || p.getGameMode() == GameMode.CREATIVE) return;
        if (Configs.cfg.getStringList("general.disabled-worlds").contains(p.getWorld().getName())) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool == null || tool.getType() == Material.AIR) return;

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
}