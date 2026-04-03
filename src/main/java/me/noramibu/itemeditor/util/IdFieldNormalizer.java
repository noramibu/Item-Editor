package me.noramibu.itemeditor.util;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class IdFieldNormalizer {

    private IdFieldNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static ResourceLocation parse(String raw) {
        String normalized = normalize(raw);
        if (normalized.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(normalized);
    }
}
