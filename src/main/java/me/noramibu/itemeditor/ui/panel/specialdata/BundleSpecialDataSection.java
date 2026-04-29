package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.ButtonFitUtil;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BundleSpecialDataSection {

    private static final int SEARCH_PICKER_BUTTON_WIDTH = 132;
    private static final int COUNT_FIELD_WIDTH = 104;
    private static final int EMPTY_HINT_WIDTH = 280;
    private static final int METRICS_HINT_WIDTH = 280;
    private static final int ENTRY_PREVIEW_HINT_WIDTH = 240;
    private static final int ENTRY_SELECTED_HINT_WIDTH = 160;
    private static final int ACTION_BUTTON_WIDTH_MIN = 72;
    private static final int ACTION_BUTTON_WIDTH_MAX = 136;
    private static final int ACTION_BUTTON_ROW_RESERVE = 12;
    private static final int COUNT_STEP_BUTTON_WIDTH_MIN = 58;
    private static final int COUNT_STEP_BUTTON_WIDTH_MAX = 118;
    private static final int COUNT_STEP_BUTTON_ROW_RESERVE = 16;
    private static final int ACTION_BUTTON_TEXT_MIN = 18;
    private static final int ACTION_BUTTON_TEXT_RESERVE = 10;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 600;

    private BundleSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsBundleData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        boolean compactLayout = isCompactLayout(context);
        normalizeSelection(special);
        syncSlots(special.bundleEntries);

        Registry<Item> itemRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.ITEM);
        List<String> itemIds = RegistryUtil.ids(itemRegistry).stream()
                .filter(id -> !Objects.equals(id, "minecraft:air"))
                .toList();

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.bundle.title"), Component.empty());
        section.child(buildMetricsCard(special));
        section.child(buildActions(context, special, compactLayout));

        if (special.bundleEntries.isEmpty()) {
            section.child(UiFactory.muted(ItemEditorText.tr("special.bundle.empty"), EMPTY_HINT_WIDTH));
            return section;
        }

        for (int index = 0; index < special.bundleEntries.size(); index++) {
            section.child(buildEntryCard(context, special, itemIds, index));
        }

        return section;
    }

    private static FlowLayout buildMetricsCard(ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.bundle.metrics")).shadow(false));

        BundleContents contents = buildContents(special.bundleEntries);
        if (contents == null) {
            card.child(UiFactory.message(ItemEditorText.tr("special.bundle.metrics.invalid"), 0xFF8A8A));
            return card;
        }

        var weightResult = contents.weight();
        if (weightResult.error().isPresent()) {
            card.child(UiFactory.message(ItemEditorText.tr("special.bundle.metrics.invalid"), 0xFF8A8A));
            return card;
        }

        Fraction weight = weightResult.result().orElseGet(() -> Fraction.getFraction(0, 1));
        double fillPercent = weight.doubleValue() * 100d;
        card.child(UiFactory.muted(ItemEditorText.tr(
                "special.bundle.metrics.value",
                contents.size(),
                weight.getNumerator(),
                weight.getDenominator(),
                formatPercent(fillPercent)
        ), METRICS_HINT_WIDTH));

        if (fillPercent > 100d) {
            card.child(UiFactory.message(ItemEditorText.tr("special.bundle.metrics.overfilled"), 0xFF8A8A));
        }

        return card;
    }

    private static FlowLayout buildActions(SpecialDataPanelContext context, ItemEditorState.SpecialData special, boolean compactLayout) {
        int contentWidth = Math.max(1, context.panelWidthHint());
        int actionButtonWidth = resolveActionButtonWidth(contentWidth);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        ButtonComponent addButton = ButtonFitUtil.fixedWidthFittedButton(
                ItemEditorText.tr("special.bundle.add"),
                UiFactory.ButtonTextPreset.STANDARD,
                actionButtonWidth,
                ACTION_BUTTON_TEXT_MIN,
                ACTION_BUTTON_TEXT_RESERVE,
                button ->
                context.mutateRefresh(() -> {
                    int insertAt = special.selectedBundleIndex < 0
                            ? special.bundleEntries.size()
                            : Math.clamp(special.selectedBundleIndex + 1, 0, special.bundleEntries.size());
                    ItemEditorState.ContainerEntryDraft draft = new ItemEditorState.ContainerEntryDraft();
                    draft.count = "1";
                    special.bundleEntries.add(insertAt, draft);
                    syncSlots(special.bundleEntries);
                    special.selectedBundleIndex = insertAt;
                })
        );
        addButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(actionButtonWidth));
        row.child(addButton);

        ButtonComponent remove = ButtonFitUtil.fixedWidthFittedButton(
                ItemEditorText.tr("special.bundle.remove_selected"),
                UiFactory.ButtonTextPreset.STANDARD,
                actionButtonWidth,
                ACTION_BUTTON_TEXT_MIN,
                ACTION_BUTTON_TEXT_RESERVE,
                button ->
                context.mutateRefresh(() -> {
                    if (special.selectedBundleIndex >= 0 && special.selectedBundleIndex < special.bundleEntries.size()) {
                        special.bundleEntries.remove(special.selectedBundleIndex);
                        syncSlots(special.bundleEntries);
                        if (special.bundleEntries.isEmpty()) {
                            special.selectedBundleIndex = -1;
                        } else {
                            special.selectedBundleIndex = Math.min(special.selectedBundleIndex, special.bundleEntries.size() - 1);
                        }
                    }
                })
        );
        remove.active(special.selectedBundleIndex >= 0 && special.selectedBundleIndex < special.bundleEntries.size());
        row.child(remove.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(actionButtonWidth)));
        return row;
    }

    private static FlowLayout buildEntryCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            List<String> itemIds,
        int index
    ) {
        boolean compactLayout = isCompactLayout(context);
        int contentWidth = Math.max(1, context.panelWidthHint());
        ItemEditorState.ContainerEntryDraft entry = special.bundleEntries.get(index);
        ItemStack stack = stackForEntry(entry);
        boolean invalidId = isInvalidItemId(entry.itemId);
        boolean cannotFit = !stack.isEmpty() && !BundleContents.canItemBeInBundle(stack);

        FlowLayout card = context.createReorderableCard(
                ItemEditorText.tr("special.bundle.entry", index + 1),
                index > 0,
                () -> moveEntry(special, index, index - 1),
                index < special.bundleEntries.size() - 1,
                () -> moveEntry(special, index, index + 1),
                () -> removeEntry(special, index)
        );

        FlowLayout preview = compactLayout ? UiFactory.column() : UiFactory.row();
        preview.child(UIComponents.item(stack).showOverlay(true));
        preview.child(UiFactory.muted(stack.isEmpty()
                ? ItemEditorText.tr("special.bundle.entry.empty")
                : ItemEditorText.tr("special.bundle.entry.stack", stack.getHoverName(), stack.getCount()), ENTRY_PREVIEW_HINT_WIDTH));
        card.child(preview);

        FlowLayout itemInput = compactLayout ? UiFactory.column() : UiFactory.row();
        itemInput.child(UiFactory.textBox(entry.itemId, value -> context.mutateRefresh(() -> updateItemId(entry, value))));
        itemInput.child(UiFactory.button(ItemEditorText.tr("special.bundle.select_item"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.openSearchablePicker(
                        ItemEditorText.str("special.bundle.item_picker_title"),
                        "",
                        itemIds,
                        value -> value,
                        selected -> context.mutateRefresh(() -> updateItemId(entry, selected))
                )
                ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(SEARCH_PICKER_BUTTON_WIDTH)));
        card.child(UiFactory.field(ItemEditorText.tr("special.container.item"), Component.empty(), itemInput));

        FlowLayout countRow = compactLayout ? UiFactory.column() : UiFactory.row();
        int countStepButtonWidth = resolveCountStepButtonWidth(contentWidth);
        ButtonComponent decreaseButton = ButtonFitUtil.fixedWidthFittedButton(
                ItemEditorText.tr("special.container.count_decrease"),
                UiFactory.ButtonTextPreset.STANDARD,
                countStepButtonWidth,
                ACTION_BUTTON_TEXT_MIN,
                ACTION_BUTTON_TEXT_RESERVE,
                button -> context.mutateRefresh(() -> stepCount(entry, -1))
        );
        countRow.child(decreaseButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(countStepButtonWidth)));
        countRow.child(UiFactory.textBox(entry.count, value -> context.mutate(() -> entry.count = value))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(COUNT_FIELD_WIDTH)));
        ButtonComponent increaseButton = ButtonFitUtil.fixedWidthFittedButton(
                ItemEditorText.tr("special.container.count_increase"),
                UiFactory.ButtonTextPreset.STANDARD,
                countStepButtonWidth,
                ACTION_BUTTON_TEXT_MIN,
                ACTION_BUTTON_TEXT_RESERVE,
                button -> context.mutateRefresh(() -> stepCount(entry, 1))
        );
        countRow.child(increaseButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(countStepButtonWidth)));
        card.child(UiFactory.field(ItemEditorText.tr("special.container.count"), Component.empty(), countRow));

        if (invalidId) {
            card.child(UiFactory.message(ItemEditorText.tr("special.container.invalid_item", entry.itemId), 0xFF8A8A));
        } else if (cannotFit) {
            card.child(UiFactory.message(ItemEditorText.tr("special.bundle.item_not_allowed"), 0xFF8A8A));
        }

        if (special.selectedBundleIndex != index) {
            int selectWidth = resolveActionButtonWidth(contentWidth);
            ButtonComponent selectEntry = ButtonFitUtil.fixedWidthFittedButton(
                    ItemEditorText.tr("special.bundle.select_entry"),
                    UiFactory.ButtonTextPreset.STANDARD,
                    selectWidth,
                    ACTION_BUTTON_TEXT_MIN,
                    ACTION_BUTTON_TEXT_RESERVE,
                    button -> context.mutateRefresh(() -> special.selectedBundleIndex = index)
            );
            card.child(selectEntry.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(selectWidth)));
        } else {
            card.child(UiFactory.muted(ItemEditorText.tr("special.bundle.selected"), ENTRY_SELECTED_HINT_WIDTH));
        }

        return card;
    }

    private static BundleContents buildContents(List<ItemEditorState.ContainerEntryDraft> entries) {
        List<ItemStackTemplate> stacks = new ArrayList<>();
        for (ItemEditorState.ContainerEntryDraft entry : entries) {
            ItemStack stack = stackForEntry(entry);
            if (!stack.isEmpty()) {
                stacks.add(ItemStackTemplate.fromNonEmptyStack(stack));
            }
        }
        try {
            return new BundleContents(stacks);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static ItemStack stackForEntry(ItemEditorState.ContainerEntryDraft entry) {
        if (entry == null || entry.itemId.isBlank()) {
            return ItemStack.EMPTY;
        }

        Item item = ContainerEntryDraftUtil.resolveItem(entry.itemId);
        if (item == null || item == Items.AIR) {
            if (entry.templateStack != null && !entry.templateStack.isEmpty()) {
                int fallbackCount = ContainerEntryDraftUtil.parseIntOrDefault(entry.count, entry.templateStack.getCount());
                return entry.templateStack.copyWithCount(Math.max(1, fallbackCount));
            }
            return ItemStack.EMPTY;
        }

        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        int count = Math.clamp(ContainerEntryDraftUtil.parseIntOrDefault(entry.count, 1), 1, max);
        if (entry.templateStack != null && !entry.templateStack.isEmpty() && entry.templateStack.is(item)) {
            return entry.templateStack.copyWithCount(count);
        }
        return new ItemStack(item, count);
    }

    private static boolean isInvalidItemId(String rawItemId) {
        if (rawItemId == null || rawItemId.isBlank()) {
            return false;
        }
        Item item = ContainerEntryDraftUtil.resolveItem(rawItemId);
        return item == null || item == Items.AIR;
    }

    private static void updateItemId(ItemEditorState.ContainerEntryDraft entry, String rawValue) {
        String normalized = IdFieldNormalizer.normalize(rawValue);
        entry.itemId = normalized;
        Item item = ContainerEntryDraftUtil.resolveItem(normalized);
        if (item == null || item == Items.AIR) {
            return;
        }

        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        int count = Math.clamp(ContainerEntryDraftUtil.parseIntOrDefault(entry.count, 1), 1, max);
        entry.count = Integer.toString(count);
        ContainerEntryDraftUtil.syncTemplateStack(entry, item, count);
    }

    private static void stepCount(ItemEditorState.ContainerEntryDraft entry, int delta) {
        Item item = ContainerEntryDraftUtil.resolveItem(entry.itemId);
        int max = item == null || item == Items.AIR ? 99 : Math.max(1, new ItemStack(item).getMaxStackSize());
        int count = Math.clamp(ContainerEntryDraftUtil.parseIntOrDefault(entry.count, 1) + delta, 1, max);
        entry.count = Integer.toString(count);
        if (entry.templateStack != null && !entry.templateStack.isEmpty()) {
            entry.templateStack = entry.templateStack.copyWithCount(count);
        }
    }

    private static void moveEntry(ItemEditorState.SpecialData special, int from, int to) {
        if (from < 0 || from >= special.bundleEntries.size() || to < 0 || to >= special.bundleEntries.size()) {
            return;
        }
        var moved = special.bundleEntries.remove(from);
        special.bundleEntries.add(to, moved);
        syncSlots(special.bundleEntries);
        special.selectedBundleIndex = to;
    }

    private static void removeEntry(ItemEditorState.SpecialData special, int index) {
        if (index < 0 || index >= special.bundleEntries.size()) {
            return;
        }
        special.bundleEntries.remove(index);
        syncSlots(special.bundleEntries);
        if (special.bundleEntries.isEmpty()) {
            special.selectedBundleIndex = -1;
            return;
        }
        special.selectedBundleIndex = Math.min(index, special.bundleEntries.size() - 1);
    }

    private static void normalizeSelection(ItemEditorState.SpecialData special) {
        if (special.bundleEntries.isEmpty()) {
            special.selectedBundleIndex = -1;
            return;
        }
        special.selectedBundleIndex = Math.clamp(special.selectedBundleIndex, 0, special.bundleEntries.size() - 1);
    }

    private static void syncSlots(List<ItemEditorState.ContainerEntryDraft> entries) {
        for (int index = 0; index < entries.size(); index++) {
            entries.get(index).slot = Integer.toString(index);
        }
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return LayoutModeUtil.isCompactPanel(context.guiScale(), context.panelWidthHint(), COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static int resolveActionButtonWidth(int contentWidth) {
        int preferred = Math.max(
                ACTION_BUTTON_WIDTH_MIN,
                Math.min(
                        ACTION_BUTTON_WIDTH_MAX,
                        (contentWidth - UiFactory.scaledPixels(ACTION_BUTTON_ROW_RESERVE)) / 2
                )
        );
        return Math.max(1, Math.min(contentWidth, preferred));
    }

    private static int resolveCountStepButtonWidth(int contentWidth) {
        int available = contentWidth - COUNT_FIELD_WIDTH - UiFactory.scaledPixels(COUNT_STEP_BUTTON_ROW_RESERVE);
        int perButton = available / 2;
        int preferred = Math.max(COUNT_STEP_BUTTON_WIDTH_MIN, Math.min(COUNT_STEP_BUTTON_WIDTH_MAX, perButton));
        return Math.max(1, Math.min(contentWidth, preferred));
    }
}
