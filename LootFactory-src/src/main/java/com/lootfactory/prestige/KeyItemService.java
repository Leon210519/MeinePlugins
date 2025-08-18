package com.lootfactory.prestige;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

/**
 * KeyItemService
 *  - Erzeugt & erkennt das Prestige-Key-Item anhand deiner config.yml.
 *
 * Erwartete Config-Struktur:
 * prestige:
 *   rewards:
 *     key:
 *       perFactoryPrestige: 1
 *       material: TRIPWIRE_HOOK
 *       name: "&6&lPrestige Key"
 *       lore:
 *         - "&7Öffnet: &ePrestige Crate"
 *         - "&8Nur durch Fabrik-Prestige erhältlich"
 *       customModelData: 10001
 *       keyId: "lootfactory:prestige_key"
 *       crateId: "Prestige"
 */
public final class KeyItemService {

    private static final String CFG_BASE = "prestige.rewards.key.";
    private final JavaPlugin plugin;
    private final NamespacedKey pdcKey;

    public KeyItemService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pdcKey = new NamespacedKey(plugin, "prestige_key"); // PDC-Tag zur eindeutigen Erkennung
    }

    /** Erzeugt 1x Prestige-Key. */
    public ItemStack createPrestigeKey() {
        return createPrestigeKey(1);
    }

    /** Erzeugt mehrere Keys (max. 64) als EINEN Stack. */
    public ItemStack createPrestigeKey(int amount) {
        FileConfiguration cfg = plugin.getConfig();

        // Material
        String matStr = cfg.getString(CFG_BASE + "material", "TRIPWIRE_HOOK");
        Material material = Material.matchMaterial(matStr);
        if (material == null) material = Material.TRIPWIRE_HOOK;

        int clamped = Math.max(1, Math.min(64, amount));
        ItemStack item = new ItemStack(material, clamped);
        ItemMeta meta = item.getItemMeta();

        // Display-Name (&-Farbcodes erlaubt)
        String name = cfg.getString(CFG_BASE + "name", "&6&lPrestige Key");
        meta.displayName(legacy(name));

        // Lore
        List<String> loreList = cfg.getStringList(CFG_BASE + "lore");
        if (loreList != null && !loreList.isEmpty()) {
            meta.lore(loreList.stream().map(this::legacy).collect(Collectors.toList()));
        }

        // Optional: CustomModelData
        if (cfg.isInt(CFG_BASE + "customModelData")) {
            meta.setCustomModelData(cfg.getInt(CFG_BASE + "customModelData"));
        }

        // PDC-Tag (keyId) setzen
        String keyId = getConfiguredKeyId();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(pdcKey, PersistentDataType.STRING, keyId);

        item.setItemMeta(meta);
        return item;
    }

    /** Prüft, ob ein Item dein Prestige-Key ist (per PDC-Tag und Wert). */
    public boolean isPrestigeKey(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return false;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        String val = pdc.get(pdcKey, PersistentDataType.STRING);
        if (val == null) return false;
        return getConfiguredKeyId().equalsIgnoreCase(val);
    }

    /** keyId aus der Config (z. B. "lootfactory:prestige_key"). */
    public String getConfiguredKeyId() {
        return plugin.getConfig().getString(CFG_BASE + "keyId", "lootfactory:prestige_key");
    }

    /** crateId aus der Config (falls das Crate-Plugin das nutzt). */
    public String getConfiguredCrateId() {
        return plugin.getConfig().getString(CFG_BASE + "crateId", "Prestige");
    }

    /** Anzahl Keys pro Fabrik-Prestige. */
    public int getKeysPerPrestige() {
        return plugin.getConfig().getInt(CFG_BASE + "perFactoryPrestige", 1);
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s == null ? "" : s);
    }
}
