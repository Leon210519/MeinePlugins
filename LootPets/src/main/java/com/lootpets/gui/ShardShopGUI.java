package com.lootpets.gui;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.OwnedPetState;
import com.lootpets.service.PetService;
import com.lootpets.util.Colors;
import com.lootpets.util.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

public class ShardShopGUI implements Listener {

    public record ShopItem(String id, String type, int cost, int xpAmount, String style) {}

    private final LootPetsPlugin plugin;
    private final PetService petService;
    private final Map<UUID, ShopItem> pending = new HashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private final Map<UUID, Map<Integer, ShopItem>> slotMap = new HashMap<>();

    public ShardShopGUI(LootPetsPlugin plugin, PetService petService) {
        this.plugin = plugin;
        this.petService = petService;
    }

    public void open(Player player) {
        player.openInventory(build(player));
        player.sendMessage(Colors.color(plugin.getLang().getString("shop-open")));
    }

    private Inventory build(Player player) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("shards.shop.gui");
        int rows = cfg == null ? 6 : cfg.getInt("rows", 6);
        String title = cfg == null ? "Shard Shop" : cfg.getString("title", "Shard Shop");
        Inventory inv = Bukkit.createInventory(null, rows * 9, Colors.color(title));
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> list = (List<Map<String, ?>>) (List<?>) plugin.getConfig().getMapList("shards.shop.items");
        Map<Integer, ShopItem> map = new HashMap<>();
        int slot = 0;
        int shards = petService.getShards(player.getUniqueId());
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        for (Map<String, ?> m : list) {
            String id = String.valueOf(m.get("id"));
            String type = String.valueOf(m.get("type"));
            Object co = m.get("cost");
            int cost = (co instanceof Number) ? ((Number) co).intValue() : 0;
            Object xo = m.get("xp_amount");
            int xp = (xo instanceof Number) ? ((Number) xo).intValue() : 0;
            String style = m.get("style") == null ? null : String.valueOf(m.get("style"));
            ShopItem item = new ShopItem(id, type, cost, xp, style);
            ItemStack icon = iconFor(item, shards, player, today);
            inv.setItem(slot, icon);
            map.put(slot, item);
            slot++;
        }
        slotMap.put(player.getUniqueId(), map);
        return inv;
    }

    private ItemStack iconFor(ShopItem item, int shards, Player player, String today) {
        Material mat;
        switch (item.type) {
            case "EVOLVE_PROGRESS" -> mat = Material.NETHER_STAR;
            case "ADD_XP" -> mat = Material.EXPERIENCE_BOTTLE;
            case "RENAME_TOKEN" -> mat = Material.NAME_TAG;
            case "ALBUM_FRAME" -> mat = Material.PAINTING;
            default -> mat = Material.PAPER;
        }
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = "&e" + item.id();
            meta.setDisplayName(Colors.color(name));
            List<String> lore = new ArrayList<>();
            lore.add(Colors.color("&7Cost: &e" + item.cost()));
            int limit = limitFor(item.type());
            if (limit < Integer.MAX_VALUE) {
                int bought = petService.getDailyBuys(player.getUniqueId(), today, item.id());
                lore.add(Colors.color("&7Today: " + bought + "/" + limit));
            }
            if (shards < item.cost()) {
                lore.add(Colors.color("&cNot enough shards"));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private int limitFor(String type) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("shards.limits");
        if (cfg == null) {
            return Integer.MAX_VALUE;
        }
        return switch (type) {
            case "EVOLVE_PROGRESS" -> cfg.getInt("max_evolve_buys_per_day", Integer.MAX_VALUE);
            case "ADD_XP" -> cfg.getInt("max_xp_buys_per_day", Integer.MAX_VALUE);
            default -> Integer.MAX_VALUE;
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("shards.shop.gui");
        String title = cfg == null ? "Shard Shop" : Colors.color(cfg.getString("title", "Shard Shop"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }
        event.setCancelled(true);
        Map<Integer, ShopItem> map = slotMap.get(player.getUniqueId());
        if (map == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("shards.safety.shop_click_cooldown_millis", 250L);
        Long last = lastClick.get(player.getUniqueId());
        if (last != null && now - last < cd) {
            return;
        }
        lastClick.put(player.getUniqueId(), now);
        ShopItem item = map.get(event.getRawSlot());
        if (item == null) {
            return;
        }
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        int shards = petService.getShards(player.getUniqueId());
        if (shards < item.cost()) {
            player.sendMessage(Colors.color(plugin.getLang().getString("not-enough-shards")));
            return;
        }
        int bought = petService.getDailyBuys(player.getUniqueId(), today, item.id());
        int limit = limitFor(item.type());
        if (bought >= limit) {
            player.sendMessage(Colors.color(plugin.getLang().getString("daily-limit-reached")));
            return;
        }
        switch (item.type()) {
            case "EVOLVE_PROGRESS", "ADD_XP" -> {
                pending.put(player.getUniqueId(), item);
                player.closeInventory();
                player.sendMessage(Colors.color(plugin.getLang().getString("choose-pet")));
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(plugin.getPetsGUI().build(player)));
            }
            case "RENAME_TOKEN" -> {
                if (petService.spendShards(player.getUniqueId(), item.cost())) {
                    petService.addRenameTokens(player.getUniqueId(), 1);
                    petService.incrementDailyBuys(player.getUniqueId(), today, item.id());
                    player.sendMessage(Colors.color(plugin.getLang().getString("purchase-success")));
                    DebugLogger.debug(plugin, "gui", player.getName() + " bought " + item.id());
                }
            }
            case "ALBUM_FRAME" -> {
                if (petService.spendShards(player.getUniqueId(), item.cost())) {
                    petService.setAlbumFrameStyle(player.getUniqueId(), item.style());
                    petService.incrementDailyBuys(player.getUniqueId(), today, item.id());
                    player.sendMessage(Colors.color(plugin.getLang().getString("purchase-success")));
                    DebugLogger.debug(plugin, "gui", player.getName() + " bought " + item.id());
                }
            }
        }
    }

    public boolean handlePetSelect(Player player, String petId) {
        ShopItem item = pending.remove(player.getUniqueId());
        if (item == null) {
            return false;
        }
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        int shards = petService.getShards(player.getUniqueId());
        if (shards < item.cost()) {
            player.sendMessage(Colors.color(plugin.getLang().getString("not-enough-shards")));
            return true;
        }
        int bought = petService.getDailyBuys(player.getUniqueId(), today, item.id());
        int limit = limitFor(item.type());
        if (bought >= limit) {
            player.sendMessage(Colors.color(plugin.getLang().getString("daily-limit-reached")));
            return true;
        }
        boolean success = false;
        switch (item.type()) {
            case "EVOLVE_PROGRESS" -> {
                Map<String, OwnedPetState> owned = petService.getOwnedPets(player.getUniqueId());
                OwnedPetState st = owned.get(petId);
                if (st != null && st.stars() < plugin.getConfig().getInt("boosts.max_stars", 5)) {
                    if (petService.spendShards(player.getUniqueId(), item.cost())) {
                        petService.incrementEvolve(player.getUniqueId(), petId);
                        success = true;
                    }
                }
            }
            case "ADD_XP" -> {
                if (petService.spendShards(player.getUniqueId(), item.cost())) {
                    int xpPerLevel = plugin.getConfig().getInt("leveling_runtime.xp_per_level", 60);
                    int base = plugin.getConfig().getInt("leveling_runtime.level_cap_base", 100);
                    int extra = plugin.getConfig().getInt("leveling_runtime.level_cap_extra_per_star", 50);
                    petService.addXp(player.getUniqueId(), petId, item.xpAmount(), xpPerLevel, base, extra);
                    success = true;
                }
            }
        }
        if (success) {
            petService.incrementDailyBuys(player.getUniqueId(), today, item.id());
            player.sendMessage(Colors.color(plugin.getLang().getString("purchase-success")));
            DebugLogger.debug(plugin, "gui", player.getName() + " bought " + item.id() + " for pet " + petId);
        }
        return true;
    }
}
