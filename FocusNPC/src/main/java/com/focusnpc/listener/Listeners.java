package com.focusnpc.listener;

import com.focusnpc.FocusNPCPlugin;
import com.focusnpc.gui.GuiFactory;
import com.focusnpc.npc.FocusNpc;
import com.focusnpc.npc.NpcType;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class Listeners implements Listener {
    private final FocusNPCPlugin plugin;

    public Listeners(FocusNPCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityClick(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        FocusNpc npc = plugin.getNpcManager().getByEntityId(entity.getUniqueId());
        if (npc != null) {
            event.setCancelled(true);
            plugin.getGuiFactory().openGui(event.getPlayer(), npc.getType());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiFactory.GuiHolder holder)) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        Material mat = item.getType();
        Player player = (Player) event.getWhoClicked();
        NpcType type = holder.getType();
        Map<Material, Integer> options = type == NpcType.FARMER ? GuiFactory.FARMER_OPTIONS : GuiFactory.MINER_OPTIONS;
        int level = type == NpcType.FARMER ? plugin.getFarmingLevel(player) : plugin.getMiningLevel(player);
        int required = options.getOrDefault(mat, Integer.MAX_VALUE);
        if (level < required) {
            player.sendMessage("§cLocked — Requires " + (type == NpcType.FARMER ? "Farming" : "Mining") + " Level " + required);
            return;
        }
        if (type == NpcType.FARMER) {
            if (plugin.getPlayerData().getFarmFocus(player.getUniqueId()) == mat) {
                player.sendMessage("§7Already selected.");
            } else {
                plugin.getPlayerData().setFarmFocus(player.getUniqueId(), mat);
                player.sendMessage("§aFarm focus set to " + mat.name());
            }
        } else {
            if (plugin.getPlayerData().getMineFocus(player.getUniqueId()) == mat) {
                player.sendMessage("§7Already selected.");
            } else {
                plugin.getPlayerData().setMineFocus(player.getUniqueId(), mat);
                player.sendMessage("§aMine focus set to " + mat.name());
            }
        }
        player.closeInventory();
    }
}
