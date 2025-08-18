package com.lootcrates.command;

import com.lootcrates.LootCratesPlugin;
import com.lootcrates.crate.Crate;
import com.lootcrates.crate.Reward;
import com.lootcrates.util.Color;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GUI {

    public static void preview(Player p, Crate c){
        Inventory inv = Bukkit.createInventory(null, 54, Color.cc("&8Preview: " + c.display));
        int i = 0;
        for (Reward r : c.rewards){
            ItemStack it = r.display.clone();
            ItemMeta m = it.getItemMeta();
            if (m != null){
                List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
                lore.add(Color.cc("&7Weight: &f" + r.weight));
                if ((r.type == Reward.Type.ITEM || r.type == Reward.Type.SPECIAL_ITEM) && r.itemAmount > 1){
                    lore.add(Color.cc("&7Amount: &fx" + r.itemAmount));
                }
                if (r.type == Reward.Type.KEY){
                    lore.add(Color.cc("&7Keys: &fx" + r.keyAmount));
                }
                m.setLore(lore);
                it.setItemMeta(m);
            }
            if (i < 54) inv.setItem(i, it);
            i++;
        }
        p.openInventory(inv);
    }

    /** Try to open a crate by consuming a key in main hand, then roll with 5-slot center line. */
    public static void tryOpenWithKey(LootCratesPlugin plugin, Player p, Crate c){
        ItemStack hand = p.getInventory().getItemInMainHand();
        ItemStack key = c.key.createItem(1);
        if (hand == null || !hand.isSimilar(key)){
            p.sendMessage("§cHold a " + Color.cc(c.display) + "§c key in your main hand.");
            return;
        }
        // consume 1
        hand.setAmount(hand.getAmount()-1);
        p.getInventory().setItemInMainHand(hand.getAmount() > 0 ? hand : null);

        // Open rolling GUI
        Inventory inv = Bukkit.createInventory(null, 27, Color.cc("&8Opening: " + c.display));
        p.openInventory(inv);
        startRoll(plugin, p, inv, c);
    }

    private static void startRoll(LootCratesPlugin plugin, Player p, Inventory inv, Crate c){
        final int[] slots = new int[]{11,12,13,14,15}; // middle row 5 center
        final Random rng = plugin.crates().rng();
        final Reward[] landed = new Reward[1];

        // Fill initial items so the animation has something to scroll through
        for (int s : slots) {
            Reward rr = c.rewards.get(rng.nextInt(c.rewards.size()));
            inv.setItem(s, rr.display.clone());
        }

        new BukkitRunnable(){
            int ticks = 0;
            int gate = 2;   // ticks between shifts
            int wait = 0;

            @Override public void run(){
                wait++;
                if (wait < gate) return;
                wait = 0;
                ticks++;

                // decelerate every second up to a cap
                if (ticks % 20 == 0 && gate < 8) gate++;

                // shift items left (rightmost item moves out, new item enters on the right)
                for (int i = 0; i < slots.length - 1; i++){
                    inv.setItem(slots[i], inv.getItem(slots[i+1]));
                }
                // new random at right
                Reward rr = c.rewards.get(rng.nextInt(c.rewards.size()));
                inv.setItem(slots[slots.length-1], rr.display.clone());

                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

                if (ticks >= 120){ // ~6s total
                    Reward r = c.roll(rng);
                    inv.setItem(slots[2], r.display.clone()); // center
                    landed[0] = r;
                    p.playSound(p.getLocation(), Sound.valueOf(plugin.getConfig().getString("settings.sounds.reward", "ENTITY_EXPERIENCE_ORB_PICKUP")), 1f, 1f);
                    new BukkitRunnable(){ @Override public void run(){
                        CrateOpener.giveReward(plugin, p, c, landed[0]);
                        p.closeInventory();
                        p.sendMessage("§aYou received: §f" + describe(landed[0]));
                    }}.runTaskLater(plugin, 20L);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static String describe(Reward r){
        return switch (r.type){
            case MONEY_XP -> "$" + (int) r.money + (r.xp>0 ? " + " + r.xp + " XP" : "");
            case ITEM, SPECIAL_ITEM -> {
                String name = (r.item.getItemMeta()!=null && r.item.getItemMeta().hasDisplayName()) ? r.item.getItemMeta().getDisplayName() : r.item.getType().name();
                yield (r.itemAmount>1 ? ("x"+r.itemAmount+" ") : "") + name;
            }
            case COMMAND -> "Special Reward";
            case KEY -> r.keyAmount + "x Key (" + r.keyCrate + ")";
        };
    }
}
