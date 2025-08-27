package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles periodic backups of pets.yml and verification on load.
 */
public class BackupService {

    private final LootPetsPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private PetService petService;

    private boolean enabled;
    private long intervalTicks;
    private int keepFiles;
    private String prefix;

    public BackupService(LootPetsPlugin plugin, PetService petService) {
        this.plugin = plugin;
        this.petService = petService;
        reload();
        schedule();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("backups.enabled", true);
        int minutes = cfg.getInt("backups.interval_minutes", 60);
        intervalTicks = minutes * 60L * 20L;
        keepFiles = cfg.getInt("backups.keep_files", 24);
        prefix = cfg.getString("backups.file_prefix", "pets-backup-");
    }

    private void schedule() {
        if (!enabled) {
            return;
        }
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::backup, intervalTicks, intervalTicks);
    }

    public String backupNow() {
        if (!enabled) {
            return null;
        }
        return backup();
    }

    private String backup() {
        File file = new File(plugin.getDataFolder(), "pets.yml");
        File dir = new File(plugin.getDataFolder(), "backups");
        String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String name = prefix + ts + ".yml";
        Path target = dir.toPath().resolve(name);
        executor.execute(() -> {
            try {
                if (petService != null) {
                    petService.save();
                }
                Files.createDirectories(dir.toPath());
                Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                cleanup(dir);
            } catch (IOException e) {
                plugin.getLogger().warning("Backup failed: " + e.getMessage());
            }
        });
        return name;
    }

    private void cleanup(File dir) {
        File[] files = dir.listFiles((d, n) -> n.startsWith(prefix) && n.endsWith(".yml"));
        if (files == null || files.length <= keepFiles) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = keepFiles; i < files.length; i++) {
            files[i].delete();
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * Verifies pets.yml on startup and restores the latest valid backup if corrupted.
     */
    public static void verifyOnLoad(LootPetsPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("backups.verify_on_load", true)) {
            return;
        }
        File pets = new File(plugin.getDataFolder(), "pets.yml");
        try {
            new org.bukkit.configuration.file.YamlConfiguration().load(pets);
        } catch (Exception ex) {
            File dir = new File(plugin.getDataFolder(), "backups");
            String prefix = cfg.getString("backups.file_prefix", "pets-backup-");
            File[] files = dir.listFiles((d, n) -> n.startsWith(prefix) && n.endsWith(".yml"));
            if (files == null || files.length == 0) {
                plugin.getLogger().warning("pets.yml corrupted and no backups found");
                return;
            }
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (File f : files) {
                try {
                    new org.bukkit.configuration.file.YamlConfiguration().load(f);
                    String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                    File corrupted = new File(dir, "corrupted-" + ts + ".yml");
                    Files.createDirectories(dir.toPath());
                    Files.move(pets.toPath(), corrupted.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(f.toPath(), pets.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().warning("Restored pets.yml from backup " + f.getName());
                    return;
                } catch (Exception ignored) {
                }
            }
            plugin.getLogger().warning("pets.yml corrupted and no valid backup found");
        }
    }
}
