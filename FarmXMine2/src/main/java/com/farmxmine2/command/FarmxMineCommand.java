package com.farmxmine2.command;

import com.farmxmine2.FarmXMine2Plugin;
import com.farmxmine2.model.PlayerStats;
import com.farmxmine2.model.TrackType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FarmxMineCommand implements CommandExecutor, TabCompleter {
    private final FarmXMine2Plugin plugin;

    public FarmxMineCommand(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("farmxmine.admin")) {
                sender.sendMessage("No permission");
                return true;
            }
            plugin.reloadConfig();
            plugin.getRegionService().reload();
            sender.sendMessage(plugin.color(plugin.getMessages().getString("reloaded")));
            return true;
        }
        if (args[0].equalsIgnoreCase("level")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Players only");
                return true;
            }
            Player p = (Player) sender;
            TrackType type = TrackType.MINE;
            if (args.length > 1 && args[1].equalsIgnoreCase("farm")) type = TrackType.FARM;
            PlayerStats ps = plugin.getLevelService().getStats(p.getUniqueId());
            int level = ps.getLevel(type);
            int xp = ps.getXp(type);
            int needed = plugin.getLevelService().xpNeeded(level);
            sender.sendMessage(type.name().toLowerCase() + " level " + level + " (" + xp + "/" + needed + ")");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) list.add("reload");
            if ("level".startsWith(args[0].toLowerCase())) list.add("level");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("level")) {
            if ("mine".startsWith(args[1].toLowerCase())) list.add("mine");
            if ("farm".startsWith(args[1].toLowerCase())) list.add("farm");
        }
        return list;
    }
}
