package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PlayerData;
import com.lootpets.storage.SqlStorageAdapter;
import com.lootpets.storage.StorageAdapter;
import com.lootpets.storage.YamlStorageAdapter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * PetService now delegates all persistence to a pluggable {@link StorageAdapter}
 * while maintaining a simple read-through/write-behind cache. Public APIs and
 * behaviour remain compatible with earlier versions.
 */
public class PetService implements Listener {

    public record EvolveResult(OwnedPetState state, boolean starUp, boolean capped) {}

    private final LootPetsPlugin plugin;
    private final StorageAdapter storage;
    private final ConcurrentMap<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final List<Consumer<UUID>> changeListeners = new ArrayList<>();
    private final int flushInterval;
    private final boolean preloadOnJoin;

    public PetService(LootPetsPlugin plugin) {
        this.plugin = plugin;
        ConfigurationSection store = plugin.getConfig().getConfigurationSection("storage");
        StorageAdapter tmp;
        int interval = 30;
        boolean preload = true;
        if (store != null) {
            String provider = store.getString("provider", "YAML");
            interval = store.getInt("flush_interval_seconds", 30);
            preload = store.getBoolean("preload_on_join", true);
            try {
                switch (provider.toUpperCase(Locale.ROOT)) {
                    case "SQLITE" -> {
                        String file = store.getConfigurationSection("sqlite").getString("file", "lootpets.db");
                        String dsn = "jdbc:sqlite:" + new File(plugin.getDataFolder(), file).getPath();
                        tmp = new SqlStorageAdapter(SqlStorageAdapter.Provider.SQLITE, dsn, "", "");
                    }
                    case "MYSQL" -> {
                        ConfigurationSection my = store.getConfigurationSection("mysql");
                        String host = my.getString("host");
                        int port = my.getInt("port");
                        String db = my.getString("database");
                        String user = my.getString("user");
                        String pass = my.getString("password");
                        String dsn = "jdbc:mysql://" + host + ":" + port + "/" + db;
                        tmp = new SqlStorageAdapter(SqlStorageAdapter.Provider.MYSQL, dsn, user, pass);
                    }
                    default -> tmp = new YamlStorageAdapter(plugin.getDataFolder());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Falling back to YAML storage: " + e.getMessage());
                tmp = new YamlStorageAdapter(plugin.getDataFolder());
            }
        } else {
            tmp = new YamlStorageAdapter(plugin.getDataFolder());
        }
        this.storage = tmp;
        this.flushInterval = interval;
        this.preloadOnJoin = preload;
        try {
            storage.init();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to init storage: " + e.getMessage());
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        long ticks = flushInterval * 20L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::flushAll, ticks, ticks);
        plugin.getLogger().info("Storage provider: " + storage.getClass().getSimpleName() + ", flush interval=" + flushInterval + "s, preload=" + preloadOnJoin);
    }

    // region lifecycle
    private PlayerData load(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> {
            try {
                PlayerData data = storage.loadPlayer(u);
                return data != null ? data : new PlayerData();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player " + u + ": " + e.getMessage());
                return new PlayerData();
            }
        });
    }

    private void markDirty(UUID uuid) {
        dirty.add(uuid);
    }

    public void flush(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        try {
            storage.savePlayer(uuid, data);
            dirty.remove(uuid);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save player " + uuid + ": " + e.getMessage());
        }
    }

    public void flushAll() {
        for (UUID u : new ArrayList<>(dirty)) {
            flush(u);
        }
        try {
            storage.flush();
        } catch (Exception e) {
            plugin.getLogger().warning("Flush failed: " + e.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!preloadOnJoin) return;
        UUID id = e.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> load(id));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        flush(id);
        cache.remove(id);
        dirty.remove(id);
    }
    // endregion

    public void addChangeListener(Consumer<UUID> listener) {
        changeListeners.add(listener);
    }

    private void notifyChange(UUID uuid) {
        for (Consumer<UUID> c : changeListeners) {
            try {
                c.accept(uuid);
            } catch (Exception ignored) {
            }
        }
    }

    // Data helpers
    private OwnedPetState owned(PlayerData data, String petId) {
        return data.owned.get(petId);
    }

    private void putOwned(PlayerData data, String petId, OwnedPetState state) {
        data.owned.put(petId, state);
    }

    // region API
    public void ensurePlayerNode(UUID uuid) {
        load(uuid); // loading creates empty record if absent
    }

