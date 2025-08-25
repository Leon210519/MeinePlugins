package com.lootcrates.listener;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.command.GUI;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class CrateBlockListener implements Listener {
    private final LootCratesPlugin plugin;
    public CrateBlockListener(LootCratesPlugin plugin){ this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_BLOCK && a != Action.LEFT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        String crateId = plugin.blocks().get(b.getLocation());
        if (crateId == null) return;

        Crate c = plugin.crates().get(crateId);
        if (c == null) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        if (a == Action.RIGHT_CLICK_BLOCK){
            GUI.preview(p, c);
        } else if (a == Action.LEFT_CLICK_BLOCK){
            GUI.tryOpenWithKey(plugin, p, c, false);
        }
    }
}
