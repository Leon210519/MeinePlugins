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
import com.lootpets.service.AuditService;
import com.lootpets.service.BackupService;
import com.lootpets.service.RuleService;
import com.lootpets.service.ConfigValidator;
import com.lootpets.service.ConfigValidator.ValidatorResult;
import com.lootpets.service.ConfigValidator.Severity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;

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
import java.util.Arrays;

public class LootPetsAdminCommand implements CommandExecutor {

    private final LootPetsPlugin plugin;
    private final PetService petService;
    private final PetRegistry petRegistry;
    private final BoostService boostService;
    private final PreviewService previewService;
    private final AuditService auditService;
    private final BackupService backupService;
    private final RuleService ruleService;

    public LootPetsAdminCommand(LootPetsPlugin plugin, PetService petService, PetRegistry petRegistry,
                                BoostService boostService, PreviewService previewService,
                                AuditService auditService, BackupService backupService,
                                RuleService ruleService) {
        this.plugin = plugin;
        this.petService = petService;
        this.petRegistry = petRegistry;
        this.boostService = boostService;
        this.previewService = previewService;
        this.auditService = auditService;
        this.backupService = backupService;
        this.ruleService = ruleService;
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
            case "doctor" -> handleDoctor(sender, args);
            case "shards" -> handleShards(sender, args);
            case "renametoken" -> handleRenameToken(sender, args);
            case "suffix" -> handleSuffix(sender, args);
            case "clearsuffix" -> handleClearSuffix(sender, args);
            case "audit" -> handleAudit(sender, args);
            case "backup" -> handleBackup(sender, args);
            case "rules" -> handleRules(sender, args);
            case "storage" -> handleStorage(sender, args);
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

    private void handleShards(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        switch (args[2].toLowerCase()) {
            case "show" -> {
                int amt = petService.getShards(target.getUniqueId());
                sender.sendMessage(Colors.color(plugin.getLang().getString("shard-balance").replace("%amount%", String.valueOf(amt))));
            }
            case "add", "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
                    return;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
                    return;
                }
                amount = Math.max(0, amount);
                if (args[2].equalsIgnoreCase("add")) {
                    petService.addShards(target.getUniqueId(), amount);
                } else {
                    int cur = petService.getShards(target.getUniqueId());
                    petService.addShards(target.getUniqueId(), amount - cur);
                }
                int amt = petService.getShards(target.getUniqueId());
                sender.sendMessage(Colors.color(plugin.getLang().getString("shard-balance").replace("%amount%", String.valueOf(amt))));
            }
            default -> sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
        }
    }

    private void handleRenameToken(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        amount = Math.max(0, amount);
        if (args[2].equalsIgnoreCase("add")) {
            petService.addRenameTokens(target.getUniqueId(), amount);
        } else if (args[2].equalsIgnoreCase("set")) {
            int cur = petService.getRenameTokens(target.getUniqueId());
            petService.addRenameTokens(target.getUniqueId(), amount - cur);
        } else {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        sender.sendMessage(Colors.color("Tokens: " + petService.getRenameTokens(target.getUniqueId())));
    }

    private void handleSuffix(CommandSender sender, String[] args) {
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
        if (!petService.getOwnedPets(target.getUniqueId()).containsKey(petId)) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("unknown-pet")));
            return;
        }
        String suffix = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        if (plugin.getConfig().getBoolean("shards.cosmetics.disallowed_colors_in_suffix", true)) {
            suffix = suffix.replaceAll("(?i)&[0-9A-FK-OR]", "");
        }
        int max = plugin.getConfig().getInt("shards.cosmetics.max_suffix_length", 16);
        if (suffix.length() > max) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("rename-invalid-length").replace("%max%", String.valueOf(max))));
            return;
        }
        petService.setSuffix(target.getUniqueId(), petId, suffix);
        sender.sendMessage(Colors.color(plugin.getLang().getString("rename-success")));
    }

    private void handleClearSuffix(CommandSender sender, String[] args) {
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
        if (!petService.getOwnedPets(target.getUniqueId()).containsKey(petId)) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("unknown-pet")));
            return;
        }
        petService.setSuffix(target.getUniqueId(), petId, null);
        sender.sendMessage(Colors.color(plugin.getLang().getString("rename-cleared")));
    }

    private void handleAudit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        if (mode.equals("on")) {
            auditService.setRuntimeEnabled(true);
        } else if (mode.equals("off")) {
            auditService.setRuntimeEnabled(false);
        } else {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        String msg = plugin.getLang().getString("audit-toggled")
                .replace("%state%", mode);
        sender.sendMessage(Colors.color(msg));
    }

    private void handleBackup(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("now")) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        String file = backupService.backupNow();
        String msg = plugin.getLang().getString("backup-created")
                .replace("%file%", file == null ? "-" : file);
        sender.sendMessage(Colors.color(msg));
    }

    private void handleRules(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("test")) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("player-not-found")));
            return;
        }
        EarningType type = null;
        if (args.length >= 4) {
            Optional<EarningType> opt = EarningType.parse(args[3]);
            if (opt.isEmpty()) {
                sender.sendMessage(Colors.color(plugin.getLang().getString("calc-unknown-type")));
                return;
            }
            type = opt.get();
        }
        String allow = plugin.getLang().getString("rules-allow", "allow");
        String deny = plugin.getLang().getString("rules-deny", "deny");
        String equip = ruleService.canEquip(target) ? allow : deny;
        String level = ruleService.canLevel(target) ? allow : deny;
        String apply = ruleService.canApply(target) ? allow : deny;
        String msg = plugin.getLang().getString("rules-test")
                .replace("%player%", target.getName())
                .replace("%world%", target.getWorld().getName())
                .replace("%type%", type == null ? "-" : type.name())
                .replace("%equip%", equip)
                .replace("%leveling%", level)
                .replace("%boost%", apply);
        sender.sendMessage(Colors.color(msg));
    }

    private void handleReload(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("reload.enabled", false)) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("reload-disabled")));
            return;
        }
        sender.sendMessage(Colors.color("&7Validating configuration..."));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ConfigValidator validator = new ConfigValidator(plugin);
            ValidatorResult vr = validator.validate(false);
            File diag = validator.writeReport(vr);
            if (vr.hasErrors()) {
                sender.sendMessage(Colors.color("&cValidation failed. See " + diag.getName()));
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.reloadEverything();
                sender.sendMessage(Colors.color(plugin.getLang().getString("reloaded")));
            });
        });
    }

    private void handleDoctor(CommandSender sender, String[] args) {
        ConfigValidator validator = new ConfigValidator(plugin);
        if (args.length == 1) {
            ValidatorResult res = validator.validate(false);
            File diag = validator.writeReport(res);
            sender.sendMessage(Colors.color("&7Doctor: " + res.count(Severity.ERROR) + " error(s), " + res.count(Severity.WARN) + " warn(s), " + res.count(Severity.INFO) + " info. Report: " + diag.getName()));
            if (res.hasErrors()) {
                sender.sendMessage(Colors.color("&cValidation failed"));
            }
            return;
        }
        switch (args[1].toLowerCase()) {
            case "fix" -> {
                ValidatorResult res = validator.validate(true);
                File diag = validator.writeReport(res);
                sender.sendMessage(Colors.color("&7Doctor: " + res.count(Severity.ERROR) + " error(s), " + res.count(Severity.WARN) + " warn(s), " + res.count(Severity.INFO) + " info. Report: " + diag.getName()));
            }
            case "players" -> runPlayerDoctor(sender);
            case "export" -> {
                validator.validate(false);
                File out = validator.exportState();
                sender.sendMessage(Colors.color("&7State exported to " + out.getName()));
            }
            default -> sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
        }
    }

    private void runPlayerDoctor(CommandSender sender) {
        File petsFile = new File(plugin.getDataFolder(), "pets.yml");
        if (!petsFile.exists()) {
            sender.sendMessage(Colors.color("&cNo pets.yml found"));
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(petsFile);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) {
            sender.sendMessage(Colors.color("&cNo player data"));
            return;
        }
        sender.sendMessage(Colors.color("&7Player Owned Active Violations"));
        int maxSlots = plugin.getConfig().getInt("default-slots", 1);
        for (String k : players.getKeys(false)) {
            ConfigurationSection sec = players.getConfigurationSection(k);
            if (sec == null) continue;
            ConfigurationSection ownedSec = sec.getConfigurationSection("owned");
            Set<String> owned = ownedSec != null ? ownedSec.getKeys(false) : new HashSet<>();
            List<String> active = sec.getStringList("active");
            int violations = 0;
            for (String a : active) {
                if (!owned.contains(a)) {
                    violations++;
                }
            }
            if (active.size() > maxSlots) {
                violations++;
            }
            sender.sendMessage(Colors.color("&7" + k.substring(0, 8) + "... " + owned.size() + " " + active.size() + " " + violations));
        }
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
    private void handleStorage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "info" -> {
                String info = plugin.getPetService().getStorageInfo();
                sender.sendMessage(Colors.color(plugin.getLang().getString("storage-info").replace("%info%", info)));
            }
            case "flush" -> {
                Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2]) : null;
                plugin.getPetService().flushCommand(target);
                sender.sendMessage(Colors.color(plugin.getLang().getString("storage-flush")));
            }
            case "migrate" -> {
                // For now migration is triggered via verifyStorage placeholder
                plugin.getPetService().verifyStorage();
                sender.sendMessage(Colors.color(plugin.getLang().getString("storage-migrate")));
            }
            case "verify" -> {
                plugin.getPetService().verifyStorage();
                sender.sendMessage(Colors.color(plugin.getLang().getString("storage-verify")));
            }
            default -> sender.sendMessage(Colors.color(plugin.getLang().getString("admin-usage")));
        }
    }
}
