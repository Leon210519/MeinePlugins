package com.specialitems2.debug;

import com.specialitems2.util.CustomModelDataUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

public final class SiCmdFix implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Player only"); return true; }
        ItemStack it = p.getInventory().getItemInMainHand();
        if (it == null || it.getType().isAir()) { p.sendMessage("Hold an item."); return true; }

        Integer before = it.hasItemMeta() && it.getItemMeta().hasCustomModelData()
                ? it.getItemMeta().getCustomModelData() : null;
        CustomModelDataUtil.normalize(it);
        ItemMeta m = it.getItemMeta();
        Integer after = (m != null && m.hasCustomModelData()) ? m.getCustomModelData() : null;
        p.getInventory().setItemInMainHand(it);
        if (!Objects.equals(before, after)) {
            p.sendMessage("CustomModelData normalized to: " + (after == null ? "none" : after));
        } else {
            p.sendMessage("CustomModelData unchanged: " + (after == null ? "none" : after));
        }
        return true;
    }
}
