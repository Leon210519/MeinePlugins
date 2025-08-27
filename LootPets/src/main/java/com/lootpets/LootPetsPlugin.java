package com.lootpets;

import com.lootpets.api.LootPetsAPI;
import com.lootpets.command.LootPetsAdminCommand;
import com.lootpets.command.PetsCommand;
import com.lootpets.gui.PetsGUI;
import com.lootpets.listener.EggListener;
import com.lootpets.service.BoostService;
import com.lootpets.service.EggService;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.service.RarityRegistry;
import com.lootpets.service.SlotService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class LootPetsPlugin extends JavaPlugin {

    private FileConfiguration lang;
    private RarityRegistry rarityRegistry;
    private SlotService slotService;
    private PetRegistry petRegistry;
    private PetService petService;
    private PetsGUI petsGUI;
    private EggService eggService;
    private BoostService boostService;
    private int levelTask = -1;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        saveResource("lang.yml", false);
        File petsFile = new File(getDataFolder(), "pets.yml");
        if (!petsFile.exists()) {
            saveResource("pets.yml", false);
        }
        File defsFile = new File(getDataFolder(), "pets_definitions.yml");
        if (!defsFile.exists()) {
            saveResource("pets_definitions.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml"));

        rarityRegistry = new RarityRegistry(this);
        petRegistry = new PetRegistry(this);
        slotService = new SlotService(this);
        petService = new PetService(this);
        eggService = new EggService(this, petService, petRegistry, rarityRegistry);
        boostService = new BoostService(this, petService, petRegistry, rarityRegistry);
        petService.addChangeListener(boostService::invalidate);
        LootPetsAPI.init(boostService);
        petsGUI = new PetsGUI(this, slotService, petService, petRegistry);

        Objects.requireNonNull(getCommand("pets"), "pets command").setExecutor(new PetsCommand(this, petsGUI));
        Objects.requireNonNull(getCommand("lootpets"), "lootpets command").setExecutor(new LootPetsAdminCommand(this, petService, petRegistry, boostService));

        getServer().getPluginManager().registerEvents(new EggListener(this, eggService), this);
        getServer().getPluginManager().registerEvents(petsGUI, this);

        int interval = getConfig().getInt("leveling_runtime.tick_interval_seconds", 30);
        levelTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            int xpPerTick = getConfig().getInt("leveling_runtime.xp_per_tick", 1);
            int xpPerLevel = getConfig().getInt("leveling_runtime.xp_per_level", 60);
            int baseCap = getConfig().getInt("leveling_runtime.level_cap_base", 100);
            int extraCap = getConfig().getInt("leveling_runtime.level_cap_extra_per_star", 50);
            getServer().getOnlinePlayers().forEach(p -> petService.addXpToActivePets(p.getUniqueId(), xpPerTick, xpPerLevel, baseCap, extraCap));
        }, interval * 20L, interval * 20L);

        if (rarityRegistry.isFallback()) {
            getLogger().warning("Registered fallback rarity");
        } else {
            getLogger().info("Imported " + rarityRegistry.size() + " rarities from SpecialItems");
        }
        getLogger().info("Loaded " + petRegistry.size() + " pets from definitions");
        getLogger().info("pets.yml ready");
        getLogger().info("BoostService initialized with mode " + getConfig().getString("boosts.stacking_mode") +
                " and cap " + getConfig().getDouble("caps.global_multiplier_max"));
    }

    @Override
    public void onDisable() {
        if (levelTask != -1) {
            getServer().getScheduler().cancelTask(levelTask);
        }
        if (petService != null) {
            petService.save();
        }
        if (boostService != null) {
            boostService.clearAll();
        }
        LootPetsAPI.shutdown();
    }

    public FileConfiguration getLang() {
        return lang;
    }

    public RarityRegistry getRarityRegistry() {
        return rarityRegistry;
    }

    public SlotService getSlotService() {
        return slotService;
    }

    public PetService getPetService() {
        return petService;
    }

    public PetRegistry getPetRegistry() {
        return petRegistry;
    }

    public EggService getEggService() {
        return eggService;
    }

    public BoostService getBoostService() {
        return boostService;
    }
}
