package com.specialitems;

import com.specialitems.commands.SiCommand;
import com.specialitems.commands.BinCommand;
import com.specialitems.effects.Effects;
import com.specialitems.listeners.BlockListener;
import com.specialitems.listeners.CombatListener;
import com.specialitems.listeners.PlayerListener;
import com.specialitems.listeners.GuiListener;
import com.specialitems.listeners.BinListener;
import com.specialitems.util.Configs;
import com.specialitems.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

// Leveling imports
import com.specialitems.leveling.LevelingService;
import com.specialitems.leveling.LevelingListener;
import com.specialitems.leveling.YieldApplier;

public class SpecialItemsPlugin extends JavaPlugin {

    private static SpecialItemsPlugin instance;

    // Leveling service (new)
    private LevelingService leveling;

    public static SpecialItemsPlugin getInstance() {
        return instance;
    }

    public LevelingService leveling() {
        return leveling;
    }

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Load configs/messages if your Configs helper does that.
            Configs.load(this);
        } catch (Throwable t) {
            Log.warn("Configs.load failed: " + t.getMessage());
        }

        // Register core listeners
        try {
            getServer().getPluginManager().registerEvents(new BlockListener(), this);
            getServer().getPluginManager().registerEvents(new CombatListener(), this);
            getServer().getPluginManager().registerEvents(new GuiListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(), this);
            getServer().getPluginManager().registerEvents(new BinListener(this), this);
        } catch (Throwable t) {
            Log.warn("Listener registration failed: " + t.getMessage());
        }

        // Commands
        if (getCommand("si") != null) {
            try {
                getCommand("si").setExecutor(new SiCommand());
            } catch (Throwable t) {
                Log.warn("Failed to set executor for /si: " + t.getMessage());
            }
        } else {
            Log.warn("Command 'si' not found in plugin.yml!");
        }
        if (getCommand("bin") != null) {
            try {
                getCommand("bin").setExecutor(new BinCommand());
            } catch (Throwable t) {
                Log.warn("Failed to set executor for /bin: " + t.getMessage());
            }
        } else {
            Log.warn("Command 'bin' not found in plugin.yml!");
        }

        // --- Leveling system (NEW) ---
        this.leveling = new LevelingService(this);
        getServer().getPluginManager().registerEvents(new LevelingListener(leveling), this);
        getServer().getPluginManager().registerEvents(new YieldApplier(leveling), this);

        // Ticker (kept from your original)
        try {
            Bukkit.getScheduler().runTaskTimer(this, PlayerListener::tickAll, 20L, 10L);
        } catch (Throwable t) {
            Log.warn("Failed to start player tick task: " + t.getMessage());
        }

        // Startup log
        try {
            Log.info("SpecialItems enabled with " + Effects.size() + " effects.");
        } catch (Throwable t) {
            Log.info("SpecialItems enabled.");
        }
    }

    @Override
    public void onDisable() {
        Log.info("SpecialItems disabled.");
        instance = null;
    }
}