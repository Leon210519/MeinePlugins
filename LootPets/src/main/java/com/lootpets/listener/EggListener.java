package com.lootpets.listener;

import com.lootpets.LootPetsPlugin;
import com.lootpets.service.EggService;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EggListener implements Listener {

    private final LootPetsPlugin plugin;
    private final EggService eggService;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private boolean malformedLogged = false;

    public EggListener(LootPetsPlugin plugin, EggService eggService) {
        this.plugin = plugin;
        this.eggService = eggService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
            }
            default -> {
                return;
            }
        }
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("eggs.redeem_block_in_creative", false) && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        List<String> blacklist = plugin.getConfig().getStringList("eggs.redeem_world_blacklist");
        if (blacklist.contains(player.getWorld().getName())) {
            return;
        }
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("eggs.redeem_cooldown_millis", 300L);
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cd) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now);
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return;
        }
        EggData data = detect(item);
        if (data == null) {
            return;
        }
        eggService.redeem(player, data.petId, data.rarityId, true, item);
        event.setCancelled(true);
    }

    private record EggData(String petId, String rarityId) {}

    private EggData detect(ItemStack item) {
        String strategy = plugin.getConfig().getString("eggs.detection.strategy", "PDC");
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        if ("PDC".equalsIgnoreCase(strategy)) {
            String namespace = plugin.getConfig().getString("eggs.detection.namespace", "lootpets");
            ConfigurationSection keys = plugin.getConfig().getConfigurationSection("eggs.detection.keys");
            String typeKey = keys != null ? keys.getString("type", "type") : "type";
            String rarityKey = keys != null ? keys.getString("rarity", "rarity") : "rarity";
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey tKey = new NamespacedKey(namespace, typeKey);
            NamespacedKey rKey = new NamespacedKey(namespace, rarityKey);
            String petId = pdc.get(tKey, PersistentDataType.STRING);
            String rarityId = pdc.get(rKey, PersistentDataType.STRING);
            if (petId == null || rarityId == null) {
                debugMalformed();
                return null;
            }
            return new EggData(petId, rarityId);
        } else if ("NAME_LORE".equalsIgnoreCase(strategy)) {
            String prefix = plugin.getConfig().getString("eggs.match.name_prefix", "");
            String contains = plugin.getConfig().getString("eggs.match.lore_contains", "");
            String name = ChatColor.stripColor(meta.getDisplayName() == null ? "" : meta.getDisplayName());
            if (!prefix.isEmpty() && !name.startsWith(prefix)) {
                return null;
            }
            String rest = name.substring(Math.min(prefix.length(), name.length())).trim();
            String[] parts = rest.split("\\s+");
            if (parts.length < 2) {
                debugMalformed();
                return null;
            }
            String petId = parts[0];
            String rarityId = parts[1];
            List<String> lore = meta.getLore();
            if (contains != null && !contains.isEmpty()) {
                boolean ok = false;
                if (lore != null) {
                    for (String line : lore) {
                        if (ChatColor.stripColor(line).contains(contains)) {
                            ok = true;
                            break;
                        }
                    }
                }
                    if (!ok) {
                        debugMalformed();
                        return null;
                    }
                }
                return new EggData(petId, rarityId);
        }
        return null;
    }

    private void debugMalformed() {
        if (!malformedLogged) {
            plugin.getLogger().fine("Egg detection failed to parse expected keys");
            malformedLogged = true;
        }
    }
}

