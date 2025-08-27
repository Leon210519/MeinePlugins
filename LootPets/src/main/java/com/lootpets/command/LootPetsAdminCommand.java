package com.lootpets.command;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PetDefinition;
import com.lootpets.service.BoostBreakdown;
import com.lootpets.service.BoostService;
import com.lootpets.service.PreviewService;
import com.lootpets.service.PetRegistry;
import com.lootpets.service.PetService;
import com.lootpets.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

public class LootPetsAdminCommand implements CommandExecutor {

    private final LootPetsPlugin plugin;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private final BoostService boostService;
    private final PreviewService previewService;

    public LootPetsAdminCommand(LootPetsPlugin plugin, PetService petService, PetRegistry petRegistry, BoostService boostService, PreviewService previewService) {
        this.plugin = plugin;
        this.petService = petService;
        this.petRegistry = petRegistry;
        this.boostService = boostService;
        this.previewService = previewService;
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
            case "calc" -> handleCalc(sender, args);
            case "preview" -> handlePreview(sender, args);
            case "reload" -> handleReload(sender);
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

    private void handleReload(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("reload.enabled", false)) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("reload-disabled")));
            return;
        }
        plugin.reloadEverything();
        sender.sendMessage(Colors.color(plugin.getLang().getString("reloaded")));
    }

    private void handleCalc(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        Optional<EarningType> opt = EarningType.parse(args[2]);
        if (opt.isEmpty()) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("calc-unknown-type")));
            return;
        }
        BoostBreakdown bd = boostService.getBreakdown(target, opt.get());
        sender.sendMessage(Colors.color(plugin.getLang().getString("calc-header")
                .replace("%player%", target.getName())
                .replace("%type%", opt.get().name())
                .replace("%mode%", bd.stackingMode().name())));
        for (BoostBreakdown.PetContribution pc : bd.contributions()) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("calc-entry")
                    .replace("%pet%", pc.petId())
                    .replace("%rarity%", String.valueOf(pc.rarityId()))
                    .replace("%level%", String.valueOf(pc.level()))
                    .replace("%stars%", String.valueOf(pc.stars()))
                    .replace("%weight%", format(pc.weight()))
                    .replace("%factor%", format(pc.typedFactor()))));
        }
        sender.sendMessage(Colors.color(plugin.getLang().getString("calc-uncapped")
                .replace("%uncapped%", format(bd.uncappedResult()))));
        sender.sendMessage(Colors.color(plugin.getLang().getString("calc-final")
                .replace("%final%", format(bd.finalMultiplier()))));
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("preview-usage")));
            return;
        }
        String petInput = args[1];
        String rarityInput = args[2];
        String petId = previewService.resolvePetId(petInput);
        if (petId == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("unknown-pet")));
            return;
        }
        String rarityId = previewService.resolveRarityId(rarityInput);
        if (rarityId == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("unknown-rarity")));
            return;
        }
        String header = plugin.getLang().getString("preview-header", "")
                .replace("%pet%", petId)
                .replace("%rarity%", rarityId);
        sender.sendMessage(Colors.color(header));
        for (var type : previewService.getShowTypes()) {
            PreviewService.Range range = previewService.getRange(petId, rarityId, type);
            String formatted = previewService.formatRange(type, range);
            String raw = previewService.formatRawRange(range);
            String line = plugin.getLang().getString("preview-entry", "{type}: {formatted} ({raw})")
                    .replace("%type%", type.name().toLowerCase(Locale.ROOT))
                    .replace("%formatted%", formatted)
                    .replace("%raw%", raw);
            sender.sendMessage(Colors.color(line));
        }
    }

    private String format(double d) {
        return format(BigDecimal.valueOf(d));
    }

    private String format(BigDecimal bd) {
        return bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
