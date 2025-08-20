package com.specialitems.leveling;

public final class LevelMath {
    private LevelMath(){}

    public static double neededXpFor(int level) {
        if (level <= 1) return 50.0;
        if (level >= 100) return Double.POSITIVE_INFINITY;
        return 50.0 * Math.pow(level, 1.35);
    }
}
