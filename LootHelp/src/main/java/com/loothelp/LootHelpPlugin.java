package com.loothelp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;

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

        p.sendMessage("§aVerfügbare Befehle:");

        ConfigurationSection playerSection = getConfig().getConfigurationSection("commands.player");
        if (playerSection != null) {
            p.sendMessage("§eSpieler-Befehle:");
            java.util.List<String> plugins = new ArrayList<>(playerSection.getKeys(false));
            Collections.sort(plugins);
            for (String pluginName : plugins) {
                p.sendMessage(" §6" + pluginName + ":");
                for (String line : playerSection.getStringList(pluginName)) {
                    p.sendMessage("   §7- " + line);
                }
            }
        }

        if (isAdmin) {
            ConfigurationSection adminSection = getConfig().getConfigurationSection("commands.admin");
            if (adminSection != null) {
                p.sendMessage("§cAdmin-Befehle:");
                java.util.List<String> plugins = new ArrayList<>(adminSection.getKeys(false));
                Collections.sort(plugins);
                for (String pluginName : plugins) {
                    p.sendMessage(" §6" + pluginName + ":");
                    for (String line : adminSection.getStringList(pluginName)) {
                        p.sendMessage("   §7- " + line);
                    }
                }
            }
        }
        return true;
    }
}
