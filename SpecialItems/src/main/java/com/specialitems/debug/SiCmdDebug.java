package com.specialitems.debug;

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

        if (label.equalsIgnoreCase("si_givecmd")) {
            // /si_givecmd <material> <cmd>
            if (a.length < 2) { p.sendMessage("Usage: /si_givecmd <iron_sword|diamond_pickaxe|netherite_hoe|...> <cmdInt>"); return true; }
            Material mat = Material.matchMaterial(a[0].toUpperCase());
            if (mat == null) { p.sendMessage("Unknown material: " + a[0]); return true; }
            int val;
            try { val = Integer.parseInt(a[1]); } catch (Exception e) { p.sendMessage("CMD must be integer"); return true; }
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta();
            if (m != null) {
                m.setCustomModelData(null);
                m.setCustomModelData(val);
                it.setItemMeta(m);
            }
            p.getInventory().addItem(it);
            p.sendMessage("Given " + mat + " with CMD=" + val);
            return true;
        }

        if (label.equalsIgnoreCase("si_cmdcheck")) {
            ItemStack it = p.getInventory().getItemInMainHand();
            if (it == null || it.getType().isAir()) { p.sendMessage("Hold an item"); return true; }
            ItemMeta m = it.getItemMeta();
            Integer cmdVal = (m != null ? m.getCustomModelData() : null);
            p.sendMessage("Mainhand: " + it.getType() + " CMD=" + (cmdVal == null ? "null" : cmdVal.toString()));
            return true;
        }

        return true;
    }
}
