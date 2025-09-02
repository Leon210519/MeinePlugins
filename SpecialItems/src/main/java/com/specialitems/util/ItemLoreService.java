package com.specialitems.util;

import com.specialitems.leveling.Keys;
import com.specialitems.leveling.LevelMath;
import com.specialitems.leveling.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
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

        int level = pdc.getOrDefault(keys.LEVEL, PersistentDataType.INTEGER, 1);
        int xp = pdc.getOrDefault(keys.XP, PersistentDataType.INTEGER, 0);
        int req = Math.max(1, (int) LevelMath.neededXpFor(level));
        String rarityRaw = pdc.get(keys.RARITY, PersistentDataType.STRING);
        Rarity rarity = Rarity.fromString(rarityRaw);

        List<Component> existing = meta.lore();
        List<Component> tail = new ArrayList<>();
        if (existing != null) {
            PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
            boolean keep = false;
            for (Component c : existing) {
                String text = plain.serialize(c).trim();
                if (text.isEmpty()) {
                    if (keep) tail.add(c);
                    continue;
                }
                if (text.startsWith("✦") || text.startsWith("Rarity:") || text.startsWith("Level:") || text.startsWith("XP:")) {
                    continue;
                }
                keep = true;
                tail.add(c);
            }
        }

        MiniMessage mm = MiniMessage.miniMessage();

        Component rarityLine = rarityComponent(mm, rarity).decoration(TextDecoration.ITALIC, false);
        Component levelLine = mm.deserialize("<gray>Level: </gray><aqua><bold>" + level + "</bold></aqua>")
                .decoration(TextDecoration.ITALIC, false);

        String bar = bar10(xp, req);
        int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, xp / (double) req)) * 100.0);
        Component xpLine = mm.deserialize(
                "<gray>XP: </gray><aqua>" + xp + "/" + req + "</aqua> <dark_gray>[</dark_gray><gradient:#60A5FA:#C084FC><bold>" +
                        bar + "</bold></gradient><dark_gray>]</dark_gray> <gray>" + pct + "%</gray>")
                .decoration(TextDecoration.ITALIC, false);

        List<Component> newLore = new ArrayList<>();
        newLore.add(rarityLine);
        newLore.add(levelLine);
        newLore.add(xpLine);
        newLore.addAll(tail);
        meta.lore(newLore);

        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_ARMOR_TRIM,
                ItemFlag.HIDE_DYE
        );
        try {
            meta.setEnchantmentGlintOverride(null);
        } catch (Throwable ignored) {}

        item.setItemMeta(meta);
    }

    private static Component rarityComponent(MiniMessage mm, Rarity rarity) {
        String text;
        switch (rarity) {
            case UNCOMMON -> text = "<bold><color:#55FF55>✦ Uncommon ✦</color></bold>";
            case RARE -> text = "<bold><color:#55AAFF>✦ Rare ✦</color></bold>";
            case EPIC -> text = "<bold><gradient:#8A2BE2:#C084FC>✦ Epic ✦</gradient></bold>";
            case LEGENDARY -> text = "<bold><gradient:#FFA500:#FF66CC>✦ Legendary ✦</gradient></bold>";
            case STARFORGED -> text = "<bold><gradient:#FF4500:#FF0000>✦ StarForged ✦</gradient></bold>";
            case COMMON -> text = "<gray><bold>✦ Common ✦</bold>";
            default -> text = "<gray><bold>✦ Common ✦</bold>";
        }
        return mm.deserialize(text);
    }

    private static String bar10(int xp, int req) {
        int r = Math.max(req, 1);
        double pct = Math.max(0, Math.min(1.0, xp / (double) r));
        int filled = (int) Math.round(pct * 10.0);
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(i < filled ? '■' : '□');
        return sb.toString();
    }
}

