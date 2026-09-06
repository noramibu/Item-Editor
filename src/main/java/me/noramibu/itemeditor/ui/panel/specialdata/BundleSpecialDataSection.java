package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ContainerEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

public final class BundleSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        return SpecialDataSearch.targets(
                context, EditorCategory.SPECIAL_DATA, "special.bundle.title", "bundle", () -> {}, Field.values());
    }

    private static final int EMPTY_HINT_WIDTH = 280;
    private static final int METRICS_HINT_WIDTH = 280;

    private BundleSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsBundleData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        int maxPage = maxBundlePage(special);
        special.bundleEditorPage = Math.clamp(special.bundleEditorPage, 0, maxPage);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.bundle.title"), Component.empty());
        section.id("bundle");
        section.child(buildMetricsCard(special));
        section.child(UiFactory.muted(
                ItemEditorText.tr("special.bundle.summary", special.bundleEntries.size(), maxPage + 1),
                EMPTY_HINT_WIDTH));
        section.child(UiFactory.actionButtonRow(openButton(context, special)));
        section.child(UiFactory.actionButtonRow(resetButton(context, special), clearButton(context, special)));
        section.child(UiFactory.muted(ItemEditorText.tr("special.bundle.editor_hint"), EMPTY_HINT_WIDTH));

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
        card.child(UiFactory.muted(
                ItemEditorText.tr(
                        "special.bundle.metrics.value",
                        contents.size(),
                        weight.getNumerator(),
                        weight.getDenominator(),
                        formatPercent(fillPercent)),
                METRICS_HINT_WIDTH));

        if (fillPercent > 100d) {
            card.child(UiFactory.message(ItemEditorText.tr("special.bundle.metrics.overfilled"), 0xFF8A8A));
        }

        return card;
    }

    private static ButtonComponent openButton(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return UiFactory.button(
                Field.OPEN_EDITOR.text(), UiFactory.ButtonTextPreset.COMPACT, ignored -> context.screen()
                        .session()
                        .minecraft()
                        .setScreen(ContainerEditorScreen.bundle(context.screen(), special)));
    }

    private static ButtonComponent resetButton(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return UiFactory.button(
                Field.RESET.text(),
                UiFactory.ButtonTextPreset.COMPACT,
                ignored -> context.mutateRefresh(() -> resetFromOriginal(special, context.originalStack())));
    }

    private static ButtonComponent clearButton(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return UiFactory.button(
                Field.CLEAR_ALL.text().copy().withColor(0xFF8A8A),
                UiFactory.ButtonTextPreset.COMPACT,
                ignored -> context.mutateRefresh(() -> {
                    special.bundleEntries.clear();
                    special.selectedBundleIndex = -1;
                    special.bundleEditorPage = 0;
                }));
    }

    private static void resetFromOriginal(ItemEditorState.SpecialData special, ItemStack originalStack) {
        special.bundleEntries.clear();
        special.selectedBundleIndex = -1;
        special.bundleEditorPage = 0;
        BundleContents contents = originalStack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return;
        }

        int index = 0;
        for (ItemStack stack : contents.items()) {
            if (stack.isEmpty() || stack.is(Items.AIR)) {
                continue;
            }
            special.bundleEntries.add(ItemEditorState.ContainerEntryDraft.fromSlot(
                    index, stack.copyWithCount(Math.max(1, stack.getCount()))));
            index++;
        }
        special.selectedBundleIndex = special.bundleEntries.isEmpty() ? -1 : 0;
    }

    private static int maxBundlePage(ItemEditorState.SpecialData special) {
        return Math.max(0, special.bundleEntries.size() / ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
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
                int fallbackCount = ValidationUtil.parseIntOrDefault(entry.count, entry.templateStack.getCount());
                return entry.templateStack.copyWithCount(Math.max(1, fallbackCount));
            }
            return ItemStack.EMPTY;
        }

        int max = entry.templateStack != null && !entry.templateStack.isEmpty() && entry.templateStack.is(item)
                ? Math.max(1, entry.templateStack.getMaxStackSize())
                : Math.max(1, new ItemStack(item).getMaxStackSize());
        int count = Math.clamp(ValidationUtil.parseIntOrDefault(entry.count, 1), 1, max);
        if (entry.templateStack != null && !entry.templateStack.isEmpty() && entry.templateStack.is(item)) {
            return entry.templateStack.copyWithCount(count);
        }
        return new ItemStack(item, count);
    }

    private enum Field implements SpecialDataSearch.Field {
        OPEN_EDITOR("special.bundle.open_editor"),
        RESET("common.reset"),
        CLEAR_ALL("common.clear_all");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
