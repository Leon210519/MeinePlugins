package com.loothelp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
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
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player p = (Player) sender;
        boolean isAdmin = p.hasPermission("loothelp.admin") || p.isOp();

        p.sendMessage("§aAvailable Commands:");

        // Player section: supports grouped sections or a simple list
        ConfigurationSection playerSection = getConfig().getConfigurationSection("commands.player");
        if (playerSection != null && !playerSection.getKeys(false).isEmpty()) {
            p.sendMessage("§ePlayer Commands:");
            List<String> plugins = new ArrayList<>(playerSection.getKeys(false));
            Collections.sort(plugins);
            for (String pluginName : plugins) {
                p.sendMessage(" §6" + pluginName + ":");
                for (String line : playerSection.getStringList(pluginName)) {
                    p.sendMessage("   §7- " + line);
                }
            }
        } else {
            List<String> playerCmds = getConfig().getStringList("commands.player");
            if (!playerCmds.isEmpty()) {
                p.sendMessage("§ePlayer Commands:");
                for (String line : playerCmds) {
                    p.sendMessage(" §7- " + line);
                }
            }
        }

        // Admin section (only if admin): supports both variants as well
        if (isAdmin) {
            ConfigurationSection adminSection = getConfig().getConfigurationSection("commands.admin");
            if (adminSection != null && !adminSection.getKeys(false).isEmpty()) {
                p.sendMessage("§cAdmin Commands:");
                List<String> plugins = new ArrayList<>(adminSection.getKeys(false));
                Collections.sort(plugins);
                for (String pluginName : plugins) {
                    p.sendMessage(" §6" + pluginName + ":");
                    for (String line : adminSection.getStringList(pluginName)) {
                        p.sendMessage("   §7- " + line);
                    }
                }
            } else {
                List<String> adminCmds = getConfig().getStringList("commands.admin");
                if (!adminCmds.isEmpty()) {
                    p.sendMessage("§cAdmin Commands:");
                    for (String line : adminCmds) {
                        p.sendMessage(" §7- " + line);
                    }
                }
            }
        }

        return true;
    }
}
