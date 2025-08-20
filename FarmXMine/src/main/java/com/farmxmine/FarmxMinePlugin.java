package com.farmxmine;

import com.farmxmine.command.ArtifactsCommand;
import com.farmxmine.command.FXMCommand;
import com.farmxmine.placeholder.FarmxMinePlaceholder;
import com.farmxmine.service.*;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class FarmxMinePlugin extends JavaPlugin {
    private StorageService storage;
    private ArtifactService artifacts;
    private EconomyService economy;
    private BossbarService bossbars;
    private LevelService levels;
    private InstancingService instancing;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initServices();
        getCommand("artifacts").setExecutor(new ArtifactsCommand(artifacts));
        getCommand("fxm").setExecutor(new FXMCommand(this));
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new FarmxMinePlaceholder(this, storage, artifacts, bossbars).register();
        }
    }

    private void initServices() {
        storage = new StorageService(this);
        bossbars = new BossbarService(this);
        artifacts = new ArtifactService(this, storage);
        economy = new EconomyService(this, artifacts);
        levels = new LevelService(this, storage, artifacts, bossbars);
        instancing = new InstancingService(this, levels);
    }

    public void reloadPlugin() {
        reloadConfig();
        if (instancing != null) HandlerList.unregisterAll(instancing);
        initServices();
    }

    @Override
    public void onDisable() {
        storage.saveAll();
    }

    public StorageService getStorage() { return storage; }
    public ArtifactService getArtifacts() { return artifacts; }
    public EconomyService getEconomy() { return economy; }
    public BossbarService getBossbars() { return bossbars; }
    public LevelService getLevels() { return levels; }
}
