package com.specialitems.util;

import com.specialitems.SpecialItemsPlugin;
import com.specialitems.effects.Effects;
import com.specialitems.leveling.Keys;
import com.specialitems.leveling.LevelMath;
import com.specialitems.leveling.Rarity;
import com.specialitems.leveling.ToolClass;
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
            meta.displayName(
                    styledName(mm, rarity, name)
                            .decoration(TextDecoration.ITALIC, false)
                            .decoration(TextDecoration.UNDERLINED, false));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(labelValue(mm, rarity, "Rarity", rarityName(rarity)));
        lore.add(labelValue(mm, rarity, "Level", String.valueOf(level)));

        String bar = bar10(rarity, xp, req);
        int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, xp / (double) req)) * 100.0);
        String[] label = accentParts(rarity);
        String[] value = colorParts(rarity);
        String xpLine = label[0] + "XP" + label[1]
                + "<dark_gray>: </dark_gray>" + value[0] + xp + value[1]
                + "<dark_gray>/</dark_gray>" + value[0] + req + value[1]
                + " <dark_gray>[</dark_gray>" + bar + "<dark_gray>]</dark_gray> "
                + label[0] + pct + "%" + label[1];
        lore.add(mm.deserialize(xpLine).decoration(TextDecoration.ITALIC, false));

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
            enchLines.add(listEntry(mm, rarity, name + (roman.isEmpty() ? "" : " " + roman)));
        }

        enchLines.sort(Comparator.comparing(c -> plain.serialize(c).toLowerCase()));
        lore.add(sectionHeading(mm, rarity, "Special Enchants:"));
        lore.addAll(enchLines);

        ToolClass tc = SpecialItemsPlugin.getInstance().leveling().detectToolClass(item);
        if (tc == ToolClass.HOE) {
            double bonus = pdc.getOrDefault(keys.BONUS_YIELD_PCT, PersistentDataType.DOUBLE, 0.0);
            lore.add(labelValue(mm, rarity, "Bonus Yield", "+" + String.format("%.0f%%", bonus)));
        } else {
            var entry = meta.getEnchants().entrySet().stream().findFirst();
            if (entry.isPresent()) {
                Enchantment ench = entry.get().getKey();
                int lvl = entry.get().getValue();
                String name = ItemUtil.prettyEnchantName(ench);
                String roman = ItemUtil.roman(Math.max(0, lvl));
                String val = name + (roman.isEmpty() ? "" : " " + roman);
                lore.add(labelValue(mm, rarity, "Enchantment", val));
            }
        }

        if (meta.isUnbreakable()) {
            lore.add(unbreakableLine(mm, rarity));
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

    private static String rarityName(Rarity rarity) {
        return switch (rarity) {
            case STARFORGED -> "StarForged";
            default -> {
                String n = rarity.name().toLowerCase();
                yield Character.toUpperCase(n.charAt(0)) + n.substring(1);
            }
        };
    }

    private static String[] accentParts(Rarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> new String[]{"<gradient:#059669:#10B981>", "</gradient>"};
            case RARE -> new String[]{"<gradient:#2563EB:#3B82F6>", "</gradient>"};
            case EPIC -> new String[]{"<gradient:#7C3AED:#A855F7>", "</gradient>"};
            case LEGENDARY -> new String[]{"<gradient:#F59E0B:#FDE68A>", "</gradient>"};
            case STARFORGED -> new String[]{"<gradient:#FF4D4D:#FF9999>", "</gradient>"};
            case COMMON -> new String[]{"<gradient:#6B7280:#9CA3AF>", "</gradient>"};
            default -> new String[]{"<gradient:#6B7280:#9CA3AF>", "</gradient>"};
        };
    }

    private static Component sectionHeading(MiniMessage mm, Rarity rarity, String text) {
        String[] g = accentParts(rarity);
        String content = "<bold>" + text + "</bold>";
        return mm.deserialize(g[0] + content + g[1]).decoration(TextDecoration.ITALIC, false);
    }

    private static Component labelValue(MiniMessage mm, Rarity rarity, String label, String value) {
        String[] l = accentParts(rarity);
        String[] v = colorParts(rarity);
        String content = l[0] + label + l[1] + "<dark_gray>: </dark_gray>" + v[0] + value + v[1];
        return mm.deserialize(content).decoration(TextDecoration.ITALIC, false);
    }

    private static Component listEntry(MiniMessage mm, Rarity rarity, String text) {
        String[] p = colorParts(rarity);
        String content = "<dark_gray>- </dark_gray>" + p[0] + text + p[1];
        return mm.deserialize(content).decoration(TextDecoration.ITALIC, false);
    }

    private static Component unbreakableLine(MiniMessage mm, Rarity rarity) {
        String[] g = accentParts(rarity);
        return mm.deserialize(g[0] + "Unbreakable" + g[1]).decoration(TextDecoration.ITALIC, false);
    }

    private static Component styledName(MiniMessage mm, Rarity rarity, String name) {
        String prefix;
        String suffix;
        switch (rarity) {
            case COMMON -> { prefix = "<gray>"; suffix = "</gray>"; }
            case UNCOMMON -> { prefix = "<color:#55FF55>"; suffix = "</color>"; }
            case RARE -> { prefix = "<color:#55AAFF><bold>"; suffix = "</bold></color>"; }
            case EPIC -> { prefix = "<gradient:#8A2BE2:#C084FC><bold>"; suffix = "</bold></gradient>"; }
            case LEGENDARY -> { prefix = "<gradient:#FFA500:#FF66CC><bold>"; suffix = "</bold></gradient>"; }
            case STARFORGED -> { prefix = "<gradient:#FF4500:#FF0000><bold>"; suffix = "</bold></gradient>"; }
            default -> { prefix = "<gray>"; suffix = "</gray>"; }
        }
        return mm.deserialize(prefix + name + suffix)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.UNDERLINED, false);
    }

    private static String bar10(Rarity rarity, int xp, int req) {
        int r = Math.max(req, 1);
        double pct = Math.max(0, Math.min(1.0, xp / (double) r));
        int filled = (int) Math.round(pct * 10.0);
        StringBuilder sb = new StringBuilder();
        if (filled > 0) sb.append("<gradient:#0EA5E9:#9333EA:#F472B6>")
                .append("■".repeat(filled))
                .append("</gradient>");
        if (filled < 10) sb.append("<dark_gray>").append("□".repeat(10 - filled)).append("</dark_gray>");
        return sb.toString();
    }
}

