package com.lootpets.command;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PetDefinition;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class LootPetsAdminCommand implements CommandExecutor {

    private final LootPetsPlugin plugin;
    private final PetService petService;
    private final PetRegistry petRegistry;

    public LootPetsAdminCommand(LootPetsPlugin plugin, PetService petService, PetRegistry petRegistry) {
        this.plugin = plugin;
        this.petService = petService;
        this.petRegistry = petRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "reset" -> handleReset(sender, args);
            case "simulateegg" -> handleSimulateEgg(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "setstars" -> handleSetStars(sender, args);
            default -> sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        String petId = args[2];
        PetDefinition def = petRegistry.byId(petId);
        if (def == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("unknown-pet")));
            return;
        }
        petService.addOwnedPet(target.getUniqueId(), petId, null);
        String sMsg = plugin.getLang().getString("pet-given-sender");
        String tMsg = plugin.getLang().getString("pet-given-target");
        sender.sendMessage(Colors.color(sMsg).replace("%player%", target.getName()).replace("%pet%", def.displayName()));
        target.sendMessage(Colors.color(tMsg).replace("%pet%", def.displayName()));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        petService.reset(target.getUniqueId());
        String sMsg = plugin.getLang().getString("pets-reset-sender");
        String tMsg = plugin.getLang().getString("pets-reset-target");
        sender.sendMessage(Colors.color(sMsg).replace("%player%", target.getName()));
        target.sendMessage(Colors.color(tMsg));
    }

    private void handleSimulateEgg(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        String petId = args[2];
        String rarityId = args[3];
        PetDefinition def = petRegistry.byId(petId);
        if (def == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("unknown-pet")));
            return;
        }
        if (!plugin.getRarityRegistry().getRarities().containsKey(rarityId)) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("unknown-rarity")));
            return;
        }
        plugin.getEggService().redeem(target, petId, rarityId, false, null);
        sender.sendMessage(Colors.color(plugin.getLang().getString("egg-simulated").replace("%player%", target.getName())));
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        String petId = args[2];
        Map<String, OwnedPetState> owned = petService.getOwnedPets(target.getUniqueId());
        OwnedPetState state = owned.get(petId);
        if (state == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("setlevel-invalid-pet")));
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("setlevel-out-of-range").replace("%max%", "?")));
            return;
        }
        int base = plugin.getConfig().getInt("leveling_runtime.level_cap_base", 100);
        int extra = plugin.getConfig().getInt("leveling_runtime.level_cap_extra_per_star", 50);
        int cap = base + extra * state.stars();
        if (level < 0 || level > cap) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("setlevel-out-of-range").replace("%max%", String.valueOf(cap))));
            return;
        }
        petService.setLevel(target.getUniqueId(), petId, level, base, extra);
        sender.sendMessage(Colors.color(plugin.getLang().getString("setlevel-updated").replace("%pet%", petId).replace("%level%", String.valueOf(level))));
    }

    private void handleSetStars(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        String petId = args[2];
        Map<String, OwnedPetState> owned = petService.getOwnedPets(target.getUniqueId());
        if (!owned.containsKey(petId)) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("setstars-invalid-pet")));
            return;
        }
        int stars;
        try {
            stars = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("setstars-out-of-range")));
            return;
        }
        if (stars < 0 || stars > 5) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("setstars-out-of-range")));
            return;
        }
        petService.setStars(target.getUniqueId(), petId, stars);
        sender.sendMessage(Colors.color(plugin.getLang().getString("setstars-updated").replace("%pet%", petId).replace("%stars%", String.valueOf(stars))));
    }
}
