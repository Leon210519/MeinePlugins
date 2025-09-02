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
import org.bukkit.enchantments.Enchantment;
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
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

        if (meta.hasDisplayName()) {
            String name = plain.serialize(meta.displayName());
            meta.displayName(styledName(mm, rarity, name).decoration(TextDecoration.ITALIC, false));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(rarityLine(mm, rarity));
        lore.add(rarityText(mm, rarity, "Level: " + level, false));

        String bar = bar10(rarity, xp, req);
        int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, xp / (double) req)) * 100.0);
        lore.add(rarityText(mm, rarity, "XP: " + xp + "/" + req + " [" + bar + "] " + pct + "%", false));

        List<Component> enchLines = new ArrayList<>();
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
            String roman = ItemUtil.roman(Math.max(0, lvl));
            enchLines.add(rarityText(mm, rarity, name + (roman.isEmpty() ? "" : " " + roman), false));
        }

        var svc = plugin instanceof com.specialitems.SpecialItemsPlugin sp ? sp.leveling() : null;
        if (svc != null) {
            var clazz = svc.detectToolClass(item);
            Enchantment ench = switch (clazz) {
                case SWORD, AXE -> Enchantment.SHARPNESS;
                case PICKAXE -> Enchantment.FORTUNE;
                case ARMOR -> Enchantment.PROTECTION;
                default -> null;
            };
            if (ench != null) {
                int lvl = meta.getEnchantLevel(ench);
                String name = ItemUtil.prettyEnchantName(ench);
                String roman = ItemUtil.roman(Math.max(0, lvl));
                enchLines.add(rarityText(mm, rarity, name + (roman.isEmpty() ? "" : " " + roman), false));
            }
        }

        enchLines.sort(Comparator.comparing(c -> plain.serialize(c).toLowerCase()));
        if (!enchLines.isEmpty()) {
            lore.add(rarityText(mm, rarity, "Special Enchants:", true));
            lore.addAll(enchLines);
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

    private static String[] colorParts(Rarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> new String[]{"<color:#55FF55>", "</color>"};
            case RARE -> new String[]{"<color:#55AAFF>", "</color>"};
            case EPIC -> new String[]{"<gradient:#8A2BE2:#C084FC>", "</gradient>"};
            case LEGENDARY -> new String[]{"<gradient:#FFA500:#FF66CC>", "</gradient>"};
            case STARFORGED -> new String[]{"<gradient:#FF4500:#FF0000>", "</gradient>"};
            case COMMON -> new String[]{"<gray>", "</gray>"};
            default -> new String[]{"<gray>", "</gray>"};
        };
    }

    private static Component rarityLine(MiniMessage mm, Rarity rarity) {
        String name = switch (rarity) {
            case STARFORGED -> "StarForged";
            default -> {
                String n = rarity.name().toLowerCase();
                yield Character.toUpperCase(n.charAt(0)) + n.substring(1);
            }
        };
        return rarityText(mm, rarity, name, true);
    }

    private static Component rarityText(MiniMessage mm, Rarity rarity, String text, boolean bold) {
        String[] p = colorParts(rarity);
        String content = bold ? "<bold>" + text + "</bold>" : text;
        return mm.deserialize(p[0] + content + p[1]).decoration(TextDecoration.ITALIC, false);
    }

    private static Component styledName(MiniMessage mm, Rarity rarity, String name) {
        String prefix;
        String suffix;
        switch (rarity) {
            case COMMON -> { prefix = "<gray>"; suffix = "</gray>"; }
            case UNCOMMON -> { prefix = "<color:#55FF55>"; suffix = "</color>"; }
            case RARE -> { prefix = "<color:#55AAFF><bold>"; suffix = "</bold></color>"; }
            case EPIC -> { prefix = "<gradient:#8A2BE2:#C084FC><bold>"; suffix = "</bold></gradient>"; }
            case LEGENDARY -> { prefix = "<gradient:#FFA500:#FF66CC><bold><u>"; suffix = "</u></bold></gradient>"; }
            case STARFORGED -> { prefix = "<gradient:#FF4500:#FF0000><bold><u>"; suffix = "</u></bold></gradient>"; }
            default -> { prefix = "<gray>"; suffix = "</gray>"; }
        }
        return mm.deserialize(prefix + name + suffix).decoration(TextDecoration.ITALIC, false);
    }

    private static String bar10(Rarity rarity, int xp, int req) {
        int r = Math.max(req, 1);
        double pct = Math.max(0, Math.min(1.0, xp / (double) r));
        int filled = (int) Math.round(pct * 10.0);
        String[] p = colorParts(rarity);
        StringBuilder sb = new StringBuilder();
        if (filled > 0) sb.append(p[0]).append("■".repeat(filled)).append(p[1]);
        if (filled < 10) sb.append("<dark_gray>").append("□".repeat(10 - filled)).append("</dark_gray>");
        return sb.toString();
    }
}

