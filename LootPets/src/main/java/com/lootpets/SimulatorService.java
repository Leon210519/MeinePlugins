package com.lootpets;

import org.bukkit.scheduler.BukkitTask;

public class SimulatorService {
    private final LootPetsPlugin plugin;
    private final Object petRegistry;
    private final Object rarityRegistry;
    private BukkitTask task;

    public SimulatorService(LootPetsPlugin plugin, Object petRegistry, Object rarityRegistry) {
        this.plugin = plugin;
        this.petRegistry = petRegistry;
        this.rarityRegistry = rarityRegistry;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    private void tick() {
        // simulation logic placeholder
    }

    public Object getPetRegistry() {
        return petRegistry;
    }

    public Object getRarityRegistry() {
        return rarityRegistry;
    }
}
