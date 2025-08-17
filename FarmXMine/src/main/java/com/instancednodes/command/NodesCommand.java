package com.instancednodes.command;

import com.instancednodes.InstancedNodesPlugin;
import com.instancednodes.util.Log;
import com.instancednodes.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NodesCommand implements CommandExecutor {

    private final InstancedNodesPlugin plugin;
    public NodesCommand(InstancedNodesPlugin plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("instancednodes.admin")) {
            sender.sendMessage(Msg.get("no_permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§7/nodes debug §8- toggle debug");
            sender.sendMessage("§7/nodes reload §8- reload config");
            return true;
        }
        if (args[0].equalsIgnoreCase("debug")) {
            boolean newVal = !plugin.getConfig().getBoolean("debug", false);
            plugin.getConfig().set("debug", newVal);
            plugin.saveConfig();
            Log.setDebug(newVal);
            sender.sendMessage(newVal ? Msg.get("debug_on") : Msg.get("debug_off"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(Msg.get("reloaded"));
            return true;
        }
        return false;
    }
}
