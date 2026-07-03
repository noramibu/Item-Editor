package me.noramibu.itemeditor.ui.panel.specialdata;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ValidationUtil;
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

    public static int slotOrMax(ItemEditorState.ContainerEntryDraft draft) {
        Integer slot = parseSlot(draft.slot);
        return slot == null ? Integer.MAX_VALUE : slot;
    }

    public static Integer parseSlot(String rawSlot) {
        int parsed = ValidationUtil.parseIntOrDefault(rawSlot, Integer.MIN_VALUE);
        return parsed == Integer.MIN_VALUE ? null : parsed;
    }
}
