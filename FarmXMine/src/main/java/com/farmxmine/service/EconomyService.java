package com.farmxmine.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {
    private final ArtifactService artifacts;
    private final Economy economy;

    public EconomyService(JavaPlugin plugin, ArtifactService artifacts) {
        this.artifacts = artifacts;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = rsp != null ? rsp.getProvider() : null;
    }

    public double apply(Player player, double amount) {
        return amount * artifacts.getMultiplier(player);
    }

    public Economy getEconomy() {
        return economy;
    }
}
