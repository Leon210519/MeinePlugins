package com.specialitems.leveling;

public final class LevelMath {
    private LevelMath(){}

    public static double neededXpFor(int level) {
        if (level <= 1) return 100.0;
        return 100.0 + 25.0 * (level - 1);
    }
}
