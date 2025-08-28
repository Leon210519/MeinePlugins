package com.lootpets.storage;

import com.lootpets.model.OwnedPetState;
import com.lootpets.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.UUID;

/**
 * JDBC based storage adapter supporting SQLite or MySQL. Only minimal batching
 * and schema management is implemented; callers are expected to invoke methods
 * asynchronously.
 */
public class SqlStorageAdapter implements StorageAdapter {
    public enum Provider { SQLITE, MYSQL }

    private final Provider provider;
    private final String dsn;
    private final String user;
    private final String pass;
    private Connection connection;

    public SqlStorageAdapter(Provider provider, String dsn, String user, String pass) {
        this.provider = provider;
        this.dsn = dsn;
        this.user = user;
        this.pass = pass;
    }

    public Provider getProvider() {
        return provider;
    }

    @Override
    public synchronized void init() throws Exception {
        this.connection = DriverManager.getConnection(dsn, user, pass);
        try (Statement st = connection.createStatement()) {
            // players table with versioning
            st.executeUpdate("CREATE TABLE IF NOT EXISTS players(uuid VARCHAR(36) PRIMARY KEY, shards INT, rename_tokens INT, album_frame_style VARCHAR(64), schema INT, version INT NOT NULL DEFAULT 0, last_updated BIGINT NOT NULL DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS owned_pets(uuid VARCHAR(36), pet_id VARCHAR(64), rarity_id VARCHAR(64), level INT, xp INT, stars INT, evolve_progress INT, suffix VARCHAR(64), PRIMARY KEY(uuid,pet_id))");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_owned_uuid ON owned_pets(uuid)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS active_pets(uuid VARCHAR(36), slot_index INT, pet_id VARCHAR(64), PRIMARY KEY(uuid,slot_index))");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_active_uuid ON active_pets(uuid)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS limits_daily(uuid VARCHAR(36), date VARCHAR(16), key VARCHAR(64), count INT, PRIMARY KEY(uuid,date,key))");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_limits_uuid ON limits_daily(uuid)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS server_registry(server_id VARCHAR(64) PRIMARY KEY, hostname VARCHAR(255), boot_time BIGINT, last_heartbeat BIGINT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS idempotency_log(uuid VARCHAR(128) PRIMARY KEY, reason VARCHAR(64), created_at BIGINT)");
        }
    }

