package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility responsible for loading and validating all configuration files. The
 * validator is deliberately conservative – it never throws and always returns a
 * structured {@link ValidatorResult}. Callers may choose to run in "fix" mode
 * which applies trivial local corrections and writes the files back with a
 * timestamped <code>.bak</code> backup.
 */
public class ConfigValidator {

    public enum Severity { ERROR, WARN, INFO }

    public record Issue(String file, String path, Severity severity, String message) {}

    public static class ValidatorResult {
        private final List<Issue> issues = new ArrayList<>();
        public void add(Issue i) { issues.add(i); }
        public boolean hasErrors() {
            return issues.stream().anyMatch(i -> i.severity == Severity.ERROR);
        }
        public long count(Severity s) {
            return issues.stream().filter(i -> i.severity == s).count();
        }
        public List<Issue> issues() { return Collections.unmodifiableList(issues); }
    }

    private final LootPetsPlugin plugin;
    private YamlConfiguration config;
    private YamlConfiguration lang;
    private YamlConfiguration gui;
    private YamlConfiguration defs;

    private static final Pattern PET_ID = Pattern.compile("^[A-Z0-9_]{1,32}$");

    public ConfigValidator(LootPetsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Run validation for all supported files. If {@code applyFixes} is true the
     * validator may correct a limited set of simple mistakes (e.g. negative
     * numbers) and write the files back with backups.
     */
    public ValidatorResult validate(boolean applyFixes) {
        ValidatorResult result = new ValidatorResult();
        config = load("config.yml", result);
        validateConfig(result, applyFixes);
        lang = load("lang.yml", result);
        validateLang(result, applyFixes);
        gui = load("gui.yml", result);
        validateGui(result, applyFixes);
        defs = load("pets_definitions.yml", result);
        validateDefs(result, applyFixes);
        return result;
    }

    private YamlConfiguration load(String name, ValidatorResult res) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) {
            res.add(new Issue(name, "/", Severity.ERROR, "file not found"));
            return new YamlConfiguration();
        }
        try {
            return YamlConfiguration.loadConfiguration(f);
        } catch (Exception e) {
            res.add(new Issue(name, "/", Severity.ERROR, "could not parse: " + e.getMessage()));
            return new YamlConfiguration();
        }
    }

    private void validateConfig(ValidatorResult res, boolean fix) {
        if (config == null) return;
        File file = new File(plugin.getDataFolder(), "config.yml");
        boolean dirty = false;
        int defSlots = config.getInt("default-slots", -1);
        if (defSlots < 0) {
            res.add(new Issue("config.yml", "default-slots", Severity.ERROR, "must be >=0"));
            if (fix) { config.set("default-slots", Math.max(0, defSlots)); dirty = true; }
        }
        String perm = config.getString("slot-permission-prefix", "");
        if (perm == null || perm.isEmpty()) {
            res.add(new Issue("config.yml", "slot-permission-prefix", Severity.ERROR, "must not be empty"));
        }
        double cap = config.getDouble("caps.global_multiplier_max", 0d);
        if (cap <= 1.0) {
            res.add(new Issue("config.yml", "caps.global_multiplier_max", Severity.WARN, "value should be >1.0"));
        }
        ConfigurationSection xsv = config.getConfigurationSection("cross_server");
        if (xsv != null) {
            boolean enabled = xsv.getBoolean("enabled", false);
            String provider = config.getString("storage.provider", "YAML").toUpperCase(Locale.ROOT);
            if (enabled && provider.equals("YAML")) {
                res.add(new Issue("config.yml", "cross_server.enabled", Severity.INFO, "cross-server requires SQL storage"));
            }
        }
        if (fix && dirty) {
            saveWithBackup(config, file);
        }
    }

    private void validateLang(ValidatorResult res, boolean fix) {
        if (lang == null) return;
        File file = new File(plugin.getDataFolder(), "lang.yml");
        boolean dirty = false;
        for (String key : Arrays.asList("admin-usage", "unknown-pet", "reloaded")) {
            if (!lang.isString(key)) {
                res.add(new Issue("lang.yml", key, Severity.ERROR, "missing or not a string"));
            }
        }
        // strip illegal color codes (anything with section sign followed by non [0-9A-FK-OR])
        if (fix) {
            for (String key : lang.getKeys(true)) {
                Object val = lang.get(key);
                if (val instanceof String s) {
                    String cleaned = s.replaceAll("(?i)§[^0-9A-FK-OR]", "");
                    if (!cleaned.equals(s)) {
                        lang.set(key, cleaned);
                        dirty = true;
                    }
                }
            }
        }
        if (fix && dirty) {
            saveWithBackup(lang, file);
        }
    }

    private void validateGui(ValidatorResult res, boolean fix) {
        if (gui == null) return;
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!gui.isString("title") || gui.getString("title", "").isEmpty()) {
            res.add(new Issue("gui.yml", "title", Severity.ERROR, "title missing or empty"));
        }
        int rows = gui.getInt("rows", 0);
        if (rows < 1 || rows > 6) {
            res.add(new Issue("gui.yml", "rows", Severity.ERROR, "rows must be between 1 and 6"));
        }
        boolean dirty = false;
        if (fix) {
            // dedupe slot lists and clamp
            int maxSlot = rows * 9;
            for (String key : gui.getKeys(true)) {
                Object val = gui.get(key);
                if (val instanceof List<?> list) {
                    List<Integer> ints = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof Number n) {
                            int v = n.intValue();
                            if (v < 0) v = 0;
                            if (v >= maxSlot) v = maxSlot - 1;
                            if (!ints.contains(v)) {
                                ints.add(v);
                            } else {
                                dirty = true;
                            }
                        }
                    }
                    gui.set(key, ints);
                }
            }
        }
        if (fix && dirty) {
            saveWithBackup(gui, file);
        }
    }

    private void validateDefs(ValidatorResult res, boolean fix) {
        if (defs == null) return;
        File file = new File(plugin.getDataFolder(), "pets_definitions.yml");
        boolean dirty = false;
        ConfigurationSection petsSec = defs.getConfigurationSection("pets");
        if (petsSec == null) {
            res.add(new Issue("pets_definitions.yml", "pets", Severity.ERROR, "no pets defined"));
            return;
        }
        Set<Integer> usedCmd = new HashSet<>();
        for (String id : petsSec.getKeys(false)) {
            if (!PET_ID.matcher(id).matches()) {
                res.add(new Issue("pets_definitions.yml", "pets." + id, Severity.ERROR, "invalid id"));
            }
            ConfigurationSection sec = petsSec.getConfigurationSection(id);
            if (sec == null) continue;
            String dn = sec.getString("display_name", "");
            if (dn.isEmpty()) {
                res.add(new Issue("pets_definitions.yml", "pets." + id + ".display_name", Severity.ERROR, "display_name empty"));
            }
            ConfigurationSection icon = sec.getConfigurationSection("icon");
            if (icon == null) {
                res.add(new Issue("pets_definitions.yml", "pets." + id + ".icon", Severity.ERROR, "icon missing"));
            } else {
                String matName = icon.getString("material");
                Material mat = matName == null ? null : Material.matchMaterial(matName);
                if (mat == null) {
                    res.add(new Issue("pets_definitions.yml", "pets." + id + ".icon.material", Severity.ERROR, "invalid material"));
                }
                if (icon.contains("custom_model_data")) {
                    int cmd = icon.getInt("custom_model_data");
                    if (cmd < 0) {
                        res.add(new Issue("pets_definitions.yml", "pets." + id + ".icon.custom_model_data", Severity.ERROR, "must be >=0"));
                        if (fix) {
                            icon.set("custom_model_data", 0);
                            dirty = true;
                        }
                    }
                    if (!usedCmd.add(cmd)) {
                        res.add(new Issue("pets_definitions.yml", "pets." + id + ".icon.custom_model_data", Severity.WARN, "duplicate custom_model_data"));
                    }
                }
            }
            ConfigurationSection weights = sec.getConfigurationSection("weights");
            if (weights != null) {
                for (String k : weights.getKeys(false)) {
                    double v = weights.getDouble(k);
                    if (v < 0 || v > 1) {
                        res.add(new Issue("pets_definitions.yml", "pets." + id + ".weights." + k, Severity.ERROR, "out of range"));
                        if (fix) {
                            double nv = Math.min(1, Math.max(0, v));
                            weights.set(k, nv);
                            dirty = true;
                        }
                    }
                }
            }
            for (String k : sec.getKeys(false)) {
                if (!Arrays.asList("display_name", "icon", "weights").contains(k)) {
                    res.add(new Issue("pets_definitions.yml", "pets." + id + "." + k, Severity.WARN, "unknown key"));
                }
            }
        }
        if (fix && dirty) {
            saveWithBackup(defs, file);
        }
    }

    public File writeReport(ValidatorResult result) {
        File dir = new File(plugin.getDataFolder(), "diagnostics");
        dir.mkdirs();
        File file = new File(dir, "validator-" + timestamp() + ".txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (Issue i : result.issues()) {
                pw.println(i.severity + " " + i.file + " " + i.path + " - " + i.message);
            }
            pw.println("Summary: " + result.count(Severity.ERROR) + " ERROR, " +
                    result.count(Severity.WARN) + " WARN, " +
                    result.count(Severity.INFO) + " INFO");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write diagnostics: " + e.getMessage());
        }
        return file;
    }

    public File exportState() {
        File dir = new File(plugin.getDataFolder(), "diagnostics");
        dir.mkdirs();
        File file = new File(dir, "state-" + timestamp() + ".json");
        Map<String, Object> root = new LinkedHashMap<>();
        if (config != null) root.put("config", sectionToMap(config));
        if (lang != null) root.put("lang", sectionToMap(lang));
        if (gui != null) root.put("gui", sectionToMap(gui));
        if (defs != null) root.put("pets_definitions", sectionToMap(defs));
        try (FileWriter w = new FileWriter(file)) {
            w.write(toJson(root));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to export state: " + e.getMessage());
        }
        return file;
    }

    private Map<String,Object> sectionToMap(ConfigurationSection sec) {
        Map<String,Object> map = new LinkedHashMap<>();
        for (String k : sec.getKeys(false)) {
            Object val = sec.get(k);
            if (val instanceof ConfigurationSection cs) {
                map.put(k, sectionToMap(cs));
            } else if (val instanceof List<?> list) {
                map.put(k, new ArrayList<>(list));
            } else {
                map.put(k, val);
            }
        }
        return map;
    }

    private String toJson(Object o) {
        if (o == null) return "null";
        if (o instanceof String s) return quote(s);
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Map<?,?> m) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?,?> e : m.entrySet()) {
                if (!first) sb.append(',');
                sb.append(quote(String.valueOf(e.getKey()))).append(':').append(toJson(e.getValue()));
                first = false;
            }
            return sb.append('}').toString();
        }
        if (o instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object e : it) {
                if (!first) sb.append(',');
                sb.append(toJson(e));
                first = false;
            }
            return sb.append(']').toString();
        }
        return quote(o.toString());
    }

    private String quote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private void saveWithBackup(YamlConfiguration cfg, File file) {
        String ts = timestamp();
        File bak = new File(file.getParentFile(), file.getName() + ".bak." + ts);
        if (file.exists()) {
            try {
                java.nio.file.Files.copy(file.toPath(), bak.toPath());
            } catch (IOException ignored) {
            }
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }
}
