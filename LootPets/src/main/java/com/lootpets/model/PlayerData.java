package com.lootpets.model;

import java.util.*;

/**
 * Container for all persisted player data. This is an internal model used by the
 * storage layer and caches. All fields are mutable and no defensive copies are
 * performed; callers are expected to synchronize externally.
 */
public class PlayerData {
    public Map<String, OwnedPetState> owned = new LinkedHashMap<>();
    public List<String> active = new ArrayList<>();
    public int shards = 0;
    public int renameTokens = 0;
    public String albumFrameStyle = null;
    public String limitsDate = null;
    public Map<String, Integer> dailyLimits = new HashMap<>();

    /** Optimistic concurrency version number. */
    public int version = 0;

    /** Last updated timestamp (epoch millis). */
    public long lastUpdated = 0L;
}
