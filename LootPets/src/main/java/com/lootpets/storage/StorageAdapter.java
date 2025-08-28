package com.lootpets.storage;

import com.lootpets.model.PlayerData;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Abstraction over player data storage. Implementations may back onto YAML or
 * SQL databases. All methods are thread-safe.
 */
public interface StorageAdapter {
    /** Initialise underlying resources (files, connections, etc). */
    void init() throws Exception;

    /** Load a single player's data, or {@code null} if not present. */
    PlayerData loadPlayer(UUID uuid) throws Exception;

    /** Persist the given player's data. */
    void savePlayer(UUID uuid, PlayerData data) throws Exception;

    /** Flush any pending writes to the backing store. */
    void flush() throws Exception;

    /** Load all players, used for migrations and verification. */
    Map<UUID, PlayerData> loadAllPlayers() throws Exception;

    /** Whether the backing store currently has zero players. */
    boolean isEmpty() throws Exception;

    /**
     * Fetch version and lastUpdated for a batch of players. The returned map
     * contains entries mapping UUID -> [version,lastUpdated]. The default
     * implementation returns an empty map for adapters that do not support
     * cross-server coordination.
     */
    default Map<UUID, long[]> fetchVersions(Collection<UUID> uuids) throws Exception {
        return Map.of();
    }

    /** Touch a player's row, bumping version/lastUpdated without modifying data. */
    default void touch(UUID uuid) throws Exception {}

    /** Update server heartbeat info, if supported. */
    default void heartbeat(String serverId, String hostname, long bootTime) throws Exception {}
}
