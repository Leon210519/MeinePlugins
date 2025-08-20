package com.focusnpc;

import com.focusnpc.command.FocusNpcCommand;
import com.focusnpc.data.PlayerData;
import com.focusnpc.gui.GuiFactory;
import com.focusnpc.listener.Listeners;
import com.focusnpc.npc.NpcManager;
import com.focusnpc.placeholder.FocusPlaceholders;
import com.focusnpc.transform.BlockTransformer;
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
    private BlockTransformer blockTransformer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.farmingPlaceholder = getConfig().getString("placeholders.farming", "%farmxmine_farming_level%");
        this.miningPlaceholder = getConfig().getString("placeholders.mining", "%farmxmine_mining_level%");
        this.playerData = new PlayerData(this);
        this.npcManager = new NpcManager(this);
        this.guiFactory = new GuiFactory(this);
        this.blockTransformer = new BlockTransformer(this);
        this.blockTransformer.reload();

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
        this.blockTransformer.reload();
    }

    public PlayerData getPlayerData() { return playerData; }
    public GuiFactory getGuiFactory() { return guiFactory; }
    public NpcManager getNpcManager() { return npcManager; }
    public BlockTransformer getBlockTransformer() { return blockTransformer; }

    public int getFarmingLevel(Player player) {
        return getLevel(player, farmingPlaceholder);
    }

    public int getMiningLevel(Player player) {
        return getLevel(player, miningPlaceholder);
    }

    private int getLevel(Player player, String placeholder) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return 0;
        }
        String value = PlaceholderAPI.setPlaceholders(player, placeholder);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
