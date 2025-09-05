package com.lootforge.armorskins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import io.papermc.paper.event.player.PlayerArmorChangeEvent;

public class SASListener implements Listener {

    private final SpecialArmorSkins plugin;

    public SASListener(SpecialArmorSkins plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getDisplayManager().refresh(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getDisplayManager().refresh(event.getPlayer()));
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        plugin.getDisplayManager().refresh(event.getPlayer());
    }

    @EventHandler
    public void onArmor(PlayerArmorChangeEvent event) {
        plugin.getDisplayManager().refresh(event.getPlayer());
    }

    @EventHandler
    public void onInv(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getDisplayManager().refresh(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDisplayManager().removePlayer(event.getPlayer());
    }
}
