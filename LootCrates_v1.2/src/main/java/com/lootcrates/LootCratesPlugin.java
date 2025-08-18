package com.lootcrates;

import com.lootcrates.crate.CrateManager;
import com.lootcrates.storage.CrateBlocks;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class LootCratesPlugin extends JavaPlugin {

    private static LootCratesPlugin instance;
    private Economy econ;
    private CrateManager crateManager;
    private CrateBlocks crateBlocks;

    public static LootCratesPlugin get() { return instance; }
    public Economy economy() { return econ; }
    public CrateManager crates() { return crateManager; }
    public CrateBlocks blocks() { return crateBlocks; }

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Vault economy was not found. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.crateManager = new CrateManager(this);
        this.crateBlocks = new CrateBlocks(this);
        getCommand("crate").setExecutor(new com.lootcrates.command.CrateCommand(this));
        getServer().getPluginManager().registerEvents(new com.lootcrates.listener.CrateListener(), this);
        getServer().getPluginManager().registerEvents(new com.lootcrates.listener.CrateBlockListener(this), this);
        getLogger().info("LootCrates enabled.");
    }

    @Override public void onDisable() {
        getLogger().info("LootCrates disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }
}
