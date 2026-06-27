package me.adamix.config.utils;

import org.jetbrains.annotations.NotNull;

public class StringUtils {
    public static @NotNull String convertCamelCaseToSnake(@NotNull String input) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append("_").append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
