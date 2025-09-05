package com.lootforge.armorskins;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SASConfig {

    private final SpecialArmorSkins plugin;
    private Material baseItem = Material.CARROT_ON_A_STICK;
    private final Map<ArmorSlot, SlotSettings> slots = new EnumMap<>(ArmorSlot.class);

    public SASConfig(SpecialArmorSkins plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        baseItem = Material.matchMaterial(cfg.getString("display_base_item", "CARROT_ON_A_STICK"));
        slots.clear();
        ConfigurationSection slotsSec = cfg.getConfigurationSection("slots");
        if (slotsSec == null) return;
        for (String key : slotsSec.getKeys(false)) {
            ArmorSlot slot = ArmorSlot.match(key);
            if (slot == null) continue;
            ConfigurationSection slotSec = slotsSec.getConfigurationSection(key);
            if (slotSec == null) continue;
            ConfigurationSection off = slotSec.getConfigurationSection("offset");
            double x=0,y=0,z=0,yaw=0,pitch=0,roll=0,scale=1;
            if (off != null) {
                x = off.getDouble("x");
                y = off.getDouble("y");
                z = off.getDouble("z");
                yaw = off.getDouble("yaw");
                pitch = off.getDouble("pitch");
                roll = off.getDouble("roll");
                scale = off.getDouble("scale",1.0);
            }
            Vector3f translation = new Vector3f((float)x, (float)y, (float)z);
            Quaternionf rotation = new Quaternionf().rotateYXZ(
                    (float)Math.toRadians(yaw),
                    (float)Math.toRadians(pitch),
                    (float)Math.toRadians(roll));
            Vector3f scaling = new Vector3f((float)scale, (float)scale, (float)scale);
            Transformation transform = new Transformation(translation, rotation, scaling, new Quaternionf());
            Map<Integer,String> models = new HashMap<>();
            ConfigurationSection modelSec = slotSec.getConfigurationSection("models");
            if (modelSec != null) {
                for (String mKey : modelSec.getKeys(false)) {
                    try {
                        int cmd = Integer.parseInt(mKey);
                        String modelKey = modelSec.getString(mKey);
                        if (modelKey != null) {
                            models.put(cmd, modelKey);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            slots.put(slot, new SlotSettings(models, transform));
        }
    }

    public Material getBaseItem() {
        return baseItem;
    }

    public SlotSettings getSlotSettings(ArmorSlot slot) {
        return slots.get(slot);
    }

    public static class SlotSettings {
        private final Map<Integer,String> models;
        private final Transformation transformation;
        public SlotSettings(Map<Integer,String> models, Transformation transformation) {
            this.models = models;
            this.transformation = transformation;
        }
        public Map<Integer,String> getModels() { return models; }
        public Transformation getTransformation() { return transformation; }
    }
}
