package com.specialitems2.listeners;

import com.specialitems2.bin.Bin;
import com.specialitems2.gui.BinGUI;
import com.specialitems2.gui.TemplateGUI;
import com.specialitems2.util.Configs;
import com.specialitems2.util.CustomModelDataUtil;
import com.specialitems2.util.TemplateItems;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (title == null) return;

        if (title.equals(BinGUI.TITLE)) {
            e.setCancelled(true);
            if (!p.hasPermission("specialitems2.admin")) {
                p.closeInventory();
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', Configs.msg.getString("no-permission","&cNo permission.")));
                return;
            }
            int raw = e.getRawSlot();
            if (raw < 0) return;
            if (raw == e.getInventory().getSize() - 1) {
                p.closeInventory();
                return;
            }
            ItemStack removed = Bin.take(raw);
            if (removed != null) {
                CustomModelDataUtil.normalize(removed);
                p.getInventory().addItem(removed);
                p.sendMessage(ChatColor.GREEN + "Recovered item.");
                BinGUI.open(p);
            }
            return;
        }

        if (!title.startsWith(TemplateGUI.TITLE)) return;
        e.setCancelled(true);
        if (!p.hasPermission("specialitems2.admin")) {
            p.closeInventory();
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', Configs.msg.getString("no-permission","&cNo permission.")));
            return;
        }
        int raw = e.getRawSlot();
        if (raw < 0) return;

        // Parse page from title like "... (1/3)"
        int page = 0;
        try {
            int l = title.lastIndexOf('(');
            int r = title.lastIndexOf(')');
            String inside = title.substring(l+1, r);
            String left = inside.split("/")[0];
            page = Integer.parseInt(left) - 1;
        } catch (Exception ignored) {}

        if (raw == 17) { // prev
            TemplateGUI.open(p, Math.max(0, page - 1));
            return;
        } else if (raw == 26) { // close
            p.closeInventory();
            return;
        } else if (raw == 8) { // next
            TemplateGUI.open(p, page + 1);
            return;
        } else if (raw == 35 || raw == 44 || raw == 53) {
            return; // decorative navigation column
        }

        // Item click -> give
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        ItemStack give = clicked.clone();
        // Reapply template data so custom model data is written as an integer
        TemplateItems.applyTemplateMeta(give);
        CustomModelDataUtil.normalize(give);
        p.getInventory().addItem(give);
        p.sendMessage(ChatColor.GREEN + "Given: " + (clicked.getItemMeta()!=null ? clicked.getItemMeta().getDisplayName() : clicked.getType().name()));
    }
}
