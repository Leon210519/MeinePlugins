package com.lootpets.command;

import com.lootpets.LootPetsPlugin;
import com.lootpets.gui.PetsGUI;
import com.lootpets.util.Colors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PetsCommand implements CommandExecutor {

    private final LootPetsPlugin plugin;
    private final PetsGUI gui;

    public PetsCommand(LootPetsPlugin plugin, PetsGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("only-players")));
            return true;
        }
        player.openInventory(gui.build(player));
        player.sendMessage(Colors.color(plugin.getLang().getString("opened-gui")));
        return true;
    }
}
