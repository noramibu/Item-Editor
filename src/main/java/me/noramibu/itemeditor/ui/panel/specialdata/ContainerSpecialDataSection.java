package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ContainerEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;

import java.util.Comparator;
import java.util.List;

public final class ContainerSpecialDataSection {
    private static final int COLUMNS = 9;
    private static final int TEXT_WIDTH_RESERVE = 28;

    private ContainerSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsContainerData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.container.title"), Component.empty());

        int editableSlots = editableSlots(context.originalStack());
        int textWidth = textWidth(context);
        section.child(UiFactory.muted(summary(special, editableSlots), textWidth));
        section.child(UiFactory.actionButtonRow(openButton(context)));
        section.child(UiFactory.actionButtonRow(resetButton(context), clearButton(context)));
        section.child(UiFactory.muted(ItemEditorText.tr("special.container.editor_hint"), textWidth));
        section.child(UiFactory.muted(ItemEditorText.tr("special.container.metadata_hint"), textWidth));
        return section;
    }

    private static int textWidth(SpecialDataPanelContext context) {
        return Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(TEXT_WIDTH_RESERVE));
    }

    private static Component summary(ItemEditorState.SpecialData special, int editableSlots) {
        int shownItems = 0;
        int preservedItems = 0;
        for (ItemEditorState.ContainerEntryDraft draft : special.containerEntries) {
            Integer slot = parseSlot(draft.slot);
            if (slot != null && slot >= 0 && slot < editableSlots) {
                shownItems++;
            } else {
                preservedItems++;
            }
        }
        return ItemEditorText.tr("special.container.summary", shownItems, preservedItems);
    }

    private static ButtonComponent openButton(SpecialDataPanelContext context) {
        return UiFactory.button(
                ItemEditorText.tr("special.container.open_editor"),
                UiFactory.ButtonTextPreset.COMPACT,
                ignored -> context.screen().session().minecraft().setScreen(new ContainerEditorScreen(
                        context.screen(),
                        context.special(),
                        context.originalStack()
                ))
        );
    }

    private static ButtonComponent resetButton(SpecialDataPanelContext context) {
        return UiFactory.button(
                ItemEditorText.tr("common.reset"),
                UiFactory.ButtonTextPreset.COMPACT,
                ignored -> context.mutateRefresh(() -> resetFromOriginal(context.special(), context.originalStack()))
        );
    }

    private static ButtonComponent clearButton(SpecialDataPanelContext context) {
        return UiFactory.button(
                ItemEditorText.tr("special.container.clear_all").copy().withColor(0xFF8A8A),
                UiFactory.ButtonTextPreset.COMPACT,
                ignored -> context.mutateRefresh(() -> context.special().containerEntries.clear())
        );
    }

    private static void resetFromOriginal(ItemEditorState.SpecialData special, ItemStack originalStack) {
        special.containerEntries.clear();
        special.selectedContainerSlot = -1;
        ItemContainerContents contents = originalStack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }

        List<ItemStack> stackList = contents.stream().toList();
        for (int slot = 0; slot < stackList.size(); slot++) {
            ItemStack stack = stackList.get(slot);
            if (!stack.isEmpty()) {
                special.containerEntries.add(ItemEditorState.ContainerEntryDraft.fromSlot(slot, stack));
                if (special.selectedContainerSlot < 0) {
                    special.selectedContainerSlot = slot;
                }
            }
        }
        special.containerEntries.sort(Comparator.comparingInt(ContainerSpecialDataSection::slotOrMax));
    }

    private static int editableSlots(ItemStack stack) {
        int slots = 27;
        if (stack.getItem() instanceof BlockItem blockItem
                && (blockItem.getBlock() instanceof HopperBlock
                || blockItem.getBlock() instanceof DispenserBlock)) {
            slots = 9;
        }
        return Math.clamp((int) Math.ceil(slots / (double) COLUMNS), 1, 6) * COLUMNS;
    }

    private static int slotOrMax(ItemEditorState.ContainerEntryDraft draft) {
        Integer slot = parseSlot(draft.slot);
        return slot == null ? Integer.MAX_VALUE : slot;
    }

    private static Integer parseSlot(String rawSlot) {
        int parsed = ContainerEntryDraftUtil.parseIntOrDefault(rawSlot, Integer.MIN_VALUE);
        return parsed == Integer.MIN_VALUE ? null : parsed;
    }
}
