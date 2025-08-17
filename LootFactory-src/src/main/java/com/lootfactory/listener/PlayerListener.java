package com.lootfactory.listener;
import com.lootfactory.factory.FactoryManager; import org.bukkit.event.EventHandler; import org.bukkit.event.Listener; import org.bukkit.event.player.PlayerQuitEvent;
public class PlayerListener implements Listener { private final FactoryManager manager; public PlayerListener(FactoryManager manager){ this.manager=manager; }
  @EventHandler public void onQuit(PlayerQuitEvent e){ manager.save(); } }
