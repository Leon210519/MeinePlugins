package com.specialitems.debug;

import com.specialitems.util.ItemUtil;
import com.specialitems.util.TemplateItems;
import com.specialitems.util.TemplateItems.TemplateItem;
import com.specialitems.util.Log;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Debug commands: /cmdprobe and /cmdscan. */
public class CmdProbeScan implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only");
            return true;
        }
        if (label.equalsIgnoreCase("cmdprobe")) {
            if (args.length < 2) {
                p.sendMessage("Usage: /cmdprobe <material> <cmd>");
                return true;
            }
            Material mat;
            try {
                mat = Material.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                p.sendMessage("Unknown material: " + args[0]);
                return true;
            }
            int val;
            try {
                val = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                p.sendMessage("CMD must be int");
                return true;
            }
            ItemStack it = new ItemStack(mat);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(val);
                it.setItemMeta(meta);
            }
            p.getInventory().addItem(it);
            Log.info("/cmdprobe gave " + mat + " CMD=" + val + " meta=" + meta);
            return true;
        }
        if (label.equalsIgnoreCase("cmdscan")) {
            Inventory inv = p.getOpenInventory().getTopInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack it = inv.getItem(i);
                if (it == null || it.getType().isAir()) continue;
                ItemMeta meta = it.getItemMeta();
                Integer cmd = ItemUtil.getCustomModelData(it);
                TemplateItem tmpl = TemplateItems.match(it);
                if (tmpl != null) {
                    if (cmd == null) {
                        Log.warn("Slot " + i + " template " + tmpl.id() + " missing CMD");
                    } else if (tmpl.customModelData() != null && !tmpl.customModelData().equals(cmd)) {
                        Log.warn("Slot " + i + " template " + tmpl.id() + " CMD=" + cmd + " expected " + tmpl.customModelData());
                    }
                    if (tmpl.stack().getType() != it.getType()) {
                        Log.warn("Slot " + i + " template " + tmpl.id() + " material " + it.getType() + " expected " + tmpl.stack().getType());
                    }
                } else if (cmd == null) {
                    String name = (meta != null && meta.hasDisplayName()) ? ChatColor.stripColor(meta.getDisplayName()) : "(no name)";
                    Log.warn("Slot " + i + " " + it.getType() + " '" + name + "' missing CMD");
                }
            }
            p.sendMessage("Scan complete");
            return true;
        }
        return false;
    }
}
