package me.noramibu.itemeditor.ui.panel.specialdata;

import me.noramibu.itemeditor.util.IdFieldNormalizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class ContainerEntryDraftUtil {
    public static final int BUNDLE_PAGE_SIZE = 54;

    private ContainerEntryDraftUtil() {
    }

    public static Item resolveItem(String rawItemId) {
        Identifier id = IdFieldNormalizer.parse(rawItemId);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    public static int parseIntOrDefault(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
