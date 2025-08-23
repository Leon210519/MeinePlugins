package com.farmxmine2;

import com.farmxmine2.command.Fm2Command;
import com.farmxmine2.listener.BlockListener;
import com.farmxmine2.listener.PlayerListener;
import com.farmxmine2.service.*;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Objects;

public class FarmXMine2Plugin extends JavaPlugin {
    private FileConfiguration messages;
    private ConfigService configService;
    private StorageService storageService;
    private LevelService levelService;
    private BossBarService bossBarService;
    private HarvestService harvestService;
    private CooldownService cooldownService;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));

        configService = new ConfigService(this);
        storageService = new StorageService(this);
        bossBarService = new BossBarService(this);
        levelService = new LevelService(this, storageService, configService);
        cooldownService = new CooldownService(this);
        harvestService = new HarvestService(configService, levelService, cooldownService);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockListener(harvestService, configService), this);
        pm.registerEvents(new PlayerListener(storageService, levelService, cooldownService), this);

        Fm2Command cmd = new Fm2Command(this);
        Objects.requireNonNull(getCommand("fm2")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("fm2")).setTabCompleter(cmd);

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
        }

        autosaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> storageService.saveAllSync(levelService.getAll()), 20L * 300, 20L * 300);
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) autosaveTask.cancel();
        cooldownService.clearAll();
        storageService.saveAllSync(levelService.getAll());
    }

    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public ConfigService getConfigService() { return configService; }
    public LevelService getLevelService() { return levelService; }
    public BossBarService getBossBarService() { return bossBarService; }
}
