package com.specialitems.effects.impl;

import com.specialitems.effects.CustomEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Telekinesis implements CustomEffect {
    @Override public String id() { return "telekinesis"; }
    @Override public String displayName() { return "Telekinesis"; }
    @Override public int maxLevel() { return 1; }
    @Override public boolean supports(Material type) { return true; }

    @Override
    public void onBlockBreak(Player player, ItemStack tool, BlockBreakEvent e, int level) {
        e.setDropItems(false);
        boolean smelt = com.specialitems.util.ItemUtil.getEffectLevel(tool, "autosmelt") > 0;
        Collection<ItemStack> vanilla = e.getBlock().getDrops(tool);
        for (ItemStack drop : vanilla) {
            ItemStack give = drop.clone();
            if (smelt) {
                var out = AutoSmelt.SMELTS.get(drop.getType());
                if (out != null) give.setType(out);
            }
            player.getInventory().addItem(give);
        }
    }

    @Override
    public void onEntityKill(Player player, ItemStack weapon, EntityDeathEvent e, int level) {
        List<ItemStack> copy = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        for (ItemStack drop : copy) player.getInventory().addItem(drop);
    }
}
