package com.lootfactory.prestige;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Verwaltet Prestige-Daten (prestige.yml) und liefert Balancing-Werte aus der config.yml.
 * Speichert pro Spieler die Prestige-Stufe je Fabriktyp.
 *
 * Datei: plugins/LootFactory/prestige.yml
 *
 * Struktur:
 * players:
 *   <uuid>:
 *     factories:
 *       IRON: 2
 *       GOLD: 0
 */
public final class PrestigeManager {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public PrestigeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "prestige.yml");
        reload();
    }

    /** Lädt oder erzeugt prestige.yml */
    public void reload() {
        try {
            if (!plugin.getDataFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                plugin.getDataFolder().mkdirs();
            }
            if (!file.exists()) {
                // Neu anlegen mit Grundstruktur
                YamlConfiguration fresh = new YamlConfiguration();
                fresh.set("players", null); // leere Map
                fresh.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte prestige.yml nicht anlegen: " + e.getMessage());
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    /** Speichert prestige.yml */
    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte prestige.yml nicht speichern: " + e.getMessage());
        }
    }

    // ------------------ Player Prestige ------------------

    /** Liefert die Prestige-Stufe eines Spielers für einen Fabriktyp (z. B. "IRON"). */
    public int getPrestige(UUID uuid, String factoryType) {
        String key = String.format(Locale.ROOT, "players.%s.factories.%s", uuid, factoryType.toUpperCase(Locale.ROOT));
        return data.getInt(key, 0);
    }

    /** Setzt die Prestige-Stufe eines Spielers für einen Fabriktyp. */
    public void setPrestige(UUID uuid, String factoryType, int level) {
        String base = String.format(Locale.ROOT, "players.%s.factories", uuid);
        String key = base + "." + factoryType.toUpperCase(Locale.ROOT);
        data.set(key, Math.max(0, level));
        save();
    }

    // ------------------ Balancing aus config.yml ------------------

    public int getRequirementLevel() {
        return plugin.getConfig().getInt("prestige.perFactory.requirementLevel", 10);
    }

    public double calcPrestigeCost(int currentPrestige) {
        double base = plugin.getConfig().getDouble("prestige.perFactory.baseCost", 100_000D);
        double factor = plugin.getConfig().getDouble("prestige.perFactory.costFactor", 3.0D);
        return base * Math.pow(factor, Math.max(0, currentPrestige));
    }

    public double getProdBonusPerLevel() {
        return plugin.getConfig().getDouble("prestige.perFactory.prodMultiplierPerLevel", 0.15D);
    }

    public double getSpeedBonusPerLevel() {
        return plugin.getConfig().getDouble("prestige.perFactory.speedBonusPerLevel", 0.05D);
    }

    public double getRareChancePerLevel() {
        return plugin.getConfig().getDouble("prestige.perFactory.rareDropChancePerLevel", 0.01D);
    }

    public boolean isKeepCosmetics() {
        return plugin.getConfig().getBoolean("prestige.perFactory.keepCosmetics", true);
    }

    public int getKeepStoragePercent() {
        return plugin.getConfig().getInt("prestige.perFactory.keepStoragePercent", 0);
    }

    public int getCooldownSeconds() {
        return plugin.getConfig().getInt("prestige.perFactory.cooldownSeconds", 10);
    }

    public long getMinTickIntervalTicks() {
        return plugin.getConfig().getLong("prestige.perFactory.minTickIntervalTicks", 20L);
    }
}
