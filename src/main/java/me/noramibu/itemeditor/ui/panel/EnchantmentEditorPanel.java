package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Collections;
import java.util.List;

public final class EnchantmentEditorPanel implements EditorPanel {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 430;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 520;
    private static final String TOOLTIP_EXPAND_ALL = "Expand All";
    private static final String TOOLTIP_COLLAPSE_ALL = "Collapse All";
    private static final int SUMMARY_WIDTH_MIN = 200;
    private static final int SUMMARY_WIDTH_RESERVE = 280;
    private static final int LEVEL_STACK_WIDTH_THRESHOLD = 620;
    private static final int LEVEL_FIELD_WIDTH_MIN = 96;
    private static final int LEVEL_FIELD_WIDTH_MAX = 156;
    private static final int LEVEL_FIELD_WIDTH_DIVISOR = 6;
    private static final int LEVEL_BUTTONS_WIDTH_MIN = 110;
    private static final int LEVEL_BUTTONS_WIDTH_BASE = 132;
    private static final int LEVEL_ROW_WIDTH_RESERVE = 140;
    private static final String SYMBOL_LEVEL_DECREMENT = "-";
    private static final String SYMBOL_LEVEL_INCREMENT = "+";

    private final ItemEditorScreen screen;

    public EnchantmentEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        Registry<Enchantment> enchantmentRegistry = this.screen.session().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> enchantmentIds = RegistryUtil.ids(enchantmentRegistry);
        FlowLayout root = UiFactory.column();

        FlowLayout intro = UiFactory.section(
                ItemEditorText.tr("enchantments.title"),
                Component.empty()
        );
        intro.child(UiFactory.checkbox(ItemEditorText.tr("enchantments.unsafe"), state.unsafeEnchantments, PanelBindings.toggle(this.screen, value -> state.unsafeEnchantments = value)));
        UiFactory.appendFillChild(root, intro);

