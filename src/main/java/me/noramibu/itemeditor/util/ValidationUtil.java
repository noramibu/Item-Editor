package me.noramibu.itemeditor.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.noramibu.itemeditor.editor.ValidationMessage;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ValidationUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{6}$");

    private ValidationUtil() {
    }

    public static Integer parseInt(String raw, String field, int min, int max, List<ValidationMessage> messages) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                messages.add(ValidationMessage.error(ItemEditorText.str("validation.range", field, min, max)));
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str("validation.whole_number", field)));
            return null;
        }
    }

    public static Float parseFloat(String raw, String field, List<ValidationMessage> messages) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str("validation.decimal", field)));
            return null;
        }
    }

    public static Double parseDouble(String raw, String field, List<ValidationMessage> messages) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str("validation.decimal", field)));
            return null;
        }
    }

    public static Integer parseColor(String raw, String field, List<ValidationMessage> messages) {
        Integer parsed = tryParseHexColor(raw);
        if (parsed == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str("validation.hex_color", field)));
            return null;
        }
        return parsed;
    }

    public static Optional<Integer> parseOptionalColor(String raw, String field, List<ValidationMessage> messages) {
        if (raw.isBlank()) return Optional.empty();
        return Optional.ofNullable(parseColor(raw, field, messages));
    }

    public static IntList parseColorList(String raw, String field, List<ValidationMessage> messages) {
        IntArrayList colors = new IntArrayList();
        for (String part : raw.split(",")) {
            if (part.isBlank()) continue;
            Integer color = parseColor(part.trim(), field, messages);
            if (color != null) {
                colors.add(color.intValue());
            }
        }
        return colors;
    }

    public static String toHex(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    public static Integer tryParseHexColor(String raw) {
        String normalized = normalizeHex(raw);
        if (!isHexColor(normalized)) {
            return null;
        }
        return Integer.parseInt(normalized, 16);
    }

    public static Integer tryParseByteChannel(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= 0 && value <= 255 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static boolean isHexColor(String raw) {
        return HEX_PATTERN.matcher(raw).matches();
    }

    public static String trimTrailingZeros(float value) {
        return trimTrailingZeros(Float.toString(value));
    }

    public static String trimTrailingZeros(double value) {
        return trimTrailingZeros(Double.toString(value));
    }

    public static String joinHexColors(IntList colors) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < colors.size(); index++) {
            if (index > 0) builder.append(", ");
            builder.append(toHex(colors.getInt(index)));
        }
        return builder.toString();
    }

    private static String normalizeHex(String raw) {
        String normalized = raw.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private static String trimTrailingZeros(String raw) {
        if (!raw.contains(".")) {
            return raw;
        }
        int end = raw.length() - 1;
        while (end > 0 && raw.charAt(end) == '0') {
            end--;
        }
        if (raw.charAt(end) == '.') {
            end--;
        }
        return raw.substring(0, end + 1);
    }
}
