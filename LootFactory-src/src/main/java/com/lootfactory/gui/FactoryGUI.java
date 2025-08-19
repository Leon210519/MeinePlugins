package com.lootfactory.gui;

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.factory.FactoryDef;
import com.lootfactory.factory.FactoryInstance;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.lootfactory.prestige.PrestigeGuiUtil;
import com.lootfactory.prestige.PrestigeStars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FactoryGUI {

    public static void open(Player p, FactoryManager manager, FactoryInstance fi) {
        FactoryDef def = manager.getDef(fi.typeId);
        int prestige = fi.prestige; // INSTANCE prestige
        String baseTitle = (def != null ? def.display : "Factory");
        String rarityCode = "&7"; // default COMMON gray
        if (def != null && def.rarity != null) {
            String rn = def.rarity.name();
            if ("UNCOMMON".equals(rn))      rarityCode = "&a";
            else if ("RARE".equals(rn))     rarityCode = "&9";
            else if ("EPIC".equals(rn))     rarityCode = "&5";
            else if ("LEGENDARY".equals(rn))rarityCode = "&6";
            else if ("INSANE".equals(rn))   rarityCode = "&d";
            else                             rarityCode = "&7"; // COMMON or fallback
        }
        String title = Msg.color(rarityCode + PrestigeStars.withStarsLegacy(baseTitle, prestige));

        Inventory inv = Bukkit.createInventory(new FactoryView(manager, fi), 27, title);
        render(inv, manager, fi, p);
        p.openInventory(inv);

        // Start 1 Hz auto-refresh while the GUI is open
        GuiAutoRefresher.start(LootFactoryPlugin.get(), p, inv, () -> {
            FactoryGUI.refreshDynamic(p, inv, manager, fi);
        });
    }

    static void render(Inventory inv, FactoryManager manager, FactoryInstance fi, Player viewer) {
        inv.clear();

        int __prestige = fi.prestige; // INSTANCE prestige
        ItemStack __prestigeBtn = PrestigeGuiUtil.buildPrestigeButton(
            LootFactoryPlugin.get(),
            viewer,
            fi.typeId,
            fi.level,
            __prestige,
            LootFactoryPlugin.get().prestige(),
            LootFactoryPlugin.get().eco(),
            fi.location
        );

        // --- Add formatted prestige cost to button lore (currency + K/M/B/T) ---
        try {
            int currentPlayerPrestige = LootFactoryPlugin.get().prestige().getPrestige(viewer.getUniqueId(), fi.typeId);
            double prestigeCost = LootFactoryPlugin.get().prestige().calcPrestigeCost(currentPlayerPrestige);
            String cur = LootFactoryPlugin.get().getConfig().getString("economy.currency_symbol", "$");

            ItemMeta pm = __prestigeBtn.getItemMeta();
            List<String> lore = pm.hasLore() ? new ArrayList<>(pm.getLore()) : new ArrayList<>();
            lore.add(Msg.color("&7Cost: &e" + cur + FactoryGUI.formatMoney(prestigeCost)));
            pm.setLore(lore);
            __prestigeBtn.setItemMeta(pm);
        } catch (Throwable ignored) {}

        int __center = inv.getSize() / 2;
        inv.setItem(__center, __prestigeBtn);

        FactoryDef def = manager.getDef(fi.typeId);

        double yMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.yield_multiplier_per_level", 1.5);
        double sMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.speed_multiplier_per_level", 1.5);
        double target = LootFactoryPlugin.get().getConfig().getDouble("xp.seconds_per_level", 300d);
        String cur = LootFactoryPlugin.get().getConfig().getString("economy.currency_symbol", "$");
        double minInterval = LootFactoryPlugin.get().getConfig().getDouble("leveling.min_interval_seconds", 1.0);

        double yield = def.baseAmount * Math.pow(yMul, fi.level - 1);
        double interval = def.baseIntervalSec / Math.pow(sMul, fi.level - 1);
        double __prestigeMul = 1.0 + __prestige;
        yield *= __prestigeMul;

        yield *= (1.0 + def.yieldBonusPct / 100.0);
        interval /= (1.0 + def.speedBonusPct / 100.0);
        if (interval < minInterval) interval = minInterval;

        // Info item
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(Msg.color("&e" + PrestigeStars.withStarsLegacy(def.display, __prestige) + " &7(Level " + fi.level + ")"));
        List<String> lore = new ArrayList<>();
        // Show money with currency + K/M/B/T abbreviations
        lore.add(Msg.color("&7Production: &a" + cur + FactoryGUI.formatMoney(yield) +
                " &7per &a" + String.format("%.2f", interval) + "s" + " &d(x" + (1 + __prestige) + ")"));
        if (def.yieldBonusPct != 0 || def.speedBonusPct != 0) {
            lore.add(Msg.color("&7Perks: &a+" + (int) def.yieldBonusPct + "% Yield &7/ &a+" + (int) def.speedBonusPct + "% Speed"));
        }
        im.setLore(lore);
        info.setItemMeta(im);
        inv.setItem(10, info);

        // XP bar (18..26)
        int xpSlots = 9;
        int filled = (int) Math.round(Math.min(1.0, fi.xpSeconds / target) * xpSlots);
        for (int i = 0; i < xpSlots; i++) {
            ItemStack pane = new ItemStack(i < filled ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta pm = pane.getItemMeta();
            pm.setDisplayName(Msg.color("&aXP Progress &7(" + (int) Math.min(100, (fi.xpSeconds / target) * 100.0) + "%)"));
            List<String> l = new ArrayList<>();
            l.add(Msg.color("&7Time to level: &a" + (int) Math.max(0, target - fi.xpSeconds) + "s"));
            pm.setLore(l);
            pane.setItemMeta(pm);
            inv.setItem(18 + i, pane);
        }

        // Instant +1 (slot 15)
        double remaining = Math.max(0, target - fi.xpSeconds);
        double cycles = interval > 0 ? remaining / interval : 0;
        double expected = cycles * yield;
        double cost = expected * 2.5;

        ItemStack levelBtn = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta bm = levelBtn.getItemMeta();
        bm.setDisplayName(Msg.color("&aInstant Level-Up"));
        List<String> bl = new ArrayList<>();
        bl.add(Msg.color("&7Cost: &e" + cur + FactoryGUI.formatMoney(cost)));
        bl.add(Msg.color("&8(= 2.5x remaining production value)"));
        bm.setLore(bl);
        levelBtn.setItemMeta(bm);
        inv.setItem(15, levelBtn);

        // +10 (slot 16) and +25 (slot 17)
        double cost10 = FactoryGUI.calcInstantCostLevels(manager, fi, 10);
        ItemStack up10 = new ItemStack(Material.EMERALD);
        ItemMeta up10m = up10.getItemMeta();
        up10m.setDisplayName(Msg.color("&aUpgrade &e+10 &aLevels"));
        up10m.setLore(Arrays.asList(
                Msg.color("&7Cost: &e" + cur + FactoryGUI.formatMoney(cost10)),
                Msg.color("&8(2.5x of remaining production over next 10 lvls)")
        ));
        up10.setItemMeta(up10m);
        inv.setItem(16, up10);

        double cost25 = FactoryGUI.calcInstantCostLevels(manager, fi, 25);
        ItemStack up25 = new ItemStack(Material.DIAMOND);
        ItemMeta up25m = up25.getItemMeta();
        up25m.setDisplayName(Msg.color("&aUpgrade &e+25 &aLevels"));
        up25m.setLore(Arrays.asList(
                Msg.color("&7Cost: &e" + cur + FactoryGUI.formatMoney(cost25)),
                Msg.color("&8(2.5x of remaining production over next 25 lvls)")
        ));
        up25.setItemMeta(up25m);
        inv.setItem(17, up25);

        // Pickup button (default 22, avoid 15 collision)
        int pickupSlot = LootFactoryPlugin.get().getConfig().getInt("pickup.button_slot", 22);
        if (pickupSlot == 15) pickupSlot = 22;
        boolean isOwner = viewer.getUniqueId().equals(fi.owner);

        ItemStack pick = new ItemStack(isOwner ? Material.HOPPER : Material.BARRIER);
        ItemMeta pickMeta = pick.getItemMeta();
        pickMeta.setDisplayName(Msg.color(isOwner ? "&aPickup Factory" : "&7Not your factory"));
        List<String> pl = new ArrayList<>();
        if (isOwner) {
            pl.add(Msg.color("&7Pick up this factory (keeps prestige,level & XP)"));
        } else {
            pl.add(Msg.color("&8Only the owner can pick this up."));
        }
        pickMeta.setLore(pl);
        pick.setItemMeta(pickMeta);
        inv.setItem(pickupSlot, pick);
    }

    public static double calcInstantCostLevels(FactoryManager manager, FactoryInstance fi, int levels) {
        FactoryDef def = manager.getDef(fi.typeId);
        double yMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.yield_multiplier_per_level", 1.5);
        double sMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.speed_multiplier_per_level", 1.5);
        double target = LootFactoryPlugin.get().getConfig().getDouble("xp.seconds_per_level", 300d);
        double minInterval = LootFactoryPlugin.get().getConfig().getDouble("leveling.min_interval_seconds", 1.0);

        int tmpLevel = fi.level;
        double tmpXp = fi.xpSeconds;
        double expected = 0.0;

        for (int i = 0; i < levels; i++) {
            FactoryDef defLoop = manager.getDef(fi.typeId);
            double yield = defLoop.baseAmount * Math.pow(yMul, tmpLevel - 1);
            double interval = defLoop.baseIntervalSec / Math.pow(sMul, tmpLevel - 1);
            yield *= (1.0 + defLoop.yieldBonusPct / 100.0);
            interval /= (1.0 + defLoop.speedBonusPct / 100.0);
            if (interval < minInterval) interval = minInterval;

            double remaining = Math.max(0, (i == 0 ? (target - tmpXp) : target));
            expected += (interval > 0 ? (remaining / interval) * yield : 0);

            tmpLevel += 1;
            tmpXp = 0;
        }
        return expected * 2.5;
    }

    public static String formatMoney(double n) {
        double abs = Math.abs(n);
        String suffix = "";
        double val = n;
        if (abs >= 1_000_000_000_000.0) { val = n / 1_000_000_000_000.0; suffix = "T"; }
        else if (abs >= 1_000_000_000.0) { val = n / 1_000_000_000.0; suffix = "B"; }
        else if (abs >= 1_000_000.0) { val = n / 1_000_000.0; suffix = "M"; }
        else if (abs >= 1_000.0) { val = n / 1_000.0; suffix = "K"; }
        if (Math.abs(val) >= 100) return String.format("%.0f%s", val, suffix);
        if (Math.abs(val) >= 10) return String.format("%.1f%s", val, suffix);
        return String.format("%.2f%s", val, suffix);
    }

    static void refreshDynamic(Player p, Inventory inv, FactoryManager manager, FactoryInstance fi) {
        if (p == null || inv == null || fi == null) return;
        if (p.getOpenInventory() == null) return;

        FactoryDef def = manager.getDef(fi.typeId);

        double yMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.yield_multiplier_per_level", 1.5);
        double sMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.speed_multiplier_per_level", 1.5);
        double target = LootFactoryPlugin.get().getConfig().getDouble("xp.seconds_per_level", 300d);
        String cur = LootFactoryPlugin.get().getConfig().getString("economy.currency_symbol", "$");
        double minInterval = LootFactoryPlugin.get().getConfig().getDouble("leveling.min_interval_seconds", 1.0);

        double yield = def.baseAmount * Math.pow(yMul, fi.level - 1);
        double interval = def.baseIntervalSec / Math.pow(sMul, fi.level - 1);
        yield *= (1.0 + fi.prestige);
        yield *= (1.0 + def.yieldBonusPct / 100.0);
        interval /= (1.0 + def.speedBonusPct / 100.0);
        if (interval < minInterval) interval = minInterval;

        // XP BAR (18..26)
        int xpSlots = 9;
        int filled = (int) Math.round(Math.min(1.0, fi.xpSeconds / target) * xpSlots);
        for (int i = 0; i < xpSlots; i++) {
            ItemStack pane = new ItemStack(i < filled ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta pm = pane.getItemMeta();
            pm.setDisplayName(Msg.color("&aXP Progress &7(" + (int) Math.min(100, (fi.xpSeconds / target) * 100.0) + "%)"));
            List<String> l = new ArrayList<>();
            l.add(Msg.color("&7Time to level: &a" + (int) Math.max(0, target - fi.xpSeconds) + "s"));
            pm.setLore(l);
            pane.setItemMeta(pm);
            inv.setItem(18 + i, pane);
        }

        // COST BUTTONS (15/16/17)
        double remaining = Math.max(0, target - fi.xpSeconds);
        double cycles = interval > 0 ? remaining / interval : 0;
        double expected = cycles * yield;
        double cost = expected * 2.5;
        ItemStack levelBtn = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta bm = levelBtn.getItemMeta();
        bm.setDisplayName(Msg.color("&aInstant Level-Up"));
        List<String> bl = new ArrayList<>();
        bl.add(Msg.color("&7Cost: &e" + cur + FactoryGUI.formatMoney(cost)));
        bl.add(Msg.color("&8(= 2.5x remaining production value)"));
        bm.setLore(bl);
        levelBtn.setItemMeta(bm);
        inv.setItem(15, levelBtn);

        double cost10 = FactoryGUI.calcInstantCostLevels(manager, fi, 10);
        ItemStack up10 = new ItemStack(Material.EMERALD);
        ItemMeta up10m = up10.getItemMeta();
        up10m.setDisplayName(Msg.color("&aUpgrade &e+10 &aLevels"));
        up10m.setLore(Arrays.asList(
                Msg.color("&7Cost: &e" + cur + FactoryGUI.formatMoney(cost10)),
                Msg.color("&8(2.5x of remaining production over next 10 lvls)")
        ));
        up10.setItemMeta(up10m);
        inv.setItem(16, up10);

        double cost25 = FactoryGUI.calcInstantCostLevels(manager, fi, 25);
        ItemStack up25 = new ItemStack(Material.DIAMOND);
        ItemMeta up25m = up25.getItemMeta();
        up25m.setDisplayName(Msg.color("&aUpgrade &e+25 &aLevels"));
        up25m.setLore(Arrays.asList(
                Msg.color("&7Cost: &e" + cur + FactoryGUI.formatMoney(cost25)),
                Msg.color("&8(2.5x of remaining production over next 25 lvls)")
        ));
        up25.setItemMeta(up25m);
        inv.setItem(17, up25);
    }
}

class FactoryView implements InventoryHolder {
    private final FactoryManager manager;
    final FactoryInstance fi;

    FactoryView(FactoryManager manager, FactoryInstance fi) {
        this.manager = manager;
        this.fi = fi;
    }

    @Override
    public Inventory getInventory() { return null; }

    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.getSlot() < 0) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        // +25 (slot 17)
        if (e.getSlot() == 17) {
            double cost = FactoryGUI.calcInstantCostLevels(manager, fi, 25);
            if (!LootFactoryPlugin.get().eco().has(cost, p)) {
                p.sendMessage(Msg.prefix() + Msg.color("&cYou don't have enough money."));
                return;
            }
            LootFactoryPlugin.get().eco().withdraw(p, cost);
            fi.level += 25;
            fi.xpSeconds = 0;
            p.sendMessage(Msg.prefix() + Msg.color("&aUpgraded &e+25 &alevels! Now &eL" + fi.level));
            FactoryGUI.open(p, manager, fi);
            return;
        }

        // +10 (slot 16)
        if (e.getSlot() == 16) {
            double cost = FactoryGUI.calcInstantCostLevels(manager, fi, 10);
            if (!LootFactoryPlugin.get().eco().has(cost, p)) {
                p.sendMessage(Msg.prefix() + Msg.color("&cYou don't have enough money."));
                return;
            }
            LootFactoryPlugin.get().eco().withdraw(p, cost);
            fi.level += 10;
            fi.xpSeconds = 0;
            p.sendMessage(Msg.prefix() + Msg.color("&aUpgraded &e+10 &alevels! Now &eL" + fi.level));
            FactoryGUI.open(p, manager, fi);
            return;
        }

        // +1 (slot 15)
        if (e.getSlot() == 15) {
            double target = LootFactoryPlugin.get().getConfig().getDouble("xp.seconds_per_level", 300d);

            FactoryDef def = manager.getDef(fi.typeId);
            double yMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.yield_multiplier_per_level", 1.5);
            double sMul = LootFactoryPlugin.get().getConfig().getDouble("leveling.speed_multiplier_per_level", 1.5);

            double yield = def.baseAmount * Math.pow(yMul, fi.level - 1);
            double interval = def.baseIntervalSec / Math.pow(sMul, fi.level - 1);
            yield *= (1.0 + def.yieldBonusPct / 100.0);
            interval /= (1.0 + def.speedBonusPct / 100.0);

            double minInterval = LootFactoryPlugin.get().getConfig().getDouble("leveling.min_interval_seconds", 1.0);
            if (interval < minInterval) interval = minInterval;

            double remaining = Math.max(0, target - fi.xpSeconds);
            double cycles = interval > 0 ? remaining / interval : 0;
            double expected = cycles * yield;
            double cost = expected * 2.5;

            if (!LootFactoryPlugin.get().eco().has(cost, p)) {
                p.sendMessage(Msg.prefix() + Msg.color("&cYou don't have enough money."));
                return;
            }

            LootFactoryPlugin.get().eco().withdraw(p, cost);
            fi.xpSeconds = 0;
            fi.level += 1;

            p.sendMessage(Msg.prefix() + Msg.color("&aInstant leveled up your &e" + def.display + "&a to &eLevel " + fi.level + "&a!"));
            FactoryGUI.open(p, manager, fi);
            return;
        }

        // Pickup button
        int pickupSlot = LootFactoryPlugin.get().getConfig().getInt("pickup.button_slot", 22);
        if (pickupSlot == 15) pickupSlot = 22;
        if (e.getSlot() == pickupSlot) {
            Player player = p;

            if (!player.getUniqueId().equals(fi.owner)) {
                player.sendMessage(Msg.prefix() + Msg.color("&cOnly the owner can pick up this factory."));
                return;
            }
            if (fi.location == null || fi.location.getWorld() == null) {
                player.sendMessage(Msg.prefix() + Msg.color("&cFactory location invalid."));
                return;
            }

            fi.location.getBlock().setType(Material.AIR);

            FactoryInstance removed = manager.removeAt(fi.location);
            if (removed == null) {
                player.sendMessage(Msg.prefix() + Msg.color("&cFactory not found."));
                return;
            }

            ItemStack item = manager.createFactoryItem(removed.typeId, removed.level, removed.xpSeconds, removed.prestige);
            java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(item);
            if (!left.isEmpty()) {
                fi.location.getWorld().dropItemNaturally(fi.location, item);
            }

            manager.save();
            player.closeInventory();
            player.sendMessage(Msg.prefix() + Msg.color("&aPicked up your factory."));
        }
    }

    public void onClose(InventoryCloseEvent e) {
        // no-op
    }
}
