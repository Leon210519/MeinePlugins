package com.focusnpc.listener;

import com.focusnpc.FocusNPCPlugin;
import com.focusnpc.gui.GuiFactory;
import com.focusnpc.npc.FocusNpc;
import com.focusnpc.npc.NpcType;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class Listeners implements Listener {

    private final FocusNPCPlugin plugin;

    public Listeners(FocusNPCPlugin plugin) {
        this.plugin = plugin;
    }

    // Citizens / ArmorStand etc. feuern i. d. R. dieses Event
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityRightClickAt(PlayerInteractAtEntityEvent event) {
        handleEntityInteract(event.getPlayer(), event.getRightClicked());
    }

    // Fallback für andere Entities / Implementationen
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityRightClick(PlayerInteractEntityEvent event) {
        handleEntityInteract(event.getPlayer(), event.getRightClicked());
    }

    private void handleEntityInteract(Player player, Entity entity) {
        FocusNpc npc = plugin.getNpcManager().getByEntityId(entity.getUniqueId());
        if (npc == null) return;

        // Interaktion vollständig übernehmen (kein Default-Handling)
        player.closeInventory();
        plugin.getGuiFactory().openGui(player, npc.getType());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiFactory.GuiHolder holder)) return;

        event.setCancelled(true); // GUI ist read-only
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        Material mat = item.getType();
        NpcType type = holder.getType();

        final Map<Material, Integer> options =
                (type == NpcType.FARMER) ? GuiFactory.FARMER_OPTIONS : GuiFactory.MINER_OPTIONS;

        final int level = (type == NpcType.FARMER)
                ? plugin.getFarmingLevel(player)
                : plugin.getMiningLevel(player);

        final int required = options.getOrDefault(mat, Integer.MAX_VALUE);
        if (level < required) {
            player.sendMessage("§cLocked — Requires " +
                    (type == NpcType.FARMER ? "Farming" : "Mining") +
                    " Level " + required);
            return;
        }

        if (type == NpcType.FARMER) {
            if (plugin.getPlayerData().getFarmFocus(player.getUniqueId()) == mat) {
                player.sendMessage("§7Already selected.");
            } else {
                plugin.getPlayerData().setFarmFocus(player.getUniqueId(), mat);
                player.sendMessage("§aFarm focus set to " + mat.name());
            }
        } else { // MINER
            if (plugin.getPlayerData().getMineFocus(player.getUniqueId()) == mat) {
                player.sendMessage("§7Already selected.");
            } else {
                plugin.getPlayerData().setMineFocus(player.getUniqueId(), mat);
                player.sendMessage("§aMine focus set to " + mat.name());
            }
        }

        // Optional: hier ggf. deine Transform-Job-Queue starten (Area-Replacement)
        // plugin.getTransformManager().queueTransform(player, type, mat);

        player.closeInventory();
    }
}
