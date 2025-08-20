package com.focusnpc;

import com.focusnpc.command.FocusNpcCommand;
import com.focusnpc.data.PlayerData;
import com.focusnpc.gui.GuiFactory;
import com.focusnpc.listener.Listeners;
import com.focusnpc.npc.NpcManager;
import com.focusnpc.placeholder.FocusPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;

public class FocusNPCPlugin extends JavaPlugin {
    private NpcManager npcManager;
    private PlayerData playerData;
    private GuiFactory guiFactory;
    private String farmingPlaceholder;
    private String miningPlaceholder;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.farmingPlaceholder = getConfig().getString("placeholders.farming", "%farmxmine_farming_level%");
        this.miningPlaceholder = getConfig().getString("placeholders.mining", "%farmxmine_mining_level%");
        this.playerData = new PlayerData(this);
        this.npcManager = new NpcManager(this);
        this.guiFactory = new GuiFactory(this);

        FocusNpcCommand cmd = new FocusNpcCommand(this);
        getCommand("focusnpc").setExecutor(cmd);
        getCommand("focusnpc").setTabCompleter(cmd);

        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new FocusPlaceholders(this).register();
        }

        npcManager.load();
    }

    public void reload() {
        reloadConfig();
        this.farmingPlaceholder = getConfig().getString("placeholders.farming", "%farmxmine_farming_level%");
        this.miningPlaceholder = getConfig().getString("placeholders.mining", "%farmxmine_mining_level%");
        this.playerData.reload();
        this.npcManager.reload();
    }

    public PlayerData getPlayerData() { return playerData; }
    public GuiFactory getGuiFactory() { return guiFactory; }
    public NpcManager getNpcManager() { return npcManager; }

    public int getFarmingLevel(Player player) {
        return getLevel(player, farmingPlaceholder);
    }

    public int getMiningLevel(Player player) {
        return getLevel(player, miningPlaceholder);
    }

    private int getLevel(Player player, String placeholder) {
    // Nur PAPI ist nötig; wenn FarmXMine fehlt oder der Placeholder nichts liefert, gibt’s 0 zurück
    if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return 0;
    try {
        String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
        if (raw == null || raw.isEmpty()) return 0;
        // Nur Ziffern extrahieren (z. B. falls "Level: 123")
        String digits = raw.replaceAll("[^0-9-]", "");
        if (digits.isEmpty()) return 0;
        int lvl = Integer.parseInt(digits);
        return Math.max(0, lvl);
    } catch (Exception ignored) {
        return 0;
    }
}
        String value = PlaceholderAPI.setPlaceholders(player, placeholder);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
