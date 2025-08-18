package com.instancednodes.command;

import com.instancednodes.InstancedNodesPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrestigeCommand implements CommandExecutor {

    private final InstancedNodesPlugin plugin;
    public PrestigeCommand(InstancedNodesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can prestige.");
            return true;
        }
        if (plugin.level().prestige(p)) {
            // message handled in prestige method
        } else {
            p.sendMessage("§cYou need a higher level to prestige.");
        }
        return true;
    }
}
