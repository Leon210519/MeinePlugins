package com.specialitems.leveling;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.CustomEffect;
import com.specialitems.effects.Effects;
import com.specialitems.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class LevelOverviewGUI implements InventoryHolder, Listener {

    private final SpecialItemsPlugin plugin;
    private final Inventory inv;
    private final Player target;

    private LevelOverviewGUI(SpecialItemsPlugin plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
        this.inv = Bukkit.createInventory(this, 6 * 9, ChatColor.DARK_AQUA + "Your Special Items");
    }

    public static void open(Player p) {
        SpecialItemsPlugin pl = SpecialItemsPlugin.getInstance();
        LevelOverviewGUI gui = new LevelOverviewGUI(pl, p);
        Bukkit.getPluginManager().registerEvents(gui, pl);
        gui.populate();
        p.openInventory(gui.inv);
    }

    private static boolean hasSpecialPdc(ItemStack it) {
        if (it == null) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        if (pdc == null) return false;
        for (NamespacedKey key : pdc.getKeys()) {
            String k = key.getKey();
            if (k != null && k.startsWith("ench_")) return true;
        }
        return false;
    }

    private static String roman(int n) {
        String[] r = {"","I","II","III","IV","V","VI","VII","VIII","IX","X"};
        if (n >= 0 && n < r.length) return r[n];
        StringBuilder sb = new StringBuilder();
        while (n >= 10) { sb.append("X"); n -= 10; }
        if (n >= 9) { sb.append("IX"); n -= 9; }
        if (n >= 5) { sb.append("V"); n -= 5; }
        if (n >= 4) { sb.append("IV"); n -= 4; }
        while (n >= 1) { sb.append("I"); n -= 1; }
        return sb.toString();
    }

    private static String prettyVanilla(Enchantment e) {
        String key = e.getKey().getKey().replace('_', ' ');
        if (key.isEmpty()) return "Unknown";
        return key.substring(0,1).toUpperCase(Locale.ROOT) + key.substring(1);
    }

    private void addIfSpecial(ItemStack it) {
        if (it == null || it.getType() == Material.AIR) return;
        var svc = plugin.leveling();
        boolean special = svc.isSpecialItem(it) || hasSpecialPdc(it);
        if (!special) return;

        int lvl = svc.getLevel(it);
        double xp = svc.getXp(it);
        double need = LevelMath.neededXpFor(lvl);

        ItemStack display = it.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            try { meta.removeItemFlags(ItemFlag.values()); } catch (Throwable ignored) {}

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(ChatColor.GRAY + " ");
            lore.add(ChatColor.GOLD + "Level: " + ChatColor.YELLOW + lvl);
            lore.add(ChatColor.GOLD + "XP: " + ChatColor.YELLOW + String.format("%.1f", xp) + ChatColor.GRAY + " / " + ChatColor.YELLOW + String.format("%.1f", need));
            if (plugin.leveling().detectToolClass(it) == ToolClass.HOE) {
                double by = plugin.leveling().getBonusYieldPct(it);
                lore.add(ChatColor.GOLD + "Bonus Yield: " + ChatColor.YELLOW + String.format("%.0f%%", by));
            }

            Map<Enchantment, Integer> enchants = it.getEnchantments();
            if (!enchants.isEmpty()) {
                lore.add(ChatColor.GRAY + " ");
                lore.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Vanilla Enchants:");
                for (Map.Entry<Enchantment, Integer> en : enchants.entrySet()) {
                    lore.add(ChatColor.BLUE + " - " + prettyVanilla(en.getKey()) + " " + roman(en.getValue()));
                }
            }

            List<String> specialLines = new ArrayList<>();
            for (String id : Effects.ids()) {
                int elv = 0;
                try { elv = ItemUtil.getEffectLevel(it, id); } catch (Throwable ignored) {}
                if (elv > 0) {
                    CustomEffect ce = Effects.get(id);
                    String name = (ce != null ? ce.displayName() : id);
                    if (!name.isEmpty()) name = name.substring(0,1).toUpperCase(Locale.ROOT) + name.substring(1);
                    specialLines.add(ChatColor.LIGHT_PURPLE + " - " + name + " " + roman(elv));
                }
            }

            if (specialLines.isEmpty()) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc != null) {
                    for (NamespacedKey key : pdc.getKeys()) {
                        String k = key.getKey();
                        if (k != null && k.startsWith("ench_")) {
                            String effectId = k.substring("ench_".length());
                            Integer lv = pdc.get(key, PersistentDataType.INTEGER);
                            int effLevel = (lv == null ? 0 : lv);
                            if (effLevel > 0) {
                                CustomEffect ce = Effects.get(effectId);
                                String name = (ce != null ? ce.displayName() : effectId);
                                if (!name.isEmpty()) name = name.substring(0,1).toUpperCase(Locale.ROOT) + name.substring(1);
                                specialLines.add(ChatColor.LIGHT_PURPLE + " - " + name + " " + roman(effLevel));
                            }
                        }
                    }
                }
            }

            if (!specialLines.isEmpty()) {
                lore.add(ChatColor.GRAY + " ");
                lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Special Enchants:");
                lore.addAll(specialLines);
            }

            meta.setLore(lore);
            display.setItemMeta(meta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, display);
                return;
            }
        }
    }

    private void populate() {
        inv.clear();
        PlayerInventory pinv = target.getInventory();
        for (ItemStack it : pinv.getContents()) addIfSpecial(it);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}