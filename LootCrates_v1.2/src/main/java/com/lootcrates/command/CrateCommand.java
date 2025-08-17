package com.lootcrates.command;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.util.Color;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class CrateCommand implements CommandExecutor {
    private final LootCratesPlugin plugin;
    public CrateCommand(LootCratesPlugin plugin){ this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0){
            sender.sendMessage("§e/crate open <CRATE>  §7Open one if you have a key");
            sender.sendMessage("§e/crate givekey <player> <CRATE> <amount> §7(Admin)");
            sender.sendMessage("§e/crate list        §7List available crates");
            sender.sendMessage("§e/crate preview <CRATE> §7Preview rewards");
            sender.sendMessage("§e/crate bind <CRATE> §7(Admin) Bind crate to targeted block");
            sender.sendMessage("§e/crate unbind      §7(Admin) Unbind targeted block");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub){
            case "list" -> {
                String list = plugin.crates().list().stream().collect(Collectors.joining(", "));
                sender.sendMessage("§6Crates: §f" + list);
            }
            case "givekey" -> {
                if (!sender.hasPermission("lootcrates.admin")) { sender.sendMessage("§cNo permission."); return true; }
                if (args.length < 4){ sender.sendMessage("§cUsage: /crate givekey <player> <CRATE> <amount>"); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null){ sender.sendMessage("§cPlayer offline."); return true; }
                String crateId = args[2];
                int amount = Integer.parseInt(args[3]);
                plugin.crates().giveKey(t, crateId, amount);
            }
            case "open" -> {
                if (!(sender instanceof Player p)){ sender.sendMessage("§cPlayers only."); return true; }
                if (args.length < 2){ p.sendMessage("§cUsage: /crate open <CRATE>"); return true; }
                Crate c = plugin.crates().get(args[1]);
                if (c == null){ p.sendMessage("§cUnknown crate."); return true; }
                GUI.tryOpenWithKey(plugin, p, c);
            }
            case "preview" -> {
                if (!(sender instanceof Player p)){ sender.sendMessage("§cPlayers only."); return true; }
                if (args.length < 2){ p.sendMessage("§cUsage: /crate preview <CRATE>"); return true; }
                Crate c = plugin.crates().get(args[1]);
                if (c == null){ p.sendMessage("§cUnknown crate."); return true; }
                GUI.preview(p, c);
            }
            case "bind" -> {
                if (!(sender instanceof Player p)){ sender.sendMessage("§cPlayers only."); return true; }
                if (!p.hasPermission("lootcrates.admin")){ p.sendMessage("§cNo permission."); return true; }
                if (args.length < 2){ p.sendMessage("§cUsage: /crate bind <CRATE>"); return true; }
                Crate c = plugin.crates().get(args[1]);
                if (c == null){ p.sendMessage("§cUnknown crate."); return true; }
                Block b = p.getTargetBlockExact(5);
                if (b == null){ p.sendMessage("§cLook at a block within 5 blocks."); return true; }
                plugin.blocks().bind(b.getLocation(), c.id);
                p.sendMessage("§aBound " + c.display + " to block at §e" + b.getLocation().getBlockX()+","+b.getLocation().getBlockY()+","+b.getLocation().getBlockZ());
            }
            case "unbind" -> {
                if (!(sender instanceof Player p)){ sender.sendMessage("§cPlayers only."); return true; }
                if (!p.hasPermission("lootcrates.admin")){ p.sendMessage("§cNo permission."); return true; }
                Block b = p.getTargetBlockExact(5);
                if (b == null){ p.sendMessage("§cLook at a block within 5 blocks."); return true; }
                plugin.blocks().unbind(b.getLocation());
                p.sendMessage("§aUnbound crate from that block.");
            }
            default -> sender.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }
}