    public Map<String, OwnedPetState> getOwnedPets(UUID uuid) {
        PlayerData data = load(uuid);
        return Collections.unmodifiableMap(new LinkedHashMap<>(data.owned));
    }

    public boolean addOwnedPet(UUID uuid, String petId, String rarityId) {
        PlayerData data = load(uuid);
        if (data.owned.containsKey(petId)) {
            return false;
        }
        putOwned(data, petId, new OwnedPetState(rarityId, 0, 0, 0, 0, null));
        markDirty(uuid);
        return true;
    }

    public EvolveResult incrementEvolve(UUID uuid, String petId) {
        PlayerData data = load(uuid);
        OwnedPetState st = owned(data, petId);
        if (st == null) return new EvolveResult(new OwnedPetState(null,0,0,0,0,null), false, false);
        int progress = st.evolveProgress() + 1;
        int stars = st.stars();
        boolean starUp = false;
        boolean capped = false;
        if (progress >= 5) {
            progress = 0;
            if (stars < 5) {
                stars++;
                starUp = true;
            } else {
                capped = true;
            }
        }
        OwnedPetState ns = new OwnedPetState(st.rarity(), st.level(), stars, progress, st.xp(), st.suffix());
        putOwned(data, petId, ns);
        markDirty(uuid);
        if (starUp) notifyChange(uuid);
        return new EvolveResult(ns, starUp, capped);
    }

    public List<String> getActivePetIds(UUID uuid, int maxSlots) {
        PlayerData data = load(uuid);
        List<String> list = new ArrayList<>(data.active);
        LinkedHashSet<String> set = new LinkedHashSet<>(list);
        List<String> cleaned = new ArrayList<>(set);
        if (cleaned.size() > maxSlots) {
            cleaned = new ArrayList<>(cleaned.subList(0, maxSlots));
        }
        if (!cleaned.equals(data.active)) {
            data.active.clear();
            data.active.addAll(cleaned);
            markDirty(uuid);
        }
        return Collections.unmodifiableList(cleaned);
    }

    public boolean equipPet(UUID uuid, String petId, int maxSlots) {
        PlayerData data = load(uuid);
        if (data.active.contains(petId)) return false;
        if (data.active.size() >= maxSlots) return false;
        data.active.add(petId);
        markDirty(uuid);
        notifyChange(uuid);
        return true;
    }

    public boolean unequipPet(UUID uuid, String petId) {
        PlayerData data = load(uuid);
        if (!data.active.remove(petId)) return false;
        markDirty(uuid);
        notifyChange(uuid);
        return true;
    }

    public void addXpToActivePets(UUID uuid, int xpPerTick, int xpPerLevel, int baseCap, int extraPerStar) {
        PlayerData data = load(uuid);
        boolean changed = false;
        boolean levelChanged = false;
        for (String petId : data.active) {
            OwnedPetState st = owned(data, petId);
            if (st == null) continue;
            int xp = st.xp() + xpPerTick;
            int level = st.level();
            int stars = st.stars();
            int cap = baseCap + extraPerStar * stars;
            int oldLevel = level;
            if (xp >= xpPerLevel && level < cap) {
                int gained = xp / xpPerLevel;
                xp %= xpPerLevel;
                level += gained;
                if (level > cap) {
                    level = cap;
                    xp = 0;
                }
            }
            OwnedPetState ns = new OwnedPetState(st.rarity(), level, stars, st.evolveProgress(), xp, st.suffix());
            putOwned(data, petId, ns);
            if (level != oldLevel) levelChanged = true;
            changed = true;
        }
        if (changed) {
            markDirty(uuid);
        }
        if (levelChanged) {
            notifyChange(uuid);
        }
    }

    public boolean setLevel(UUID uuid, String petId, int level, int baseCap, int extraPerStar) {
        PlayerData data = load(uuid);
        OwnedPetState st = owned(data, petId);
        if (st == null) return false;
        int cap = baseCap + extraPerStar * st.stars();
        int clamped = Math.max(0, Math.min(level, cap));
        OwnedPetState ns = new OwnedPetState(st.rarity(), clamped, st.stars(), st.evolveProgress(), 0, st.suffix());
        putOwned(data, petId, ns);
        markDirty(uuid);
        notifyChange(uuid);
        return true;
    }

    public boolean setStars(UUID uuid, String petId, int stars) {
        PlayerData data = load(uuid);
        OwnedPetState st = owned(data, petId);
        if (st == null) return false;
        OwnedPetState ns = new OwnedPetState(st.rarity(), st.level(), stars, st.evolveProgress(), st.xp(), st.suffix());
        putOwned(data, petId, ns);
        markDirty(uuid);
        notifyChange(uuid);
        return true;
    }

