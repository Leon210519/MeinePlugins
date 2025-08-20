package com.focusnpc.command;

import com.focusnpc.FocusNPCPlugin;
import com.focusnpc.npc.FocusNpc;
import com.focusnpc.npc.NpcManager;
import com.focusnpc.npc.NpcType;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FocusNpcCommand implements CommandExecutor, TabCompleter {
    private final FocusNPCPlugin plugin;

    public FocusNpcCommand(FocusNPCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("focusnpc.admin")) {
            player.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§eUse /focusnpc <spawn|remove|list|reload>");
            return true;
        }
        NpcManager manager = plugin.getNpcManager();
        switch (args[0].toLowerCase()) {
            case "spawn":
                if (args.length < 2) {
                    player.sendMessage("§cSpecify type: farmer or miner");
                    return true;
                }
                NpcType type;
                try { type = NpcType.valueOf(args[1].toUpperCase()); }
                catch (IllegalArgumentException ex) {
                    player.sendMessage("§cUnknown type.");
                    return true;
                }
                String skin = plugin.getConfig().getString("npcs." + args[1].toLowerCase() + ".skin", "");
                Location loc = player.getLocation();
                manager.spawnNpc(type, loc, skin);
                manager.save();
                player.sendMessage("§aSpawned " + type.name().toLowerCase() + " NPC.");
                break;
            case "remove":
                if (manager.removeNearest(player.getLocation())) {
                    player.sendMessage("§aRemoved nearest NPC.");
                } else {
                    player.sendMessage("§cNo NPC found nearby.");
                }
                break;
            case "list":
                List<FocusNpc> list = manager.getNpcs();
                if (list.isEmpty()) {
                    player.sendMessage("§7No NPCs.");
                } else {
                    int i = 1;
                    for (FocusNpc npc : list) {
                        Location l = npc.getLocation();
                        player.sendMessage(i++ + ". " + npc.getType() + " - " + l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ());
                    }
                }
                break;
            case "reload":
                plugin.reload();
                player.sendMessage("§aFocusNPC reloaded.");
                break;
            default:
                player.sendMessage("§eUse /focusnpc <spawn|remove|list|reload>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spawn", "remove", "list", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return Arrays.asList("farmer", "miner");
        }
        return new ArrayList<>();
    }
}
