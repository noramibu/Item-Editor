package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class GeneralEditorPanel implements EditorPanel {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 1150;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 760;
    private static final int STACK_COLUMNS_WIDTH_THRESHOLD = 720;
    private static final int STACK_ROW_MARGIN_TOP = 2;
    private static final int FIELD_LABEL_WIDTH_MIN = 72;
    private static final int FIELD_LABEL_DYNAMIC_MIN = 88;
    private static final int FIELD_LABEL_COMPACT_RESERVE = 56;
    private static final int FIELD_LABEL_REGULAR_RESERVE = 42;
    private static final int RARITY_BUTTON_WIDTH = 140;
    private static final int DURABILITY_NUMERIC_FIELD_WIDTH = 96;
    private static final int DURABILITY_LABEL_WIDTH_CURRENT = 140;
    private static final int DURABILITY_LABEL_WIDTH_MAX = 120;
    private static final int DURABILITY_LABEL_WIDTH_REPAIR = 110;
    private static final int ITEM_MODEL_ID_LABEL_WIDTH = 190;
    private static final int ITEM_MODEL_VALUE_LABEL_WIDTH = 170;
    private static final int ITEM_MODEL_PICK_BUTTON_WIDTH_MIN = 64;
    private static final int ITEM_MODEL_PICK_BUTTON_WIDTH_MAX = 116;
    private static final int ITEM_MODEL_PICK_BUTTON_WIDTH_DIVISOR = 7;
    private static final int ITEM_MODEL_STACK_PICK_THRESHOLD = 420;
    private static final int ITEM_MODEL_VALUE_ROW_STACK_THRESHOLD = 720;
    private static final int ITEM_MODEL_ID_INPUT_MIN_WIDTH = 140;
    private static final int ITEM_MODEL_TEXTBOX_HEIGHT_REDUCTION = 2;
    private static final int ITEM_MODEL_TEXTBOX_MIN_HEIGHT = 14;
    private static final int UNBOUNDED_TEXT_LIMIT = Integer.MAX_VALUE;
    private static final int CUSTOM_NAME_EDITOR_HEIGHT = 54;
    private static final int ADVENTURE_COUNT_HINT_WIDTH = 60;
    private static final int ADVENTURE_EMPTY_HINT_WIDTH = 320;
    private static final String SYMBOL_MOVE_UP = "^";
    private static final String SYMBOL_MOVE_DOWN = "v";
    private static final String SYMBOL_REMOVE = "x";
    private static final String SYMBOL_ADD = "+";
    private static final int INLINE_CHECKBOX_SIZE_BASE = 18;
    private static final int INLINE_CHECKBOX_SIZE_MIN = 14;
    private static final int ADVENTURE_COLLAPSE_TOGGLE_MIN_WIDTH = 36;
    private static final int ADVENTURE_COLLAPSE_TOGGLE_BASE_WIDTH = 42;
    private static final int ADVENTURE_STACK_CONTROLS_WIDTH_THRESHOLD = 520;
    private static final int RESPONSIVE_CHECKBOX_LABEL_RESERVE = 220;
    private static final int RESPONSIVE_CHECKBOX_LABEL_MIN_WIDTH = 120;
    private static final int ADVENTURE_ENTRY_LABEL_MIN_WIDTH = 120;
    private static final int ADVENTURE_ENTRY_LABEL_INLINE_RESERVE = 220;
    private static final int ENTRY_ACTION_BUTTON_SIZE_BASE = 32;
    private static final int ENTRY_ACTION_BUTTON_SIZE_MIN = 26;

    private final ItemEditorScreen screen;

    public GeneralEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        ItemStack stack = this.screen.session().originalStack();
        RegistryAccess registryAccess = this.screen.session().registryAccess();
        boolean compactLayout = this.useCompactLayout();
        boolean supportsDurability = ItemEditorCapabilities.supportsDurability(stack);
        boolean supportsRepairCost = ItemEditorCapabilities.supportsRepairCost(stack);
        boolean supportsCanBreak = ItemEditorCapabilities.supportsComponent(stack, registryAccess, "minecraft:can_break");
        boolean supportsCanPlaceOn = ItemEditorCapabilities.supportsComponent(stack, registryAccess, "minecraft:can_place_on");

        FlowLayout root = UiFactory.column();
        UiFactory.appendFillChild(root, this.buildIdentitySection(state, compactLayout));

        if (supportsDurability || supportsRepairCost) {
            UiFactory.appendFillChild(root, this.buildDurabilitySection(state, supportsDurability, supportsRepairCost, compactLayout));
        }

        if (supportsCanBreak || supportsCanPlaceOn) {
            UiFactory.appendFillChild(root, this.buildAdventureSection(state, supportsCanBreak, supportsCanPlaceOn, compactLayout));
        }
        UiFactory.appendFillChild(root, this.buildVisualOverridesSection(state));
        UiFactory.appendFillChild(root, this.buildItemModelSection(state, compactLayout));
        return root;
    }

    private FlowLayout buildIdentitySection(ItemEditorState state, boolean compactLayout) {
        FlowLayout identity = UiFactory.section(ItemEditorText.tr("general.identity.title"), Component.empty());

        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                this.screen,
                RichTextDocument.fromMarkup(state.customName),
                Sizing.fill(100),
                UiFactory.fixed(CUSTOM_NAME_EDITOR_HEIGHT),
                ItemEditorText.str("general.identity.custom_name.placeholder"),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str("general.identity.custom_name.color_title"),
                ItemEditorText.str("general.identity.custom_name.gradient_title"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1 ? ItemEditorText.str("general.identity.custom_name.single_line") : null,
                document -> PanelBindings.text(this.screen, value -> state.customName = value)
                        .accept(TextComponentUtil.ensureObjectTokenColors(TextComponentUtil.serializeEditorDocument(document), 0xFFFFFF)),
                compactLayout
        );
        this.applyRichEditorRenderMode(nameSection.editor(), state.uiRenderObjectsInCustomName);

        FlowLayout editorFrame = UiFactory.framedEditorCard();
        editorFrame.child(nameSection.toolbar());
        editorFrame.child(UiFactory.checkbox(
                ItemEditorText.tr("general.identity.custom_name.render_objects"),
                state.uiRenderObjectsInCustomName,
                value -> {
                    state.uiRenderObjectsInCustomName = value;
                    this.applyRichEditorRenderMode(nameSection.editor(), value);
                }
        ));
        editorFrame.child(nameSection.editor());
        editorFrame.child(nameSection.validation());
        identity.child(editorFrame);

        int defaultMaxStackSize = this.defaultMaxStackSize();
        String displayedMaxStackSize = state.special.maxStackSize.isBlank()
                ? Integer.toString(defaultMaxStackSize)
                : state.special.maxStackSize;

        double guiScale = this.screen.session().minecraft().getWindow().getGuiScale();
        int scaledWidth = this.screen.session().minecraft().getWindow().getGuiScaledWidth();
        boolean stackAsColumns = compactLayout
                && (scaledWidth < STACK_COLUMNS_WIDTH_THRESHOLD || guiScale >= LayoutModeUtil.DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD);
        FlowLayout stackRow = stackAsColumns ? UiFactory.column() : UiFactory.row();
        stackRow.gap(Math.max(2, UiFactory.scaleProfile().tightSpacing()));
        stackRow.margins(Insets.top(STACK_ROW_MARGIN_TOP));

        FlowLayout stackCountField = this.compactField(
                ItemEditorText.tr("general.stack_count"),
                UiFactory.textBox(state.count, PanelBindings.text(this.screen, value -> state.count = value)).horizontalSizing(Sizing.fill(100)),
                140,
                true
        );
        FlowLayout maxStackField = this.compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.max_stack_size"),
                UiFactory.textBox(
                        displayedMaxStackSize,
                        PanelBindings.text(this.screen, value -> {
                            String trimmed = value == null ? "" : value.trim();
                            state.special.maxStackSize = trimmed.isBlank() || trimmed.equals(Integer.toString(defaultMaxStackSize))
                                    ? ""
                                    : value;
                        })
                ).horizontalSizing(Sizing.fill(100)),
                180,
                true
        );

        if (!stackAsColumns) {
            stackCountField.horizontalSizing(Sizing.expand(100));
            maxStackField.horizontalSizing(Sizing.expand(100));
        }
        stackRow.child(stackCountField);
        stackRow.child(maxStackField);
        identity.child(stackRow);

        var rarityButton = UiFactory.button(state.rarity, UiFactory.ButtonTextPreset.STANDARD,  button -> this.screen.openDropdown(
                button,
                Arrays.asList(Rarity.values()),
                Rarity::name,
                PanelBindings.value(this.screen, rarity -> state.rarity = rarity.name())
        ));
        FlowLayout rarityRow = compactLayout ? UiFactory.column() : UiFactory.row();
        rarityRow.child(this.compactField(
                ItemEditorText.tr("general.rarity"),
                rarityButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(RARITY_BUTTON_WIDTH)),
                150,
                compactLayout
        ));
        identity.child(rarityRow);
        return identity;
    }

    private FlowLayout compactField(Component label, UIComponent input, int labelWidth, boolean compactLayout) {
        FlowLayout field = UiFactory.column();
        field.gap(2);
        int contentWidth = this.availableContentWidth();
        int dynamicCap = compactLayout
                ? Math.max(1, contentWidth - UiFactory.scaledPixels(FIELD_LABEL_COMPACT_RESERVE))
                : Math.max(1, contentWidth - UiFactory.scaledPixels(FIELD_LABEL_REGULAR_RESERVE));
        int preferredLabelWidth = Math.max(FIELD_LABEL_WIDTH_MIN, Math.min(labelWidth, Math.max(FIELD_LABEL_DYNAMIC_MIN, dynamicCap)));
        int effectiveLabelWidth = Math.max(1, Math.min(contentWidth, preferredLabelWidth));
        Component fittedLabel = UiFactory.fitToWidth(label, effectiveLabelWidth);
        LabelComponent labelComponent = UiFactory.muted(fittedLabel, effectiveLabelWidth);
        labelComponent.horizontalSizing(Sizing.fill(100));
        if (!fittedLabel.getString().equals(label.getString())) {
            labelComponent.tooltip(List.of(label));
        }
        labelComponent.margins(Insets.top(1));
        field.child(labelComponent);
        field.child(input);
        return field;
    }

    private int defaultMaxStackSize() {
        ItemStack original = this.screen.session().originalStack();
        ItemStack base = new ItemStack(original.getItem());
        return Math.max(1, base.getMaxStackSize());
    }

    private FlowLayout buildDurabilitySection(ItemEditorState state, boolean supportsDurability, boolean supportsRepairCost, boolean compactLayout) {
        FlowLayout durability = UiFactory.section(ItemEditorText.tr("general.durability.title"), Component.empty());
        int numericWidth = DURABILITY_NUMERIC_FIELD_WIDTH;

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        if (supportsDurability) {
            row.child(this.compactField(
                    ItemEditorText.tr("general.current_damage"),
                    UiFactory.textBox(state.currentDamage, PanelBindings.text(this.screen, value -> state.currentDamage = value))
                            .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)),
                    DURABILITY_LABEL_WIDTH_CURRENT,
                    compactLayout
            ));
            row.child(this.compactField(
                    ItemEditorText.tr("general.max_damage"),
                    UiFactory.textBox(state.maxDamage, PanelBindings.text(this.screen, value -> state.maxDamage = value))
                            .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)),
                    DURABILITY_LABEL_WIDTH_MAX,
                    compactLayout
            ));
        }
        if (supportsRepairCost) {
            row.child(this.compactField(
                    ItemEditorText.tr("general.repair_cost"),
                    UiFactory.textBox(state.repairCost, PanelBindings.text(this.screen, value -> state.repairCost = value))
                            .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)),
                    DURABILITY_LABEL_WIDTH_REPAIR,
                    compactLayout
            ));
        }
        if (!compactLayout) {
            this.distributeRowChildren(row);
        }
        durability.child(row);

        if (supportsDurability) {
            durability.child(UiFactory.checkbox(ItemEditorText.tr("general.unbreakable"), state.unbreakable, PanelBindings.toggle(this.screen, value -> state.unbreakable = value)));
        }
        return durability;
    }

    private FlowLayout buildAdventureSection(ItemEditorState state, boolean supportsCanBreak, boolean supportsCanPlaceOn, boolean compactLayout) {
        FlowLayout adventure = UiFactory.section(ItemEditorText.tr("general.adventure.title"), Component.empty());
        List<String> blockIds = BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString).sorted().toList();

        if (supportsCanBreak) {
            adventure.child(this.buildAdventureListCard(
                    ItemEditorText.tr("general.adventure.can_break"),
                    state.canBreakBlockIds,
                    state.canBreakShowInTooltip,
                    value -> {
                        state.canBreakShowInTooltip = value;
                        if (value) {
                            state.hiddenTooltipComponents.remove("minecraft:can_break");
                        } else {
                            state.hiddenTooltipComponents.add("minecraft:can_break");
                        }
                    },
                    blockIds,
                    state.canBreakCollapsed,
                    value -> state.canBreakCollapsed = value,
                    compactLayout
            ));
        }
        if (supportsCanPlaceOn) {
            adventure.child(this.buildAdventureListCard(
                    ItemEditorText.tr("general.adventure.can_place_on"),
                    state.canPlaceOnBlockIds,
                    state.canPlaceOnShowInTooltip,
                    value -> {
                        state.canPlaceOnShowInTooltip = value;
                        if (value) {
                            state.hiddenTooltipComponents.remove("minecraft:can_place_on");
                        } else {
                            state.hiddenTooltipComponents.add("minecraft:can_place_on");
                        }
                    },
                    blockIds,
                    state.canPlaceOnCollapsed,
                    value -> state.canPlaceOnCollapsed = value,
                    compactLayout
            ));
        }
        return adventure;
    }

    private FlowLayout buildAdventureListCard(
            Component title,
            List<String> blockIds,
            boolean showInTooltip,
            Consumer<Boolean> tooltipUpdater,
            List<String> allBlocks,
            boolean collapsed,
            Consumer<Boolean> collapsedSetter,
            boolean compactLayout
    ) {
        FlowLayout card = UiFactory.subCard();
        int contentWidth = this.availableContentWidth();
        Runnable openAddBlockPicker = () -> this.screen.openSearchablePickerDialog(
                ItemEditorText.str("general.adventure.picker_title"),
                ItemEditorText.str("general.adventure.picker_body"),
                allBlocks,
                id -> id,
                selected -> PanelBindings.mutateRefresh(this.screen, () -> this.addUnique(blockIds, selected))
        );
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(title).shadow(false).horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.muted(Component.literal("(" + blockIds.size() + ")"), ADVENTURE_COUNT_HINT_WIDTH));
        ButtonComponent headerAddButton = UiFactory.button(Component.literal(SYMBOL_ADD), UiFactory.ButtonTextPreset.STANDARD,  button ->
                openAddBlockPicker.run()
        );
        headerAddButton.horizontalSizing(Sizing.fixed(Math.max(ADVENTURE_COLLAPSE_TOGGLE_MIN_WIDTH, UiFactory.scaledPixels(ADVENTURE_COLLAPSE_TOGGLE_BASE_WIDTH))));
        headerAddButton.tooltip(List.of(ItemEditorText.tr("general.adventure.add_block")));
        header.child(headerAddButton);
        ButtonComponent collapseToggle = UiFactory.button(LayoutModeUtil.sectionToggleText(collapsed), UiFactory.ButtonTextPreset.STANDARD,  button ->
                PanelBindings.mutateRefresh(this.screen, () -> collapsedSetter.accept(!collapsed))
        );
        collapseToggle.horizontalSizing(Sizing.fixed(Math.max(ADVENTURE_COLLAPSE_TOGGLE_MIN_WIDTH, UiFactory.scaledPixels(ADVENTURE_COLLAPSE_TOGGLE_BASE_WIDTH))));
        header.child(collapseToggle);
        card.child(header);

        boolean stackControls = compactLayout || contentWidth < UiFactory.scaledPixels(ADVENTURE_STACK_CONTROLS_WIDTH_THRESHOLD);
        FlowLayout controls = stackControls ? UiFactory.column() : UiFactory.row();
        controls.child(UiFactory.checkbox(
                ItemEditorText.tr("general.adventure.show_tooltip"),
                showInTooltip,
                PanelBindings.toggle(this.screen, tooltipUpdater)
        ));

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("general.adventure.add_block"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                openAddBlockPicker.run()
        );
        addButton.horizontalSizing(stackControls ? Sizing.fill(100) : Sizing.expand(100));
        controls.child(addButton);

        if (!blockIds.isEmpty()) {
            ButtonComponent clearButton = UiFactory.button(ItemEditorText.tr("general.adventure.clear"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, blockIds::clear)
            );
            clearButton.horizontalSizing(stackControls ? Sizing.fill(100) : Sizing.expand(100));
            controls.child(clearButton);
        }

        if (!stackControls) {
            this.distributeRowChildren(controls);
        }
        card.child(controls);

        if (collapsed) {
            return card;
        }

        if (blockIds.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("general.adventure.empty"), ADVENTURE_EMPTY_HINT_WIDTH));
            return card;
        }

        int effectiveContentWidth = Math.max(
                contentWidth,
                UiFactory.scaledPixels(ADVENTURE_ENTRY_LABEL_INLINE_RESERVE + ADVENTURE_ENTRY_LABEL_MIN_WIDTH)
        );
        for (int index = 0; index < blockIds.size(); index++) {
            int currentIndex = index;
            String blockId = blockIds.get(index);
            FlowLayout row = UiFactory.row();
            int labelWidth = Math.max(
                    ADVENTURE_ENTRY_LABEL_MIN_WIDTH,
                    effectiveContentWidth - UiFactory.scaledPixels(ADVENTURE_ENTRY_LABEL_INLINE_RESERVE)
            );
            LabelComponent label = UiFactory.muted(UiFactory.fitToWidth(Component.literal(blockId), labelWidth), labelWidth);
            label.tooltip(List.of(Component.literal(blockId)));
            label.horizontalSizing(Sizing.content());
            row.child(label);

            FlowLayout actionRow = UiFactory.row();
            if (currentIndex > 0) {
                actionRow.child(this.actionButton(Component.literal(SYMBOL_MOVE_UP), ItemEditorText.tr("common.up"), () ->
                        PanelBindings.mutateRefresh(this.screen, () -> this.swap(blockIds, currentIndex, currentIndex - 1))
                ));
            }
            if (currentIndex < blockIds.size() - 1) {
                actionRow.child(this.actionButton(Component.literal(SYMBOL_MOVE_DOWN), ItemEditorText.tr("common.down"), () ->
                        PanelBindings.mutateRefresh(this.screen, () -> this.swap(blockIds, currentIndex, currentIndex + 1))
                ));
            }
            actionRow.child(this.actionButton(Component.literal(SYMBOL_REMOVE), ItemEditorText.tr("common.remove"), () ->
                    PanelBindings.mutateRefresh(this.screen, () -> blockIds.remove(currentIndex))
            ));
            row.child(actionRow);
            card.child(row);
        }

        return card;
    }

    private ButtonComponent actionButton(Component label, Component tooltip, Runnable action) {
        ButtonComponent button = UiFactory.button(label, UiFactory.ButtonTextPreset.COMPACT, component -> action.run());
        int size = Math.max(ENTRY_ACTION_BUTTON_SIZE_MIN, UiFactory.scaledPixels(ENTRY_ACTION_BUTTON_SIZE_BASE));
        button.horizontalSizing(Sizing.fixed(size));
        button.tooltip(List.of(tooltip));
        return button;
    }

    private void addUnique(List<String> values, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private void swap(List<String> values, int left, int right) {
        if (left < 0 || right < 0 || left >= values.size() || right >= values.size()) {
            return;
        }
        String temp = values.get(left);
        values.set(left, values.get(right));
        values.set(right, temp);
    }

    private FlowLayout buildVisualOverridesSection(ItemEditorState state) {
        FlowLayout visual = UiFactory.section(ItemEditorText.tr("general.visual_overrides.title"), Component.empty());
        visual.child(this.responsiveCheckboxLine(
                ItemEditorText.tr("general.glint_override.enable"),
                state.glintOverrideEnabled,
                PanelBindings.toggle(this.screen, value -> state.glintOverrideEnabled = value)
        ));
        visual.child(this.responsiveCheckboxLine(
                ItemEditorText.tr("general.glint_override.force"),
                state.glintOverride,
                PanelBindings.toggle(this.screen, value -> state.glintOverride = value)
        ));
        return visual;
    }

    private FlowLayout buildItemModelSection(ItemEditorState state, boolean compactLayout) {
        FlowLayout itemModel = UiFactory.section(ItemEditorText.tr("general.item_model.title"), Component.empty());
        itemModel.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        int contentWidth = this.availableContentWidth();
        int pickItemModelWidth = Math.max(
                ITEM_MODEL_PICK_BUTTON_WIDTH_MIN,
                Math.min(ITEM_MODEL_PICK_BUTTON_WIDTH_MAX, contentWidth / ITEM_MODEL_PICK_BUTTON_WIDTH_DIVISOR)
        );
        int idRowGap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        int minIdInputWidth = UiFactory.scaledPixels(ITEM_MODEL_ID_INPUT_MIN_WIDTH);
        boolean stackPickButton = compactLayout
                || contentWidth < UiFactory.scaledPixels(ITEM_MODEL_STACK_PICK_THRESHOLD)
                || contentWidth < (pickItemModelWidth + minIdInputWidth + idRowGap);
        FlowLayout itemModelIdInputRow = stackPickButton ? UiFactory.column() : UiFactory.row().gap(idRowGap);
        TextBoxComponent itemModelIdInput = this.itemModelTextBox(
                state.itemModelId,
                PanelBindings.text(this.screen, value -> state.itemModelId = value)
        );
        itemModelIdInput.setMaxLength(UNBOUNDED_TEXT_LIMIT);
        itemModelIdInput.horizontalSizing(stackPickButton ? Sizing.fill(100) : Sizing.expand(100));
        itemModelIdInputRow.child(itemModelIdInput);
        ButtonComponent pickItemModelButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                this.screen.openSearchablePickerDialog(
                        ItemEditorText.str("general.item_model.id"),
                        "",
                        BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString).sorted().toList(),
                        id -> id,
                        selected -> PanelBindings.mutateRefresh(this.screen, () -> state.itemModelId = selected)
                )
        );
        pickItemModelButton.horizontalSizing(stackPickButton ? Sizing.fill(100) : UiFactory.fixed(pickItemModelWidth));
        pickItemModelButton.verticalSizing(UiFactory.fixed(this.itemModelControlHeight()));
        itemModelIdInputRow.child(pickItemModelButton);
        itemModel.child(this.compactField(
                ItemEditorText.tr("general.item_model.id"),
                itemModelIdInputRow,
                ITEM_MODEL_ID_LABEL_WIDTH,
                compactLayout
        ));

        boolean stackValueFields = compactLayout || contentWidth < UiFactory.scaledPixels(ITEM_MODEL_VALUE_ROW_STACK_THRESHOLD);
        FlowLayout customModelValues = stackValueFields ? UiFactory.column() : UiFactory.row();
        customModelValues.child(this.compactField(
                ItemEditorText.tr("general.item_model.float"),
                this.itemModelTextBox(state.customModelFloat, PanelBindings.text(this.screen, value -> state.customModelFloat = value)).horizontalSizing(Sizing.fill(100)),
                ITEM_MODEL_VALUE_LABEL_WIDTH,
                true
        ));
        customModelValues.child(this.compactField(
                ItemEditorText.tr("general.item_model.string"),
                this.itemModelTextBox(state.customModelString, PanelBindings.text(this.screen, value -> state.customModelString = value)).horizontalSizing(Sizing.fill(100)),
                ITEM_MODEL_VALUE_LABEL_WIDTH,
                true
        ));
        if (!stackValueFields) {
            this.distributeRowChildren(customModelValues);
        }
        itemModel.child(customModelValues);
        itemModel.child(this.compactField(
                ItemEditorText.tr("general.item_model.color"),
                this.itemModelTextBox(state.customModelColor, PanelBindings.text(this.screen, value -> state.customModelColor = value)).horizontalSizing(Sizing.fill(100)),
                ITEM_MODEL_VALUE_LABEL_WIDTH,
                true
        ));

        FlowLayout customModelFlags = compactLayout ? UiFactory.column() : UiFactory.row();
        customModelFlags.child(UiFactory.checkbox(ItemEditorText.tr("general.item_model.flag_enable"), state.customModelFlagEnabled, PanelBindings.toggle(this.screen, value -> state.customModelFlagEnabled = value)));
        customModelFlags.child(UiFactory.checkbox(ItemEditorText.tr("general.item_model.flag_value"), state.customModelFlag, PanelBindings.toggle(this.screen, value -> state.customModelFlag = value)));
        if (!compactLayout) {
            this.distributeRowChildren(customModelFlags);
        }
        itemModel.child(customModelFlags);
        return itemModel;
    }

    private TextBoxComponent itemModelTextBox(String value, Consumer<String> onChanged) {
        TextBoxComponent input = UiFactory.textBox(value, onChanged);
        input.verticalSizing(Sizing.fixed(this.itemModelControlHeight()));
        return input;
    }

    private int itemModelControlHeight() {
        return Math.max(
                ITEM_MODEL_TEXTBOX_MIN_HEIGHT,
                UiFactory.scaleProfile().controlHeight() - UiFactory.scaledPixels(ITEM_MODEL_TEXTBOX_HEIGHT_REDUCTION)
        );
    }

    private boolean useCompactLayout() {
        var window = this.screen.session().minecraft().getWindow();
        return LayoutModeUtil.isCompactWindowAndContent(
                window.getGuiScale(),
                window.getGuiScaledWidth(),
                COMPACT_LAYOUT_WIDTH_THRESHOLD,
                this.availableContentWidth(),
                COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD
        );
    }

    private FlowLayout responsiveCheckboxLine(Component text, Consumer<Boolean> onChanged) {
        return this.responsiveCheckboxLine(text, false, onChanged);
    }

    private FlowLayout responsiveCheckboxLine(Component text, boolean checked, Consumer<Boolean> onChanged) {
        FlowLayout row = UiFactory.row();

        var checkbox = UiFactory.checkbox(Component.empty(), checked, onChanged);
        int checkboxSize = Math.max(INLINE_CHECKBOX_SIZE_MIN, UiFactory.scaledPixels(INLINE_CHECKBOX_SIZE_BASE));
        checkbox.horizontalSizing(Sizing.fixed(checkboxSize));
        row.child(checkbox);

        int contentWidth = this.availableContentWidth();
        int labelWidth = Math.max(RESPONSIVE_CHECKBOX_LABEL_MIN_WIDTH, contentWidth - UiFactory.scaledPixels(RESPONSIVE_CHECKBOX_LABEL_RESERVE));
        labelWidth = Math.max(1, Math.min(contentWidth, labelWidth));
        Component fitted = UiFactory.fitToWidth(text, labelWidth);
        LabelComponent label = UiFactory.muted(fitted, labelWidth);
        if (!fitted.getString().equals(text.getString())) {
            label.tooltip(List.of(text));
        }
        label.horizontalSizing(Sizing.expand(100));
        row.child(label);
        return row;
    }

    private void distributeRowChildren(FlowLayout row) {
        if (row.children().size() <= 1) {
            return;
        }
        for (UIComponent child : row.children()) {
            child.horizontalSizing(Sizing.expand(100));
        }
    }

    private int availableContentWidth() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

    private void applyRichEditorRenderMode(me.noramibu.itemeditor.ui.component.RichTextAreaComponent editor, boolean renderStructured) {
        editor.structuredRenderMode(renderStructured);
    }

}
