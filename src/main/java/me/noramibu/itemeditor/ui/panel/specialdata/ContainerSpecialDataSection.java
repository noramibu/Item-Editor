package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
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
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ContainerSpecialDataSection {

    private static final int GRID_COLUMNS = 9;
    private static final int MAX_VISUAL_SLOTS = 54;
    private static final int MIN_SLOT = 0;
    private static final int MAX_SLOT = 255;

    private static final int SLOT_SIZE = 66;
    private static final int SLOT_SELECT_BUTTON_HEIGHT = 12;
    private static final int AUTOCOMPLETE_LIMIT = 7;
    private static final Surface SLOT_DEFAULT_SURFACE = Surface.flat(0xAA1A222C).and(Surface.outline(0xFF425063));
    private static final Surface SLOT_SELECTED_SURFACE = Surface.flat(0xAA1A222C).and(Surface.outline(0xFF74D39C));
    private static final Surface SLOT_DRAG_SURFACE = Surface.flat(0xAA2A243A).and(Surface.outline(0xFFB28AFF));
    private static final Surface SLOT_INVALID_SURFACE = Surface.flat(0xAA3A1E1E).and(Surface.outline(0xFFE36D6D));
    private static final Surface SLOT_EDITED_SURFACE = Surface.flat(0xAA1A222C).and(Surface.outline(0xFFE7B766));

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
            section.child(UiFactory.muted(ItemEditorText.tr("special.container.hidden_slots", hiddenEntries), 280));
        }

        return section;
    }

    private static FlowLayout buildActionRow(SpecialDataPanelContext context, ItemEditorState.SpecialData special, int selectedSlot) {
        FlowLayout row = UiFactory.row();
        row.child(UiFactory.button(ItemEditorText.tr("special.container.add"), button ->
                context.mutateRefresh(() -> {
                    int nextSlot = nextFreeSlot(special.containerEntries);
                    ItemEditorState.ContainerEntryDraft draft = ensureEntryForSlot(special.containerEntries, nextSlot);
                    if (draft.count.isBlank()) {
                        draft.count = "1";
                    }
                    special.selectedContainerSlot = nextSlot;
                    special.draggingContainerSlot = -1;
                })
        ).horizontalSizing(Sizing.content()));

        row.child(UiFactory.button(ItemEditorText.tr("special.container.clear_slot"), button ->
                context.mutateRefresh(() -> {
                    removeEntryForSlot(special.containerEntries, selectedSlot);
                    if (special.draggingContainerSlot == selectedSlot) {
                        special.draggingContainerSlot = -1;
                    }
                })
        ).horizontalSizing(Sizing.content()));

        if (special.draggingContainerSlot >= 0) {
            row.child(UiFactory.button(ItemEditorText.tr("special.container.drag_cancel"), button ->
                    context.mutateRefresh(() -> special.draggingContainerSlot = -1)
            ).horizontalSizing(Sizing.content()));
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
        boolean edited = isEditedSlot(slotStack, originalSlotStack);
        String countOverlay = stackOverlayText(draft, slotStack);

        FlowLayout cell = UiFactory.subCard();
        cell.padding(Insets.of(4));
        cell.horizontalSizing(Sizing.fixed(SLOT_SIZE));
        cell.verticalSizing(Sizing.fixed(SLOT_SIZE));
        cell.gap(1);
        cell.surface(slotSurface(selected, dragSource, invalidItem, edited));

        FlowLayout topMarkers = UiFactory.row();
        topMarkers.horizontalSizing(Sizing.fill(100));
        if (dragSource) {
            topMarkers.child(UIComponents.label(Component.literal("↕").withColor(0xC8A0FF)));
        } else if (edited) {
            topMarkers.child(UIComponents.label(Component.literal("•").withColor(0xE7B766)));
        } else {
            topMarkers.child(UIComponents.label(Component.literal(" ")));
        }
        if (invalidItem) {
            topMarkers.child(UIComponents.label(Component.literal("!").withColor(0xFF8A8A)));
        }
        cell.child(topMarkers);

        cell.child(UIComponents.item(slotStack).showOverlay(true));

        FlowLayout footer = UiFactory.row();
        footer.horizontalSizing(Sizing.fill(100));
        footer.child(UiFactory.muted(Component.literal(countOverlay), 70));
        cell.child(footer);

        Component buttonLabel = Component.literal(Integer.toString(slot));
        if (selected) {
            buttonLabel = buttonLabel.copy().withColor(0x6DFF8D);
        } else if (dragSource) {
            buttonLabel = buttonLabel.copy().withColor(0xC8A0FF);
        }

        ButtonComponent selectButton = UiFactory.button(buttonLabel, button ->
                context.mutateRefresh(() -> handleSlotClick(special, slot))
        );
        selectButton.horizontalSizing(Sizing.fill(100));
        selectButton.verticalSizing(Sizing.fixed(SLOT_SELECT_BUTTON_HEIGHT));
        selectButton.tooltip(buildSlotTooltip(slot, draft, slotStack, invalidItem, edited, countOverlay));
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

        FlowLayout top = UiFactory.row();
        top.child(UIComponents.item(selectedStack).showOverlay(true).margins(Insets.right(6)));
        top.child(UiFactory.muted(ItemEditorText.tr("special.container.selected_slot", selectedSlot), 240));
        card.child(top);

        if (selectedStack.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.container.slot_tooltip.empty"), 260));
        } else {
            card.child(UiFactory.title(selectedStack.getHoverName()).shadow(false));
            if (selectedDraft != null) {
                card.child(UiFactory.muted(ItemEditorText.tr("special.container.item_id_value", selectedDraft.itemId), 280));
            }
            card.child(UiFactory.muted(ItemEditorText.tr("special.container.max_stack", selectedStack.getMaxStackSize()), 200));
        }
        if (invalidItem && selectedDraft != null) {
            card.child(UiFactory.message(ItemEditorText.tr("special.container.invalid_item", selectedDraft.itemId), 0xFF8A8A));
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
                        .horizontalSizing(Sizing.fixed(120))
        ));

        if (selectedDraft != null) {
            FlowLayout countRow = UiFactory.row();
            countRow.child(UiFactory.button(ItemEditorText.tr("special.container.count_decrease"), button ->
                    context.mutateRefresh(() -> stepCount(selectedDraft, -1))
            ).horizontalSizing(Sizing.content()));
            countRow.child(UiFactory.textBox(selectedDraft.count, value -> context.mutateRefresh(() -> selectedDraft.count = value))
                    .horizontalSizing(Sizing.fixed(110)));
            countRow.child(UiFactory.button(ItemEditorText.tr("special.container.count_increase"), button ->
                    context.mutateRefresh(() -> stepCount(selectedDraft, 1))
            ).horizontalSizing(Sizing.content()));
            card.child(UiFactory.field(ItemEditorText.tr("special.container.count"), Component.empty(), countRow));
        }

        return card;
    }

    private static Surface slotSurface(boolean selected, boolean dragSource, boolean invalid, boolean edited) {
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

    private static boolean isInvalidItemDraft(ItemEditorState.ContainerEntryDraft draft) {
        if (draft == null || draft.itemId.isBlank()) {
            return false;
        }
        Item item = ContainerEntryDraftUtil.resolveItem(draft.itemId);
        return item == null || item == Items.AIR;
    }

    private static String stackOverlayText(ItemEditorState.ContainerEntryDraft draft, ItemStack slotStack) {
        if (draft == null || draft.itemId.isBlank()) {
            return "-";
        }

        Item resolvedItem = ContainerEntryDraftUtil.resolveItem(draft.itemId);
        int requestedCount = parseIntOrDefault(draft.count, slotStack.isEmpty() ? 1 : slotStack.getCount());
        if (resolvedItem == null || resolvedItem == Items.AIR) {
            return Integer.toString(Math.max(1, requestedCount));
        }

        int max = Math.max(1, new ItemStack(resolvedItem).getMaxStackSize());
        return requestedCount + "/" + max;
    }

    private static List<Component> buildSlotTooltip(
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

    private static void handleSlotClick(ItemEditorState.SpecialData special, int clickedSlot) {
        int dragSource = special.draggingContainerSlot;
        if (dragSource >= 0) {
            if (dragSource == clickedSlot) {
                special.draggingContainerSlot = -1;
                special.selectedContainerSlot = clickedSlot;
                return;
            }
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
        int count = parseIntOrDefault(draft.count, 1);
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
        int current = parseIntOrDefault(draft.count, 1);
        draft.count = Integer.toString(Math.clamp(current + delta, 1, maxStack));
        if (draft.templateStack != null && !draft.templateStack.isEmpty()) {
            draft.templateStack = draft.templateStack.copyWithCount(parseIntOrDefault(draft.count, 1));
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
                return draft.templateStack.copyWithCount(Math.max(1, parseIntOrDefault(draft.count, draft.templateStack.getCount())));
            }
            return ItemStack.EMPTY;
        }

        int count = parseIntOrDefault(draft.count, 1);
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
            suggestions.child(UiFactory.muted(fallbackLabel, 260));
            return suggestions;
        }

        List<String> matches = itemIds.stream()
                .filter(id -> id.contains(query))
                .limit(AUTOCOMPLETE_LIMIT)
                .toList();

        if (matches.isEmpty()) {
            suggestions.child(UiFactory.muted(ItemEditorText.tr("special.container.autocomplete.none"), 260));
            return suggestions;
        }

        for (String match : matches) {
            ButtonComponent suggestion = UiFactory.button(
                    UiFactory.fitToWidth(Component.literal(match), 250),
                    button -> context.mutateRefresh(() -> assignItemToSelectedSlot(special, selectedSlot, match))
            );
            suggestion.horizontalSizing(Sizing.fill(100));
            suggestion.tooltip(List.of(Component.literal(match)));
            suggestions.child(suggestion);
        }

        return suggestions;
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
        if (block instanceof DispenserBlock || block instanceof DropperBlock) {
            return 9;
        }
        if (block instanceof ChestBlock || block instanceof TrappedChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock) {
            return 27;
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

    private static boolean isEditedSlot(ItemStack current, ItemStack original) {
        if (current.isEmpty() && original.isEmpty()) {
            return false;
        }
        if (current.isEmpty() != original.isEmpty()) {
            return true;
        }
        return !ItemStack.isSameItemSameComponents(current, original) || current.getCount() != original.getCount();
    }

    private static Integer parseSlotIndex(String rawSlot) {
        try {
            int parsed = Integer.parseInt(rawSlot.trim());
            if (parsed < MIN_SLOT || parsed > MAX_SLOT) {
                return null;
            }
            return parsed;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int parseIntOrDefault(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Integer entrySlotOrNull(ItemEditorState.ContainerEntryDraft draft) {
        return parseSlotIndex(draft.slot);
    }

    private static int entrySlotOrMax(ItemEditorState.ContainerEntryDraft draft) {
        Integer slot = parseSlotIndex(draft.slot);
        return slot == null ? Integer.MAX_VALUE : slot;
    }
}
