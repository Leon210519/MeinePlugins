package com.farmxmine.command;

import com.farmxmine.service.ArtifactService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public class ArtifactsCommand implements CommandExecutor {
    private final ArtifactService artifacts;
    
    public ArtifactsCommand(ArtifactService artifacts) {
        this.artifacts = artifacts;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only");
            return true;
        }
        Set<Integer> owned = artifacts.getArtifacts(player);
        int size = ((owned.size() / 9) + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, Math.max(9, Math.min(size, 54)), "Artifacts");
        for (int id : owned) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("Artifact #" + id);
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        player.openInventory(inv);
        player.sendMessage("Total artifacts: " + owned.size() + " Multiplier x" + String.format("%.2f", artifacts.getMultiplier(player)));
        return true;
    }
}
