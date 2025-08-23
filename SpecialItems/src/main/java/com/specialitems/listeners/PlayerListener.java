package com.specialitems.listeners;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.CustomEffect;
import com.specialitems.effects.Effects;
import com.specialitems.util.Configs;
import com.specialitems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.LinkedHashSet;
import java.util.Set;

public class PlayerListener implements Listener {

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

    public static void tickAll() {
        if (Effects.size() == 0) {
            try { Effects.registerDefaults(); } catch (Throwable ignored) {}
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Configs.cfg.getStringList("general.disabled-worlds").contains(p.getWorld().getName())) continue;
            ItemStack main = p.getInventory().getItemInMainHand();
            ItemStack off = p.getInventory().getItemInOffHand();
            ItemStack[] armor = p.getInventory().getArmorContents();

            Set<String> candidates = new LinkedHashSet<>(Effects.ids());
            candidates.addAll(idsFromItem(main));
            candidates.addAll(idsFromItem(off));
            if (armor != null) for (ItemStack a : armor) candidates.addAll(idsFromItem(a));

            for (String id : candidates) {
                if (!Configs.effectEnabled(id)) continue;
                var eff = Effects.get(id);
                if (eff == null) continue;
                int best = 0;
                if (main != null && main.getType()!=Material.AIR && eff.supports(main.getType())) best = Math.max(best, ItemUtil.getEffectLevel(main, id));
                if (off != null && off.getType()!=Material.AIR && eff.supports(off.getType())) best = Math.max(best, ItemUtil.getEffectLevel(off, id));
                if (armor != null) {
                    for (ItemStack a : armor) {
                        if (a==null || a.getType()==Material.AIR) continue;
                        if (eff.supports(a.getType())) best = Math.max(best, ItemUtil.getEffectLevel(a, id));
                    }
                }
                if (best > 0) eff.onTick(p, main, Math.min(best, eff.maxLevel()));
            }
        }
    }

    public static void tickAbsorption() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Configs.cfg.getStringList("general.disabled-worlds").contains(p.getWorld().getName())) continue;
            ItemStack[] armor = p.getInventory().getArmorContents();
            int total = 0;
            if (armor != null) {
                for (ItemStack a : armor) {
                    total += ItemUtil.getEffectLevel(a, "absorption_shield");
                }
            }
            if (total > 0) {
                int amp = Math.min(3, total - 1);
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, amp, true, false, false));
            } else {
                p.removePotionEffect(PotionEffectType.ABSORPTION);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack newItem = p.getInventory().getItem(e.getNewSlot());
        if (Effects.size() == 0) {
            try { Effects.registerDefaults(); } catch (Throwable ignored) {}
        }
        Set<String> candidates = new LinkedHashSet<>(Effects.ids());
        candidates.addAll(idsFromItem(newItem));
        for (String id : candidates) {
            if (!Configs.effectEnabled(id)) continue;
            CustomEffect eff = Effects.get(id);
            if (eff == null) continue;
            if (newItem == null || newItem.getType() == Material.AIR || !eff.supports(newItem.getType())) continue;
            int level = ItemUtil.getEffectLevel(newItem, id);
            if (level <= 0) continue;
            eff.onItemHeld(p, newItem, Math.min(level, eff.maxLevel()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(SpecialItemsPlugin.getInstance(), PlayerListener::tickAll, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        p.removePotionEffect(PotionEffectType.HASTE);
        p.removePotionEffect(PotionEffectType.ABSORPTION);
    }
}