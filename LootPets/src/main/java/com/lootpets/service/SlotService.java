package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlotService {

    private final LootPetsPlugin plugin;
    private final Map<UUID, Integer> cache = new HashMap<>();

    public SlotService(LootPetsPlugin plugin) {
        this.plugin = plugin;
    }

    public int getMaxSlots(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), id -> computeSlots(player));
    }

    private int computeSlots(Player player) {
        int max = plugin.getConfig().getInt("default-slots");
        String prefix = plugin.getConfig().getString("slot-permission-prefix", "");
        if (prefix != null && !prefix.isEmpty()) {
            for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
                String perm = pai.getPermission();
                if (perm.startsWith(prefix)) {
                    String num = perm.substring(prefix.length());
                    try {
                        int value = Integer.parseInt(num);
                        if (value > max) {
                            max = value;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return max;
    }
}
