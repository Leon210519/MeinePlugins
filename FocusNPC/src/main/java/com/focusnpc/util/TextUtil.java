package com.focusnpc.util;

import org.bukkit.Material;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextUtil {
    public static String format(Material mat) {
        String name = mat.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Stream.of(name.split(" "))
                .map(s -> s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
