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

public class Fm2Command implements CommandExecutor, TabCompleter {
    private final FarmXMine2Plugin plugin;

    public Fm2Command(FarmXMine2Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("farmxmine2.admin")) {
                sender.sendMessage("No permission");
                return true;
            }
            plugin.reloadConfig();
            plugin.getConfigService().reload();
            sender.sendMessage(plugin.color(plugin.getMessages().getString("reloaded")));
            return true;
        }
        if (args[0].equalsIgnoreCase("level")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only");
                return true;
            }
            PlayerStats ps = plugin.getLevelService().getStats(player.getUniqueId());
            int mineLevel = ps.getLevel(TrackType.MINE);
            int mineXp = ps.getXp(TrackType.MINE);
            int mineNeeded = plugin.getLevelService().xpNeeded(mineLevel);
            int farmLevel = ps.getLevel(TrackType.FARM);
            int farmXp = ps.getXp(TrackType.FARM);
            int farmNeeded = plugin.getLevelService().xpNeeded(farmLevel);
            player.sendMessage(plugin.color("&aMine&7: Lv. " + mineLevel + " (" + mineXp + "/" + mineNeeded + " XP)"));
            player.sendMessage(plugin.color("&aFarm&7: Lv. " + farmLevel + " (" + farmXp + "/" + farmNeeded + " XP)"));
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
        }
        return list;
    }
}
