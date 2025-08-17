package com.lootfactory.listener;

import com.lootfactory.gui.GuiAutoRefresher;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

/**
 * GuiRefreshListener
 * ------------------
 * Leichtgewichtiger Listener, der NUR die Hintergrund-Refresh-Tasks für offene GUIs verwaltet.
 * Kollisionen mit bestehenden GUI-Click-Listenern werden vermieden.
 */
public class GuiRefreshListener implements Listener {

    private final Plugin plugin;

    public GuiRefreshListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Stoppt den Auto-Refresh, sobald für dieses Inventar keine Viewer mehr übrig sind.
        if (e.getInventory().getViewers().isEmpty()) {
            GuiAutoRefresher.stop(e.getInventory());
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        if (e.getPlugin().equals(this.plugin)) {
            GuiAutoRefresher.stopAll();
        }
    }
}
