package com.specialitems.bin;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Bin {
    private static final List<ItemStack> ITEMS = new ArrayList<>();

    private Bin() {}

    public static void store(ItemStack it) {
        if (it == null || it.getType().isAir()) return;
        ITEMS.add(it.clone());
    }

    public static List<ItemStack> getItems() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static ItemStack take(int index) {
        return (index >= 0 && index < ITEMS.size()) ? ITEMS.remove(index) : null;
    }
}
