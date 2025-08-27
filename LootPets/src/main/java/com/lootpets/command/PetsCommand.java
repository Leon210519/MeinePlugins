package com.lootpets.command;

import com.lootpets.LootPetsPlugin;
import com.lootpets.gui.PetsGUI;
import com.lootpets.util.Colors;
import java.util.Arrays;
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
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "shards" -> {
                    int amt = plugin.getPetService().getShards(player.getUniqueId());
                    player.sendMessage(Colors.color(plugin.getLang().getString("shard-balance").replace("%amount%", String.valueOf(amt))));
                    return true;
                }
                case "suffix" -> {
                    if (args.length < 3) {
                        player.sendMessage(Colors.color(plugin.getLang().getString("rename-invalid-length")));
                        return true;
                    }
                    String petId = args[1];
                    String suffix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    if (!plugin.getPetService().getOwnedPets(player.getUniqueId()).containsKey(petId)) {
                        player.sendMessage(Colors.color(plugin.getLang().getString("unknown-pet")));
                        return true;
                    }
                    if (suffix.equalsIgnoreCase("clear")) {
                        plugin.getPetService().setSuffix(player.getUniqueId(), petId, null);
                        player.sendMessage(Colors.color(plugin.getLang().getString("rename-cleared")));
                        return true;
                    }
                    if (!plugin.getConfig().getBoolean("shards.cosmetics.allow_custom_suffix", true)) {
                        player.sendMessage(Colors.color(plugin.getLang().getString("rename-invalid-colors")));
                        return true;
                    }
                    if (plugin.getConfig().getBoolean("shards.cosmetics.disallowed_colors_in_suffix", true)) {
                        suffix = suffix.replaceAll("(?i)&[0-9A-FK-OR]", "");
                    }
                    int max = plugin.getConfig().getInt("shards.cosmetics.max_suffix_length", 16);
                    if (suffix.length() > max) {
                        player.sendMessage(Colors.color(plugin.getLang().getString("rename-invalid-length").replace("%max%", String.valueOf(max))));
                        return true;
                    }
                    if (!plugin.getPetService().consumeRenameToken(player.getUniqueId())) {
                        player.sendMessage(Colors.color(plugin.getLang().getString("no-rename-token")));
                        return true;
                    }
                    plugin.getPetService().setSuffix(player.getUniqueId(), petId, suffix);
                    player.sendMessage(Colors.color(plugin.getLang().getString("rename-success")));
                    return true;
                }
            }
        }
        player.openInventory(gui.build(player));
        player.sendMessage(Colors.color(plugin.getLang().getString("opened-gui")));
        return true;
    }
}
