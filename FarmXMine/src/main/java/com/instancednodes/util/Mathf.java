package com.instancednodes.util;

public class Mathf {
    public static int yieldFor(long harvests, long targetHarvests, double exponent) {
        if (targetHarvests <= 0) return 1;
        double ratio = Math.max(0.0, Math.min(1.0, (double)harvests / (double)targetHarvests));
        double scaled = 1.0 + Math.floor(63.0 * Math.pow(ratio, exponent));
        int val = (int)scaled;
        if (val < 1) val = 1;
        if (val > 64) val = 64;
        return val;
    }
}
