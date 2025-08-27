package com.lootpets.command;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.PetDefinition;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        petService.addOwnedPet(target.getUniqueId(), petId);
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
}
