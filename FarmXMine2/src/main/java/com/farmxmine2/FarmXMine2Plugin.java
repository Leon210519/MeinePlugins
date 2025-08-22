package com.farmxmine2;

import com.farmxmine2.command.FarmxMineCommand;
import com.farmxmine2.listener.BlockListener;
import com.farmxmine2.listener.BlockListenerOverride;
import com.farmxmine2.listener.PlayerListener;
import com.farmxmine2.service.*;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class FarmXMine2Plugin extends JavaPlugin {
    private FileConfiguration messages;
    private RegionService regionService;
    private StorageService storageService;
    private LevelService levelService;
    private BossBarService bossBarService;
    private HarvestService harvestService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));

        regionService = new RegionService(this);
        storageService = new StorageService(this);
        bossBarService = new BossBarService(this);
        levelService = new LevelService(this, storageService);
        VeinMinerCompat veinMiner = new VeinMinerCompat(this);
        harvestService = new HarvestService(this, regionService, levelService, bossBarService, veinMiner);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockListener(harvestService), this);
        if (getConfig().getBoolean("general.override_cancelled")) {
            pm.registerEvents(new BlockListenerOverride(harvestService), this);
        }
        pm.registerEvents(new PlayerListener(storageService, levelService, harvestService), this);

        FarmxMineCommand cmd = new FarmxMineCommand(this);
        Objects.requireNonNull(getCommand("farmxmine")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("farmxmine")).setTabCompleter(cmd);

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
        }
    }

    @Override
    public void onDisable() {
        harvestService.clearAll();
        storageService.saveAllSync(levelService.getAll());
    }

    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public RegionService getRegionService() { return regionService; }
    public LevelService getLevelService() { return levelService; }
    public BossBarService getBossBarService() { return bossBarService; }
}
