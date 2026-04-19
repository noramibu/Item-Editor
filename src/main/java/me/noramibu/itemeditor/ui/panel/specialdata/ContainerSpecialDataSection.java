package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ContainerSpecialDataSection {

    private static final int GRID_COLUMNS = 9;
    private static final int MAX_VISUAL_SLOTS = 54;
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int MIN_SLOT = 0;
    private static final int MAX_SLOT = 255;

    private static final int SLOT_SIZE = 66;
    private static final int SLOT_SELECT_BUTTON_HEIGHT = 12;
    private static final int AUTOCOMPLETE_LIMIT = 7;
    private static final int SLOT_FIELD_WIDTH = 120;
    private static final int COUNT_FIELD_WIDTH = 110;
    private static final int HIDDEN_SLOTS_HINT_WIDTH = 280;
    private static final int DETAILS_SELECTED_SLOT_HINT_WIDTH = 240;
    private static final int DETAILS_EMPTY_HINT_WIDTH = 260;
    private static final int DETAILS_ITEM_ID_HINT_WIDTH = 280;
    private static final int DETAILS_MAX_STACK_HINT_WIDTH = 200;
    private static final int SLOT_OVERLAY_HINT_WIDTH = 70;
    private static final int AUTOCOMPLETE_HINT_WIDTH = 260;
    private static final int AUTOCOMPLETE_SUGGESTION_FIT_WIDTH = 250;
    private static final int ACTION_BUTTON_WIDTH_MIN = 72;
    private static final int ACTION_BUTTON_WIDTH_MAX = 136;
    private static final int ACTION_BUTTON_ROW_RESERVE = 12;
    private static final int COUNT_STEP_BUTTON_WIDTH_MIN = 56;
    private static final int COUNT_STEP_BUTTON_WIDTH_MAX = 116;
    private static final int COUNT_STEP_BUTTON_ROW_RESERVE = 16;
    private static final String SLOT_MARKER_DRAG = "↕";
    private static final String SLOT_MARKER_EDITED = "•";
    private static final String SLOT_MARKER_EMPTY = " ";
    private static final String SLOT_MARKER_INVALID = "!";
    private static final int COLOR_SLOT_MARKER_DRAG = 0xC8A0FF;
    private static final int COLOR_SLOT_MARKER_EDITED = 0xE7B766;
    private static final int COLOR_SLOT_MARKER_INVALID = 0xFF8A8A;
    private static final int COLOR_SLOT_INDEX_SELECTED = 0x6DFF8D;
    private static final int COLOR_SLOT_INDEX_DRAG_SOURCE = 0xC8A0FF;

    private ContainerSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsContainerData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        int selectedSlot = normalizeSelectedSlot(special);
        if (special.draggingContainerSlot < MIN_SLOT || special.draggingContainerSlot > MAX_SLOT) {
            special.draggingContainerSlot = -1;
        }

        int visualSlots = inferVisualSlotCount(context.originalStack(), special.containerEntries);
        Registry<Item> itemRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.ITEM);
        List<String> itemIds = RegistryUtil.ids(itemRegistry).stream()
                .filter(id -> !Objects.equals(id, "minecraft:air"))
                .toList();
        Map<Integer, ItemStack> originalSlots = originalContainerMap(context.originalStack());

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.container.title"), Component.empty());
        section.child(buildActionRow(context, special, selectedSlot));
        if (special.draggingContainerSlot >= 0) {
            section.child(UiFactory.message(
                    ItemEditorText.tr("special.container.dragging", special.draggingContainerSlot),
                    0xC8A0FF
            ));
        }
        section.child(buildGridCard(context, special, visualSlots, selectedSlot, originalSlots));
        section.child(buildDetailsCard(context, special, itemIds, selectedSlot));

        int hiddenEntries = countHiddenEntries(special.containerEntries, visualSlots);
        if (hiddenEntries > 0) {
            section.child(UiFactory.muted(ItemEditorText.tr("special.container.hidden_slots", hiddenEntries), HIDDEN_SLOTS_HINT_WIDTH));
        }

        return section;
    }

    private static FlowLayout buildActionRow(SpecialDataPanelContext context, ItemEditorState.SpecialData special, int selectedSlot) {
        boolean compactLayout = isCompactLayout(context);
        int contentWidth = context.panelWidthHint();
        int actionButtonCount = special.draggingContainerSlot >= 0 ? 3 : 2;
        int actionButtonWidth = resolveActionButtonWidth(contentWidth, actionButtonCount);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.container.add"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    int nextSlot = nextFreeSlot(special.containerEntries);
                    ItemEditorState.ContainerEntryDraft draft = ensureEntryForSlot(special.containerEntries, nextSlot);
                    if (draft.count.isBlank()) {
                        draft.count = "1";
                    }
                    special.selectedContainerSlot = nextSlot;
                    special.draggingContainerSlot = -1;
                })
        );
        addButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(actionButtonWidth));
        row.child(addButton);

        ButtonComponent clearButton = UiFactory.button(ItemEditorText.tr("special.container.clear_slot"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    removeEntryForSlot(special.containerEntries, selectedSlot);
                    if (special.draggingContainerSlot == selectedSlot) {
                        special.draggingContainerSlot = -1;
                    }
                })
        );
        clearButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(actionButtonWidth));
        row.child(clearButton);

        if (special.draggingContainerSlot >= 0) {
            ButtonComponent cancelDragButton = UiFactory.button(ItemEditorText.tr("special.container.drag_cancel"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.draggingContainerSlot = -1)
            );
            cancelDragButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(actionButtonWidth));
            row.child(cancelDragButton);
        }

        return row;
    }

    private static FlowLayout buildGridCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int visualSlots,
            int selectedSlot,
            Map<Integer, ItemStack> originalSlots
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.container.grid")).shadow(false));

        for (int rowStart = 0; rowStart < visualSlots; rowStart += GRID_COLUMNS) {
            FlowLayout row = UiFactory.row();
            row.gap(2);
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int slot = rowStart + column;
                row.child(buildSlotCell(
                        context,
                        special,
                        slot,
                        slot == selectedSlot,
                        slot == special.draggingContainerSlot,
                        originalSlots.getOrDefault(slot, ItemStack.EMPTY)
                ));
            }
            card.child(row);
        }

        return card;
    }

    private static FlowLayout buildSlotCell(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int slot,
            boolean selected,
            boolean dragSource,
            ItemStack originalSlotStack
    ) {
        ItemEditorState.ContainerEntryDraft draft = findEntryForSlot(special.containerEntries, slot);
        ItemStack slotStack = stackForDraft(draft);

        boolean invalidItem = isInvalidItemDraft(draft);
        boolean edited = ContainerSlotVisuals.isEditedSlot(slotStack, originalSlotStack);
        String countOverlay = ContainerSlotVisuals.stackOverlayText(draft, slotStack);

        FlowLayout cell = UiFactory.subCard();
        cell.padding(Insets.of(4));
        cell.horizontalSizing(UiFactory.fixed(SLOT_SIZE));
        cell.verticalSizing(UiFactory.fixed(SLOT_SIZE));
        cell.gap(1);
        cell.surface(ContainerSlotVisuals.slotSurface(selected, dragSource, invalidItem, edited));

        FlowLayout topMarkers = UiFactory.row();
        if (dragSource) {
            topMarkers.child(UiFactory.bodyLabel(Component.literal(SLOT_MARKER_DRAG).withColor(COLOR_SLOT_MARKER_DRAG)));
        } else if (edited) {
            topMarkers.child(UiFactory.bodyLabel(Component.literal(SLOT_MARKER_EDITED).withColor(COLOR_SLOT_MARKER_EDITED)));
        } else {
            topMarkers.child(UiFactory.bodyLabel(Component.literal(SLOT_MARKER_EMPTY)));
        }
        if (invalidItem) {
            topMarkers.child(UiFactory.bodyLabel(Component.literal(SLOT_MARKER_INVALID).withColor(COLOR_SLOT_MARKER_INVALID)));
        }
        cell.child(topMarkers);

        cell.child(UIComponents.item(slotStack).showOverlay(true));

        FlowLayout footer = UiFactory.row();
        footer.child(UiFactory.muted(Component.literal(countOverlay), SLOT_OVERLAY_HINT_WIDTH));
        cell.child(footer);

        Component buttonLabel = Component.literal(Integer.toString(slot));
        if (selected) {
            buttonLabel = buttonLabel.copy().withColor(COLOR_SLOT_INDEX_SELECTED);
        } else if (dragSource) {
            buttonLabel = buttonLabel.copy().withColor(COLOR_SLOT_INDEX_DRAG_SOURCE);
        }

        ButtonComponent selectButton = UiFactory.button(buttonLabel, UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> handleSlotClick(special, slot))
        );
        selectButton.horizontalSizing(Sizing.fill(100));
        selectButton.verticalSizing(UiFactory.fixed(SLOT_SELECT_BUTTON_HEIGHT));
        selectButton.tooltip(ContainerSlotVisuals.buildSlotTooltip(slot, draft, slotStack, invalidItem, edited, countOverlay));
        cell.child(selectButton);

        return cell;
    }

    private static FlowLayout buildDetailsCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            List<String> itemIds,
            int selectedSlot
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.container.details")).shadow(false));

        ItemEditorState.ContainerEntryDraft selectedDraft = findEntryForSlot(special.containerEntries, selectedSlot);
        ItemStack selectedStack = stackForDraft(selectedDraft);
        boolean invalidItem = isInvalidItemDraft(selectedDraft);
        String selectedItemId = selectedDraft == null ? "" : selectedDraft.itemId;
        boolean compactLayout = isCompactLayout(context);

        FlowLayout top = compactLayout ? UiFactory.column() : UiFactory.row();
        top.child(UIComponents.item(selectedStack).showOverlay(true).margins(Insets.right(6)));
        top.child(UiFactory.muted(ItemEditorText.tr("special.container.selected_slot", selectedSlot), DETAILS_SELECTED_SLOT_HINT_WIDTH));
        card.child(top);

        if (selectedStack.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.container.slot_tooltip.empty"), DETAILS_EMPTY_HINT_WIDTH));
        } else {
            card.child(UiFactory.title(selectedStack.getHoverName()).shadow(false));
            card.child(UiFactory.muted(ItemEditorText.tr("special.container.item_id_value", selectedItemId), DETAILS_ITEM_ID_HINT_WIDTH));
            card.child(UiFactory.muted(ItemEditorText.tr("special.container.max_stack", selectedStack.getMaxStackSize()), DETAILS_MAX_STACK_HINT_WIDTH));
        }
        if (invalidItem) {
            card.child(UiFactory.message(ItemEditorText.tr("special.container.invalid_item", selectedItemId), 0xFF8A8A));
        }

        Component pickerLabel = selectedDraft == null || selectedDraft.itemId.isBlank()
                ? ItemEditorText.tr("special.container.pick_item")
                : Component.literal(selectedDraft.itemId);
        FlowLayout itemInput = UiFactory.column().gap(2);
        itemInput.child(UiFactory.textBox(
                selectedDraft == null ? "" : selectedDraft.itemId,
                value -> context.mutateRefresh(() -> updateTypedItemForSelectedSlot(special, selectedSlot, value))
        ));
        itemInput.child(buildItemAutocomplete(
                context,
                special,
                selectedSlot,
                itemIds,
                selectedDraft == null ? "" : selectedDraft.itemId,
                pickerLabel
        ));
        card.child(UiFactory.field(
                ItemEditorText.tr("special.container.item"),
                Component.empty(),
                itemInput
        ));

        String slotFieldValue = selectedDraft == null ? Integer.toString(selectedSlot) : selectedDraft.slot;
        card.child(UiFactory.field(
                ItemEditorText.tr("special.container.slot_index"),
                Component.empty(),
                UiFactory.textBox(slotFieldValue, value -> context.mutateRefresh(() -> updateSlotField(special, selectedSlot, value)))
                        .horizontalSizing(UiFactory.fixed(SLOT_FIELD_WIDTH))
        ));

        if (selectedDraft != null) {
            FlowLayout countRow = compactLayout ? UiFactory.column() : UiFactory.row();
            int countStepButtonWidth = resolveCountStepButtonWidth(context.panelWidthHint());
            ButtonComponent decreaseButton = UiFactory.button(ItemEditorText.tr("special.container.count_decrease"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> stepCount(selectedDraft, -1))
            );
            decreaseButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(countStepButtonWidth));
            countRow.child(decreaseButton);
            countRow.child(UiFactory.textBox(selectedDraft.count, value -> context.mutateRefresh(() -> selectedDraft.count = value))
                    .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(COUNT_FIELD_WIDTH)));
            ButtonComponent increaseButton = UiFactory.button(ItemEditorText.tr("special.container.count_increase"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> stepCount(selectedDraft, 1))
            );
            increaseButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(countStepButtonWidth));
            countRow.child(increaseButton);
            card.child(UiFactory.field(ItemEditorText.tr("special.container.count"), Component.empty(), countRow));
        }

        return card;
    }

    private static boolean isInvalidItemDraft(ItemEditorState.ContainerEntryDraft draft) {
        if (draft == null || draft.itemId.isBlank()) {
            return false;
        }
        Item item = ContainerEntryDraftUtil.resolveItem(draft.itemId);
        return item == null || item == Items.AIR;
    }

    private static void handleSlotClick(ItemEditorState.SpecialData special, int clickedSlot) {
        int dragSource = special.draggingContainerSlot;
        if (dragSource == clickedSlot) {
            special.draggingContainerSlot = -1;
            special.selectedContainerSlot = clickedSlot;
            return;
        }
        if (dragSource >= 0) {
            moveOrSwapEntries(special.containerEntries, dragSource, clickedSlot);
            special.selectedContainerSlot = clickedSlot;
            special.draggingContainerSlot = -1;
            return;
        }

        // First click selects; clicking the selected slot again arms drag mode.
        if (special.selectedContainerSlot == clickedSlot) {
            ItemEditorState.ContainerEntryDraft selectedDraft = findEntryForSlot(special.containerEntries, clickedSlot);
            ItemStack selectedStack = stackForDraft(selectedDraft);
            special.draggingContainerSlot = selectedStack.isEmpty() ? -1 : clickedSlot;
            return;
        }

        special.selectedContainerSlot = clickedSlot;
        special.draggingContainerSlot = -1;
    }

    private static void moveOrSwapEntries(List<ItemEditorState.ContainerEntryDraft> entries, int sourceSlot, int targetSlot) {
        if (sourceSlot == targetSlot) {
            return;
        }

        ItemEditorState.ContainerEntryDraft sourceDraft = findEntryForSlot(entries, sourceSlot);
        if (sourceDraft == null) {
            return;
        }

        ItemEditorState.ContainerEntryDraft targetDraft = findEntryForSlot(entries, targetSlot);
        sourceDraft.slot = Integer.toString(targetSlot);
        if (targetDraft != null) {
            targetDraft.slot = Integer.toString(sourceSlot);
        }

        entries.sort(Comparator.comparingInt(ContainerSpecialDataSection::entrySlotOrMax));
    }

    private static void assignItemToSelectedSlot(ItemEditorState.SpecialData special, int selectedSlot, String itemId) {
        itemId = IdFieldNormalizer.normalize(itemId);
        ItemEditorState.ContainerEntryDraft draft = ensureEntryForSlot(special.containerEntries, selectedSlot);
        draft.itemId = itemId;

        if (itemId.isBlank()) {
            draft.templateStack = ItemStack.EMPTY;
            return;
        }

        Item item = ContainerEntryDraftUtil.resolveItem(itemId);
        if (item == null || item == Items.AIR) {
            return;
        }

        int maxStack = Math.max(1, new ItemStack(item).getMaxStackSize());
        int count = ContainerEntryDraftUtil.parseIntOrDefault(draft.count, 1);
        count = Math.clamp(count, 1, maxStack);
        draft.count = Integer.toString(count);
        ContainerEntryDraftUtil.syncTemplateStack(draft, item, count);
    }

    private static void updateTypedItemForSelectedSlot(ItemEditorState.SpecialData special, int selectedSlot, String rawItemId) {
        String normalized = IdFieldNormalizer.normalize(rawItemId);
        if (normalized.isBlank()) {
            removeEntryForSlot(special.containerEntries, selectedSlot);
            return;
        }
        assignItemToSelectedSlot(special, selectedSlot, normalized);
    }

    private static void updateSlotField(ItemEditorState.SpecialData special, int selectedSlot, String rawValue) {
        ItemEditorState.ContainerEntryDraft selectedDraft = findEntryForSlot(special.containerEntries, selectedSlot);
        if (selectedDraft == null) {
            Integer parsed = parseSlotIndex(rawValue);
            if (parsed != null) {
                special.selectedContainerSlot = parsed;
            }
            return;
        }

        Integer previousSlot = parseSlotIndex(selectedDraft.slot);
        selectedDraft.slot = rawValue;

        Integer parsedSlot = parseSlotIndex(rawValue);
        if (parsedSlot == null) {
            if (previousSlot != null) {
                special.selectedContainerSlot = previousSlot;
            }
            return;
        }

        ItemEditorState.ContainerEntryDraft conflicting = findEntryForSlot(special.containerEntries, parsedSlot);
        if (conflicting != null && conflicting != selectedDraft) {
            special.containerEntries.remove(conflicting);
        }
        special.selectedContainerSlot = parsedSlot;
        if (previousSlot != null && special.draggingContainerSlot == previousSlot) {
            special.draggingContainerSlot = parsedSlot;
        }
    }

    private static void stepCount(ItemEditorState.ContainerEntryDraft draft, int delta) {
        Item item = ContainerEntryDraftUtil.resolveItem(draft.itemId);
        int maxStack = item == null || item == Items.AIR ? 99 : Math.max(1, new ItemStack(item).getMaxStackSize());
        int current = ContainerEntryDraftUtil.parseIntOrDefault(draft.count, 1);
        draft.count = Integer.toString(Math.clamp(current + delta, 1, maxStack));
        if (draft.templateStack != null && !draft.templateStack.isEmpty()) {
            draft.templateStack = draft.templateStack.copyWithCount(ContainerEntryDraftUtil.parseIntOrDefault(draft.count, 1));
        }
    }

    private static ItemEditorState.ContainerEntryDraft ensureEntryForSlot(List<ItemEditorState.ContainerEntryDraft> entries, int slot) {
        ItemEditorState.ContainerEntryDraft existing = findEntryForSlot(entries, slot);
        if (existing != null) {
            return existing;
        }

        ItemEditorState.ContainerEntryDraft draft = new ItemEditorState.ContainerEntryDraft();
        draft.slot = Integer.toString(slot);
        entries.add(draft);
        entries.sort(Comparator.comparingInt(ContainerSpecialDataSection::entrySlotOrMax));
        return draft;
    }

    private static ItemEditorState.ContainerEntryDraft findEntryForSlot(List<ItemEditorState.ContainerEntryDraft> entries, int slot) {
        for (ItemEditorState.ContainerEntryDraft draft : entries) {
            Integer parsed = parseSlotIndex(draft.slot);
            if (parsed != null && parsed == slot) {
                return draft;
            }
        }
        return null;
    }

    private static void removeEntryForSlot(List<ItemEditorState.ContainerEntryDraft> entries, int slot) {
        ItemEditorState.ContainerEntryDraft entry = findEntryForSlot(entries, slot);
        if (entry != null) {
            entries.remove(entry);
        }
    }

    private static ItemStack stackForDraft(ItemEditorState.ContainerEntryDraft draft) {
        if (draft == null || draft.itemId.isBlank()) {
            return ItemStack.EMPTY;
        }

        Item item = ContainerEntryDraftUtil.resolveItem(draft.itemId);
        if (item == null || item == Items.AIR) {
            if (draft.templateStack != null && !draft.templateStack.isEmpty()) {
                return draft.templateStack.copyWithCount(Math.max(1, ContainerEntryDraftUtil.parseIntOrDefault(draft.count, draft.templateStack.getCount())));
            }
            return ItemStack.EMPTY;
        }

        int count = ContainerEntryDraftUtil.parseIntOrDefault(draft.count, 1);
        int maxStack = Math.max(1, new ItemStack(item).getMaxStackSize());
        count = Math.clamp(count, 1, maxStack);

        if (draft.templateStack != null && !draft.templateStack.isEmpty() && draft.templateStack.is(item)) {
            return draft.templateStack.copyWithCount(count);
        }
        return new ItemStack(item, count);
    }

    private static FlowLayout buildItemAutocomplete(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int selectedSlot,
            List<String> itemIds,
            String rawInput,
            Component fallbackLabel
    ) {
        FlowLayout suggestions = UiFactory.column().gap(1);
        String query = IdFieldNormalizer.normalize(rawInput);
        if (query.isBlank()) {
            suggestions.child(UiFactory.muted(fallbackLabel, AUTOCOMPLETE_HINT_WIDTH));
            return suggestions;
        }

        List<String> matches = itemIds.stream()
                .filter(id -> id.contains(query))
                .limit(AUTOCOMPLETE_LIMIT)
                .toList();

        if (matches.isEmpty()) {
            suggestions.child(UiFactory.muted(ItemEditorText.tr("special.container.autocomplete.none"), AUTOCOMPLETE_HINT_WIDTH));
            return suggestions;
        }

        for (String match : matches) {
            ButtonComponent suggestion = UiFactory.button(
                    UiFactory.fitToWidth(Component.literal(match), AUTOCOMPLETE_SUGGESTION_FIT_WIDTH), UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> assignItemToSelectedSlot(special, selectedSlot, match))
            );
            suggestion.horizontalSizing(Sizing.fill(100));
            suggestion.tooltip(List.of(Component.literal(match)));
            suggestions.child(suggestion);
        }

        return suggestions;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static int normalizeSelectedSlot(ItemEditorState.SpecialData special) {
        if (special.selectedContainerSlot >= MIN_SLOT && special.selectedContainerSlot <= MAX_SLOT) {
            return special.selectedContainerSlot;
        }

        int fallback = special.containerEntries.stream()
                .map(ContainerSpecialDataSection::entrySlotOrNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
        special.selectedContainerSlot = fallback;
        return fallback;
    }

    private static int inferVisualSlotCount(ItemStack originalStack, List<ItemEditorState.ContainerEntryDraft> entries) {
        int base = switch (containerTypeSlots(originalStack)) {
            case 5, 9, 27 -> containerTypeSlots(originalStack);
            default -> 27;
        };
        int highestSlot = entries.stream()
                .map(ContainerSpecialDataSection::entrySlotOrNull)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1);
        int needed = Math.max(base, highestSlot + 1);
        int roundedToRow = ((needed + GRID_COLUMNS - 1) / GRID_COLUMNS) * GRID_COLUMNS;
        return Math.clamp(roundedToRow, GRID_COLUMNS, MAX_VISUAL_SLOTS);
    }

    private static int containerTypeSlots(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return 27;
        }

        var block = blockItem.getBlock();
        if (block instanceof HopperBlock) {
            return 5;
        }
        if (block instanceof DispenserBlock) {
            return 9;
        }
        return 27;
    }

    private static int countHiddenEntries(List<ItemEditorState.ContainerEntryDraft> entries, int visibleSlots) {
        int hidden = 0;
        for (ItemEditorState.ContainerEntryDraft draft : entries) {
            Integer slot = parseSlotIndex(draft.slot);
            if (slot == null || slot < MIN_SLOT || slot >= visibleSlots) {
                hidden++;
            }
        }
        return hidden;
    }

    private static int nextFreeSlot(List<ItemEditorState.ContainerEntryDraft> entries) {
        boolean[] used = new boolean[MAX_SLOT + 1];
        for (ItemEditorState.ContainerEntryDraft draft : entries) {
            Integer slot = parseSlotIndex(draft.slot);
            if (slot != null && slot >= MIN_SLOT && slot <= MAX_SLOT) {
                used[slot] = true;
            }
        }
        for (int slot = 0; slot <= MAX_SLOT; slot++) {
            if (!used[slot]) {
                return slot;
            }
        }
        return MAX_SLOT;
    }

    private static Map<Integer, ItemStack> originalContainerMap(ItemStack originalStack) {
        Map<Integer, ItemStack> slots = new HashMap<>();
        ItemContainerContents contents = originalStack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return slots;
        }

        List<ItemStack> stackList = contents.allItemsCopyStream().toList();
        for (int index = 0; index < stackList.size(); index++) {
            ItemStack stack = stackList.get(index);
            if (!stack.isEmpty()) {
                slots.put(index, stack.copy());
            }
        }
        return slots;
    }

    private static Integer parseSlotIndex(String rawSlot) {
        int parsed = ContainerEntryDraftUtil.parseIntOrDefault(rawSlot, Integer.MIN_VALUE);
        if (parsed < MIN_SLOT || parsed > MAX_SLOT) {
            return null;
        }
        return parsed;
    }

    private static Integer entrySlotOrNull(ItemEditorState.ContainerEntryDraft draft) {
        return parseSlotIndex(draft.slot);
    }

    private static int entrySlotOrMax(ItemEditorState.ContainerEntryDraft draft) {
        Integer slot = parseSlotIndex(draft.slot);
        return slot == null ? Integer.MAX_VALUE : slot;
    }

    private static int resolveActionButtonWidth(int contentWidth, int buttonCount) {
        int preferred = Math.max(
                ACTION_BUTTON_WIDTH_MIN,
                Math.min(
                        ACTION_BUTTON_WIDTH_MAX,
                        (contentWidth - UiFactory.scaledPixels(ACTION_BUTTON_ROW_RESERVE)) / Math.max(1, buttonCount)
                )
        );
        return Math.max(1, Math.min(contentWidth, preferred));
    }

    private static int resolveCountStepButtonWidth(int panelWidthHint) {
        int contentWidth = Math.max(1, panelWidthHint);
        int available = contentWidth - COUNT_FIELD_WIDTH - UiFactory.scaledPixels(COUNT_STEP_BUTTON_ROW_RESERVE);
        int perButton = available / 2;
        int preferred = Math.max(COUNT_STEP_BUTTON_WIDTH_MIN, Math.min(COUNT_STEP_BUTTON_WIDTH_MAX, perButton));
        return Math.max(1, Math.min(contentWidth, preferred));
    }
}
