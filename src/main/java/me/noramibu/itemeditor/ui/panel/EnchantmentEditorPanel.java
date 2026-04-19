package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
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
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 520;
    private static final int TOGGLE_BUTTON_WIDTH_MIN = 26;
    private static final int TOGGLE_BUTTON_WIDTH_BASE = 34;
    private static final int CLEAR_BUTTON_WIDTH_MIN = 72;
    private static final int CLEAR_BUTTON_WIDTH_BASE = 92;
    private static final int ACTIONS_WIDTH_RESERVE = 10;
    private static final int ACTION_ROW_WIDTH_RESERVE = 16;
    private static final int ADD_BUTTON_WIDTH_MIN = 112;
    private static final int ADD_BUTTON_WIDTH_BASE = 156;
    private static final String SYMBOL_SECTION_COLLAPSED = "[+]";
    private static final String SYMBOL_SECTION_EXPANDED = "[-]";
    private static final String TOOLTIP_EXPAND_ALL = "Expand all";
    private static final String TOOLTIP_COLLAPSE_ALL = "Collapse all";
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
        int toggleWidth = Math.max(TOGGLE_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(TOGGLE_BUTTON_WIDTH_BASE));
        int clearWidth = Math.max(CLEAR_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(CLEAR_BUTTON_WIDTH_BASE));
        int staticActionsWidth = hasEntries
                ? (clearWidth + (toggleWidth * 2) + UiFactory.scaledPixels(ACTIONS_WIDTH_RESERVE))
                : 0;
        int minAddWidth = Math.max(ADD_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(ADD_BUTTON_WIDTH_BASE));
        int minActionRowWidth = minAddWidth + staticActionsWidth + UiFactory.scaledPixels(ACTION_ROW_WIDTH_RESERVE);
        boolean stackActions = compactLayout || contentWidth < minActionRowWidth;
        FlowLayout actions = stackActions ? UiFactory.column() : UiFactory.row();
        ButtonComponent addButton = UiFactory.button(stored ? ItemEditorText.tr("enchantments.stored.add") : ItemEditorText.tr("enchantments.regular.add"), UiFactory.ButtonTextPreset.STANDARD,  button -> {
            PanelBindings.mutateRefresh(this.screen, () -> {
                ItemEditorState.EnchantmentDraft draft = new ItemEditorState.EnchantmentDraft();
                draft.uiCollapsed = false;
                drafts.add(draft);
            });
        });
        addButton.horizontalSizing(stackActions ? Sizing.fill(100) : Sizing.expand(100));
        actions.child(addButton);
        if (hasEntries) {
            ButtonComponent clearButton = UiFactory.button(ItemEditorText.tr("general.adventure.clear"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, drafts::clear)
            );
            clearButton.horizontalSizing(stackActions ? Sizing.fill(100) : Sizing.fixed(clearWidth));
            actions.child(clearButton);

            ButtonComponent expandAll = UiFactory.button(Component.literal(SYMBOL_SECTION_COLLAPSED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> drafts.forEach(entry -> entry.uiCollapsed = false))
            );
            expandAll.horizontalSizing(stackActions ? Sizing.fill(100) : Sizing.fixed(toggleWidth));
            expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL)));
            actions.child(expandAll);

            ButtonComponent collapseAll = UiFactory.button(Component.literal(SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> drafts.forEach(entry -> entry.uiCollapsed = true))
            );
            collapseAll.horizontalSizing(stackActions ? Sizing.fill(100) : Sizing.fixed(toggleWidth));
            collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL)));
            actions.child(collapseAll);
        }
        section.child(actions);
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
            FlowLayout card = UiFactory.reorderableSubCard(
                    ItemEditorText.tr(entryKey, index + 1),
                    currentIndex > 0,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(drafts, currentIndex, currentIndex - 1)),
                    currentIndex < drafts.size() - 1,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(drafts, currentIndex, currentIndex + 1)),
                    () -> PanelBindings.mutateRefresh(this.screen, () -> drafts.remove(currentIndex))
            );

            card.child(UiFactory.collapsibleSummaryRow(
                    Component.literal(this.enchantmentSummary(draft)),
                    summaryWidth,
                    draft.uiCollapsed,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> draft.uiCollapsed = !draft.uiCollapsed)
            ));

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
            ButtonComponent minusLevel = UiFactory.button(Component.literal(SYMBOL_LEVEL_DECREMENT), UiFactory.ButtonTextPreset.COMPACT,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> draft.level = Integer.toString(this.adjustLevel(draft.level, -1)))
            );
            minusLevel.horizontalSizing(Sizing.fill(50));
            levelButtons.child(minusLevel);
            ButtonComponent plusLevel = UiFactory.button(Component.literal(SYMBOL_LEVEL_INCREMENT), UiFactory.ButtonTextPreset.COMPACT,  button ->
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
        return window.getGuiScaledWidth() <= COMPACT_LAYOUT_WIDTH_THRESHOLD
                || window.getGuiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || contentWidth < UiFactory.scaledPixels(COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
    }

    private int availableContentWidth() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

}
