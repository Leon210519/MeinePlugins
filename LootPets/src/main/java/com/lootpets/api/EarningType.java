package com.lootpets.api;

import java.util.Locale;
import java.util.Optional;

/**
 * Types of earnings that pets can boost.
 */
public enum EarningType {
    EARNINGS_LOOTFACTORY("earnings_lootfactory"),
    EARNINGS_SELL("earnings_sell"),
    EARNINGS_MISSIONS("earnings_missions");

    private final String weightKey;

    EarningType(String weightKey) {
        this.weightKey = weightKey;
    }

    public String weightKey() {
        return weightKey;
    }

    /**
     * Parses the given string into an {@link EarningType}.
     * The match is case-insensitive and supports values with or without the
     * {@code EARNINGS_} prefix.
     *
     * @param input string to parse
     * @return optional earning type
     */
    public static Optional<EarningType> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String norm = input.trim().toUpperCase(Locale.ROOT);
        for (EarningType type : values()) {
            if (type.name().equals(norm) || type.name().replace("EARNINGS_", "").equals(norm)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
