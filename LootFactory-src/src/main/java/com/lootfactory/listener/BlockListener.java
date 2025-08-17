package com.lootfactory.listener;  

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.factory.FactoryDef;
import com.lootfactory.factory.FactoryInstance;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.util.Msg;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {
    private final FactoryManager manager;
    public BlockListener(FactoryManager manager) { this.manager = manager; }

    // --- PLACE: nur als Factory-Item, mit Slotlimit & Permission ---
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        ItemStack inHand = e.getItemInHand();

        // Kein Factory-Item? -> normaler Block, nichts tun
        if (!manager.isFactoryItem(inHand)) return;

        // Rechte & Limits prüfen
        if (!p.hasPermission("lootfactory.place")) {
            e.setCancelled(true);
            p.sendMessage(Msg.prefix() + Msg.color("&cYou don't have permission to place factories."));
            return;
        }
        if (!manager.canPlace(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendMessage(Msg.prefix() + Msg.color("&cYou have reached your factory placement limit."));
            return;
        }

        // Fabrik aus dem Item platzieren (liest type/level/xp/prestige aus dem PDC)
        manager.placeFactoryFromItem(p, e.getBlockPlaced().getLocation(), inHand);

        // Optional: hübsche Nachricht
        String type = manager.getItemType(inHand);
        int level = manager.getItemLevel(inHand);
        FactoryDef def = manager.getDef(type);
        p.sendMessage(Msg.prefix() + Msg.color("&aPlaced &e" + (def != null ? def.display : type) + " &7(Level " + level + ")"));

        manager.save();
    }


    // --- BREAK: standardmäßig blockiert; Admin/OP immer; Besitzer nur wenn erlaubt ---
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        FactoryInstance fi = manager.getAt(b.getLocation());
        if (fi == null) return;

        Player p = e.getPlayer();
        boolean adminBypass = LootFactoryPlugin.get().getConfig().getBoolean("security.admin_bypass", true);
        boolean allowOwnerBreak = LootFactoryPlugin.get().getConfig().getBoolean("pickup.allow_block_break_pickup", false);

        // Admin/OP darf abbauen (Moderation)
        if (adminBypass && p.hasPermission("lootfactory.admin")) {
            e.setDropItems(false); e.setExpToDrop(0);
            manager.removeAt(b.getLocation());
            // WICHTIG: Prestige beibehalten beim Drop!
            ItemStack item = manager.createFactoryItem(fi.typeId, fi.level, fi.xpSeconds, fi.prestige);
            b.getWorld().dropItemNaturally(b.getLocation(), item);
            manager.save();
            return;
        }

        // Besitzer darf nur, wenn explizit erlaubt
        if (p.getUniqueId().equals(fi.owner) && allowOwnerBreak) {
            e.setDropItems(false); e.setExpToDrop(0);
            manager.removeAt(b.getLocation());
            // WICHTIG: Prestige beibehalten beim Drop!
            ItemStack item = manager.createFactoryItem(fi.typeId, fi.level, fi.xpSeconds, fi.prestige);
            b.getWorld().dropItemNaturally(b.getLocation(), item);
            manager.save();
            return;
        }

        // Alle anderen (und Besitzer falls verboten): blockieren
        e.setCancelled(true);
        p.sendMessage(Msg.prefix() + Msg.color("&7Use the &ePickup &7button in the factory GUI."));
    }

    // --- INTERACT: GUI nur für Besitzer (oder Admin bei Bypass) ---
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block b = e.getClickedBlock();
        if (b == null || b.getType() == Material.AIR) return;

        FactoryInstance fi = manager.getAt(b.getLocation());
        if (fi == null) return;

        boolean ownerOnlyGui = LootFactoryPlugin.get().getConfig().getBoolean("security.owner_only_gui", true);
        boolean adminBypass  = LootFactoryPlugin.get().getConfig().getBoolean("security.admin_bypass", true);

        Player p = e.getPlayer();
        if (ownerOnlyGui) {
            boolean isOwner = p.getUniqueId().equals(fi.owner);
            boolean isAdmin = adminBypass && p.hasPermission("lootfactory.admin");
            if (!isOwner && !isAdmin) {
                e.setCancelled(true);
                p.sendMessage(Msg.prefix() + Msg.color("&cOnly the owner can open this factory."));
                return;
            }
        }

        e.setCancelled(true);
        com.lootfactory.gui.FactoryGUI.open(p, manager, fi);
    }
}
