package com.instancednodes;

import com.instancednodes.command.NodesCommand;
import com.instancednodes.command.PrestigeCommand;
import com.instancednodes.leveling.LevelManager;
import com.instancednodes.nodes.NodeManager;
import com.instancednodes.util.Cfg;
import com.instancednodes.util.Log;
import com.instancednodes.util.Msg;
import com.instancednodes.integration.RegionService;
import com.instancednodes.integration.SpecialItemsApi;
import com.instancednodes.integration.HarvestService;
import com.instancednodes.integration.SpecialItemsIntegrationListener;
import com.instancednodes.placeholder.FarmXMinePlaceholders;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class InstancedNodesPlugin extends JavaPlugin {

    private static InstancedNodesPlugin inst;
    private final Random random = new Random();
    private DataManager dataManager;
    private LevelManager levelManager;

    public static InstancedNodesPlugin get() { return inst; }

    @Override
    public void onEnable() {
        inst = this;
        saveDefaultConfig();
        Msg.load(this);
        Cfg.load(this);
        Log.init(this);
        this.dataManager = new DataManager(this);
        this.levelManager = new LevelManager(this);
        getServer().getPluginManager().registerEvents(new NodeManager(this), this);
        if (Cfg.INTEGRATE_SPECIALITEMS) {
            RegionService regionService = getServer().getServicesManager().load(RegionService.class);
            SpecialItemsApi specialApi = getServer().getServicesManager().load(SpecialItemsApi.class);
            HarvestService harvestService = getServer().getServicesManager().load(HarvestService.class);
            if (regionService != null && specialApi != null && harvestService != null) {
                getServer().getPluginManager().registerEvents(
                        new SpecialItemsIntegrationListener(regionService, specialApi, harvestService), this);
            }
        }
        if (getCommand("nodes") != null) getCommand("nodes").setExecutor(new NodesCommand(this));
        if (getCommand("prestige") != null) getCommand("prestige").setExecutor(new PrestigeCommand(this));
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new FarmXMinePlaceholders(this).register();
        }
        getLogger().info("InstancedNodes enabled v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.save();
    }

    public DataManager data() { return dataManager; }
    public LevelManager level() { return levelManager; }
    public Random getRandom() { return random; }

    public void reloadAll() {
        reloadConfig();
        Cfg.load(this);
        Msg.load(this);
        Log.init(this);
    }
}
