package com.farmxmine2.util;

import com.farmxmine2.FarmXMine2Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/** Utility for showing fake blocks to players. */
public final class Visuals {
    private Visuals() {}

    public static void show(FarmXMine2Plugin plugin, Player player, Location loc, Material mat) {
        show(plugin, player, loc, Bukkit.createBlockData(mat));
    }

    public static void show(FarmXMine2Plugin plugin, Player player, Location loc, BlockData data) {
        player.sendBlockChange(loc, data);
        Bukkit.getScheduler().runTask(plugin, () -> player.sendBlockChange(loc, data));
    }
}
