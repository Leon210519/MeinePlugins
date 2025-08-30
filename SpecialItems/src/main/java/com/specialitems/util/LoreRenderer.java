package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.leveling.LevelMath;
import com.specialitems.leveling.LevelingService;
import com.specialitems.leveling.ToolClass;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Utility to keep item lore in sync with level and XP. */
public final class LoreRenderer {
    private LoreRenderer() {}

    public static boolean updateItemLore(ItemStack item) {
        LevelingService svc = SpecialItemsPlugin.getInstance().leveling();
        if (item == null || svc == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        int level = svc.getLevel(item);
        int xp = svc.getXp(item);
        int needed = (int) LevelMath.neededXpFor(level);
        boolean isHoe = svc.detectToolClass(item) == ToolClass.HOE;
        double bonus = isHoe ? svc.getBonusYieldPct(item) : 0.0;

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        int lvlIdx = indexOfPrefix(lore, "Level:");
        int xpIdx = indexOfPrefix(lore, "XP:");
        int bonusIdx = indexOfPrefix(lore, "Harvest Bonus:");

        String lvlLine = ChatColor.GOLD + "Level: " + ChatColor.YELLOW + level;
        String xpLine = ChatColor.GOLD + "XP: " + ChatColor.YELLOW + String.format("%.1f", (double) xp) + ChatColor.GRAY +
                " / " + ChatColor.YELLOW + String.format("%.1f", (double) needed);
        String bonusLine = ChatColor.GOLD + "Harvest Bonus: " + ChatColor.YELLOW + String.format("%.0f%%", bonus);

        boolean changed = false;
        if (lvlIdx >= 0) {
            if (!lore.get(lvlIdx).equals(lvlLine)) { lore.set(lvlIdx, lvlLine); changed = true; }
        } else {
            lore.add(0, lvlLine);
            changed = true;
            xpIdx++;
            bonusIdx++;
        }

        if (xpIdx >= 0) {
            if (!lore.get(xpIdx).equals(xpLine)) { lore.set(xpIdx, xpLine); changed = true; }
        } else {
            int insert = Math.min(lore.size(), (lvlIdx >= 0 ? lvlIdx + 1 : lore.size()));
            lore.add(insert, xpLine);
            changed = true;
            bonusIdx++;
        }

        if (isHoe) {
            if (bonusIdx >= 0) {
                if (!lore.get(bonusIdx).equals(bonusLine)) { lore.set(bonusIdx, bonusLine); changed = true; }
            } else {
                int insert = Math.min(lore.size(), Math.max(lvlIdx, xpIdx) + 1);
                lore.add(insert, bonusLine);
                changed = true;
            }
        } else if (bonusIdx >= 0) {
            lore.remove(bonusIdx);
            changed = true;
        }

        if (changed) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return changed;
    }

    private static int indexOfPrefix(List<String> lore, String prefix) {
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).startsWith(prefix)) return i;
        }
        return -1;
    }
}
