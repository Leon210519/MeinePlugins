package com.lootfactory.prestige;

import com.lootfactory.economy.Eco;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PrestigeGuiUtil {

    private PrestigeGuiUtil() {}

    public static final String PDC_KEY_BUTTON = "prestige_button";
    public static final String PDC_KEY_FACTORY_TYPE = "factory_type";
    public static final String PDC_KEY_WORLD_UUID = "factory_world_uuid";
    public static final String PDC_KEY_WORLD_NAME = "factory_world_name";
    public static final String PDC_KEY_X = "factory_x";
    public static final String PDC_KEY_Y = "factory_y";
    public static final String PDC_KEY_Z = "factory_z";

    // Backwards compatible builder (no location)
    public static ItemStack buildPrestigeButton(JavaPlugin plugin,
                                                Player player,
                                                String factoryType,
                                                int factoryLevel,
                                                int currentPrestige,
                                                PrestigeManager pm,
                                                Eco eco) {
        return buildPrestigeButton(plugin, player, factoryType, factoryLevel, currentPrestige, pm, eco, null);
    }

    // New builder with location (recommended)
    public static ItemStack buildPrestigeButton(JavaPlugin plugin,
                                                Player player,
                                                String factoryType,
                                                int factoryLevel,
                                                int currentPrestige,
                                                PrestigeManager pm,
                                                Eco eco,
                                                Location loc) {

        int req = Math.max(100, pm.getRequirementLevel());
        double cost = pm.calcPrestigeCost(currentPrestige);
        boolean hasLvl = factoryLevel >= req;
        boolean hasMoney = hasEnoughMoney(player, cost, eco);
        boolean eligible = hasLvl && hasMoney && currentPrestige < 5;

        Material mat = eligible ? Material.NETHER_STAR : Material.GRAY_DYE;
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();

        Component name = legacy(eligible ? "&6&lPrestige &7(&aReady&7)" : "&7&lPrestige &8(Locked)")
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
                lore.add(legacy("&7Current Prestige: &6" + currentPrestige + " &7→ &6" + Math.min(currentPrestige + 1, 5)).decoration(TextDecoration.ITALIC, false));
        lore.add(legacy("&d+1 Prestige Key").decoration(TextDecoration.ITALIC, false));
        lore.add(legacy("&d+100% Yield").decoration(TextDecoration.ITALIC, false));
lore.add(Component.empty());
        lore.add(legacy("&7Required Level: " + (hasLvl ? "&a" : "&c") + req + " &7(You: " + factoryLevel + ")").decoration(TextDecoration.ITALIC, false));
        lore.add(legacy("&7Cost: " + (hasMoney ? "&a" : "&c") + money(cost)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        if (eligible) {
            lore.add(legacy("&eClick to prestige this factory.").decoration(TextDecoration.ITALIC, false));
        } else {
            if (!hasLvl) lore.add(legacy("&cYour factory level is too low.").decoration(TextDecoration.ITALIC, false));
            if (!hasMoney) lore.add(legacy("&cYou don't have enough money.").decoration(TextDecoration.ITALIC, false));
            if (currentPrestige >= 5) lore.add(legacy("&cMaximum prestige reached.").decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        // PDC tags (store RAW type + exact location if provided)
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, PDC_KEY_BUTTON), PersistentDataType.BYTE, (byte)1);
        pdc.set(new NamespacedKey(plugin, PDC_KEY_FACTORY_TYPE), PersistentDataType.STRING, factoryType);

        if (loc != null && loc.getWorld() != null) {
            pdc.set(new NamespacedKey(plugin, PDC_KEY_WORLD_UUID), PersistentDataType.STRING, loc.getWorld().getUID().toString());
            pdc.set(new NamespacedKey(plugin, PDC_KEY_WORLD_NAME), PersistentDataType.STRING, loc.getWorld().getName());
            pdc.set(new NamespacedKey(plugin, PDC_KEY_X), PersistentDataType.INTEGER, loc.getBlockX());
            pdc.set(new NamespacedKey(plugin, PDC_KEY_Y), PersistentDataType.INTEGER, loc.getBlockY());
            pdc.set(new NamespacedKey(plugin, PDC_KEY_Z), PersistentDataType.INTEGER, loc.getBlockZ());
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(meta);
        return it;
    }

    private static boolean hasEnoughMoney(Player player, double cost, Eco eco) {
        try {
            if (eco != null) {
                try { java.lang.reflect.Method m = eco.getClass().getMethod("has", Player.class, double.class);
                    Object r = m.invoke(eco, player, cost);
                    if (r instanceof Boolean) return (Boolean) r;
                } catch (NoSuchMethodException ignored) {}
                try { java.lang.reflect.Method m = eco.getClass().getMethod("has", java.util.UUID.class, double.class);
                    Object r = m.invoke(eco, player.getUniqueId(), cost);
                    if (r instanceof Boolean) return (Boolean) r;
                } catch (NoSuchMethodException ignored) {}
                try { java.lang.reflect.Method m = eco.getClass().getMethod("has", String.class, double.class);
                    Object r = m.invoke(eco, player.getName(), cost);
                    if (r instanceof Boolean) return (Boolean) r;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp != null && rsp.getProvider() != null) {
            return rsp.getProvider().has(player, cost);
        }
        return false;
    }

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s == null ? "" : s);
    }

    private static String money(double v) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(0);
        return "$" + nf.format(v);
    }
}
