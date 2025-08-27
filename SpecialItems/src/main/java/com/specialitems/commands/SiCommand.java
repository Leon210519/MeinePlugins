package com.specialitems.commands;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.Effects;
import com.specialitems.gui.TemplateGUI;
import com.specialitems.leveling.LevelMath;
import com.specialitems.leveling.LevelOverviewGUI;
import com.specialitems.leveling.ToolClass;
import com.specialitems.util.Configs;
import com.specialitems.util.ItemUtil;
import com.specialitems.util.Tagger;
import com.specialitems.util.TemplateItems;
import com.specialitems.util.GuiItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SiCommand implements CommandExecutor {

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "SpecialItems " + ChatColor.GRAY + "— Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/si gui" + ChatColor.GRAY + " — Open templates GUI (admin)");
        sender.sendMessage(ChatColor.YELLOW + "/si give " + ChatColor.WHITE + "<player> <template>" + ChatColor.GRAY + " — Give a template (admin)");
        sender.sendMessage(ChatColor.YELLOW + "/si list " + ChatColor.WHITE + "<effects|templates>" + ChatColor.GRAY + " — Show lists");
        sender.sendMessage(ChatColor.YELLOW + "/si reload" + ChatColor.GRAY + " — Reload configuration (admin)");
        sender.sendMessage(ChatColor.YELLOW + "/si levels" + ChatColor.GRAY + " — Open level overview");
        sender.sendMessage(ChatColor.YELLOW + "/si inspect" + ChatColor.GRAY + " — Inspect held item");
        sender.sendMessage(ChatColor.YELLOW + "/si publicinspect" + ChatColor.GRAY + " — Broadcast held item");
        sender.sendMessage(ChatColor.YELLOW + "/si retag " + ChatColor.WHITE + "[id]" + ChatColor.GRAY + " — Tag held item as Special (admin)");
    }

    private static boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Players only.");
        return false;
    }

    private static boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("specialitems.admin")) {
            return true;
        }
        sender.sendMessage(ChatColor.RED + "No permission.");
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "gui" -> {
                if (!requirePlayer(sender) || !requireAdmin(sender)) return true;
                TemplateGUI.open((Player) sender, 0);
                return true;
            }
            case "give" -> {
                if (!requireAdmin(sender)) return true;
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /si give <player> <templateId>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }
                String tid = args[2];
                ConfigurationSection tsec = Configs.templates.getConfigurationSection("templates." + tid);
                if (tsec == null) {
                    sender.sendMessage(ChatColor.RED + "Template not found: " + tid);
                    return true;
                }
                TemplateItems.TemplateItem tmpl = TemplateItems.buildFrom(tid, tsec);
                if (tmpl != null) {
                    ItemStack give = GuiItemUtil.forDisplay(SpecialItemsPlugin.getInstance(), tmpl.stack());
                    if (give == null) {
                        give = tmpl.stack().clone();
                    }
                    if (tmpl.customModelData() != null) {
                        ItemUtil.forceSetCustomModelData(give, tmpl.customModelData());
                    }
                    ItemUtil.normalizeCustomModelData(give);
                    target.getInventory().addItem(give);
                }
                sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.YELLOW + tid + ChatColor.GREEN + " to " + ChatColor.YELLOW + target.getName());
                return true;
            }
            case "reload" -> {
                if (!requireAdmin(sender)) return true;
                Configs.load(SpecialItemsPlugin.getInstance());
                TemplateItems.loadAll();
                sender.sendMessage(ChatColor.GREEN + "SpecialItems configuration reloaded.");
                return true;
            }
            case "retag" -> {
                if (!requirePlayer(sender) || !requireAdmin(sender)) return true;
                Player p = (Player) sender;
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    p.sendMessage(ChatColor.RED + "Hold the item you want to tag.");
                    return true;
                }
                String id = (args.length >= 2) ? args[1] : (held.hasItemMeta() && held.getItemMeta().hasDisplayName() ? held.getItemMeta().getDisplayName() : held.getType().name());
                id = id.replaceAll("§.", "").replaceAll("[^A-Za-z0-9]+", "_").toLowerCase(Locale.ROOT);
                Tagger.tagAsSpecial(SpecialItemsPlugin.getInstance(), held, id);
                p.sendMessage(ChatColor.GREEN + "Item tagged as Special: " + ChatColor.YELLOW + id);
                return true;
            }
            case "levels", "level" -> {
                if (!requirePlayer(sender)) return true;
                LevelOverviewGUI.open((Player) sender);
                return true;
            }
            case "inspect" -> {
                if (!requirePlayer(sender)) return true;
                Player p = (Player) sender;
                SpecialItemsPlugin pl = SpecialItemsPlugin.getInstance();
                ItemStack held = p.getInventory().getItemInMainHand();
                if (!pl.leveling().isSpecialItem(held) && ItemUtil.getEffectLevel(held, "veinminer") <= 0 && ItemUtil.getEffectLevel(held, "telekinesis") <= 0) {
                    p.sendMessage(ChatColor.RED + "Hold a Special Item.");
                    return true;
                }
                int lvl = pl.leveling().getLevel(held);
                double xp = pl.leveling().getXp(held);
                double need = LevelMath.neededXpFor(lvl);
                ToolClass tc = pl.leveling().detectToolClass(held);
                p.sendMessage(ChatColor.GOLD + "Tool: " + ChatColor.YELLOW + tc);
                p.sendMessage(ChatColor.GOLD + "Level: " + ChatColor.YELLOW + lvl +
                        ChatColor.GRAY + " (" + String.format("%.1f", xp) + " / " + String.format("%.1f", need) + ")");
                if (tc == ToolClass.HOE) {
                    double by = pl.leveling().getBonusYieldPct(held);
                    p.sendMessage(ChatColor.GOLD + "Bonus Yield: " + ChatColor.YELLOW + String.format("%.0f%%", by));
                }
                return true;
            }
            case "publicinspect", "pi" -> {
                if (!requirePlayer(sender)) return true;
                Player p = (Player) sender;
                SpecialItemsPlugin pl = SpecialItemsPlugin.getInstance();
                ItemStack held = p.getInventory().getItemInMainHand();
                if (!pl.leveling().isSpecialItem(held) && ItemUtil.getEffectLevel(held, "veinminer") <= 0 && ItemUtil.getEffectLevel(held, "telekinesis") <= 0) {
                    p.sendMessage(ChatColor.RED + "Hold a Special Item.");
                    return true;
                }
                int lvl = pl.leveling().getLevel(held);
                double xp = pl.leveling().getXp(held);
                double need = LevelMath.neededXpFor(lvl);
                ToolClass tc = pl.leveling().detectToolClass(held);
                Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "[PublicInspect] " +
                        ChatColor.AQUA + p.getName() + ChatColor.GRAY + " shows a Special Item:");
                Bukkit.broadcastMessage(ChatColor.GOLD + "✦ Tool: " + ChatColor.YELLOW + tc);
                Bukkit.broadcastMessage(ChatColor.GOLD + "✦ Level: " + ChatColor.YELLOW + lvl +
                        ChatColor.GRAY + " (" + String.format("%.1f", xp) + " / " + String.format("%.1f", need) + ")");
                if (tc == ToolClass.HOE) {
                    double by = pl.leveling().getBonusYieldPct(held);
                    Bukkit.broadcastMessage(ChatColor.GOLD + "✦ Bonus Yield: " + ChatColor.YELLOW + String.format("%.0f%%", by));
                }
                return true;
            }
            case "list" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /si list <effects|templates>");
                    return true;
                }
                String kind = args[1].toLowerCase(Locale.ROOT);
                if (kind.equals("effects")) {
                    List<String> ids = new ArrayList<>(Effects.ids());
                    if (ids.isEmpty()) {
                        Set<String> fallback = new LinkedHashSet<>();
                        var troot = Configs.templates.getConfigurationSection("templates");
                        if (troot != null) {
                            for (String id : troot.getKeys(false)) {
                                var t = troot.getConfigurationSection(id);
                                if (t != null) {
                                    var ench = t.getConfigurationSection("enchants");
                                    if (ench != null) fallback.addAll(ench.getKeys(false));
                                }
                            }
                        }
                        ids.addAll(fallback);
                    }
                    sender.sendMessage(ChatColor.GOLD + "Registered effects: " + (ids.isEmpty() ? (ChatColor.RED + "(none)") : (ChatColor.YELLOW + String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, ids))));
                    return true;
                } else if (kind.equals("templates")) {
                    ConfigurationSection troot = Configs.templates.getConfigurationSection("templates");
                    if (troot == null) {
                        sender.sendMessage(ChatColor.RED + "No templates found.");
                        return true;
                    }
                    Set<String> keys = troot.getKeys(false);
                    sender.sendMessage(ChatColor.GOLD + "Templates: " + (keys.isEmpty() ? (ChatColor.RED + "(none)") : (ChatColor.YELLOW + String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, keys))));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /si list <effects|templates>");
                    return true;
                }
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }
}