    public boolean addXp(UUID uuid, String petId, int amount, int xpPerLevel, int baseCap, int extraPerStar) {
        PlayerData data = load(uuid);
        OwnedPetState st = owned(data, petId);
        if (st == null) return false;
        int xp = st.xp() + Math.max(0, amount);
        int level = st.level();
        int stars = st.stars();
        int cap = baseCap + extraPerStar * stars;
        boolean levelChanged = false;
        if (xp >= xpPerLevel && level < cap) {
            int gained = xp / xpPerLevel;
            xp %= xpPerLevel;
            level += gained;
            if (level > cap) {
                level = cap;
                xp = 0;
            }
            levelChanged = true;
        }
        OwnedPetState ns = new OwnedPetState(st.rarity(), level, stars, st.evolveProgress(), xp, st.suffix());
        putOwned(data, petId, ns);
        markDirty(uuid);
        if (levelChanged) notifyChange(uuid);
        return true;
    }

    public int getShards(UUID uuid) {
        PlayerData data = load(uuid);
        return data.shards;
    }

    public void addShards(UUID uuid, int amount) {
        PlayerData data = load(uuid);
        data.shards = Math.max(0, data.shards + amount);
        markDirty(uuid);
    }

    public boolean spendShards(UUID uuid, int amount) {
        PlayerData data = load(uuid);
        if (data.shards < amount) return false;
        data.shards -= amount;
        markDirty(uuid);
        return true;
    }

    public int getRenameTokens(UUID uuid) {
        PlayerData data = load(uuid);
        return data.renameTokens;
    }

    public void addRenameTokens(UUID uuid, int amount) {
        PlayerData data = load(uuid);
        data.renameTokens = Math.max(0, data.renameTokens + amount);
        markDirty(uuid);
    }

    public boolean consumeRenameToken(UUID uuid) {
        PlayerData data = load(uuid);
        if (data.renameTokens <= 0) return false;
        data.renameTokens--;
        markDirty(uuid);
        return true;
    }

    public String getAlbumFrameStyle(UUID uuid) {
        PlayerData data = load(uuid);
        return data.albumFrameStyle;
    }

    public void setAlbumFrameStyle(UUID uuid, String style) {
        PlayerData data = load(uuid);
        data.albumFrameStyle = style;
        markDirty(uuid);
    }

    public String getSuffix(UUID uuid, String petId) {
        PlayerData data = load(uuid);
        OwnedPetState st = owned(data, petId);
        return st != null ? st.suffix() : null;
    }

    public void setSuffix(UUID uuid, String petId, String suffix) {
        PlayerData data = load(uuid);
        OwnedPetState st = owned(data, petId);
        if (st == null) return;
        putOwned(data, petId, new OwnedPetState(st.rarity(), st.level(), st.stars(), st.evolveProgress(), st.xp(), suffix));
        markDirty(uuid);
    }

    public int getDailyBuys(UUID uuid, String today, String itemId) {
        PlayerData data = load(uuid);
        if (!Objects.equals(today, data.limitsDate)) {
            return 0;
        }
        return data.dailyLimits.getOrDefault(itemId, 0);
    }

    public void incrementDailyBuys(UUID uuid, String today, String itemId) {
        PlayerData data = load(uuid);
        if (!Objects.equals(today, data.limitsDate)) {
            data.limitsDate = today;
            data.dailyLimits.clear();
        }
        data.dailyLimits.put(itemId, data.dailyLimits.getOrDefault(itemId, 0) + 1);
        markDirty(uuid);
    }

    public void reset(UUID uuid) {
        cache.put(uuid, new PlayerData());
        dirty.add(uuid);
        notifyChange(uuid);
    }

    public void save() {
        flushAll();
    }

    // Admin helpers
    public String getStorageInfo() {
        return storage.getClass().getSimpleName() + ", dirty=" + dirty.size();
    }

    public void flushCommand(Player target) {
        if (target != null) {
            flush(target.getUniqueId());
        } else {
            flushAll();
        }
    }

    public void verifyStorage() {
        try {
            Map<UUID, PlayerData> all = storage.loadAllPlayers();
            plugin.getLogger().info("Storage contains " + all.size() + " players");
        } catch (Exception e) {
            plugin.getLogger().warning("Verification failed: " + e.getMessage());
        }
    }
    // endregion
}
