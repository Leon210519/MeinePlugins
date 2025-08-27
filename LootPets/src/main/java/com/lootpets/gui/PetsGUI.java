package com.lootpets.gui;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PetDefinition;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.service.RarityRegistry;
import com.lootpets.service.SlotService;
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

import java.util.*;

public class PetsGUI implements Listener {

    private final LootPetsPlugin plugin;
    private final SlotService slotService;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private final Map<UUID, Map<Integer, String>> ownedSlots = new HashMap<>();
    private final Map<UUID, Long> lastChange = new HashMap<>();
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

        List<String> activeIds = petService.getActivePetIds(player.getUniqueId(), Math.min(limit, active.size()));
        Map<String, OwnedPetState> owned = petService.getOwnedPets(player.getUniqueId());
        for (int i = 0; i < active.size(); i++) {
            int index = active.get(i);
            if (i < limit) {
                if (i < activeIds.size()) {
                    String id = activeIds.get(i);
                    OwnedPetState state = owned.get(id);
                    PetDefinition def = petRegistry.byId(id);
                    if (def == null) {
                        plugin.getLogger().warning("Missing pet definition for id " + id);
                    }
                    inv.setItem(index, petIcon(def, state, true));
                } else {
                    inv.setItem(index, empty);
                }
            } else {
                inv.setItem(index, locked);
            }
        }

        int start = cfg.getInt("gui.owned-range.start");
        int end = cfg.getInt("gui.owned-range.end");
        int capacity = Math.max(0, end - start + 1);
        int placed = 0;
        Map<Integer, String> map = new HashMap<>();
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
                plugin.getLogger().warning("Missing pet definition for id " + id);
            }
            ItemStack icon = petIcon(def, state, false);
            int slot = start + placed;
            inv.setItem(slot, icon);
            map.put(slot, id);
            placed++;
        }
        ownedSlots.put(player.getUniqueId(), map);
        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = Colors.color(plugin.getConfig().getString("gui.title"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }
        int slot = event.getRawSlot();
        FileConfiguration cfg = plugin.getConfig();
        List<Integer> active = cfg.getIntegerList("gui.active-slot-indices");
        int limit = slotService.getMaxSlots(player);
        long now = System.currentTimeMillis();
        long cooldown = cfg.getLong("equip-cooldown-ms", 300);
        Long last = lastChange.get(player.getUniqueId());
        if (last != null && now - last < cooldown) {
            return;
        }
        int activeIndex = active.indexOf(slot);
        if (activeIndex >= 0) {
            List<String> activeIds = petService.getActivePetIds(player.getUniqueId(), Math.min(limit, active.size()));
            if (activeIndex < activeIds.size()) {
                String petId = activeIds.get(activeIndex);
                if (petService.unequipPet(player.getUniqueId(), petId)) {
                    lastChange.put(player.getUniqueId(), now);
                    player.openInventory(build(player));
                }
            }
            return;
        }
        Map<Integer, String> map = ownedSlots.get(player.getUniqueId());
        if (map != null) {
            String petId = map.get(slot);
            if (petId != null) {
                List<String> activeIds = petService.getActivePetIds(player.getUniqueId(), Math.min(limit, active.size()));
                if (!activeIds.contains(petId)) {
                    if (petService.equipPet(player.getUniqueId(), petId, Math.min(limit, active.size()))) {
                        lastChange.put(player.getUniqueId(), now);
                        player.openInventory(build(player));
                    }
                }
            }
        }
    }

    private ItemStack petIcon(PetDefinition def, OwnedPetState state, boolean active) {
        Material material = def == null ? Material.BARRIER : def.iconMaterial();
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            if (def != null && def.iconCustomModelData() != null) {
                meta.setCustomModelData(def.iconCustomModelData());
            }
            String name = def == null ? "Unknown Pet" : def.displayName();
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
            if (active) {
                lore.add(Colors.color("&7Active"));
            }
            int cap = plugin.getConfig().getInt("leveling_runtime.level_cap_base", 100) +
                    plugin.getConfig().getInt("leveling_runtime.level_cap_extra_per_star", 50) * state.stars();
            lore.add(Colors.color("&7Level " + state.level() + "/" + cap));
            lore.add(Colors.color("&7Evolve " + state.evolveProgress() + "/5"));
            lore.add(Colors.color("&7Stars: " + "★".repeat(state.stars())));
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
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
