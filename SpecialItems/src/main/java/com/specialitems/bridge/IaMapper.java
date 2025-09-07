package com.specialitems.bridge;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Returns the original SI item unless a mapped IA item exists.
 * If ItemsAdder is missing or the IA item can't be built, we fall back cleanly.
 * Copies meta so SI names/lore/enchants etc. remain intact.
 */
public final class IaMapper {
    private IaMapper() {}

    // Map ONLY the armor templates that have skins
    private static final Map<String, String> TEMPLATE_TO_IA = new HashMap<>();
    static {
        // Legendary (Omega)
        TEMPLATE_TO_IA.put("legendary_helm",  "lootforge:omega_helmet");
        TEMPLATE_TO_IA.put("legendary_chest", "lootforge:omega_chestplate");
        TEMPLATE_TO_IA.put("legendary_legs",  "lootforge:omega_leggings");
        TEMPLATE_TO_IA.put("legendary_boots", "lootforge:omega_boots");
        // Epic (Galaxy)
        TEMPLATE_TO_IA.put("epic_helm",  "lootforge:epic_helmet");
        TEMPLATE_TO_IA.put("epic_chest", "lootforge:epic_chestplate");
        TEMPLATE_TO_IA.put("epic_legs",  "lootforge:epic_leggings");
        TEMPLATE_TO_IA.put("epic_boots", "lootforge:epic_boots");
    }

    public static ItemStack maybeSwap(String templateId, ItemStack vanilla) {
        if (vanilla == null) return null;
        String iaId = TEMPLATE_TO_IA.get(templateId);
        if (iaId == null) return vanilla; // not a mapped template

        try {
            // Lazy-load via reflection to avoid hard failure if IA is absent
            Class<?> csClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            var getInstance = csClass.getMethod("getInstance", String.class);
            Object cs = getInstance.invoke(null, iaId);
            if (cs == null) return vanilla;

            var getItemStack = csClass.getMethod("getItemStack");
            ItemStack out = ((ItemStack) getItemStack.invoke(cs)).clone();
            out.setAmount(vanilla.getAmount());

            ItemMeta src = vanilla.getItemMeta();
            ItemMeta dst = out.getItemMeta();
            if (src != null && dst != null) {
                if (src.hasDisplayName()) dst.setDisplayName(src.getDisplayName());
                if (src.hasLore())        dst.setLore(src.getLore());
                src.getEnchants().forEach((e,l) -> dst.addEnchant(e,l,true));
                dst.setUnbreakable(src.isUnbreakable());
                for (ItemFlag f : src.getItemFlags()) dst.addItemFlags(f);

                if (src instanceof Damageable s && dst instanceof Damageable d) {
                    d.setDamage(s.getDamage());
                }
                // keep original CMD if present (harmlos)
                try { if (src.hasCustomModelData()) dst.setCustomModelData(src.getCustomModelData()); } catch (Throwable ignored) {}

                // Copy common PDC types
                var sPdc = src.getPersistentDataContainer();
                var dPdc = dst.getPersistentDataContainer();
                for (var key : sPdc.getKeys()) {
                    if (sPdc.has(key, PersistentDataType.STRING))
                        dPdc.set(key, PersistentDataType.STRING, sPdc.get(key, PersistentDataType.STRING));
                    else if (sPdc.has(key, PersistentDataType.INTEGER))
                        dPdc.set(key, PersistentDataType.INTEGER, sPdc.get(key, PersistentDataType.INTEGER));
                    else if (sPdc.has(key, PersistentDataType.LONG))
                        dPdc.set(key, PersistentDataType.LONG, sPdc.get(key, PersistentDataType.LONG));
                    else if (sPdc.has(key, PersistentDataType.DOUBLE))
                        dPdc.set(key, PersistentDataType.DOUBLE, sPdc.get(key, PersistentDataType.DOUBLE));
                }
                out.setItemMeta(dst);
            }
            return out;
        } catch (Throwable t) {
            // IA not installed or API not present -> keep vanilla
            return vanilla;
        }
    }
}

