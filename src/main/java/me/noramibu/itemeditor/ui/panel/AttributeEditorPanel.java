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
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class AttributeEditorPanel implements EditorPanel {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 430;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 560;
    private static final int AMOUNT_STEP_BUTTON_MIN = 24;
    private static final int AMOUNT_STEP_BUTTON_BASE = 30;
    private static final int BUILT_IN_NOTE_HINT_WIDTH = 220;
    private static final int AMOUNT_FIELD_WIDTH = 100;
    private static final int OPERATION_PICKER_WIDTH = 160;
    private static final int SLOT_PICKER_WIDTH = 140;
    private static final int MODIFIER_ID_FIELD_WIDTH = 220;
    private static final int NOTE_HINT_WIDTH_WIDE = 520;
    private static final int NOTE_HINT_WIDTH_MEDIUM = 320;
    private static final int PREVIEW_LINE_MAX_WIDTH = 520;
    private static final int INTRO_ACTION_BUTTON_WIDTH_MIN = 64;
    private static final int INTRO_ACTION_BUTTON_WIDTH_MAX = 132;
    private static final int INTRO_ACTION_BUTTON_ROW_RESERVE = 12;
    private static final int INTRO_ACTION_BUTTON_TEXT_MIN = 18;
    private static final int INTRO_ACTION_BUTTON_TEXT_RESERVE = 10;
    private static final String SYMBOL_STEP_DECREMENT = "-";
    private static final String SYMBOL_STEP_INCREMENT = "+";
    private static final String TOOLTIP_EXPAND_ALL = "Expand all";
    private static final String TOOLTIP_COLLAPSE_ALL = "Collapse all";

    private final ItemEditorScreen screen;

    public AttributeEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        boolean compactLayout = this.isCompactLayout();
        Registry<Attribute> attributeRegistry = this.screen.session().registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
        List<String> attributeIds = RegistryUtil.ids(attributeRegistry);
        ItemAttributeModifiers prototypeModifiers = this.screen.session().originalStack().getPrototype().get(DataComponents.ATTRIBUTE_MODIFIERS);
        Set<String> prototypeKeys = prototypeModifiers == null
                ? Set.of()
                : prototypeModifiers.modifiers().stream().map(this::entryKey).collect(Collectors.toSet());
        int contentWidth = this.availableContentWidth();

        FlowLayout root = UiFactory.column();
        root.child(this.buildEffectiveAttributesSection());

        FlowLayout intro = UiFactory.section(
                ItemEditorText.tr("attributes.modifiers.title"),
                Component.empty()
        );
        FlowLayout actions = compactLayout ? UiFactory.column() : UiFactory.row();
        int introActionButtonCount = state.attributeModifiers.isEmpty() ? 2 : 4;
        int introActionButtonWidth = this.resolveIntroActionButtonWidth(contentWidth, introActionButtonCount);
        Component resetText = ItemEditorText.tr("attributes.modifiers.reset");
        ButtonComponent resetButton = this.introActionButton(
                resetText,
                compactLayout,
                introActionButtonWidth,
                button -> PanelBindings.mutateRefresh(this.screen, () -> this.resetAttributeModifiersFromOriginal(state))
        );
        actions.child(resetButton);
        Component addText = ItemEditorText.tr("attributes.modifiers.add");
        ButtonComponent addButton = this.introActionButton(
                addText,
                compactLayout,
                introActionButtonWidth,
                button -> PanelBindings.mutateRefresh(this.screen, () -> {
                    ItemEditorState.AttributeModifierDraft draft = new ItemEditorState.AttributeModifierDraft();
                    draft.uiCollapsed = false;
                    state.attributeModifiers.add(draft);
                })
        );
        actions.child(addButton);
        if (!state.attributeModifiers.isEmpty()) {
            Component expandText = LayoutModeUtil.sectionToggleText(true);
            ButtonComponent expandAll = this.introActionButton(expandText, compactLayout, introActionButtonWidth, button ->
                    PanelBindings.mutateRefresh(this.screen, () -> state.attributeModifiers.forEach(entry -> entry.uiCollapsed = false))
            );
            expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL)));
            actions.child(expandAll);

            Component collapseText = LayoutModeUtil.sectionToggleText(false);
            ButtonComponent collapseAll = this.introActionButton(collapseText, compactLayout, introActionButtonWidth, button ->
                    PanelBindings.mutateRefresh(this.screen, () -> state.attributeModifiers.forEach(entry -> entry.uiCollapsed = true))
            );
            collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL)));
            actions.child(collapseAll);
        }
        intro.child(actions);
        root.child(intro);

        for (int index = 0; index < state.attributeModifiers.size(); index++) {
            int currentIndex = index;
            ItemEditorState.AttributeModifierDraft draft = state.attributeModifiers.get(currentIndex);
            FlowLayout card = UiFactory.reorderableSubCard(
                    ItemEditorText.tr("attributes.modifier.row", index + 1),
                    currentIndex > 0,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(state.attributeModifiers, currentIndex, currentIndex - 1)),
                    currentIndex < state.attributeModifiers.size() - 1,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(state.attributeModifiers, currentIndex, currentIndex + 1)),
                    () -> PanelBindings.mutateRefresh(this.screen, () -> state.attributeModifiers.remove(currentIndex))
            );

            if (prototypeKeys.contains(this.draftKey(draft))) {
                card.child(UiFactory.muted(ItemEditorText.tr("attributes.modifier.built_in"), BUILT_IN_NOTE_HINT_WIDTH));
            }

            card.child(UiFactory.collapsibleSummaryRow(
                    Component.literal(this.attributeSummary(draft)),
                    NOTE_HINT_WIDTH_WIDE,
                    draft.uiCollapsed,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> draft.uiCollapsed = !draft.uiCollapsed)
            ));

            if (draft.uiCollapsed) {
                root.child(card);
                continue;
            }

            ButtonComponent attributeButton = UiFactory.button(
                    draft.attributeId.isBlank() ? ItemEditorText.str("attributes.modifier.select_attribute") : draft.attributeId, UiFactory.ButtonTextPreset.STANDARD, 
                    button -> this.screen.openSearchablePickerDialog(
                            ItemEditorText.str("attributes.modifier.attribute"),
                            "",
                            attributeIds,
                            id -> id,
                            id -> PanelBindings.mutate(this.screen, () -> draft.attributeId = id)
                    )
            );
            attributeButton.horizontalSizing(Sizing.fill(100));
            card.child(UiFactory.field(ItemEditorText.tr("attributes.modifier.attribute"), Component.empty(), attributeButton));

            FlowLayout rowOne = compactLayout ? UiFactory.column() : UiFactory.row();

            FlowLayout amountRow = UiFactory.row();
            amountRow.child(UiFactory.field(
                    ItemEditorText.tr("attributes.modifier.amount"),
                    Component.empty(),
                    UiFactory.textBox(draft.amount, PanelBindings.text(this.screen, value -> draft.amount = value)).horizontalSizing(UiFactory.fixed(AMOUNT_FIELD_WIDTH))
            ));
            ButtonComponent minusAmount = UiFactory.button(Component.literal(SYMBOL_STEP_DECREMENT), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> draft.amount = this.adjustAmount(draft.amount, -1.0))
            );
            int stepButtonWidth = Math.max(AMOUNT_STEP_BUTTON_MIN, UiFactory.scaledPixels(AMOUNT_STEP_BUTTON_BASE));
            minusAmount.horizontalSizing(Sizing.fixed(stepButtonWidth));
            amountRow.child(minusAmount);
            ButtonComponent plusAmount = UiFactory.button(Component.literal(SYMBOL_STEP_INCREMENT), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    PanelBindings.mutateRefresh(this.screen, () -> draft.amount = this.adjustAmount(draft.amount, 1.0))
            );
            plusAmount.horizontalSizing(Sizing.fixed(stepButtonWidth));
            amountRow.child(plusAmount);
            rowOne.child(amountRow);

            var operationButton = UiFactory.button(draft.operation, UiFactory.ButtonTextPreset.STANDARD,  button -> this.screen.openDropdown(
                    button,
                    Arrays.asList(AttributeModifier.Operation.values()),
                    AttributeModifier.Operation::name,
                    operation -> PanelBindings.mutate(this.screen, () -> draft.operation = operation.name())
            ));
            rowOne.child(UiFactory.field(ItemEditorText.tr("attributes.modifier.operation"), Component.empty(), operationButton.horizontalSizing(UiFactory.fixed(OPERATION_PICKER_WIDTH))));

            var slotButton = UiFactory.button(draft.slotGroup, UiFactory.ButtonTextPreset.STANDARD,  button -> this.screen.openDropdown(
                    button,
                    Arrays.asList(EquipmentSlotGroup.values()),
                    EquipmentSlotGroup::name,
                    slot -> PanelBindings.mutate(this.screen, () -> draft.slotGroup = slot.name())
            ));
            rowOne.child(UiFactory.field(ItemEditorText.tr("attributes.modifier.slot"), Component.empty(), slotButton.horizontalSizing(UiFactory.fixed(SLOT_PICKER_WIDTH))));
            card.child(rowOne);

            card.child(UiFactory.field(
                    ItemEditorText.tr("attributes.modifier.id"),
                    Component.empty(),
                    UiFactory.textBox(draft.modifierId, PanelBindings.text(this.screen, value -> draft.modifierId = value)).horizontalSizing(UiFactory.fixed(MODIFIER_ID_FIELD_WIDTH))
            ));

            String vanillaTooltipNote = this.vanillaTooltipNote(draft);
            if (vanillaTooltipNote != null) {
                card.child(UiFactory.muted(vanillaTooltipNote, NOTE_HINT_WIDTH_WIDE));
            }

            root.child(card);
        }

        return root;
    }

    private FlowLayout buildEffectiveAttributesSection() {
        ItemAttributeModifiers effectiveModifiers = this.screen.session().previewStack().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        FlowLayout section = UiFactory.section(
                ItemEditorText.tr("attributes.preview.title"),
                Component.empty()
        );

        if (effectiveModifiers.modifiers().isEmpty()) {
            section.child(UiFactory.muted(ItemEditorText.tr("attributes.preview.none")));
            return section;
        }

        if (this.screen.session().minecraft().player != null) {
            section.child(UiFactory.muted(
                    ItemEditorText.tr(
                            "attributes.preview.player_base",
                            this.formatAmount(this.screen.session().minecraft().player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)),
                            this.formatAmount(this.screen.session().minecraft().player.getAttributeBaseValue(Attributes.ATTACK_SPEED))
                    ),
                    300
            ));
        }

        for (ItemAttributeModifiers.Entry entry : effectiveModifiers.modifiers()) {
            section.child(this.buildAttributeSummaryCard(entry, ItemEditorText.str("attributes.preview.current")));
        }

        return section;
    }

    private String attributeSummary(ItemEditorState.AttributeModifierDraft draft) {
        String attribute = draft.attributeId == null || draft.attributeId.isBlank() ? "-" : draft.attributeId;
        String operation = draft.operation == null || draft.operation.isBlank() ? AttributeModifier.Operation.ADD_VALUE.name() : draft.operation;
        String slot = draft.slotGroup == null || draft.slotGroup.isBlank() ? EquipmentSlotGroup.ANY.name() : draft.slotGroup;
        String amount = draft.amount == null || draft.amount.isBlank() ? "0" : draft.amount;
        return attribute + " | " + operation + " | " + slot + " | " + amount;
    }

    private String adjustAmount(String raw, double delta) {
        double value;
        try {
            value = Double.parseDouble(raw == null || raw.isBlank() ? "0" : raw.trim());
        } catch (NumberFormatException ignored) {
            value = 0.0;
        }
        return this.formatAmount(value + delta);
    }

    private void resetAttributeModifiersFromOriginal(ItemEditorState state) {
        state.attributeModifiers.clear();
        ItemAttributeModifiers originalModifiers = this.screen.session().originalStack().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        originalModifiers.modifiers().forEach(entry -> state.attributeModifiers.add(ItemEditorState.AttributeModifierDraft.fromEntry(entry)));
    }

    private String draftKey(ItemEditorState.AttributeModifierDraft draft) {
        return draft.attributeId
                + "|" + draft.modifierId
                + "|" + draft.amount
                + "|" + draft.operation
                + "|" + draft.slotGroup;
    }

    private String entryKey(ItemAttributeModifiers.Entry entry) {
        return entry.attribute().unwrapKey().map(key -> key.identifier().toString()).orElse("")
                + "|" + entry.modifier().id()
                + "|" + entry.modifier().amount()
                + "|" + entry.modifier().operation().name()
                + "|" + entry.slot().name();
    }

    private FlowLayout buildAttributeSummaryCard(ItemAttributeModifiers.Entry entry, String sourceLabel) {
        FlowLayout card = UiFactory.subCard();
        List<Component> previewLines = new ArrayList<>();
        entry.display().apply(
                previewLines::add,
                this.screen.session().minecraft().player,
                entry.attribute(),
                entry.modifier()
        );

        if (previewLines.isEmpty()) {
            previewLines.add(Component.translatable(entry.attribute().value().getDescriptionId()));
        }

        for (Component line : previewLines) {
            card.child(UiFactory.bodyLabel(line).maxWidth(PREVIEW_LINE_MAX_WIDTH));
        }

        String attributeId = entry.attribute().unwrapKey().map(key -> key.identifier().toString()).orElse(ItemEditorText.str("attributes.preview.unbound"));
            card.child(UiFactory.muted(
                ItemEditorText.str(
                        "attributes.preview.source_line",
                        sourceLabel,
                        entry.slot().name(),
                        entry.modifier().operation().name(),
                        this.formatAmount(entry.modifier().amount())
                ),
                NOTE_HINT_WIDTH_MEDIUM
        ));
        card.child(UiFactory.muted(attributeId, NOTE_HINT_WIDTH_MEDIUM));
        return card;
    }

    private String vanillaTooltipNote(ItemEditorState.AttributeModifierDraft draft) {
        double amount;
        try {
            amount = Double.parseDouble(draft.amount.trim());
        } catch (NumberFormatException exception) {
            return null;
        }

        if (Item.BASE_ATTACK_DAMAGE_ID.toString().equals(draft.modifierId)
                && draft.operation.equals(AttributeModifier.Operation.ADD_VALUE.name())) {
            double playerBase = this.screen.session().minecraft().player != null
                    ? this.screen.session().minecraft().player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                    : 1.0D;
            return ItemEditorText.str(
                    "attributes.preview.note.base_damage",
                    this.formatAmount(amount),
                    this.formatAmount(amount + playerBase)
            );
        }

        if (Item.BASE_ATTACK_SPEED_ID.toString().equals(draft.modifierId)
                && draft.operation.equals(AttributeModifier.Operation.ADD_VALUE.name())) {
            double playerBase = this.screen.session().minecraft().player != null
                    ? this.screen.session().minecraft().player.getAttributeBaseValue(Attributes.ATTACK_SPEED)
                    : 4.0D;
            return ItemEditorText.str(
                    "attributes.preview.note.base_speed",
                    this.formatAmount(amount),
                    this.formatAmount(amount + playerBase)
            );
        }

        return null;
    }

    private String formatAmount(double amount) {
        if (Math.rint(amount) == amount) {
            return Integer.toString((int) amount);
        }
        return String.format(Locale.ROOT, "%.2f", amount);
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

    private ButtonComponent introActionButton(
            Component fullText,
            boolean compactLayout,
            int nonCompactWidth,
            Consumer<ButtonComponent> onPress
    ) {
        Component fitted = UiFactory.fitToWidth(
                fullText,
                Math.max(INTRO_ACTION_BUTTON_TEXT_MIN, nonCompactWidth - UiFactory.scaledPixels(INTRO_ACTION_BUTTON_TEXT_RESERVE))
        );
        ButtonComponent button = UiFactory.button(fitted, UiFactory.ButtonTextPreset.STANDARD, onPress);
        button.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(nonCompactWidth));
        if (!fitted.getString().equals(fullText.getString())) {
            button.tooltip(List.of(fullText));
        }
        return button;
    }

    private int resolveIntroActionButtonWidth(int contentWidth, int buttonCount) {
        int preferred = Math.max(
                INTRO_ACTION_BUTTON_WIDTH_MIN,
                Math.min(
                        INTRO_ACTION_BUTTON_WIDTH_MAX,
                        (contentWidth - UiFactory.scaledPixels(INTRO_ACTION_BUTTON_ROW_RESERVE)) / Math.max(1, buttonCount)
                )
        );
        return Math.max(1, Math.min(contentWidth, preferred));
    }

    private int availableContentWidth() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }
}
