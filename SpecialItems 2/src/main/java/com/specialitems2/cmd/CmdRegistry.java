package com.specialitems2.cmd;

import com.specialitems2.util.Log;
import java.util.Set;

public final class CmdRegistry {
    private static final Set<Integer> WHITELIST = Set.of(
            1001,1003,1004,
            1101,1102,1103,1104,
            1201,1202,1203,1204
    );

    private CmdRegistry() {}

    public static Integer clamp(Integer value) {
        if (value == null) return null;
        if (!WHITELIST.contains(value)) {
            Log.warn("Skipped CMD not in whitelist: " + value);
            return null;
        }
        return value;
    }
}
