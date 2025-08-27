package com.specialitems.gui;

import com.specialitems.util.TemplateItems;
import com.specialitems.util.Configs;
import com.specialitems.util.GuiItemUtil;
import com.specialitems.SpecialItemsPlugin;
import com.specialitems.leveling.Keys;
import com.specialitems.leveling.Rarity;
import com.specialitems.leveling.RarityUtil;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class TemplateGUI {

    public static final String TITLE = ChatColor.AQUA + "SpecialItems Templates";
    private static final int ROWS = 6;

    private static final Rarity[] ORDER = {
            Rarity.COMMON,
            Rarity.UNCOMMON,
            Rarity.RARE,
            Rarity.EPIC,
            Rarity.LEGENDARY,
            Rarity.STARFORGED
    };

    static int slotOf(int rowIndex, int colIndex) {
        return rowIndex * 9 + colIndex;
    }

    static int rowForRarity(Rarity r) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == r) {
                return ORDER.length - 1 - i;
            }
        }
        return ORDER.length - 1;
    }
    private static ItemStack tagWithTemplateId(ItemStack stack, String templateId) {
    if (stack == null || templateId == null) return stack;
    ItemStack copy = stack.clone();
    ItemMeta meta = copy.getItemMeta();
    if (meta != null) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(SpecialItemsPlugin.getInstance(), "si_template_id");
        pdc.set(key, PersistentDataType.STRING, templateId);
        copy.setItemMeta(meta);
    }
    return copy;
    }
    public static void open(Player p, int page) {
        if (!p.hasPermission("specialitems.admin")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', Configs.msg.getString("no-permission","&cNo permission.")));
            return;
        }
        List<TemplateItems.TemplateItem> all = TemplateItems.getAll();
        Keys keys = new Keys(SpecialItemsPlugin.getInstance());

        Map<Rarity, List<TemplateItems.TemplateItem>> byRarity = new EnumMap<>(Rarity.class);
        for (TemplateItems.TemplateItem t : all) {
            Rarity r = RarityUtil.get(t.stack(), keys);
            byRarity.computeIfAbsent(r, k -> new ArrayList<>()).add(t);
        }
        for (List<TemplateItems.TemplateItem> list : byRarity.values()) {
            list.sort(Comparator.comparing(t -> typeOrder(t.stack().getType())));
        }

        List<Rarity> rarityList = Arrays.asList(ORDER);
        int itemRows = ROWS;
        int pages = Math.max(1, (int) Math.ceil(rarityList.size() / (double) itemRows));
        if (page < 0) page = 0;
        if (page >= pages) page = pages - 1;
        Inventory inv = Bukkit.createInventory(p, ROWS * 9,
                TITLE + " §7(" + (page + 1) + "/" + pages + ")");

        inv.setItem(slotOf(0, 8), GuiIcons.navNext(page < pages - 1));
        inv.setItem(slotOf(1, 8), GuiIcons.navPrev(page > 0));
        inv.setItem(slotOf(2, 8), GuiIcons.navClose());
        inv.setItem(slotOf(3, 8), GuiIcons.navFiller());
        inv.setItem(slotOf(4, 8), GuiIcons.navFiller());
        inv.setItem(slotOf(5, 8), GuiIcons.navFiller());

        int start = page * itemRows;
        int end = Math.min(rarityList.size(), start + itemRows);
        for (int rIndex = start; rIndex < end; rIndex++) {
            Rarity rarity = rarityList.get(rIndex);
            int rowIndex = rowForRarity(rarity) - start;
            List<TemplateItems.TemplateItem> items = byRarity.getOrDefault(rarity, Collections.emptyList());

            for (TemplateItems.TemplateItem t : items) {
                int col = typeOrder(t.stack().getType());
                if (col < 0 || col > 7) continue;
                ItemStack display = GuiItemUtil.forDisplay(SpecialItemsPlugin.getInstance(), t.stack());
                if (display == null) display = t.stack().clone();
                // <<NEU: Template-ID im GUI-Item hinterlegen>>
                display = tagWithTemplateId(display, t.id());
                inv.setItem(slotOf(rowIndex, col), display);
            }
        }

        p.openInventory(inv);
    }

    private static int typeOrder(Material m) {
        String name = m.name();
        if (name.endsWith("_HELMET")) return 0;
        if (name.endsWith("_CHESTPLATE")) return 1;
        if (name.endsWith("_LEGGINGS")) return 2;
        if (name.endsWith("_BOOTS")) return 3;
        if (name.endsWith("_SWORD")) return 4;
        if (name.endsWith("_PICKAXE")) return 5;
        if (name.endsWith("_AXE")) return 6;
        if (name.endsWith("_HOE")) return 7;
        return 100;
    }
}
