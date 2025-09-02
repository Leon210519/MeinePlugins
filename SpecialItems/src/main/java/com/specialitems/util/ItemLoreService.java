package com.specialitems.util;

import com.specialitems.leveling.Keys;
import com.specialitems.leveling.LevelMath;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Service for rendering level/XP lore lines on special items. */
public final class ItemLoreService {
    private ItemLoreService() {}

    public static void renderLore(ItemStack item, Plugin plugin) {
        if (item == null || plugin == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Keys keys = new Keys(plugin);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc == null || !pdc.has(keys.SI_ID, PersistentDataType.STRING)) {
            return; // not a special item
        }

        Integer lvl = pdc.get(keys.LEVEL, PersistentDataType.INTEGER);
        int level = (lvl == null ? 1 : lvl);
        Integer xpVal = pdc.get(keys.XP, PersistentDataType.INTEGER);
        int xp = (xpVal == null ? 0 : xpVal);
        int req = (int) LevelMath.neededXpFor(level);

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        Iterator<Component> it = lore.iterator();
        while (it.hasNext()) {
            String text = plain.serialize(it.next());
            if (text.startsWith("Level:") || text.startsWith("XP:")) {
                it.remove();
            }
        }

        double ratio = req > 0 ? (double) xp / (double) req : 0.0;
        int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, ratio)) * 10.0);
        StringBuilder bar = new StringBuilder(10);
        for (int i = 0; i < filled; i++) bar.append('■');
        for (int i = filled; i < 10; i++) bar.append('□');

        Component levelLine = Component.text("Level: ", NamedTextColor.GRAY)
                .append(Component.text(level, NamedTextColor.AQUA));
        Component xpLine = Component.text("XP: ", NamedTextColor.GRAY)
                .append(Component.text(xp + "/" + req + " ", NamedTextColor.AQUA))
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(bar.toString(), NamedTextColor.AQUA))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));

        lore.add(0, levelLine);
        lore.add(1, xpLine);
        meta.lore(lore);

        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_ARMOR_TRIM
        );

        item.setItemMeta(meta);
    }
}

