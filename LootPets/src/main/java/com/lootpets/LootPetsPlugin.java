package com.lootpets;

import com.lootpets.api.LootPetsAPI;
import com.lootpets.command.LootPetsAdminCommand;
import com.lootpets.command.PetsCommand;
import com.lootpets.gui.PetsGUI;
import com.lootpets.gui.AlbumGUI;
import com.lootpets.gui.CompareGUI;
import com.lootpets.listener.EggListener;
import com.lootpets.service.BoostService;
import com.lootpets.service.EggService;
import com.lootpets.service.LootPetsExpansion;
import com.lootpets.service.PreviewService;
import com.lootpets.service.PermissionTierService;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.service.RarityRegistry;
import com.lootpets.service.SlotService;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
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
    private AlbumGUI albumGUI;
    private CompareGUI compareGUI;
    private EggService eggService;
    private BoostService boostService;
    private PreviewService previewService;
    private LootPetsExpansion papiExpansion;
    private PermissionTierService permissionTierService;
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
        previewService = new PreviewService(this, petRegistry, rarityRegistry);
        petService.addChangeListener(uuid -> {
            boostService.invalidate(uuid);
            if (papiExpansion != null) {
                papiExpansion.invalidate(uuid);
            }
            if (permissionTierService != null) {
                permissionTierService.invalidate(uuid);
            }
        });
        LootPetsAPI.init(boostService);
        petsGUI = new PetsGUI(this, slotService, petService, petRegistry);
        albumGUI = new AlbumGUI(this, petRegistry, petService);
        compareGUI = new CompareGUI(this, petRegistry, petService);

        Objects.requireNonNull(getCommand("pets"), "pets command").setExecutor(new PetsCommand(this, petsGUI));
        Objects.requireNonNull(getCommand("lootpets"), "lootpets command").setExecutor(new LootPetsAdminCommand(this, petService, petRegistry, boostService, previewService));

        getServer().getPluginManager().registerEvents(new EggListener(this, eggService), this);
        getServer().getPluginManager().registerEvents(petsGUI, this);
        getServer().getPluginManager().registerEvents(albumGUI, this);
        getServer().getPluginManager().registerEvents(compareGUI, this);

        if (getConfig().getBoolean("placeholders.enabled", true)) {
          if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
              papiExpansion = new LootPetsExpansion(this, previewService);
              papiExpansion.register();
            } else {
                getLogger().info("PAPI not found, placeholders disabled");
            }
        }

        ConfigurationSection ptSec = getConfig().getConfigurationSection("permission_tiers");
        if (ptSec != null && ptSec.getBoolean("enabled", false)) {
            RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp != null) {
                permissionTierService = new PermissionTierService(this, boostService, rsp.getProvider(), ptSec);
                permissionTierService.start();
                petService.addChangeListener(permissionTierService::invalidate);
            } else {
                getLogger().info("Vault Permission not found, permission tiers disabled");
            }
        }

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
        getLogger().info("PreviewService initialized with types " + previewService.getShowTypes() +
                " and display cap " + getConfig().getDouble("preview.cap_multiplier", 6.0));
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
        if (previewService != null) {
            previewService.clearAll();
        }
        if (papiExpansion != null) {
            papiExpansion.unregister();
            papiExpansion.clearAll();
        }
        if (permissionTierService != null) {
            permissionTierService.stop();
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

    public PreviewService getPreviewService() {
        return previewService;
    }

    public PetsGUI getPetsGUI() {
        return petsGUI;
    }

    public AlbumGUI getAlbumGUI() {
        return albumGUI;
    }

    public CompareGUI getCompareGUI() {
        return compareGUI;
    }

    public void reloadEverything() {
        reloadConfig();
        lang = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml"));
        rarityRegistry = new RarityRegistry(this);
        petRegistry.reload();
        boostService.clearAll();
        previewService = new PreviewService(this, petRegistry, rarityRegistry);
        previewService.clearAll();
        if (papiExpansion != null) {
            papiExpansion.unregister();
            papiExpansion.clearAll();
            papiExpansion = new LootPetsExpansion(this, previewService);
            papiExpansion.register();
        }
        petsGUI.reset();
        getLogger().info("Reloaded " + rarityRegistry.size() + " rarities and " + petRegistry.size() + " pets");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (previewService != null) {
            previewService.clearAll();
        }
    }
}
