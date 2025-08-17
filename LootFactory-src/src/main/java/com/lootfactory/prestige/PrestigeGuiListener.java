package com.lootfactory.prestige;

import com.lootfactory.LootFactoryPlugin;
import com.lootfactory.gui.FactoryGUI;
import com.lootfactory.util.ClickDebounce;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PrestigeGuiListener (debounced)
 * - Cancels inventory clicks
 * - Debounces the prestige button to prevent double execution
 * - Reopens the Factory GUI one tick later
 */
public final class PrestigeGuiListener implements Listener {

    private final JavaPlugin plugin;

    public PrestigeGuiListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        ItemStack current = e.getCurrentItem();
        if (current == null || !current.hasItemMeta()) return;

        PersistentDataContainer pdc = current.getItemMeta().getPersistentDataContainer();
        NamespacedKey keyBtn = new NamespacedKey(plugin, PrestigeGuiUtil.PDC_KEY_BUTTON);
        if (!pdc.has(keyBtn, PersistentDataType.BYTE)) return;

        // Always cancel GUI clicks for safety (prevents double handling by other listeners)
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;

        // Debounce prestige action (300ms window)
        if (!ClickDebounce.shouldRun(p.getUniqueId(), "prestige-click", 300)) {
            return;
        }

        String type = pdc.get(new NamespacedKey(plugin, PrestigeGuiUtil.PDC_KEY_FACTORY_TYPE), PersistentDataType.STRING);
        if (type == null || type.isEmpty()) {
            p.sendMessage(Component.text("Unknown factory type.", NamedTextColor.RED));
            return;
        }

        // Build prestige command with location tags (UUID and/or name) if present
        String cmd = "prestige " + type;

        String worldUUID = pdc.get(new NamespacedKey(plugin, PrestigeGuiUtil.PDC_KEY_WORLD_UUID), PersistentDataType.STRING);
        String worldName = pdc.get(new NamespacedKey(plugin, PrestigeGuiUtil.PDC_KEY_WORLD_NAME), PersistentDataType.STRING);
        Integer x = pdc.get(new NamespacedKey(plugin, PrestigeGuiUtil.PDC_KEY_X), PersistentDataType.INTEGER);
        Integer y = pdc.get(new NamespacedKey(plugin, PrestigeGuiUtil.PDC_KEY_Y), PersistentDataType.INTEGER);
        Integer z = pdc.get(new NamespacedKey(plugin, PrestigeGuiUtil.PDC_KEY_Z), PersistentDataType.INTEGER);
        if (x != null && y != null && z != null) {
            if (worldUUID != null) cmd += " @loc:" + worldUUID + ":" + x + ":" + y + ":" + z;
            if (worldName != null) cmd += " @locname:" + worldName + ":" + x + ":" + y + ":" + z;
        }

        // Close and run the prestige command
        p.closeInventory();
        p.performCommand(cmd);

        // Re-open the factory GUI one tick later so the player immediately sees updated stars/title
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // Reconstruct location from PDC (UUID or name)
                World w = null;
                if (x != null && y != null && z != null) {
                    if (worldUUID != null) {
                        try { w = Bukkit.getWorld(java.util.UUID.fromString(worldUUID)); } catch (IllegalArgumentException ignored) {}
                    }
                    if (w == null && worldName != null) {
                        w = Bukkit.getWorld(worldName);
                    }
                    if (w != null) {
                        org.bukkit.Location loc = new org.bukkit.Location(w, x, y, z);
                        var fm = ((LootFactoryPlugin) plugin).factories();
                        var fi = fm.getAt(loc);
                        if (fi != null) {
                            FactoryGUI.open(p, fm, fi);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        });
    }
}
