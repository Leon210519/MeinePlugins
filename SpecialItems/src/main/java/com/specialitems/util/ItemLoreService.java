package com.specialitems.util;

import com.specialitems.effects.Effects;
import com.specialitems.leveling.Keys;
import com.specialitems.leveling.LevelMath;
import com.specialitems.leveling.Rarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
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
        Rarity rarity = Rarity.fromString(pdc.get(keys.RARITY, PersistentDataType.STRING));

        MiniMessage mm = MiniMessage.miniMessage();
        List<Component> lore = new ArrayList<>();

        lore.add(rarityComponent(mm, rarity).decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<gray>Level: </gray><aqua><bold>" + level + "</bold></aqua>")
                .decoration(TextDecoration.ITALIC, false));

        String bar = bar10(xp, req);
        int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, xp / (double) req)) * 100.0);
        lore.add(mm.deserialize("<gray>XP: </gray><aqua>" + xp + "/" + req + "</aqua> <dark_gray>[</dark_gray><gradient:#60A5FA:#C084FC><bold>"
                + bar + "</bold></gradient><dark_gray>]</dark_gray> <gray>" + pct + "%</gray>")
                .decoration(TextDecoration.ITALIC, false));

        List<Component> enchLines = new ArrayList<>();
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        for (NamespacedKey key : pdc.getKeys()) {
            String k = key.getKey();
            if (k == null || !k.startsWith("ench_")) continue;
            Integer lvl = pdc.get(key, PersistentDataType.INTEGER);
            if (lvl == null) continue;
            String id = k.substring(5);
            String name = id;
            try {
                var ce = Effects.get(id);
                if (ce != null && ce.displayName() != null && !ce.displayName().isEmpty()) {
                    name = ce.displayName();
                }
            } catch (Throwable ignored) {}
            String roman = ItemUtil.roman(Math.max(1, lvl));
            enchLines.add(rarityText(mm, rarity, name + " " + roman, false));
        }
        enchLines.sort(Comparator.comparing(c -> plain.serialize(c).toLowerCase()));
        if (!enchLines.isEmpty()) {
            lore.add(rarityText(mm, rarity, "Special Enchants:", true));
            lore.addAll(enchLines);
        }

        if (meta.isUnbreakable()) {
            lore.add(rarityText(mm, rarity, "Unbreakable", false));
        }

        meta.lore(lore);

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
        String name;
        String prefix;
        String suffix;
        switch (rarity) {
            case UNCOMMON -> { prefix = "<gray>Rarity: </gray><color:#55FF55><bold>"; suffix = "</bold></color>"; name = "Uncommon"; }
            case RARE -> { prefix = "<gray>Rarity: </gray><color:#55AAFF><bold>"; suffix = "</bold></color>"; name = "Rare"; }
            case EPIC -> { prefix = "<gray>Rarity: </gray><gradient:#8A2BE2:#C084FC><bold>"; suffix = "</bold></gradient>"; name = "Epic"; }
            case LEGENDARY -> { prefix = "<gray>Rarity: </gray><gradient:#FFA500:#FF66CC><bold>"; suffix = "</bold></gradient>"; name = "Legendary"; }
            case STARFORGED -> { prefix = "<gray>Rarity: </gray><gradient:#FF4500:#FF0000><bold>"; suffix = "</bold></gradient>"; name = "StarForged"; }
            case COMMON -> { prefix = "<gray>Rarity: </gray><gray><bold>"; suffix = "</bold></gray>"; name = "Common"; }
            default -> { prefix = "<gray>Rarity: </gray><gray><bold>"; suffix = "</bold></gray>"; name = "Common"; }
        }
        return mm.deserialize(prefix + name + suffix).decoration(TextDecoration.ITALIC, false);
    }

    private static Component rarityText(MiniMessage mm, Rarity rarity, String text, boolean bold) {
        String prefix;
        String suffix;
        switch (rarity) {
            case UNCOMMON -> { prefix = "<color:#55FF55>"; suffix = "</color>"; }
            case RARE -> { prefix = "<color:#55AAFF>"; suffix = "</color>"; }
            case EPIC -> { prefix = "<gradient:#8A2BE2:#C084FC>"; suffix = "</gradient>"; }
            case LEGENDARY -> { prefix = "<gradient:#FFA500:#FF66CC>"; suffix = "</gradient>"; }
            case STARFORGED -> { prefix = "<gradient:#FF4500:#FF0000>"; suffix = "</gradient>"; }
            case COMMON -> { prefix = "<gray>"; suffix = "</gray>"; }
            default -> { prefix = "<gray>"; suffix = "</gray>"; }
        }
        String content = bold ? "<bold>" + text + "</bold>" : text;
        return mm.deserialize(prefix + content + suffix).decoration(TextDecoration.ITALIC, false);
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

