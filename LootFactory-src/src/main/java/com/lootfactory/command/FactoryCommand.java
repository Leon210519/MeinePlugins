package com.lootfactory.command;

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.factory.FactoryDef;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.gui.FactoryTypesGUI;
import com.lootfactory.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FactoryCommand implements CommandExecutor {

    private final FactoryManager manager;

    public FactoryCommand(FactoryManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Msg.color("&e/factory give <player> <type> [level]"));
            sender.sendMessage(Msg.color("&e/factory reload"));
            sender.sendMessage(Msg.color("&e/factory listtypes"));
            sender.sendMessage(Msg.color("&e/factories"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give": {
                if (!sender.hasPermission("lootfactory.admin")) {
                    sender.sendMessage(Msg.prefix() + Msg.color("&cNo permission."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Msg.color("&cUsage: /factory give <player> <type> [level]"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Msg.color("&cPlayer not found."));
                    return true;
                }
                String type = args[2].toUpperCase();
                FactoryDef def = manager.getDef(type);
                if (def == null) {
                    sender.sendMessage(Msg.color("&cUnknown factory type. Use /factories"));
                    return true;
                }
                int level = 1;
                if (args.length >= 4) {
                    try {
                        level = Math.max(1, Integer.parseInt(args[3]));
                    } catch (Exception ignored) { }
                }
                target.getInventory().addItem(manager.createFactoryItem(target.getUniqueId(), type, level, 0d));
                sender.sendMessage(Msg.prefix() + Msg.color("&aGave &e" + def.display + " &7(Level " + level + ") to &e" + target.getName()));
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("lootfactory.admin")) {
                    sender.sendMessage(Msg.prefix() + Msg.color("&cNo permission."));
                    return true;
                }
                LootFactoryPlugin.get().reloadConfig();
                sender.sendMessage(Msg.prefix() + Msg.color("&aConfig reloaded."));
                return true;
            }
            case "listtypes": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                Player p = (Player) sender;
                FactoryTypesGUI.open(p, manager);
                return true;
            }
            default:
                sender.sendMessage(Msg.color("&cUnknown subcommand."));
                return true;
        }
    }
}

