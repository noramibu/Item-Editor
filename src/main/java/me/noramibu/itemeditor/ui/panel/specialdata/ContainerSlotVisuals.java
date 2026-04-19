package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.core.Surface;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

final class ContainerSlotVisuals {

    private static final Surface SLOT_DEFAULT_SURFACE = Surface.flat(0xAA1A222C).and(Surface.outline(0xFF425063));
    private static final Surface SLOT_SELECTED_SURFACE = Surface.flat(0xAA1A222C).and(Surface.outline(0xFF74D39C));
    private static final Surface SLOT_DRAG_SURFACE = Surface.flat(0xAA2A243A).and(Surface.outline(0xFFB28AFF));
    private static final Surface SLOT_INVALID_SURFACE = Surface.flat(0xAA3A1E1E).and(Surface.outline(0xFFE36D6D));
    private static final Surface SLOT_EDITED_SURFACE = Surface.flat(0xAA1A222C).and(Surface.outline(0xFFE7B766));

    private ContainerSlotVisuals() {
    }

    static Surface slotSurface(boolean selected, boolean dragSource, boolean invalid, boolean edited) {
        if (invalid) {
            return SLOT_INVALID_SURFACE;
        }
        if (dragSource) {
            return SLOT_DRAG_SURFACE;
        }
        if (selected) {
            return SLOT_SELECTED_SURFACE;
        }
        if (edited) {
            return SLOT_EDITED_SURFACE;
        }
        return SLOT_DEFAULT_SURFACE;
    }

    static String stackOverlayText(ItemEditorState.ContainerEntryDraft draft, ItemStack slotStack) {
        if (draft == null || draft.itemId.isBlank()) {
            return "-";
        }

        Item resolvedItem = ContainerEntryDraftUtil.resolveItem(draft.itemId);
        int requestedCount = ContainerEntryDraftUtil.parseIntOrDefault(draft.count, slotStack.isEmpty() ? 1 : slotStack.getCount());
        if (resolvedItem == null || resolvedItem == Items.AIR) {
            return Integer.toString(Math.max(1, requestedCount));
        }

        int max = Math.max(1, new ItemStack(resolvedItem).getMaxStackSize());
        return requestedCount + "/" + max;
    }

    static List<Component> buildSlotTooltip(
            int slot,
            ItemEditorState.ContainerEntryDraft draft,
            ItemStack stack,
            boolean invalidItem,
            boolean edited,
            String countOverlay
    ) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(ItemEditorText.tr("special.container.slot_tooltip.slot", slot));
        if (stack.isEmpty() || draft == null) {
            tooltip.add(ItemEditorText.tr("special.container.slot_tooltip.empty"));
            return tooltip;
        }

        tooltip.add(stack.getHoverName());
        tooltip.add(ItemEditorText.tr("special.container.slot_tooltip.item", draft.itemId));
        tooltip.add(ItemEditorText.tr("special.container.slot_tooltip.count", countOverlay));
        if (edited) {
            tooltip.add(ItemEditorText.tr("special.container.slot_tooltip.edited").copy().withColor(0xE7B766));
        }
        if (invalidItem) {
            tooltip.add(ItemEditorText.tr("special.container.slot_tooltip.invalid").copy().withColor(0xFF8A8A));
        }
        return tooltip;
    }

    static boolean isEditedSlot(ItemStack current, ItemStack original) {
        if (current.isEmpty() && original.isEmpty()) {
            return false;
        }
        if (current.isEmpty() != original.isEmpty()) {
            return true;
        }
        return !ItemStack.isSameItemSameComponents(current, original) || current.getCount() != original.getCount();
    }
}
