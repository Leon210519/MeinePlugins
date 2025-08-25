package com.specialitems2;

import com.specialitems2.commands.SiCommand;
import com.specialitems2.commands.BinCommand;
import com.specialitems2.effects.Effects;
import com.specialitems2.listeners.BlockListener;
import com.specialitems2.listeners.CombatListener;
import com.specialitems2.listeners.PlayerListener;
import com.specialitems2.listeners.GuiListener;
import com.specialitems2.listeners.BinListener;
import com.specialitems2.listeners.NormalizeListener;
import com.specialitems2.util.Configs;
import com.specialitems2.util.Log;
import com.specialitems2.util.TemplateItems;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.specialitems2.util.CustomModelDataUtil;
import org.bukkit.plugin.ServicePriority;

import com.instancednodes.integration.SpecialItemsApi;
import com.specialitems2.integration.SpecialItemsBridge;

// Leveling imports
import com.specialitems2.leveling.LevelingService;
import com.specialitems2.leveling.LevelingListener;

public class SpecialItems2Plugin extends JavaPlugin {

    private static SpecialItems2Plugin instance;

    // Leveling service (new)
    private LevelingService leveling;

    public static SpecialItems2Plugin getInstance() {
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
            // Preload templates to resolve custom model data mappings
            java.util.List<com.specialitems2.util.TemplateItems.TemplateItem> list = TemplateItems.loadAll();
            for (com.specialitems2.util.TemplateItems.TemplateItem t : list) com.specialitems2.util.CustomModelDataUtil.normalize(t.stack());
        } catch (Throwable t) {
            Log.warn("Configs.load failed: " + t.getMessage());
        }

        // Register core listeners
        try {
            getServer().getPluginManager().registerEvents(new BlockListener(), this);
            getServer().getPluginManager().registerEvents(new CombatListener(), this);
getServer().getPluginManager().registerEvents(new NormalizeListener(), this);
            getServer().getPluginManager().registerEvents(new GuiListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(), this);
            getServer().getPluginManager().registerEvents(new BinListener(this), this);
        } catch (Throwable t) {
            Log.warn("Listener registration failed: " + t.getMessage());
        }

        // Commands
        if (getCommand("si2") != null) {
            try {
                getCommand("si2").setExecutor(new SiCommand());
            } catch (Throwable t) {
                Log.warn("Failed to set executor for /si2: " + t.getMessage());
            }
        } else {
            Log.warn("Command 'si2' not found in plugin.yml!");
        }
        if (getCommand("bin2") != null) {
            try {
                getCommand("bin2").setExecutor(new BinCommand());
            } catch (Throwable t) {
                Log.warn("Failed to set executor for /bin2: " + t.getMessage());
            }
        } else {
            Log.warn("Command 'bin2' not found in plugin.yml!");
        }
        if (getCommand("si2_givecmd") != null && getCommand("si2_cmdcheck") != null) {
            try {
                var dbg = new com.specialitems2.debug.SiCmdDebug();
                getCommand("si2_givecmd").setExecutor(dbg);
                getCommand("si2_cmdcheck").setExecutor(dbg);
            } catch (Throwable t) {
                Log.warn("Failed to set executor for debug commands: " + t.getMessage());
            }
        } else {
            Log.warn("Debug commands not found in plugin.yml!");
        }

        if (getCommand("si2_cmdfix") != null) {
            getCommand("si2_cmdfix").setExecutor(new com.specialitems2.debug.SiCmdFix());
        } else {
            Log.warn("Command 'si2_cmdfix' not found in plugin.yml!");
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
            Log.info("SpecialItems2 enabled with " + Effects.size() + " effects.");
        } catch (Throwable t) {
            Log.info("SpecialItems2 enabled.");
        }
    }

    @Override
    public void onDisable() {
        Log.info("SpecialItems2 disabled.");
        instance = null;
    }
}