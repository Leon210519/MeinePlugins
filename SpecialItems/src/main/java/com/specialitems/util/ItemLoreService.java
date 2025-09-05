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
        lore.add(labelValue(mm, "Rarity", rarityName(rarity)));
        lore.add(labelValue(mm, "Level", String.valueOf(level)));

        String[] p = colorParts(rarity);
        String bar = bar10(rarity, xp, req).replace(p[0], "").replace(p[1], "");
        int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, xp / (double) req)) * 100.0);
        String xpLine = "<gray>XP: </gray><white>" + xp + "/" + req + "</white> "
                + "<dark_gray>[</dark_gray><gradient:#60A5FA:#8B5CF6:#EC4899:#F59E0B:#60A5FA>" + bar
                + "</gradient><dark_gray>]</dark_gray> <white>" + pct + "%</white>";
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

        List<Component> vanillaLines = new ArrayList<>();
        for (var entry : meta.getEnchants().entrySet()) {
            Enchantment ench = entry.getKey();
            int lvl = entry.getValue();
            String name = ItemUtil.prettyEnchantName(ench);
            String roman = ItemUtil.roman(Math.max(0, lvl));
            vanillaLines.add(listEntry(mm, rarity, name + (roman.isEmpty() ? "" : " " + roman)));
        }

        enchLines.sort(Comparator.comparing(c -> plain.serialize(c).toLowerCase()));
        vanillaLines.sort(Comparator.comparing(c -> plain.serialize(c).toLowerCase()));
        if (!enchLines.isEmpty()) {
            lore.add(sectionHeading(mm, rarity, "Special Enchants:"));
            lore.addAll(enchLines);
        }
        if (!vanillaLines.isEmpty()) {
            lore.add(sectionHeading(mm, rarity, "Vanilla Enchants:"));
            lore.addAll(vanillaLines);
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

    private static Component labelValue(MiniMessage mm, String label, String value) {
        String content = "<gray>" + label + ": </gray><white>" + value + "</white>";
        return mm.deserialize(content).decoration(TextDecoration.ITALIC, false);
    }

    private static Component listEntry(MiniMessage mm, Rarity rarity, String text) {
        String[] p = colorParts(rarity);
        String content = "<dark_gray>- </dark_gray>" + p[0] + text + p[1];
        return mm.deserialize(content).decoration(TextDecoration.ITALIC, false);
    }

    private static Component unbreakableLine(MiniMessage mm, Rarity rarity) {
        String[] p = colorParts(rarity);
        return mm.deserialize(p[0] + "Unbreakable" + p[1]).decoration(TextDecoration.ITALIC, false);
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

