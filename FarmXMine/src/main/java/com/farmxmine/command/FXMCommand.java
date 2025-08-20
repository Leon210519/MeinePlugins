package com.farmxmine.command;

import com.farmxmine.FarmxMinePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FXMCommand implements CommandExecutor {
    private final FarmxMinePlugin plugin;

    public FXMCommand(FarmxMinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage("FarmxMine reloaded");
            return true;
        }
        sender.sendMessage("/fxm reload");
        return true;
    }
}
