package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.PetDefinition;
import com.lootpets.model.OwnedPetState;
import com.lootpets.util.Colors;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class EggService {

    private final LootPetsPlugin plugin;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private final RarityRegistry rarityRegistry;

    public EggService(LootPetsPlugin plugin, PetService petService, PetRegistry petRegistry, RarityRegistry rarityRegistry) {
        this.plugin = plugin;
        this.petService = petService;
        this.petRegistry = petRegistry;
        this.rarityRegistry = rarityRegistry;
    }

    public void redeem(Player player, String petId, String rarityId, boolean consumeItem, ItemStack hand) {
        PetDefinition def = petRegistry.byId(petId);
        RarityRegistry.Rarity rarity = rarityRegistry.getRarities().get(rarityId);
        if (def == null || rarity == null) {
            sendMessage(player, plugin.getLang().getString("egg-invalid"));
            return;
        }
        if (petService.addOwnedPet(player.getUniqueId(), petId, rarityId)) {
            if (consumeItem && hand != null) {
                consume(player, hand);
            }
            playSound(player, plugin.getConfig().getString("eggs.sfx.unlocked"));
            sendMessage(player, plugin.getLang().getString("egg-unlocked").replace("%pet%", def.displayName()));
            return;
        }
        Map<String, OwnedPetState> owned = petService.getOwnedPets(player.getUniqueId());
        OwnedPetState state = owned.get(petId);
        int maxStars = plugin.getConfig().getInt("boosts.max_stars", 5);
        if (state != null && state.stars() >= maxStars && plugin.getConfig().getBoolean("shards.enabled", true) && plugin.getConfig().getBoolean("shards.overflow.convert_when_max_stars", true)) {
            int amount = plugin.getConfig().getInt("shards.overflow.amounts_per_rarity." + rarityId,
                    plugin.getConfig().getInt("shards.overflow.default_amount", 1));
            petService.addShards(player.getUniqueId(), amount);
            if (consumeItem && hand != null) {
                consume(player, hand);
            }
            playSound(player, plugin.getConfig().getString("eggs.sfx.duplicate"));
            sendMessage(player, plugin.getLang().getString("shard-gain").replace("%amount%", String.valueOf(amount)));
            return;
        }
        PetService.EvolveResult result = petService.incrementEvolve(player.getUniqueId(), petId);
        if (consumeItem && hand != null) {
            consume(player, hand);
        }
        playSound(player, plugin.getConfig().getString("eggs.sfx.duplicate"));
        if (result.capped()) {
            sendMessage(player, plugin.getLang().getString("egg-max-stars").replace("%pet%", def.displayName()));
        } else if (result.starUp()) {
            sendMessage(player, plugin.getLang().getString("egg-star-up").replace("%pet%", def.displayName()).replace("%stars%", String.valueOf(result.state().stars())));
        } else {
            sendMessage(player, plugin.getLang().getString("egg-duplicate-progress").replace("%pet%", def.displayName()).replace("%progress%", String.valueOf(result.state().evolveProgress())));
        }
    }

    private void consume(Player player, ItemStack hand) {
        if (!plugin.getConfig().getBoolean("eggs.consume_on_redeem", true)) {
            return;
        }
        int amount = hand.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(amount - 1);
        }
    }

    private void playSound(Player player, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void sendMessage(Player player, String msg) {
        if (!plugin.getConfig().getBoolean("eggs.messages.enabled", true)) {
            return;
        }
        player.sendMessage(Colors.color(msg));
    }
}

