package com.specialitems.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves custom model data values for template parts. If a template does not
 * define an explicit value, a deterministic value in the range 100000-999999
 * is generated from the template id. Collisions are avoided by incrementing
 * until a free id is found. Values are kept only in-memory and regenerated on
 * each plugin start.
 */
public final class CustomModels {

    private static final Map<String, Integer> ID_TO_CMD = new HashMap<>();
    private static final Set<Integer> USED = new HashSet<>();

    private CustomModels() {}

    /**
     * Clears all cached mappings. Should be called when templates are (re)loaded.
     */
    public static void reset() {
        ID_TO_CMD.clear();
        USED.clear();
    }

    /**
     * Registers the given id with a desired custom model data value. If the
     * desired value is {@code <= 0}, a deterministic value based on the id is
     * generated. Collisions are resolved by incrementing within the safe range.
     *
     * @param id      template id
     * @param desired optional custom model data from the template
     * @return resolved custom model data
     */
    public static int register(String id, int desired) {
        if (ID_TO_CMD.containsKey(id)) {
            return ID_TO_CMD.get(id);
        }
        int cmd = desired;
        if (cmd <= 0) {
            int base = Math.abs(("SETID:" + id).hashCode());
            cmd = 100000 + (base % 900000);
        }
        while (USED.contains(cmd)) {
            cmd++;
            if (cmd > 999999) cmd = 100000;
        }
        ID_TO_CMD.put(id, cmd);
        USED.add(cmd);
        return cmd;
    }

    /**
     * Returns the resolved custom model data for the id, generating one if
     * necessary.
     */
    public static int cmdFor(String id, String rawName) {
        return register(id, 0);
    }
}
