package com.specialitems2.debug;

import com.specialitems2.cmd.CmdRegistry;
import com.specialitems2.util.CustomModelDataUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SiCmdDebug implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Player only"); return true; }

        if (label.equalsIgnoreCase("si2_givecmd")) {
            if (a.length < 2) { p.sendMessage("Usage: /si2_givecmd <material> <cmd>"); return true; }
            Material mat = Material.matchMaterial(a[0].toUpperCase());
            if (mat == null) { p.sendMessage("Unknown material: " + a[0]); return true; }
            int val;
            try { val = Integer.parseInt(a[1]); } catch (Exception e) { p.sendMessage("CMD must be integer"); return true; }
            Integer allowed = CmdRegistry.clamp(val);
            if (allowed == null) {
                p.sendMessage(ChatColor.RED + "CMD not whitelisted");
                return true;
            }
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                m.setCustomModelData(allowed);
                it.setItemMeta(m);
            }
            CustomModelDataUtil.normalize(it);
            p.getInventory().addItem(it);
            p.sendMessage("Given " + mat + " with CMD=" + allowed);
            return true;
        }

        if (label.equalsIgnoreCase("si2_cmdcheck")) {
            ItemStack it = p.getInventory().getItemInMainHand();
            if (it == null || it.getType().isAir()) { p.sendMessage("Hold an item"); return true; }
            CustomModelDataUtil.normalize(it);
            ItemMeta m = it.getItemMeta();
            Integer cmdVal = (m != null ? m.getCustomModelData() : null);
            p.sendMessage("CustomModelData (int): " + (cmdVal == null ? "none" : cmdVal));
            return true;
        }

        return true;
    }
}
