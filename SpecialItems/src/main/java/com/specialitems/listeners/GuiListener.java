package com.specialitems.listeners;

import com.specialitems.bin.Bin;
import com.specialitems.gui.BinGUI;
import com.specialitems.gui.TemplateGUI;
import com.specialitems.util.Configs;
import com.specialitems.util.TemplateItems;
import com.specialitems.util.ItemUtil;
import com.specialitems.SpecialItemsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (title == null) return;

        // ==== Bin GUI ====
        if (title.equals(BinGUI.TITLE)) {
            e.setCancelled(true);
            if (!p.hasPermission("specialitems.admin")) {
                p.closeInventory();
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        Configs.msg.getString("no-permission","&cNo permission.")));
                return;
            }
            int raw = e.getRawSlot();
            if (raw < 0) return;
            if (raw >= e.getInventory().getSize()) return; // ignore bottom inv
            if (raw == e.getInventory().getSize() - 1) {
                p.closeInventory();
                return;
            }
            ItemStack removed = Bin.take(raw);
            if (removed != null) {
                p.getInventory().addItem(removed);
                p.sendMessage(ChatColor.GREEN + "Recovered item.");
                BinGUI.open(p);
            }
            return;
        }

        // ==== SpecialItems Templates GUI ====
        if (!title.startsWith(ChatColor.AQUA + "SpecialItems Templates")) return;
        e.setCancelled(true);
        if (!p.hasPermission("specialitems.admin")) {
            p.closeInventory();
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Configs.msg.getString("no-permission","&cNo permission.")));
            return;
        }

        int raw = e.getRawSlot();
        if (raw < 0) return;
        if (raw >= e.getInventory().getSize()) return; // nur Top-Inventar behandeln

        // Parse page from title like "... (1/3)"
        int page = 0;
        try {
            int l = title.lastIndexOf('(');
            int r = title.lastIndexOf(')');
            String inside = title.substring(l + 1, r);
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
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Template-ID aus dem Slot lesen
        String templateId = null;
        try {
            var meta = clicked.getItemMeta();
            var pdc = meta.getPersistentDataContainer();
            var key = new NamespacedKey(SpecialItemsPlugin.getInstance(), "si_template_id");
            templateId = pdc.get(key, PersistentDataType.STRING);
        } catch (Throwable ignored) {}

        if (templateId == null) {
            p.sendMessage(ChatColor.RED + "No template id bound to this item.");
            return;
        }

        // Template neu aufbauen
        var tsec = Configs.templates.getConfigurationSection("templates." + templateId);
        if (tsec == null) {
            p.sendMessage(ChatColor.RED + "Template not found: " + templateId);
            return;
        }
        TemplateItems.TemplateItem tmpl = TemplateItems.buildFrom(templateId, tsec);
        if (tmpl == null) {
            p.sendMessage(ChatColor.RED + "Failed to build template: " + templateId);
            return;
        }

        // CMD auf beiden Pfaden (Component + Legacy) erzwingen
        Integer cmd = tmpl.customModelData();
        if (cmd == null && tmpl.stack().hasItemMeta() && tmpl.stack().getItemMeta().hasCustomModelData()) {
            cmd = tmpl.stack().getItemMeta().getCustomModelData();
        }
        if (cmd == null) cmd = 0; // not expected, aber schützen

        ItemStack give = ItemUtil.forceSetCustomModelDataBoth(tmpl.stack(), cmd);

        // letzte Sicherheit: TemplateMeta anwenden (räumt Altlasten)
        TemplateItems.applyTemplateMeta(give);

        p.getInventory().addItem(give);
        p.sendMessage(ChatColor.GREEN + "Given: " + ChatColor.YELLOW + templateId);
    }
}
