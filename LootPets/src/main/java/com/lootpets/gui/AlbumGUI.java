package com.lootpets.gui;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.PetDefinition;
import com.lootpets.model.OwnedPetState;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlbumGUI implements Listener {

    private final LootPetsPlugin plugin;
    private final PetRegistry petRegistry;
    private final PetService petService;
    private final Map<UUID, Map<Integer, String>> slots = new HashMap<>();
    private boolean overflowWarned = false;

    public AlbumGUI(LootPetsPlugin plugin, PetRegistry petRegistry, PetService petService) {
        this.plugin = plugin;
        this.petRegistry = petRegistry;
        this.petService = petService;
    }

    public void open(Player player) {
        player.openInventory(build(player));
    }

    public Inventory build(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        int rows = cfg.getInt("gui.rows");
        Inventory inv = Bukkit.createInventory(null, rows * 9, Colors.color(lang.getString("album-button")));
        Map<String, PetDefinition> all = new HashMap<>();
        petRegistry.all().forEach(def -> all.put(def.id(), def));
        Map<Integer, String> map = new HashMap<>();
        int start = cfg.getInt("gui.owned-range.start");
        int end = cfg.getInt("gui.owned-range.end");
        int capacity = Math.max(0, end - start + 1);
        int placed = 0;
        Map<String, OwnedPetState> owned = petService.getOwnedPets(player.getUniqueId());
        String frame = petService.getAlbumFrameStyle(player.getUniqueId());
        for (Map.Entry<String, PetDefinition> entry : all.entrySet()) {
            if (placed >= capacity) {
                if (!overflowWarned) {
                    overflowWarned = true;
                    plugin.getLogger().warning("Some pets could not be displayed in Album; increase owned-range.");
                }
                break;
            }
            String id = entry.getKey();
            PetDefinition def = entry.getValue();
            ItemStack icon;
            OwnedPetState st = owned.get(id);
            if (st != null) {
                icon = new ItemStack(def.iconMaterial());
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    String name = def.displayName();
                    if (st.suffix() != null && !st.suffix().isEmpty()) {
                        name = name + " " + st.suffix();
                    }
                    if (frame != null) {
                        name = "[Frame: " + frame + "] " + name;
                    }
                    meta.setDisplayName(Colors.color(name));
                    if (def.iconCustomModelData() != null) {
                        meta.setCustomModelData(def.iconCustomModelData());
                    }
                    icon.setItemMeta(meta);
                }
            } else {
                if (cfg.getBoolean("album.show_unknown_greyed", true)) {
                    icon = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta meta = icon.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(Colors.color("&7" + def.displayName()));
                        icon.setItemMeta(meta);
                    }
                } else {
                    continue;
                }
            }
            int slot = start + placed;
            inv.setItem(slot, icon);
            map.put(slot, id);
            placed++;
        }
        slots.put(player.getUniqueId(), map);
        inv.setItem(0, item(Material.BARRIER, Colors.color(lang.getString("back-button"))));
        if (placed == 0) {
            inv.setItem(start, item(Material.BARRIER, Colors.color(lang.getString("no-pets"))));
        }
        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = Colors.color(plugin.getLang().getString("album-button"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getRawSlot() == 0) {
            player.openInventory(plugin.getPetsGUI().build(player));
        }
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