    private PlayerData fromResult(UUID uuid) throws SQLException {
        PlayerData data = new PlayerData();
        try (PreparedStatement ps = connection.prepareStatement("SELECT shards,rename_tokens,album_frame_style,version,last_updated FROM players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.shards = rs.getInt(1);
                    data.renameTokens = rs.getInt(2);
                    data.albumFrameStyle = rs.getString(3);
                    data.version = rs.getInt(4);
                    data.lastUpdated = rs.getLong(5);
                } else {
                    return null;
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT pet_id,rarity_id,level,xp,stars,evolve_progress,suffix FROM owned_pets WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pid = rs.getString(1);
                    String rid = rs.getString(2);
                    int level = rs.getInt(3);
                    int xp = rs.getInt(4);
                    int stars = rs.getInt(5);
                    int prog = rs.getInt(6);
                    String suffix = rs.getString(7);
                    data.owned.put(pid, new OwnedPetState(rid, level, xp, stars, prog, suffix));
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT slot_index,pet_id FROM active_pets WHERE uuid=? ORDER BY slot_index")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.active.add(rs.getString(2));
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT date,key,count FROM limits_daily WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.limitsDate = rs.getString(1);
                    data.dailyLimits.put(rs.getString(2), rs.getInt(3));
                }
            }
        }
        return data;
    }

    @Override
    public synchronized PlayerData loadPlayer(UUID uuid) throws Exception {
        return fromResult(uuid);
    }

    @Override
    public synchronized void savePlayer(UUID uuid, PlayerData data) throws Exception {
        long now = System.currentTimeMillis();
        connection.setAutoCommit(false);
        try {
            int currentVersion = 0;
            try (PreparedStatement ps = connection.prepareStatement("SELECT version FROM players WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentVersion = rs.getInt(1);
                    }
                }
            }
            int newVersion = currentVersion + 1;
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE players SET shards=?,rename_tokens=?,album_frame_style=?,schema=1,version=?,last_updated=? WHERE uuid=? AND version=?")) {
                ps.setInt(1, data.shards);
                ps.setInt(2, data.renameTokens);
                ps.setString(3, data.albumFrameStyle);
                ps.setInt(4, newVersion);
                ps.setLong(5, now);
                ps.setString(6, uuid.toString());
                ps.setInt(7, currentVersion);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    if (currentVersion == 0) {
                        try (PreparedStatement ins = connection.prepareStatement(
                                "INSERT INTO players(uuid,shards,rename_tokens,album_frame_style,schema,version,last_updated) VALUES(?,?,?,?,?,?,?)")) {
                            ins.setString(1, uuid.toString());
                            ins.setInt(2, data.shards);
                            ins.setInt(3, data.renameTokens);
                            ins.setString(4, data.albumFrameStyle);
                            ins.setInt(5, 1);
                            ins.setInt(6, newVersion);
                            ins.setLong(7, now);
                            ins.executeUpdate();
                        }
                    } else {
                        connection.rollback();
                        throw new SQLException("CAS conflict");
                    }
                }
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM owned_pets WHERE uuid=?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement("INSERT INTO owned_pets(uuid,pet_id,rarity_id,level,xp,stars,evolve_progress,suffix) VALUES(?,?,?,?,?,?,?,?)")) {
                for (Map.Entry<String, OwnedPetState> e : data.owned.entrySet()) {
                    ins.setString(1, uuid.toString());
                    ins.setString(2, e.getKey());
                    OwnedPetState st = e.getValue();
                    ins.setString(3, st.rarity());
                    ins.setInt(4, st.level());
                    ins.setInt(5, st.xp());
                    ins.setInt(6, st.stars());
                    ins.setInt(7, st.evolveProgress());
                    ins.setString(8, st.suffix());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM active_pets WHERE uuid=?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement("INSERT INTO active_pets(uuid,slot_index,pet_id) VALUES(?,?,?)")) {
                for (int i = 0; i < data.active.size(); i++) {
                    ins.setString(1, uuid.toString());
                    ins.setInt(2, i);
                    ins.setString(3, data.active.get(i));
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM limits_daily WHERE uuid=?")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
            }
            if (data.limitsDate != null) {
                try (PreparedStatement ins = connection.prepareStatement("INSERT INTO limits_daily(uuid,date,key,count) VALUES(?,?,?,?)")) {
                    for (Map.Entry<String,Integer> e : data.dailyLimits.entrySet()) {
                        ins.setString(1, uuid.toString());
                        ins.setString(2, data.limitsDate);
                        ins.setString(3, e.getKey());
                        ins.setInt(4, e.getValue());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
            connection.commit();
            data.version = newVersion;
            data.lastUpdated = now;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @Override
    public synchronized void flush() {
        // No-op: writes are executed immediately within savePlayer.
    }

    @Override
    public synchronized Map<UUID, PlayerData> loadAllPlayers() throws Exception {
        Map<UUID, PlayerData> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT uuid FROM players")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString(1));
                    PlayerData data = fromResult(uuid);
                    if (data != null) {
                        map.put(uuid, data);
                    }
                }
            }
        }
        return map;
    }

    @Override
    public synchronized boolean isEmpty() throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM players")) {
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next() || rs.getInt(1) == 0;
            }
        }
    }

    @Override
    public synchronized Map<UUID, long[]> fetchVersions(Collection<UUID> uuids) throws Exception {
        if (uuids.isEmpty()) return Collections.emptyMap();
        StringBuilder sb = new StringBuilder("SELECT uuid,version,last_updated FROM players WHERE uuid IN (");
        for (int i = 0; i < uuids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        sb.append(')');
        Map<UUID, long[]> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sb.toString())) {
            int i = 1;
            for (UUID u : uuids) {
                ps.setString(i++, u.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID u = UUID.fromString(rs.getString(1));
                    map.put(u, new long[]{rs.getInt(2), rs.getLong(3)});
                }
            }
        }
        return map;
    }

    @Override
    public synchronized void touch(UUID uuid) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE players SET version=version+1,last_updated=? WHERE uuid=?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized void heartbeat(String serverId, String hostname, long bootTime) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO server_registry(server_id,hostname,boot_time,last_heartbeat) VALUES(?,?,?,?)")) {
            ps.setString(1, serverId);
            ps.setString(2, hostname);
            ps.setLong(3, bootTime);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }
}
