package com.lootpets.gui;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PetDefinition;
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

import java.util.List;

public class CompareGUI implements Listener {

    private final LootPetsPlugin plugin;
    private final PetRegistry petRegistry;
    private final PetService petService;

    public CompareGUI(LootPetsPlugin plugin, PetRegistry petRegistry, PetService petService) {
        this.plugin = plugin;
        this.petRegistry = petRegistry;
        this.petService = petService;
    }

    public void open(Player player, List<String> ids) {
        player.openInventory(build(player, ids));
    }

    public Inventory build(Player player, List<String> ids) {
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        Inventory inv = Bukkit.createInventory(null, 27, Colors.color(lang.getString("compare-button").replace("%count%/%max%", "")));
        if (ids.isEmpty()) {
            inv.setItem(13, item(Material.BARRIER, Colors.color(lang.getString("no-pets"))));
        } else {
            for (int i = 0; i < ids.size() && i < 2; i++) {
                String id = ids.get(i);
                OwnedPetState state = petService.getOwnedPets(player.getUniqueId()).get(id);
                PetDefinition def = petRegistry.byId(id);
                ItemStack icon = buildIcon(def, state, cfg);
                int slot = i == 0 ? 11 : 15;
                inv.setItem(slot, icon);
            }
        }
        inv.setItem(26, item(Material.BARRIER, Colors.color(lang.getString("back-button"))));
        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = Colors.color(plugin.getLang().getString("compare-button").replace("%count%/%max%", ""));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getRawSlot() == 26) {
            player.openInventory(plugin.getPetsGUI().build(player));
        }
    }

    private ItemStack buildIcon(PetDefinition def, OwnedPetState state, FileConfiguration cfg) {
        Material material = def == null ? Material.BARRIER : def.iconMaterial();
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            if (def != null && def.iconCustomModelData() != null) {
                meta.setCustomModelData(def.iconCustomModelData());
            }
            String name = def == null ? "Unknown" : def.displayName();
            if (state != null && state.rarity() != null) {
                var rr = plugin.getRarityRegistry().getRarities().get(state.rarity());
                if (rr != null) {
                    name = rr.color() + Colors.color(name);
                } else {
                    name = Colors.color(name);
                }
            } else {
                name = Colors.color(name);
            }
            meta.setDisplayName(name);
            List<String> lore = new java.util.ArrayList<>();
            if (state != null) {
                int cap = cfg.getInt("leveling_runtime.level_cap_base", 100) +
                        cfg.getInt("leveling_runtime.level_cap_extra_per_star", 50) * state.stars();
                lore.add(Colors.color("&7Rarity: " + (state.rarity() == null ? "?" : state.rarity())));
                lore.add(Colors.color("&7Stars: " + cfg.getString("placeholders.format.star_symbol", "★").repeat(state.stars())));
                lore.add(Colors.color("&7Level " + state.level() + "/" + cap));
                lore.add(Colors.color("&7Evolve " + state.evolveProgress() + "/5"));
                for (EarningType type : EarningType.values()) {
                    double w = def != null ? def.weights().getOrDefault(type.weightKey(), 1.0) : 1.0;
                    lore.add(Colors.color("&7" + type.name().replace("EARNINGS_", "") + ": " + w));
                }
            }
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
