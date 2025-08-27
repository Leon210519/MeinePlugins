package com.lootpets.gui;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PetDefinition;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.service.RarityRegistry;
import com.lootpets.service.SlotService;
import com.lootpets.util.Colors;
import com.lootpets.util.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PetsGUI implements Listener {

    private final LootPetsPlugin plugin;
    private final SlotService slotService;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Map<UUID, Long> lastChange = new HashMap<>();
    private final Set<String> missingWarned = new HashSet<>();
    private boolean overflowWarned = false;

    private static class PlayerState {
        Map<Integer, String> ownedSlots = new HashMap<>();
        int page = 0;
        int rarityIndex = 0;
        int typeIndex = 0;
        int sortIndex = -1; // -1 = default
        LinkedList<String> compare = new LinkedList<>();
    }

    public PetsGUI(LootPetsPlugin plugin, SlotService slotService, PetService petService, PetRegistry petRegistry) {
        this.plugin = plugin;
        this.slotService = slotService;
        this.petService = petService;
        this.petRegistry = petRegistry;
    }

    public Inventory build(Player player) {
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
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
                    OwnedPetState st = owned.get(id);
                    PetDefinition def = petRegistry.byId(id);
                    if (def == null && missingWarned.add(id)) {
                        DebugLogger.debug(plugin, "gui", "Missing pet definition for id " + id);
                    }
                    inv.setItem(index, petIcon(player, def, st, true));
                } else {
                    inv.setItem(index, empty);
                }
            } else {
                inv.setItem(index, locked);
            }
        }

        // Build owned list with filters, sorting, pagination
        List<Map.Entry<String, OwnedPetState>> list = new ArrayList<>(owned.entrySet());
        boolean filtersEnabled = cfg.getBoolean("gui.features.filters_enabled", true);
        boolean sortingEnabled = cfg.getBoolean("gui.features.sorting_enabled", true);
        boolean paginationEnabled = cfg.getBoolean("gui.features.pagination_enabled", true);

        // rarity filter
        List<String> rarityKeys = new ArrayList<>(plugin.getRarityRegistry().getRarities().keySet());
        rarityKeys.add(0, "ALL");
        if (filtersEnabled && cfg.getBoolean("gui.filters.rarity_filter", true)) {
            String selected = rarityKeys.get(state.rarityIndex % rarityKeys.size());
            if (!selected.equals("ALL")) {
                list.removeIf(e -> {
                    String r = e.getValue().rarity();
                    return r == null || !r.equals(selected);
                });
            }
        }

        // type filter
        EarningType[] types = EarningType.values();
        if (filtersEnabled && cfg.getBoolean("gui.filters.type_filter", true)) {
            int idx = state.typeIndex % (types.length + 1);
            if (idx > 0) {
                EarningType type = types[idx - 1];
                list.removeIf(e -> {
                    PetDefinition def = petRegistry.byId(e.getKey());
                    if (def == null) {
                        return true;
                    }
                    double w = def.weights().getOrDefault(type.weightKey(), 1.0);
                    return w <= 0;
                });
            }
        }

        // sorting
        if (sortingEnabled) {
            List<String> defaults = Arrays.stream(cfg.getString("gui.sorting.default", "").split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            List<String> modes = cfg.getStringList("gui.sorting.modes");
            List<String> order = new ArrayList<>();
            if (state.sortIndex >= 0 && state.sortIndex < modes.size()) {
                order.add(modes.get(state.sortIndex));
            }
            for (String d : defaults) {
                if (!order.contains(d)) {
                    order.add(d);
                }
            }
            list.sort(buildComparator(order));
        }

        // pagination
        int start = cfg.getInt("gui.owned-range.start");
        int end = cfg.getInt("gui.owned-range.end");
        int capacity = Math.max(0, end - start + 1);
        int pageSize = paginationEnabled ? cfg.getInt("gui.pagination.page_size", capacity) : capacity;
        int totalPages = Math.max(1, (int) Math.ceil(list.size() / (double) pageSize));
        if (state.page >= totalPages) {
            state.page = totalPages - 1;
        }
        if (state.page < 0) {
            state.page = 0;
        }
        int from = state.page * pageSize;
        int to = Math.min(list.size(), from + pageSize);
        List<Map.Entry<String, OwnedPetState>> pageList = list.subList(from, to);

        state.ownedSlots.clear();
        int placed = 0;
        for (Map.Entry<String, OwnedPetState> entry : pageList) {
            if (placed >= capacity) {
                if (!overflowWarned) {
                    overflowWarned = true;
                    plugin.getLogger().warning("Some pets could not be displayed in GUI; increase owned-range.");
                }
                break;
            }
            String id = entry.getKey();
            OwnedPetState st = entry.getValue();
            PetDefinition def = petRegistry.byId(id);
            if (def == null && missingWarned.add(id)) {
                DebugLogger.debug(plugin, "gui", "Missing pet definition for id " + id);
            }
            int slot = start + placed;
            inv.setItem(slot, petIcon(player, def, st, false));
            state.ownedSlots.put(slot, id);
            placed++;
        }
        if (placed == 0) {
            inv.setItem(start, item(Material.BARRIER, Colors.color(lang.getString("no-pets"))));
        }

        // controls
        int prevSlot = cfg.getInt("gui.pagination.controls.prev_slot", 45);
        int nextSlot = cfg.getInt("gui.pagination.controls.next_slot", 53);
        if (paginationEnabled) {
            inv.setItem(prevSlot, item(Material.ARROW, Colors.color(lang.getString("prev-page"))));
            inv.setItem(nextSlot, item(Material.ARROW, Colors.color(lang.getString("next-page"))));
        }
        int sortSlot = 46;
        int raritySlot = 47;
        int typeSlot = 48;
        int compareSlot = 49;
        int albumSlot = 50;
        int shopSlot = 51;
        if (sortingEnabled) {
            String mode = state.sortIndex >= 0 && state.sortIndex < cfg.getStringList("gui.sorting.modes").size()
                    ? cfg.getStringList("gui.sorting.modes").get(state.sortIndex)
                    : "DEFAULT";
            inv.setItem(sortSlot, item(Material.COMPASS, Colors.color(lang.getString("sort-label").replace("%mode%", mode))));
        }
        if (filtersEnabled && cfg.getBoolean("gui.filters.rarity_filter", true)) {
            String val = rarityKeys.get(state.rarityIndex % rarityKeys.size());
            inv.setItem(raritySlot, item(Material.BOOK, Colors.color(lang.getString("filter-label").replace("%value%", val))));
        }
        if (filtersEnabled && cfg.getBoolean("gui.filters.type_filter", true)) {
            String val = state.typeIndex == 0 ? "ALL" : types[state.typeIndex - 1].name().replace("EARNINGS_", "");
            inv.setItem(typeSlot, item(Material.PAPER, Colors.color(lang.getString("filter-label").replace("%value%", val))));
        }
        if (cfg.getBoolean("gui.features.compare_enabled", true)) {
            String label = lang.getString("compare-button")
                    .replace("%count%", String.valueOf(state.compare.size()))
                    .replace("%max%", String.valueOf(cfg.getInt("gui.compare.max_items", 2)));
            inv.setItem(compareSlot, item(Material.ANVIL, Colors.color(label)));
        }
        if (cfg.getBoolean("album.open_from_pets_gui", true) && cfg.getBoolean("gui.features.album_enabled", true)) {
            inv.setItem(albumSlot, item(Material.CHEST, Colors.color(lang.getString("album-button"))));
        }
        if (plugin.getConfig().getBoolean("shards.shop.gui.open_from_pets_gui", true)
                && plugin.getConfig().getBoolean("shards.shop.enabled", true)) {
            inv.setItem(shopSlot, item(Material.EMERALD, Colors.color(lang.getString("shop-button"))));
        }

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
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration lang = plugin.getLang();
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
        int prevSlot = cfg.getInt("gui.pagination.controls.prev_slot", 45);
        int nextSlot = cfg.getInt("gui.pagination.controls.next_slot", 53);
        int sortSlot = 46;
        int raritySlot = 47;
        int typeSlot = 48;
        int compareSlot = 49;
        int albumSlot = 50;
        int slot = event.getRawSlot();

        if (slot == prevSlot) {
            state.page--;
            DebugLogger.debug(plugin, "gui", player.getName() + " page " + state.page);
            player.openInventory(build(player));
            return;
        }
        if (slot == nextSlot) {
            state.page++;
            DebugLogger.debug(plugin, "gui", player.getName() + " page " + state.page);
            player.openInventory(build(player));
            return;
        }
        if (slot == sortSlot) {
            state.sortIndex++;
            if (state.sortIndex >= cfg.getStringList("gui.sorting.modes").size()) {
                state.sortIndex = -1;
            }
            DebugLogger.debug(plugin, "gui", player.getName() + " sort " + state.sortIndex);
            player.openInventory(build(player));
            return;
        }
        if (slot == raritySlot) {
            state.rarityIndex++;
            DebugLogger.debug(plugin, "gui", player.getName() + " rarity " + state.rarityIndex);
            player.openInventory(build(player));
            return;
        }
        if (slot == typeSlot) {
            state.typeIndex++;
            if (state.typeIndex > EarningType.values().length) {
                state.typeIndex = 0;
            }
            DebugLogger.debug(plugin, "gui", player.getName() + " type " + state.typeIndex);
            player.openInventory(build(player));
            return;
        }
        if (slot == compareSlot) {
            if (!state.compare.isEmpty()) {
                plugin.getCompareGUI().open(player, new ArrayList<>(state.compare));
            }
            return;
        }
        if (slot == albumSlot) {
            plugin.getAlbumGUI().open(player);
            return;
        }
        if (slot == shopSlot && plugin.getShardShopGUI() != null) {
            plugin.getShardShopGUI().open(player);
            return;
        }

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
        String petId = state.ownedSlots.get(slot);
        if (petId != null) {
            if (plugin.getShardShopGUI() != null && plugin.getShardShopGUI().handlePetSelect(player, petId)) {
                player.openInventory(build(player));
                return;
            }
            boolean openKey = cfg.getString("gui.compare.open_key", "SHIFT_LEFT_CLICK").equalsIgnoreCase("SHIFT_LEFT_CLICK")
                    && event.isShiftClick() && event.isLeftClick();
            if (openKey && cfg.getBoolean("gui.features.compare_enabled", true)) {
                if (state.compare.contains(petId)) {
                    state.compare.remove(petId);
                } else {
                    int max = cfg.getInt("gui.compare.max_items", 2);
                    if (state.compare.size() >= max) {
                        state.compare.removeFirst();
                    }
                    state.compare.add(petId);
                }
                DebugLogger.debug(plugin, "gui", player.getName() + " compare " + state.compare);
                player.openInventory(build(player));
                return;
            }
            List<String> activeIds = petService.getActivePetIds(player.getUniqueId(), Math.min(limit, active.size()));
            if (!activeIds.contains(petId)) {
                if (petService.equipPet(player.getUniqueId(), petId, Math.min(limit, active.size()))) {
                    lastChange.put(player.getUniqueId(), now);
                    player.openInventory(build(player));
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String title = Colors.color(plugin.getConfig().getString("gui.title"));
            if (event.getView().getTitle().equals(title)) {
                states.remove(player.getUniqueId());
                lastChange.remove(player.getUniqueId());
            }
        }
    }

    private ItemStack petIcon(Player player, PetDefinition def, OwnedPetState state, boolean active) {
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
            if (state.suffix() != null && !state.suffix().isEmpty()) {
                name = name + " " + state.suffix();
            }
            String frame = petService.getAlbumFrameStyle(player.getUniqueId());
            if (frame != null) {
                name = "[Frame: " + frame + "] " + name;
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
            lore.add(Colors.color("&7Stars: " + plugin.getConfig().getString("placeholders.format.star_symbol", "★").repeat(state.stars())));
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

    public void reset() {
        states.clear();
        lastChange.clear();
        overflowWarned = false;
        missingWarned.clear();
    }

    private Comparator<Map.Entry<String, OwnedPetState>> buildComparator(List<String> order) {
        return (a, b) -> {
            for (String key : order) {
                int result = switch (key) {
                    case "RARITY_ASC" -> compareRarity(a.getValue(), b.getValue(), true);
                    case "RARITY_DESC" -> compareRarity(a.getValue(), b.getValue(), false);
                    case "STARS_ASC" -> Integer.compare(a.getValue().stars(), b.getValue().stars());
                    case "STARS_DESC" -> Integer.compare(b.getValue().stars(), a.getValue().stars());
                    case "LEVEL_ASC" -> Integer.compare(a.getValue().level(), b.getValue().level());
                    case "LEVEL_DESC" -> Integer.compare(b.getValue().level(), a.getValue().level());
                    case "NAME_DESC" -> compareName(a.getKey(), b.getKey(), false);
                    case "NAME_ASC" -> compareName(a.getKey(), b.getKey(), true);
                    default -> 0;
                };
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        };
    }

    private int compareRarity(OwnedPetState a, OwnedPetState b, boolean asc) {
        Map<String, RarityRegistry.Rarity> map = plugin.getRarityRegistry().getRarities();
        int ia = indexOf(map, a.rarity());
        int ib = indexOf(map, b.rarity());
        return asc ? Integer.compare(ia, ib) : Integer.compare(ib, ia);
    }

    private int indexOf(Map<String, RarityRegistry.Rarity> map, String key) {
        int idx = 0;
        for (String k : map.keySet()) {
            if (Objects.equals(k, key)) {
                return idx;
            }
            idx++;
        }
        return idx;
    }

    private int compareName(String a, String b, boolean asc) {
        PetDefinition da = petRegistry.byId(a);
        PetDefinition db = petRegistry.byId(b);
        String na = da == null ? a : da.displayName();
        String nb = db == null ? b : db.displayName();
        return asc ? na.compareToIgnoreCase(nb) : nb.compareToIgnoreCase(na);
    }
}
