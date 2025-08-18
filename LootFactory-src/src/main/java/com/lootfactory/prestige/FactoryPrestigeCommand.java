package com.lootfactory.prestige;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.factory.FactoryInstance;
import com.lootfactory.factory.FactoryManager;

/**
 * Handles /prestige <factoryType> [@loc:... | @locname:...]
 * IMPORTANT: This command no longer creates or gives any key item directly.
 * The single source of truth for keys is PrestigeService.givePrestigeKeys(p).
 */
public final class FactoryPrestigeCommand implements CommandExecutor {

    private static final int MAX_PRESTIGE = 5; // hard cap

    private final PrestigeManager pm;
    private final FactoryGateway factories;
    private final Economy economy; // Vault
    private final Map<UUID, Long> lastUse = new HashMap<>();

    // keep the KeyItemService parameter to stay binary/source compatible with your plugin's bootstrap
    @SuppressWarnings("unused")
    public FactoryPrestigeCommand(PrestigeManager pm, KeyItemService keyItems, FactoryGateway factories) {
        this.pm = pm;
        this.factories = factories;
        this.economy = resolveEconomy();
    }

    private Economy resolveEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("In-game only.");
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(Component.text("Usage: /" + label + " <factoryType> [@loc:<worldUUID>:<x>:<y>:<z> | @locname:<worldName>:<x>:<y>:<z>]", NamedTextColor.YELLOW));
            return true;
        }

        // Cooldown
        int cd = pm.getCooldownSeconds();
        long now = System.currentTimeMillis();
        Long last = lastUse.get(p.getUniqueId());
        if (last != null && (now - last) < cd * 1000L) {
            long left = cd * 1000L - (now - last);
            p.sendMessage(Component.text("Please wait " + (left / 1000) + "s before prestiging again.", NamedTextColor.GRAY));
            return true;
        }

        String typeArg = args[0];

        // Parse location from any additional arg token
        Location loc = parseLocationFromArgs(args);

        FactoryGateway.FactoryHandle f;
        if (loc != null) {
            f = factories.getFactoryByLocation(p.getUniqueId(), loc);
            if (f == null) {
                p.sendMessage(Component.text("You don't own a factory at this location.", NamedTextColor.RED));
                return true;
            }
        } else {
            // search by type variants (fallback if no @loc/@locname)
            List<String> candidates = new ArrayList<>();
            candidates.add(typeArg);
            candidates.add(typeArg.toUpperCase(Locale.ROOT));
            candidates.add(typeArg.toLowerCase(Locale.ROOT));
            candidates.add(typeArg.replace(' ', '_'));
            candidates.add(typeArg.replace(' ', '_').toUpperCase(Locale.ROOT));
            f = null;
            for (String c : candidates) {
                f = factories.getFactory(p.getUniqueId(), c);
                if (f != null) break;
            }
            if (f == null) {
                p.sendMessage(Component.text("You don't own the factory ").append(Component.text(typeArg, NamedTextColor.AQUA)).append(Component.text(".", NamedTextColor.RED)));
                return true;
            }
        }

        String typeKey = f.getType();

        // Requirement: enforce at least configured level (default 100)
        int reqLevel = Math.max(100, pm.getRequirementLevel());
        if (f.getLevel() < reqLevel) {
            p.sendMessage(Component.text("Requires factory level " + reqLevel + ".", NamedTextColor.RED));
            return true;
        }

        // Cap
        int currentPrestige = pm.getPrestige(p.getUniqueId(), typeKey);
        if (currentPrestige >= MAX_PRESTIGE) {
            p.sendMessage(Component.text("You have reached the maximum prestige (" + MAX_PRESTIGE + ").", NamedTextColor.RED));
            return true;
        }

        // Economy check
        if (economy == null) {
            p.sendMessage(Component.text("Economy (Vault) not found.", NamedTextColor.RED));
            return true;
        }
        double cost = pm.calcPrestigeCost(currentPrestige);
        if (!economy.has(p, cost)) {
            p.sendMessage(Component.text("You don't have enough money. Need " + formatMoney(cost) + ".", NamedTextColor.RED));
            return true;
        }

        // 1) Reset BEFORE charging
        try {
            f.resetToLevel(1, pm.getKeepStoragePercent(), pm.isKeepCosmetics());
        } catch (Throwable t) {
            p.sendMessage(Component.text("Could not reset the factory. Contact an admin.", NamedTextColor.RED));
            return true;
        }

        // 2) Increase prestige (+1, capped)
        int newPrestige = Math.min(currentPrestige + 1, MAX_PRESTIGE);
        pm.setPrestige(p.getUniqueId(), typeKey, newPrestige);
        bestEffortSave(pm);

        // 2b) INSTANCE update if location known (from @loc or @locname)
        if (loc != null) {
            try {
                FactoryManager fm = LootFactoryPlugin.get().factories();
                FactoryInstance inst = fm.getAt(loc);
                if (inst != null && inst.owner.equals(p.getUniqueId()) && inst.typeId.equalsIgnoreCase(typeKey)) {
                    inst.prestige = newPrestige;
                    inst.level = 1;
                    inst.xpSeconds = 0d;
                    fm.save();
                }
            } catch (Throwable ignored) {}
        }

        // 3) Charge AFTER successful reset + prestige set
        EconomyResponse er = economy.withdrawPlayer(p, cost);
        if (er == null || !er.transactionSuccess()) {
            p.sendMessage(Component.text("Transaction failed. Your balance was not charged.", NamedTextColor.RED));
            return true;
        }

        // 4) Give keys via the single central place
        PrestigeService.givePrestigeKeys(p);

        // 5) Feedback (no "+x Prestige Key" here; PrestigeService prints its own info)
        p.sendMessage(Component.text("Prestige successful! [")
                .append(Component.text(typeKey, NamedTextColor.AQUA))
                .append(Component.text("] to Level " + newPrestige + ".", NamedTextColor.GREEN)));

        lastUse.put(p.getUniqueId(), now);
        return true;
    }

    private void bestEffortSave(PrestigeManager pm) {
        try {
            Method m = pm.getClass().getMethod("save");
            m.setAccessible(true);
            m.invoke(pm);
        } catch (Throwable ignored) {}
    }

    // Robust parsing of both @loc and @locname
    private Location parseLocationFromArgs(String[] args) {
        for (String a : args) {
            if (a == null) continue;
            if (a.startsWith("@loc:")) {
                // @loc:<uuid>:x:y:z
                String[] parts = a.split(":");
                if (parts.length == 5) {
                    try {
                        UUID worldId = UUID.fromString(parts[1]);
                        World w = Bukkit.getWorld(worldId);
                        if (w == null) continue;
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int z = Integer.parseInt(parts[4]);
                        return new Location(w, x, y, z);
                    } catch (Exception ignored) {}
                }
            } else if (a.startsWith("@locname:")) {
                // @locname:<worldName>:x:y:z
                String[] parts = a.split(":");
                if (parts.length == 5) {
                    World w = Bukkit.getWorld(parts[1]);
                    if (w == null) continue;
                    try {
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int z = Integer.parseInt(parts[4]);
                        return new Location(w, x, y, z);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    private String formatMoney(double amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(0);
        return "$" + nf.format(amount);
    }
}
