package com.specialitems.listeners;

import com.specialitems.gui.TemplateGUI;
import com.specialitems.util.TemplateItems;
import com.specialitems.util.Configs;
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
        if (title == null || !title.startsWith(ChatColor.AQUA + "SpecialItems Templates")) return;
        e.setCancelled(true);
        if (!p.hasPermission("specialitems.admin")) {
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

        if (raw == 45) { // prev
            TemplateGUI.open(p, Math.max(0, page - 1));
            return;
        } else if (raw == 49) { // close
            p.closeInventory();
            return;
        } else if (raw == 53) { // next
            TemplateGUI.open(p, page + 1);
            return;
        }

        // Item click -> give
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        p.getInventory().addItem(clicked.clone());
        p.sendMessage(ChatColor.GREEN + "Given: " + (clicked.getItemMeta()!=null ? clicked.getItemMeta().getDisplayName() : clicked.getType().name()));
    }
}
