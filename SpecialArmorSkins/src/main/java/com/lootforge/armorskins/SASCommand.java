package com.lootforge.armorskins;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SASCommand implements CommandExecutor {

    private final SpecialArmorSkins plugin;

    public SASCommand(SpecialArmorSkins plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("specialarmorskins.admin")) {
                sender.sendMessage("No permission");
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage("SpecialArmorSkins reloaded.");
            return true;
        }
        sender.sendMessage("Usage: /sas reload");
        return true;
    }
}
