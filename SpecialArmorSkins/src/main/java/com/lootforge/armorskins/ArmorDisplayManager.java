package com.lootforge.armorskins;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.bukkit.entity.Display.Brightness;

public class ArmorDisplayManager {

    private final SpecialArmorSkins plugin;
    private final Map<UUID, EnumMap<ArmorSlot, ItemDisplay>> displays = new HashMap<>();

    public ArmorDisplayManager(SpecialArmorSkins plugin) {
        this.plugin = plugin;
    }

    public void refresh(Player player) {
        for (ArmorSlot slot : ArmorSlot.values()) {
            SASConfig.SlotSettings settings = plugin.getSASConfig().getSlotSettings(slot);
            if (settings == null) {
                removeDisplay(player, slot);
                continue;
            }
            ItemStack armor = slot.getItem(player);
            Integer cmd = getCMD(armor);
            if (cmd != null && settings.getModels().containsKey(cmd)) {
                EnumMap<ArmorSlot, ItemDisplay> map = displays.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(ArmorSlot.class));
                ItemDisplay display = map.get(slot);
                if (display == null || display.isDead()) {
                    if (display != null) display.remove();
                    display = spawnDisplay(player, settings.getTransformation());
                    map.put(slot, display);
                } else {
                    display.setTransformation(settings.getTransformation());
                }
                ItemStack item = new ItemStack(plugin.getSASConfig().getBaseItem());
                ItemMeta meta = item.getItemMeta();
                meta.setCustomModelData(cmd);
                item.setItemMeta(meta);
                display.setItemStack(item);
            } else {
                removeDisplay(player, slot);
            }
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void removePlayer(Player player) {
        EnumMap<ArmorSlot, ItemDisplay> map = displays.remove(player.getUniqueId());
        if (map != null) {
            for (ItemDisplay d : map.values()) {
                d.remove();
            }
        }
    }

    public void clear() {
        for (Map.Entry<UUID, EnumMap<ArmorSlot, ItemDisplay>> entry : displays.entrySet()) {
            for (ItemDisplay d : entry.getValue().values()) {
                d.remove();
            }
        }
        displays.clear();
    }

    private void removeDisplay(Player player, ArmorSlot slot) {
        EnumMap<ArmorSlot, ItemDisplay> map = displays.get(player.getUniqueId());
        if (map == null) return;
        ItemDisplay d = map.remove(slot);
        if (d != null) d.remove();
        if (map.isEmpty()) displays.remove(player.getUniqueId());
    }

    private ItemDisplay spawnDisplay(Player player, Transformation transformation) {
        ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
            d.setTransformation(transformation);
            d.setBrightness(new Brightness(15, 15));
            d.setInterpolationDuration(0);
        });
        player.addPassenger(display);
        return display;
    }

    private Integer getCMD(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return null;
        return meta.getCustomModelData();
    }
}