        UiFactory.appendFillChild(root, this.buildListSection(
                "enchantments.regular.title",
                "enchantments.regular.entry",
                state.enchantments,
                false,
                enchantmentIds
        ));
        UiFactory.appendFillChild(root, this.buildListSection(
                "enchantments.stored.title",
                "enchantments.stored.entry",
                state.storedEnchantments,
                true,
                enchantmentIds
        ));
        return root;
    }

    private FlowLayout buildListSection(
            String titleKey,
            String entryKey,
            List<ItemEditorState.EnchantmentDraft> drafts,
            boolean stored,
            List<String> enchantmentIds
    ) {
        boolean compactLayout = this.isCompactLayout();
        int contentWidth = this.availableContentWidth();
        FlowLayout section = UiFactory.section(ItemEditorText.tr(titleKey), Component.empty());

        boolean hasEntries = !drafts.isEmpty();
        ButtonComponent addButton = UiFactory.positiveButton(
                stored ? ItemEditorText.tr("enchantments.stored.add") : ItemEditorText.tr("enchantments.regular.add"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> PanelBindings.mutateRefresh(this.screen, () -> {
                    ItemEditorState.EnchantmentDraft draft = new ItemEditorState.EnchantmentDraft();
                    draft.uiCollapsed = false;
                    drafts.add(draft);
                })
        );
        ButtonComponent clearButton = hasEntries
                ? UiFactory.negativeButton(ItemEditorText.tr("enchantments.clear_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                        PanelBindings.mutateRefresh(this.screen, drafts::clear)
                )
                : null;
        section.child(UiFactory.actionButtonRow(addButton, clearButton));
        if (hasEntries) {
            ButtonComponent expandAll = UiFactory.button(Component.literal(TOOLTIP_EXPAND_ALL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> drafts.forEach(entry -> entry.uiCollapsed = false))
            );
            expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL)));

            ButtonComponent collapseAll = UiFactory.button(Component.literal(TOOLTIP_COLLAPSE_ALL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> drafts.forEach(entry -> entry.uiCollapsed = true))
            );
            collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL)));
            section.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }
        int summaryWidth = Math.max(
                1,
                Math.min(
                        contentWidth,
                        Math.max(SUMMARY_WIDTH_MIN, contentWidth - UiFactory.scaledPixels(SUMMARY_WIDTH_RESERVE))
                )
        );

        for (int index = 0; index < drafts.size(); index++) {
            int currentIndex = index;
            ItemEditorState.EnchantmentDraft draft = drafts.get(currentIndex);
            FlowLayout card = UiFactory.reorderableCollapsibleSubCard(
                    ItemEditorText.tr(entryKey, index + 1),
                    Component.literal(this.enchantmentSummary(draft)),
                    summaryWidth,
                    draft.uiCollapsed,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> draft.uiCollapsed = !draft.uiCollapsed),
                    currentIndex > 0,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(drafts, currentIndex, currentIndex - 1)),
                    currentIndex < drafts.size() - 1,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(drafts, currentIndex, currentIndex + 1)),
                    () -> PanelBindings.mutateRefresh(this.screen, () -> drafts.remove(currentIndex))
            );

            if (draft.uiCollapsed) {
                section.child(card);
                continue;
            }

            ButtonComponent enchantmentButton = UiFactory.pickerButton(
                    draft.enchantmentId.isBlank() ? ItemEditorText.tr("enchantments.select") : Component.literal(draft.enchantmentId),
                    -1,
                    button -> this.screen.openSearchablePickerDialog(
                            ItemEditorText.str("enchantments.entry.enchantment"),
                            "",
                            enchantmentIds,
                            id -> id,
                            id -> PanelBindings.mutate(this.screen, () -> draft.enchantmentId = id)
                    )
            );

            card.child(UiFactory.field(
                    ItemEditorText.tr("enchantments.entry.enchantment"),
                    Component.empty(),
                    enchantmentButton
            ));

            int levelFieldWidth = Math.max(LEVEL_FIELD_WIDTH_MIN, Math.min(LEVEL_FIELD_WIDTH_MAX, contentWidth / LEVEL_FIELD_WIDTH_DIVISOR));
            int levelButtonsWidth = Math.max(LEVEL_BUTTONS_WIDTH_MIN, UiFactory.scaledPixels(LEVEL_BUTTONS_WIDTH_BASE));
            int staticStackThreshold = UiFactory.scaledPixels(LEVEL_STACK_WIDTH_THRESHOLD);
            int dynamicStackThreshold = levelFieldWidth + levelButtonsWidth + UiFactory.scaledPixels(LEVEL_ROW_WIDTH_RESERVE);
            boolean stackLevelControls = compactLayout || contentWidth < Math.max(staticStackThreshold, dynamicStackThreshold);
            FlowLayout levelControls = stackLevelControls ? UiFactory.column() : UiFactory.row();
            levelControls.horizontalSizing(Sizing.fill(100));
            levelControls.child(UiFactory.textBox(
                    draft.level,
                    PanelBindings.text(this.screen, value -> draft.level = value)
            ).horizontalSizing(stackLevelControls ? Sizing.fill(100) : UiFactory.fixed(levelFieldWidth)));

            FlowLayout levelButtons = UiFactory.row();
            levelButtons.horizontalSizing(stackLevelControls ? Sizing.fill(100) : Sizing.fixed(levelButtonsWidth));
            ButtonComponent minusLevel = UiFactory.negativeButton(Component.literal(SYMBOL_LEVEL_DECREMENT), UiFactory.ButtonTextPreset.COMPACT,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> draft.level = Integer.toString(this.adjustLevel(draft.level, -1)))
            );
            minusLevel.horizontalSizing(Sizing.fill(50));
            levelButtons.child(minusLevel);
            ButtonComponent plusLevel = UiFactory.positiveButton(Component.literal(SYMBOL_LEVEL_INCREMENT), UiFactory.ButtonTextPreset.COMPACT,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> draft.level = Integer.toString(this.adjustLevel(draft.level, 1)))
            );
            plusLevel.horizontalSizing(Sizing.fill(50));
            levelButtons.child(plusLevel);
            levelControls.child(levelButtons);
            card.child(UiFactory.field(
                    ItemEditorText.tr("enchantments.entry.level"),
                    Component.empty(),
                    levelControls
            ));
            section.child(card);
        }

        return section;
    }

    private String enchantmentSummary(ItemEditorState.EnchantmentDraft draft) {
        String id = draft.enchantmentId == null || draft.enchantmentId.isBlank() ? "-" : draft.enchantmentId;
        String level = draft.level == null || draft.level.isBlank() ? "1" : draft.level;
        return id + " (" + level + ")";
    }

    private int adjustLevel(String raw, int delta) {
        int value;
        try {
            value = Integer.parseInt(raw == null || raw.isBlank() ? "1" : raw.trim());
        } catch (NumberFormatException ignored) {
            value = 1;
        }
        value += delta;
        return Math.max(1, value);
    }

    private boolean isCompactLayout() {
        int contentWidth = this.availableContentWidth();
        var window = Minecraft.getInstance().getWindow();
        return LayoutModeUtil.isCompactWindowAndContentInclusive(
                window.getGuiScale(),
                window.getGuiScaledWidth(),
                COMPACT_LAYOUT_WIDTH_THRESHOLD,
                contentWidth,
                COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD
        );
    }

    private int availableContentWidth() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

}
