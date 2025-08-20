package com.focusnpc.gui;

import com.focusnpc.FocusNPCPlugin;
import com.focusnpc.npc.NpcType;
import com.focusnpc.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiFactory {
    public static final Map<Material, Integer> FARMER_OPTIONS = Map.of(
            Material.WHEAT, 0,
            Material.CARROT, 100,
            Material.POTATO, 200,
            Material.NETHER_WART, 300
    );

    public static final Map<Material, Integer> MINER_OPTIONS = Map.ofEntries(
            Map.entry(Material.COAL_ORE, 0),
            Map.entry(Material.IRON_ORE, 100),
            Map.entry(Material.COPPER_ORE, 150),
            Map.entry(Material.REDSTONE_ORE, 200),
            Map.entry(Material.LAPIS_ORE, 220),
            Map.entry(Material.GOLD_ORE, 250),
            Map.entry(Material.DIAMOND_ORE, 300),
            Map.entry(Material.EMERALD_ORE, 350),
            Map.entry(Material.NETHER_QUARTZ_ORE, 120),
            Map.entry(Material.ANCIENT_DEBRIS, 400)
    );

    private final FocusNPCPlugin plugin;

    public GuiFactory(FocusNPCPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGui(Player player, NpcType type) {
        player.openInventory(buildGui(player, type));
    }

    private Inventory buildGui(Player player, NpcType type) {
        Inventory inv = Bukkit.createInventory(new GuiHolder(type), 9, type == NpcType.FARMER ? "Farmer Focus" : "Miner Focus");
        Map<Material, Integer> options = type == NpcType.FARMER ? FARMER_OPTIONS : MINER_OPTIONS;
        Material current = type == NpcType.FARMER ? plugin.getPlayerData().getFarmFocus(player.getUniqueId()) : plugin.getPlayerData().getMineFocus(player.getUniqueId());
        int level = type == NpcType.FARMER ? plugin.getFarmingLevel(player) : plugin.getMiningLevel(player);
        for (Map.Entry<Material, Integer> e : options.entrySet()) {
            Material mat = e.getKey();
            int required = e.getValue();
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + TextUtil.format(mat));
            List<String> lore = new ArrayList<>();
            if (mat == current) {
                lore.add("§7Current Focus");
            } else if (level >= required) {
                lore.add("§aClick to select");
            } else {
                lore.add("§cLocked — Requires " + (type == NpcType.FARMER ? "Farming" : "Mining") + " Level " + required);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        return inv;
    }

    public static class GuiHolder implements InventoryHolder {
        private final NpcType type;
        public GuiHolder(NpcType type) { this.type = type; }
        public NpcType getType() { return type; }
        @Override
        public Inventory getInventory() { return null; }
    }
}
