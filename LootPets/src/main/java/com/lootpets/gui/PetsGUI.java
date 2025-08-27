package com.lootpets.gui;

import com.lootpets.LootPetsPlugin;
import com.lootpets.service.SlotService;
import com.lootpets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PetsGUI {

    private final LootPetsPlugin plugin;
    private final SlotService slotService;

    public PetsGUI(LootPetsPlugin plugin, SlotService slotService) {
        this.plugin = plugin;
        this.slotService = slotService;
    }

    public Inventory build(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        int rows = cfg.getInt("gui.rows");
        Inventory inv = Bukkit.createInventory(null, rows * 9, Colors.color(cfg.getString("gui.title")));

        List<Integer> active = cfg.getIntegerList("gui.active-slot-indices");
        int limit = slotService.getMaxSlots(player);
        ItemStack empty = item(Material.LIME_STAINED_GLASS_PANE, Colors.color(lang.getString("empty-slot-name")));
        ItemStack locked = item(Material.GRAY_STAINED_GLASS_PANE, Colors.color(lang.getString("locked-slot-name")));
        for (int i = 0; i < active.size(); i++) {
            int index = active.get(i);
            if (i < limit) {
                inv.setItem(index, empty);
            } else {
                inv.setItem(index, locked);
            }
        }
        return inv;
    }

    private ItemStack item(Material mat, String name) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
