package com.specialitems;

import com.specialitems.commands.SiCommand;
import com.specialitems.commands.BinCommand;
import com.specialitems.effects.Effects;
import com.specialitems.listeners.BlockListener;
import com.specialitems.listeners.CombatListener;
import com.specialitems.listeners.PlayerListener;
import com.specialitems.listeners.GuiListener;
import com.specialitems.listeners.BinListener;
import com.specialitems.listeners.JoinFixListener;
import com.specialitems.listeners.InventorySanityListener;
import com.specialitems.listeners.GiveInterceptListener;
import com.specialitems.util.Configs;
import com.specialitems.util.Log;
import com.specialitems.util.TemplateItems;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

import com.instancednodes.integration.SpecialItemsApi;
import com.specialitems.integration.SpecialItemsBridge;

// Leveling imports
import com.specialitems.leveling.LevelingService;
import com.specialitems.leveling.LevelingListener;

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
            TemplateItems.loadAll();
            Log.info("Loaded " + TemplateItems.loadedCount() + " templates; normalized " + TemplateItems.normalizedNonIntCount() + " CMD values.");
            Log.info("Rarities with sets: " + TemplateItems.byRarity().keySet());
        } catch (Throwable t) {
            Log.warn("Configs.load failed: " + t.getMessage());
        }

        // Register core listeners
        try {
            getServer().getPluginManager().registerEvents(new BlockListener(), this);
            getServer().getPluginManager().registerEvents(new CombatListener(), this);
            getServer().getPluginManager().registerEvents(new GuiListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(), this);
            getServer().getPluginManager().registerEvents(new JoinFixListener(), this);
            getServer().getPluginManager().registerEvents(new InventorySanityListener(), this);
            getServer().getPluginManager().registerEvents(new BinListener(this), this);
            getServer().getPluginManager().registerEvents(new GiveInterceptListener(), this);
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
        if (getCommand("si_givecmd") != null && getCommand("si_cmdcheck") != null) {
            try {
                var dbg = new com.specialitems.debug.SiCmdDebug();
                getCommand("si_givecmd").setExecutor(dbg);
                getCommand("si_cmdcheck").setExecutor(dbg);
            } catch (Throwable t) {
                Log.warn("Failed to set executor for debug commands: " + t.getMessage());
            }
        } else {
            Log.warn("Debug commands not found in plugin.yml!");
        }

        if (getCommand("si_cmdfix") != null) {
            getCommand("si_cmdfix").setExecutor(new com.specialitems.debug.SiCmdFix());
        } else {
            Log.warn("Command 'si_cmdfix' not found in plugin.yml!");
        }

        if (getCommand("cmdprobe") != null && getCommand("cmdscan") != null) {
            var probe = new com.specialitems.debug.CmdProbeScan();
            getCommand("cmdprobe").setExecutor(probe);
            getCommand("cmdscan").setExecutor(probe);
        } else {
            Log.warn("CmdProbe/CmdScan commands not found in plugin.yml!");
        }

        // --- Leveling system (NEW) ---
        this.leveling = new LevelingService(this);
        getServer().getPluginManager().registerEvents(new LevelingListener(leveling), this);
        getServer().getServicesManager().register(SpecialItemsApi.class, new SpecialItemsBridge(leveling), this, ServicePriority.Normal);

        // Ticker (kept from your original)
        try {
            Bukkit.getScheduler().runTaskTimer(this, PlayerListener::tickAll, 20L, 10L);
            Bukkit.getScheduler().runTaskTimer(this, PlayerListener::tickAbsorption, 20L, 80L);
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