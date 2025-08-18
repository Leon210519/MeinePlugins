package com.instancednodes;

import com.instancednodes.command.NodesCommand;
import com.instancednodes.command.PrestigeCommand;
import com.instancednodes.leveling.LevelManager;
import com.instancednodes.nodes.NodeManager;
import com.instancednodes.util.Cfg;
import com.instancednodes.util.Log;
import com.instancednodes.util.Msg;
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
        if (getCommand("nodes") != null) getCommand("nodes").setExecutor(new NodesCommand(this));
        if (getCommand("prestige") != null) getCommand("prestige").setExecutor(new PrestigeCommand(this));
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
