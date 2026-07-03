package me.noramibu.itemeditor.util;

import java.util.Locale;
import java.util.regex.Pattern;

final class RawValueClassifier {

    private static final Pattern NUMBER_TOKEN_PATTERN =
            Pattern.compile("^-?(?:\\d+|\\d+\\.\\d+)(?:[bBsSlLfFdD])?$");

    private RawValueClassifier() {
    }

    static RawValueMode classify(String value) {
        if (value == null || value.isBlank()) {
            return RawValueMode.NONE;
        }

        String normalized = value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "true", "false" -> RawValueMode.BOOLEAN;
            case "null" -> RawValueMode.LITERAL;
            case "infinityf", "infinityd", "nanf", "nand", "nan" -> RawValueMode.NUMBER;
            default -> {
                if (NUMBER_TOKEN_PATTERN.matcher(normalized).matches()) {
                    yield RawValueMode.NUMBER;
                }
                if (normalized.startsWith("{") || normalized.startsWith("[")) {
                    yield RawValueMode.NONE;
                }
                yield RawValueMode.STRING;
            }
        };
    }

}
