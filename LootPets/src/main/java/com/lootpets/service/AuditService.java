package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.api.EarningType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous auditing logger for multiplier applications.
 */
public class AuditService {

    private final LootPetsPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    private boolean configEnabled;
    private boolean runtimeEnabled = true;
    private double sampleRate;
    private String logDir;
    private boolean rotateDaily;
    private boolean includeBreakdown;

    public AuditService(LootPetsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        configEnabled = cfg.getBoolean("auditing.enabled", true);
        sampleRate = Math.max(0.0, Math.min(1.0, cfg.getDouble("auditing.sample_rate", 1.0)));
        logDir = cfg.getString("auditing.log_dir", "logs");
        rotateDaily = cfg.getBoolean("auditing.rotate_daily", true);
        includeBreakdown = cfg.getBoolean("auditing.include_breakdown", true);
        runtimeEnabled = true;
    }

    public boolean isEnabled() {
        return configEnabled && runtimeEnabled;
    }

    public boolean isIncludeBreakdown() {
        return includeBreakdown;
    }

    public void setRuntimeEnabled(boolean enabled) {
        this.runtimeEnabled = enabled;
    }

    public void log(Player player, EarningType type, BigDecimal base, double mult, BigDecimal amount, BoostBreakdown breakdown) {
        if (!isEnabled()) {
            return;
        }
        if (random.nextDouble() > sampleRate) {
            return;
        }
        List<BoostBreakdown.PetContribution> list = breakdown == null ? List.of() : breakdown.contributions();
        String bd = "";
        if (includeBreakdown && !list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (BoostBreakdown.PetContribution pc : list) {
                if (sb.length() > 0) sb.append(';').append(' ');
                sb.append(pc.petId()).append(':')
                        .append("typed=").append(String.format(Locale.ROOT, "%.2f", pc.typedFactor()))
                        .append(",L").append(pc.level())
                        .append(",S").append(pc.stars());
            }
            bd = " " + sb;
        }
        String line = String.format(Locale.ROOT, "%s %s %s %s base=%s mult=%.2f amt=%s%s", timestamp(),
                player.getName(), player.getWorld().getName(), type.name(), base.toPlainString(), mult,
                amount.toPlainString(), bd);
        Path dir = new File(plugin.getDataFolder(), logDir).toPath();
        String fileName = rotateDaily ? "audit-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log" : "audit.log";
        Path file = dir.resolve(fileName);
        executor.execute(() -> {
            try {
                Files.createDirectories(dir);
                Files.writeString(file, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write audit log: " + e.getMessage());
            }
        });
    }

    private String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
