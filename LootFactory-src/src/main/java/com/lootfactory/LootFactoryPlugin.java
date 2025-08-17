package com.lootfactory;

import com.lootfactory.command.FactoryCommand;
import com.lootfactory.command.FactoryShopCommand;
import com.lootfactory.economy.Eco;
import com.lootfactory.factory.FactoryManager;
import com.lootfactory.gui.GUIListener;
import com.lootfactory.listener.BlockListener;
import com.lootfactory.listener.PlayerListener;
import com.lootfactory.listener.GuiRefreshListener;


// --- Prestige System imports ---
import com.lootfactory.prestige.PrestigeManager;
import com.lootfactory.prestige.KeyItemService;
import com.lootfactory.prestige.FactoryGateway;
import com.lootfactory.prestige.FactoryPrestigeCommand;
import com.lootfactory.prestige.FactoryGatewayImpl;
import com.lootfactory.prestige.PrestigeGuiListener;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LootFactoryPlugin — mit integriertem Prestige-System.
 * - PrestigeManager speichert Prestige-Stufen (prestige.yml) und liest Balancing aus config.yml
 * - KeyItemService baut/erkennt das Prestige-Key-Item (für dein Crate-Plugin)
 * - /prestige <Typ> setzt die Fabrik bei erfüllten Voraussetzungen zurück, erhöht Prestige und vergibt den Key
 * - PrestigeGuiListener erlaubt klicken im Factory-GUI (Mitte) auf den Prestige-Button
 *
 * Erforderliche Einträge in plugin.yml:
 *
 * commands:
 *   prestige:
 *     description: Prestige a factory
 *     permission: lootfactory.prestige
 *
 * permissions:
 *   lootfactory.prestige:
 *     default: true
 */
public class LootFactoryPlugin extends JavaPlugin {
    private static LootFactoryPlugin instance;

    private Eco eco;
    private FactoryManager factoryManager;

    // Prestige
    private PrestigeManager prestigeManager;
    private KeyItemService keyItemService;
    private FactoryGateway factoryGateway;

    public static LootFactoryPlugin get(){ return instance; }

    @Override public void onEnable(){
        instance = this;

        // Config & Messages
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Economy (Vault)
        eco = new Eco(this);
        if(!eco.hook()) {
            getLogger().severe("Vault economy not found. Install Vault + EconomyPlus!");
        }

        // Factory-Manager
        factoryManager = new FactoryManager(this);
        factoryManager.load();

        // Prestige init
        prestigeManager = new PrestigeManager(this);
        keyItemService = new KeyItemService(this);

        // Adapter auf deine Factory-API (Reflection-basiert, funktioniert mit vielen Varianten)
        factoryGateway = new FactoryGatewayImpl(factoryManager);

        // Events 
        Bukkit.getPluginManager().registerEvents(new BlockListener(factoryManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(factoryManager), this);

        // dein bestehender Klick-Listener (bleibt wie gehabt, Paket meist: com.lootfactory.gui.GUIListener)
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);

        // NEU: Aufräum-Listener für Auto-Refresh
        Bukkit.getPluginManager().registerEvents(new GuiRefreshListener(this), this);

        // Prestige-GUI-Listener
        Bukkit.getPluginManager().registerEvents(new PrestigeGuiListener(this), this);


  

        // Commands
        if (getCommand("factory") != null) {
            getCommand("factory").setExecutor(new FactoryCommand(factoryManager));
        } else {
            getLogger().warning("Command 'factory' nicht in plugin.yml gefunden.");
        }
        if (getCommand("factoryshop") != null) {
            getCommand("factoryshop").setExecutor(new FactoryShopCommand(factoryManager));
        }
        if (getCommand("prestige") != null) {
            getCommand("prestige").setExecutor(new FactoryPrestigeCommand(prestigeManager, keyItemService, factoryGateway));
        } else {
            getLogger().warning("Command 'prestige' nicht in plugin.yml gefunden.");
        }

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.lootfactory.placeholder.LootFactoryPlaceholders(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: %lootfactory_*%");
        } else {
            getLogger().warning("PlaceholderAPI not found. Scoreboard placeholders will be unavailable.");
        }

        // Start Factory-Ticker
        factoryManager.startTicker();
    }

    @Override public void onDisable(){
        if(factoryManager!=null){
            factoryManager.save();
            factoryManager.stopTicker();
        }
    }

    // --- Getter ---
    public Eco eco(){ return eco; }
    public FactoryManager factories() { return factoryManager; }
    public PrestigeManager prestige() { return prestigeManager; }
    public KeyItemService prestigeKeys() { return keyItemService; }
    public FactoryGateway prestigeGateway() { return factoryGateway; }
}
