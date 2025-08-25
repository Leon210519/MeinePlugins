package com.specialitems2.leveling;

import com.specialitems2.SpecialItems2Plugin;
import com.specialitems2.util.GuiItemUtil;
import org.bukkit.Bukkit;
import com.specialitems2.util.CustomModelDataUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class LevelOverviewGUI implements InventoryHolder, Listener {

    private final SpecialItems2Plugin plugin;
    private final Inventory inv;
    private final Player target;

    private LevelOverviewGUI(SpecialItems2Plugin plugin, Player target) {
        this.plugin = plugin;
        this.target = target;
        this.inv = Bukkit.createInventory(this, 6 * 9, ChatColor.DARK_AQUA + "Your Special Items");
    }

    public static void open(Player p) {
        SpecialItems2Plugin pl = SpecialItems2Plugin.getInstance();
        LevelOverviewGUI gui = new LevelOverviewGUI(pl, p);
        Bukkit.getPluginManager().registerEvents(gui, pl);
        gui.populate();
        p.openInventory(gui.inv);
    }

    private void addIfSpecial(ItemStack it) {
        ItemStack display = GuiItemUtil.forDisplay(plugin, it);
        if (display == null) return;
        CustomModelDataUtil.normalize(display);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, display);
                return;
            }
        }
    }

    private void populate() {
        inv.clear();
        PlayerInventory pinv = target.getInventory();
        for (ItemStack it : pinv.getContents()) addIfSpecial(it);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this) return;
        e.setCancelled(true);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}