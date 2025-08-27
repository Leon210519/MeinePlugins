package com.lootpets.gui;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PetDefinition;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.service.SlotService;
import com.lootpets.service.RarityRegistry;
import com.lootpets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PetsGUI {

    private final LootPetsPlugin plugin;
    private final SlotService slotService;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private boolean overflowWarned = false;

    public PetsGUI(LootPetsPlugin plugin, SlotService slotService, PetService petService, PetRegistry petRegistry) {
        this.plugin = plugin;
        this.slotService = slotService;
        this.petService = petService;
        this.petRegistry = petRegistry;
    }

    public Inventory build(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        int rows = cfg.getInt("gui.rows");
        Inventory inv = Bukkit.createInventory(null, rows * 9, Colors.color(cfg.getString("gui.title")));

        List<Integer> active = cfg.getIntegerList("gui.active-slot-indices");
        int limit = slotService.getMaxSlots(player);
        ItemStack empty = item(Material.LIME_STAINED_GLASS_PANE, Colors.color(lang.getString("empty-slot-name")));
        ItemStack locked = item(Material.GRAY_STAINED_GLASS_PANE, Colors.color(lang.getString("locked-slot-name")));
        for (int i = 0; i < active.size(); i++) {
            int index = active.get(i);
            if (i < limit) {
                inv.setItem(index, empty);
            } else {
                inv.setItem(index, locked);
            }
        }

        int start = cfg.getInt("gui.owned-range.start");
        int end = cfg.getInt("gui.owned-range.end");
        int capacity = Math.max(0, end - start + 1);
        Map<String, OwnedPetState> owned = petService.getOwnedPets(player.getUniqueId());
        int placed = 0;
        for (Map.Entry<String, OwnedPetState> entry : owned.entrySet()) {
            if (placed >= capacity) {
                if (!overflowWarned) {
                    overflowWarned = true;
                    plugin.getLogger().warning("Some pets could not be displayed in GUI; increase owned-range.");
                }
                break;
            }
            String id = entry.getKey();
            OwnedPetState state = entry.getValue();
            PetDefinition def = petRegistry.byId(id);
            if (def == null) {
                continue;
            }
            ItemStack icon = new ItemStack(def.iconMaterial());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                if (def.iconCustomModelData() != null) {
                    meta.setCustomModelData(def.iconCustomModelData());
                }
                String name = def.displayName();
                if (state.rarity() != null) {
                    RarityRegistry.Rarity rr = plugin.getRarityRegistry().getRarities().get(state.rarity());
                    if (rr != null) {
                        name = rr.color() + Colors.color(name);
                    } else {
                        name = Colors.color(name);
                    }
                } else {
                    name = Colors.color(name);
                }
                meta.setDisplayName(name);
                List<String> lore = new ArrayList<>();
                lore.add(Colors.color("&7Rarity: " + (state.rarity() == null ? "?" : state.rarity())));
                lore.add(Colors.color("&7Stars: " + "★".repeat(state.stars())));
                lore.add(Colors.color("&7Evolve: " + state.evolveProgress() + "/5"));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(start + placed, icon);
            placed++;
        }

        return inv;
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

