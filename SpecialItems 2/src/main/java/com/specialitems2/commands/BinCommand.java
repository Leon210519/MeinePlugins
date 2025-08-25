package com.specialitems2.commands;

import com.specialitems2.gui.BinGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BinCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!p.hasPermission("specialitems2.admin")) {
            p.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        BinGUI.open(p);
        return true;
    }
}
