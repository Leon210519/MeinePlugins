package com.lootpets;

import com.lootpets.command.PetsCommand;
import com.lootpets.gui.PetsGUI;
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
    private PetService petService;
    private PetsGUI petsGUI;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        saveResource("lang.yml", false);
        File petsFile = new File(getDataFolder(), "pets.yml");
        if (!petsFile.exists()) {
            saveResource("pets.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml"));

        rarityRegistry = new RarityRegistry(this);
        slotService = new SlotService(this);
        petService = new PetService(this);
        petsGUI = new PetsGUI(this, slotService);

        Objects.requireNonNull(getCommand("pets"), "pets command").setExecutor(new PetsCommand(this, petsGUI));

        if (rarityRegistry.isFallback()) {
            getLogger().warning("Registered fallback rarity");
        } else {
            getLogger().info("Imported " + rarityRegistry.size() + " rarities from SpecialItems");
        }
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
}
