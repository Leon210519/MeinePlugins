package com.lootfactory.command;

import com.lootfactory.factory.FactoryManager;
import com.lootfactory.gui.ShopGUI;
import com.lootfactory.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FactoryShopCommand implements CommandExecutor {

    private final FactoryManager manager;

    public FactoryShopCommand(FactoryManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player p = (Player) sender;

        if (!p.hasPermission("lootfactory.shop")) {
            p.sendMessage(Msg.prefix() + Msg.color("&cNo permission."));
            return true;
        }

        ShopGUI.open(p, manager);
        return true;
    }
}
