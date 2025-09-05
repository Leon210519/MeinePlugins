package com.lootforge.armorskins;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SpecialArmorSkins extends JavaPlugin {

    private SASConfig config;
    private ArmorDisplayManager displayManager;
    private PacketHider packetHider;

    @Override
    public void onEnable() {
        this.config = new SASConfig(this);
        this.config.reload();
        this.displayManager = new ArmorDisplayManager(this);
        this.packetHider = new PacketHider(this);

        Bukkit.getPluginManager().registerEvents(new SASListener(this), this);
        getCommand("sas").setExecutor(new SASCommand(this));

        displayManager.refreshAll();
    }

    @Override
    public void onDisable() {
        displayManager.clear();
        packetHider.shutdown();
    }

    public SASConfig getSASConfig() {
        return config;
    }

    public ArmorDisplayManager getDisplayManager() {
        return displayManager;
    }

    public void reloadPlugin() {
        config.reload();
        displayManager.clear();
        displayManager.refreshAll();
    }
}
