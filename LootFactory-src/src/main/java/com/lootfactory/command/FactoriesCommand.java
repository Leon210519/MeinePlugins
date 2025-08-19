package com.lootfactory.command;

import com.lootfactory.factory.FactoryDef;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.gui.FactoryTypesGUI;
import com.lootfactory.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FactoriesCommand implements CommandExecutor {
    private final FactoryManager manager;

    public FactoriesCommand(FactoryManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission("lootfactory.list")) {
                p.sendMessage(Msg.prefix() + Msg.color("&cNo permission."));
                return true;
            }
            FactoryTypesGUI.open(p, manager);
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append("Available factory types: ");
        boolean first = true;
        for (FactoryDef def : manager.getAllDefs()) {
            if (!first) sb.append(ChatColor.GRAY).append(", ");
            first = false;
            sb.append(ChatColor.YELLOW).append(def.id)
              .append(ChatColor.GRAY).append(" (")
              .append(def.display).append(")");
        }
        sender.sendMessage(sb.toString());
        return true;
    }
}
