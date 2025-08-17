package com.loothelp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class LootHelpPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("loothelp")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }
        Player p = (Player) sender;
        boolean isAdmin = p.hasPermission("loothelp.admin") || p.isOp();

        List<String> playerCmds = getConfig().getStringList("commands.player");
        List<String> adminCmds = getConfig().getStringList("commands.admin");

        p.sendMessage("§aVerfügbare Befehle:");
        p.sendMessage("§eSpieler:");
        for (String line : playerCmds) {
            p.sendMessage(" §7- " + line);
        }
        if (isAdmin) {
            p.sendMessage("§cAdmin:");
            for (String line : adminCmds) {
                p.sendMessage(" §7- " + line);
            }
        }
        return true;
    }
}
