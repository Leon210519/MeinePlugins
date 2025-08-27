package com.specialitems.debug;

import com.specialitems.util.ItemUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Command to normalize the held item's CustomModelData. */
public final class SiCmdFix implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Player only"); return true; }

        ItemStack it = p.getInventory().getItemInMainHand();
        if (it == null || it.getType().isAir()) { p.sendMessage("Hold an item."); return true; }

        boolean changed = ItemUtil.normalizeCustomModelData(it);
        if (changed) {
            p.getInventory().setItemInMainHand(it);
        }
        Integer val = ItemUtil.getCustomModelData(it);
        p.sendMessage("CustomModelData: " + (val == null ? "none" : val) + (changed ? " (fixed)" : ""));
        return true;
    }
}

