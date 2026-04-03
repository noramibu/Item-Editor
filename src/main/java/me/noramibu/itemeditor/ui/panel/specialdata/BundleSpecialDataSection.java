package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BundleSpecialDataSection {

    private static final int SEARCH_PICKER_BUTTON_WIDTH = 132;

    private BundleSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsBundleData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        normalizeSelection(special);
        syncSlots(special.bundleEntries);

        Registry<Item> itemRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.ITEM);
        List<String> itemIds = RegistryUtil.ids(itemRegistry).stream()
                .filter(id -> !Objects.equals(id, "minecraft:air"))
                .toList();

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.bundle.title"), Component.empty());
        section.child(buildMetricsCard(special));
        section.child(buildActions(context, special));

        if (special.bundleEntries.isEmpty()) {
            section.child(UiFactory.muted(ItemEditorText.tr("special.bundle.empty"), 280));
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

        Fraction weight = contents.weight();
        double fillPercent = weight.doubleValue() * 100d;
        card.child(UiFactory.muted(ItemEditorText.tr(
                "special.bundle.metrics.value",
                contents.size(),
                weight.getNumerator(),
                weight.getDenominator(),
                formatPercent(fillPercent)
        ), 280));

        if (fillPercent > 100d) {
            card.child(UiFactory.message(ItemEditorText.tr("special.bundle.metrics.overfilled"), 0xFF8A8A));
        }

        return card;
    }

    private static FlowLayout buildActions(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout row = UiFactory.row();
        row.child(UiFactory.button(ItemEditorText.tr("special.bundle.add"), button ->
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
        ).horizontalSizing(Sizing.content()));

        ButtonComponent remove = UiFactory.button(ItemEditorText.tr("special.bundle.remove_selected"), button ->
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
        row.child(remove.horizontalSizing(Sizing.content()));
        return row;
    }

    private static FlowLayout buildEntryCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            List<String> itemIds,
            int index
    ) {
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

        FlowLayout preview = UiFactory.row();
        preview.child(UIComponents.item(stack).showOverlay(true));
        preview.child(UiFactory.muted(stack.isEmpty()
                ? ItemEditorText.tr("special.bundle.entry.empty")
                : ItemEditorText.tr("special.bundle.entry.stack", stack.getHoverName(), stack.getCount()), 240));
        card.child(preview);

        FlowLayout itemInput = UiFactory.row();
        itemInput.child(UiFactory.textBox(entry.itemId, value -> context.mutateRefresh(() -> updateItemId(entry, value))));
        itemInput.child(UiFactory.button(ItemEditorText.tr("special.bundle.select_item"), button ->
                context.openSearchablePicker(
                        ItemEditorText.str("special.bundle.item_picker_title"),
                        "",
                        itemIds,
                        value -> value,
                        selected -> context.mutateRefresh(() -> updateItemId(entry, selected))
                )
        ).horizontalSizing(Sizing.fixed(SEARCH_PICKER_BUTTON_WIDTH)));
        card.child(UiFactory.field(ItemEditorText.tr("special.container.item"), Component.empty(), itemInput));

        FlowLayout countRow = UiFactory.row();
        countRow.child(UiFactory.button(ItemEditorText.tr("special.container.count_decrease"), button ->
                context.mutateRefresh(() -> stepCount(entry, -1))
        ).horizontalSizing(Sizing.content()));
        countRow.child(UiFactory.textBox(entry.count, value -> context.mutate(() -> entry.count = value)).horizontalSizing(Sizing.fixed(104)));
        countRow.child(UiFactory.button(ItemEditorText.tr("special.container.count_increase"), button ->
                context.mutateRefresh(() -> stepCount(entry, 1))
        ).horizontalSizing(Sizing.content()));
        card.child(UiFactory.field(ItemEditorText.tr("special.container.count"), Component.empty(), countRow));

        if (invalidId) {
            card.child(UiFactory.message(ItemEditorText.tr("special.container.invalid_item", entry.itemId), 0xFF8A8A));
        } else if (cannotFit) {
            card.child(UiFactory.message(ItemEditorText.tr("special.bundle.item_not_allowed"), 0xFF8A8A));
        }

        if (special.selectedBundleIndex != index) {
            card.child(UiFactory.button(ItemEditorText.tr("special.bundle.select_entry"), button ->
                    context.mutateRefresh(() -> special.selectedBundleIndex = index)
            ).horizontalSizing(Sizing.content()));
        } else {
            card.child(UiFactory.muted(ItemEditorText.tr("special.bundle.selected"), 160));
        }

        return card;
    }

    private static BundleContents buildContents(List<ItemEditorState.ContainerEntryDraft> entries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEditorState.ContainerEntryDraft entry : entries) {
            ItemStack stack = stackForEntry(entry);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
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
                int fallbackCount = parseCount(entry.count, entry.templateStack.getCount());
                return entry.templateStack.copyWithCount(Math.max(1, fallbackCount));
            }
            return ItemStack.EMPTY;
        }

        int max = Math.max(1, new ItemStack(item).getMaxStackSize());
        int count = Math.clamp(parseCount(entry.count, 1), 1, max);
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
        int count = Math.clamp(parseCount(entry.count, 1), 1, max);
        entry.count = Integer.toString(count);
        ContainerEntryDraftUtil.syncTemplateStack(entry, item, count);
    }

    private static void stepCount(ItemEditorState.ContainerEntryDraft entry, int delta) {
        Item item = ContainerEntryDraftUtil.resolveItem(entry.itemId);
        int max = item == null || item == Items.AIR ? 99 : Math.max(1, new ItemStack(item).getMaxStackSize());
        int count = Math.clamp(parseCount(entry.count, 1) + delta, 1, max);
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

    private static int parseCount(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }
}
