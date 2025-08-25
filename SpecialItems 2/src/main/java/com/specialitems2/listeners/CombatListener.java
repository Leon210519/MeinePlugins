package com.specialitems2.listeners;

import com.specialitems2.SpecialItems2Plugin;
import com.specialitems2.effects.CustomEffect;
import com.specialitems2.effects.Effects;
import com.specialitems2.util.Configs;
import com.specialitems2.util.ItemUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.LinkedHashSet;
import java.util.Set;

public class CombatListener implements Listener {

    private static Set<String> idsFromItem(ItemStack item) {
        Set<String> ids = new LinkedHashSet<>();
        if (item == null || item.getType() == Material.AIR) return ids;
        ItemMeta m = item.getItemMeta();
        if (m == null) return ids;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (pdc == null) return ids;
        for (var key : pdc.getKeys()) {
            String k = key.getKey();
            if (k != null && k.startsWith("ench_")) {
                ids.add(k.substring("ench_".length()));
            }
        }
        return ids;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
        if (Configs.cfg.getStringList("general.disabled-worlds").contains(p.getWorld().getName())) return;
        ItemStack weapon = p.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() == Material.AIR) return;

        String wn = weapon.getType().name();
        if (wn.endsWith("_SWORD") || wn.endsWith("_AXE")) {
            int dl = ItemUtil.getEffectLevel(weapon, "double_damage");
            if (dl > 0) {
                double mult = dl >= 5 ? 2.0 : 1.0 + 0.2 * dl;
                e.setDamage(e.getDamage() * mult);
            }
        }

        if (Effects.size() == 0) {
            try { Effects.registerDefaults(); } catch (Throwable ignored) {}
        }
        Set<String> candidates = new LinkedHashSet<>(Effects.ids());
        candidates.addAll(idsFromItem(weapon));

        for (String id : candidates) {
            if (!Configs.effectEnabled(id)) continue;
            var eff = Effects.get(id);
            if (eff == null) continue;
            if (!eff.supports(weapon.getType())) continue;
            int level = ItemUtil.getEffectLevel(weapon, id);
            if (level <= 0) continue;
            eff.onEntityDamage(p, weapon, e, Math.min(level, eff.maxLevel()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        if (!(e.getEntity().getKiller() instanceof Player p)) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
        if (Configs.cfg.getStringList("general.disabled-worlds").contains(p.getWorld().getName())) return;
        ItemStack weapon = p.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType() == Material.AIR) return;

        if (Effects.size() == 0) {
            try { Effects.registerDefaults(); } catch (Throwable ignored) {}
        }
        Set<String> candidates = new LinkedHashSet<>(Effects.ids());
        candidates.addAll(idsFromItem(weapon));

        for (String id : candidates) {
            if (!Configs.effectEnabled(id)) continue;
            var eff = Effects.get(id);
            if (eff == null) continue;
            if (!eff.supports(weapon.getType())) continue;
            int level = ItemUtil.getEffectLevel(weapon, id);
            if (level <= 0) continue;
            eff.onEntityKill(p, weapon, e, Math.min(level, eff.maxLevel()));
        }
    }
}