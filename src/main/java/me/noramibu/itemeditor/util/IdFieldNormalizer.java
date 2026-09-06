package me.noramibu.itemeditor.util;

import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class IdFieldNormalizer {

    private IdFieldNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static Identifier parse(String raw) {
        String normalized = normalize(raw);
        if (normalized.isBlank()) {
            return null;
        }
        return Identifier.tryParse(normalized);
    }
}
