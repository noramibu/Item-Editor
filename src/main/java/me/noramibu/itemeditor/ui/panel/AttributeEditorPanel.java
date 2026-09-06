package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import me.noramibu.itemeditor.ItemEditorClient;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class AttributeEditorPanel implements EditorPanel {
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 560;
    private static final int AMOUNT_STEP_BUTTON_MIN = 24;
    private static final int AMOUNT_STEP_BUTTON_BASE = 30;
    private static final int BUILT_IN_NOTE_HINT_WIDTH = 220;
    private static final int NOTE_HINT_WIDTH_WIDE = 520;
    private static final int NOTE_HINT_WIDTH_MEDIUM = 320;
    private static final int PREVIEW_LINE_MAX_WIDTH = 520;
    private static final int MODIFIER_ID_GENERATE_PERCENT = 16;

    private final ItemEditorScreen screen;

    public AttributeEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public List<EditorSearchDialog.Target> searchTargets() {
        ItemEditorState state = this.screen.session().state();
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        List<String> parents = PanelSearchDeclaration.parents(EditorCategory.ATTRIBUTES);
        for (PanelField field : PanelField.values()) {
            if ((field == PanelField.EXPAND || field == PanelField.COLLAPSE) && state.attributeModifiers.isEmpty()) {
                continue;
            }
            targets.add(field.target(
                    this.screen,
                    EditorCategory.ATTRIBUTES,
                    "attribute-modifiers",
                    parents,
                    EditorSearchDialog.english(PanelField.TITLE.path()),
                    () -> {}));
        }
        ItemAttributeModifiers original = this.screen
                .session()
                .originalStack()
                .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Map<String, ItemEditorState.AttributeModifierDraft> originals = this.originalModifiersByIdentity(original);
        for (int index = 0; index < state.attributeModifiers.size(); index++) {
            ItemEditorState.AttributeModifierDraft draft = state.attributeModifiers.get(index);
            List<String> entryParents = PanelSearchDeclaration.parents(
                    EditorCategory.ATTRIBUTES, PanelField.TITLE.label(), ModifierField.ROW.label(index + 1));
            Runnable expand = () -> draft.uiCollapsed = false;
            String aliases = this.attributeSummary(draft) + " " + EditorSearchDialog.english(PanelField.TITLE.path());
            for (ModifierField field : ModifierField.values()) {
                if (!field.appliesTo(draft.display.type())) {
                    continue;
                }
                if (field == ModifierField.RESET && !originals.containsKey(this.draftIdentityKey(draft))) {
                    continue;
                }
                if ((field == ModifierField.UP && index == 0)
                        || (field == ModifierField.DOWN && index == state.attributeModifiers.size() - 1)) {
                    continue;
                }
                targets.add(field.target(
                        this.screen,
                        EditorCategory.ATTRIBUTES,
                        field.scope(index),
                        field == ModifierField.ROW
                                ? PanelSearchDeclaration.parents(EditorCategory.ATTRIBUTES, PanelField.TITLE.label())
                                : entryParents,
                        aliases + " " + field.aliases(),
                        expand,
                        index + 1));
            }
        }
        return List.copyOf(targets);
    }

    private static String modifierScope(int index) {
        return "attribute-modifier:" + index;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        boolean compactLayout = LayoutModeUtil.isCompactWidth(
                this.screen.editorContentWidthHint(), COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
        Registry<Attribute> attributeRegistry =
                this.screen.session().registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
        List<String> attributeIds = RegistryUtil.ids(attributeRegistry);
        ItemAttributeModifiers originalModifiers = this.screen
                .session()
                .originalStack()
                .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Map<String, ItemEditorState.AttributeModifierDraft> originalByIdentity =
                this.originalModifiersByIdentity(originalModifiers);
        ItemAttributeModifiers prototypeModifiers =
                this.screen.session().originalStack().getPrototype().get(DataComponents.ATTRIBUTE_MODIFIERS);
        Set<String> prototypeKeys = prototypeModifiers == null
                ? Set.of()
                : prototypeModifiers.modifiers().stream()
                        .map(ItemEditorState.AttributeModifierDraft::fromEntry)
                        .map(this::draftKey)
                        .collect(Collectors.toSet());

        FlowLayout root = UiFactory.column();
        root.child(this.buildEffectiveAttributesSection());

        FlowLayout intro = UiFactory.section(PanelField.TITLE.label(), Component.empty());
        intro.id("attribute-modifiers");
        Component resetText = ItemEditorText.tr("attributes.modifiers.reset");
        ButtonComponent resetButton = this.introActionButton(
                PanelField.RESET.label(),
                UiFactory.ActionTone.NEGATIVE,
                button -> PanelBindings.mutateRefresh(
                        this.screen, () -> this.resetAttributeModifiersFromOriginal(state)));
        resetButton.tooltip(List.of(resetText));
        Component addText = PanelField.ADD.label();
        ButtonComponent addButton = this.introActionButton(
                addText,
                UiFactory.ActionTone.POSITIVE,
                button -> PanelBindings.mutateRefresh(this.screen, () -> {
                    ItemEditorState.AttributeModifierDraft draft = new ItemEditorState.AttributeModifierDraft();
                    draft.uiCollapsed = false;
                    state.attributeModifiers.add(draft);
                }));
        intro.child(UiFactory.actionButtonRow(addButton, resetButton));
        if (!state.attributeModifiers.isEmpty()) {
            Component expandText = PanelField.EXPAND.label();
            ButtonComponent expandAll = this.introActionButton(
                    expandText,
                    UiFactory.ActionTone.NEUTRAL,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> state.attributeModifiers.forEach(entry -> entry.uiCollapsed = false)));
            expandAll.tooltip(List.of(expandText));

            Component collapseText = PanelField.COLLAPSE.label();
            ButtonComponent collapseAll = this.introActionButton(
                    collapseText,
                    UiFactory.ActionTone.NEUTRAL,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> state.attributeModifiers.forEach(entry -> entry.uiCollapsed = true)));
            collapseAll.tooltip(List.of(collapseText));
            intro.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }
        root.child(intro);

        for (int index = 0; index < state.attributeModifiers.size(); index++) {
            ItemEditorState.AttributeModifierDraft draft = state.attributeModifiers.get(index);
            ItemEditorState.AttributeModifierDraft originalDraft = originalByIdentity.get(this.draftIdentityKey(draft));
            FlowLayout card = this.attributeModifierCard(state, draft, originalDraft, index);

            if (prototypeKeys.contains(this.draftKey(draft))) {
                card.child(
                        UiFactory.muted(ItemEditorText.tr("attributes.modifier.built_in"), BUILT_IN_NOTE_HINT_WIDTH));
            }

            var summary = UiFactory.muted(Component.literal(this.attributeSummary(draft)), NOTE_HINT_WIDTH_WIDE);
            card.child(summary);

            Component previewLine = this.attributePreviewLine(draft, attributeRegistry, index);
            var preview = UiFactory.bodyLabel(previewLine == null ? Component.empty() : previewLine)
                    .maxWidth(PREVIEW_LINE_MAX_WIDTH);
            card.child(preview);
            int modifierIndex = index;
            Runnable updateSummary = () -> {
                summary.text(Component.literal(this.attributeSummary(draft)));
                Component updated = this.attributePreviewLine(draft, attributeRegistry, modifierIndex);
                preview.text(updated == null ? Component.empty() : updated);
            };

            if (draft.uiCollapsed) {
                root.child(card);
                continue;
            }

            ButtonComponent attributeButton = UiFactory.button(
                    draft.attributeId.isBlank()
                            ? ItemEditorText.str("attributes.modifier.select_attribute")
                            : draft.attributeId,
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> this.screen.openSearchablePickerDialog(
                            ModifierField.ATTRIBUTE.text(),
                            "",
                            attributeIds,
                            id -> id,
                            id -> PanelBindings.mutate(this.screen, () -> draft.attributeId = id)));
            attributeButton.horizontalSizing(Sizing.fill(100));
            card.child(UiFactory.field(ModifierField.ATTRIBUTE.label(), Component.empty(), attributeButton));

            FlowLayout rowOne = compactLayout ? UiFactory.column() : UiFactory.row();

            FlowLayout amountRow = UiFactory.row();
            amountRow.child(UiFactory.textBox(draft.amount, PanelBindings.text(this.screen, value -> {
                        draft.amount = value;
                        updateSummary.run();
                    }))
                    .horizontalSizing(Sizing.expand(100)));
            ButtonComponent minusAmount = UiFactory.negativeButton(
                    Component.literal("-"),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> draft.amount = this.adjustAmount(draft.amount, -1.0)));
            int stepButtonWidth = Math.max(AMOUNT_STEP_BUTTON_MIN, UiFactory.scaledPixels(AMOUNT_STEP_BUTTON_BASE));
            minusAmount.horizontalSizing(Sizing.fixed(stepButtonWidth));
            amountRow.child(minusAmount);
            ButtonComponent plusAmount = UiFactory.positiveButton(
                    Component.literal("+"),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> draft.amount = this.adjustAmount(draft.amount, 1.0)));
            plusAmount.horizontalSizing(Sizing.fixed(stepButtonWidth));
            amountRow.child(plusAmount);
            rowOne.child(UiFactory.field(ModifierField.AMOUNT.label(), Component.empty(), amountRow)
                    .horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(34)));

            var operationButton = UiFactory.actionToneButton(
                    Component.literal(this.safeOperationName(draft.operation)),
                    UiFactory.ButtonTextPreset.STANDARD,
                    this.operationTone(draft.operation),
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> draft.operation = nextOperation(draft.operation)));
            rowOne.child(UiFactory.field(ModifierField.OPERATION.label(), Component.empty(), operationButton)
                    .horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(33)));

            var slotButton = UiFactory.button(
                    draft.slotGroup,
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> this.screen.openDropdown(
                            button,
                            Arrays.asList(EquipmentSlotGroup.values()),
                            EquipmentSlotGroup::name,
                            slot -> PanelBindings.mutate(this.screen, () -> draft.slotGroup = slot.name())));
            rowOne.child(UiFactory.field(ModifierField.SLOT.label(), Component.empty(), slotButton)
                    .horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(33)));
            card.child(rowOne);

            FlowLayout modifierIdRow = this.modifierIdRow(draft, compactLayout);
            card.child(modifierIdRow);
            card.child(UiFactory.field(
                    ModifierField.DISPLAY.label(),
                    Component.empty(),
                    UiFactory.button(
                            ItemEditorText.tr("attributes.modifier.display."
                                    + draft.display.type().getSerializedName()),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> PanelBindings.mutateRefresh(screen, () -> {
                                draft.display = switch (draft.display.type()) {
                                    case DEFAULT -> ItemAttributeModifiers.Display.hidden();
                                    case HIDDEN -> ItemAttributeModifiers.Display.override(draft.displayText);
                                    case OVERRIDE -> ItemAttributeModifiers.Display.attributeModifiers();
                                };
                            }))));
            if (draft.display.type() == ItemAttributeModifiers.Display.Type.OVERRIDE) {
                var editor = StyledTextFieldSection.create(
                        screen,
                        RichTextDocument.fromComponent(draft.displayText),
                        Sizing.fill(100),
                        UiFactory.fixed(80),
                        "",
                        StyledTextFieldSection.StylePreset.name(),
                        ItemEditorText.str("display.lore.color_title"),
                        ItemEditorText.str("display.lore.gradient_title"),
                        "",
                        "",
                        null,
                        null,
                        document -> PanelBindings.mutate(screen, () -> {
                            draft.displayText = document.toComponent();
                            draft.display = ItemAttributeModifiers.Display.override(draft.displayText);
                        }));
                card.child(editor.toolbar());
                FlowLayout overrideText =
                        UiFactory.field(ModifierField.OVERRIDE_TEXT.label(), Component.empty(), editor.editor());
                overrideText.id(ModifierField.OVERRIDE_TEXT.scope(index));
                card.child(overrideText);
                card.child(editor.validation());
            }

            String vanillaTooltipNote = this.vanillaTooltipNote(draft);
            if (vanillaTooltipNote != null) {
                card.child(UiFactory.muted(vanillaTooltipNote, NOTE_HINT_WIDTH_WIDE));
            }

            root.child(card);
        }

        return root;
    }

    private FlowLayout attributeModifierCard(
            ItemEditorState state,
            ItemEditorState.AttributeModifierDraft draft,
            ItemEditorState.AttributeModifierDraft originalDraft,
            int currentIndex) {
        FlowLayout card = UiFactory.subCard();
        card.id(modifierScope(currentIndex));
        FlowLayout titleRow = UiFactory.row();
        titleRow.child(UiFactory.title(ModifierField.ROW.label(currentIndex + 1))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        titleRow.child(UiFactory.collapseToggleButton(
                draft.uiCollapsed,
                () -> PanelBindings.mutateRefresh(this.screen, () -> draft.uiCollapsed = !draft.uiCollapsed)));
        card.child(titleRow);

        ButtonComponent upButton = UiFactory.button(
                ModifierField.UP.label(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> PanelBindings.mutateRefresh(
                        this.screen, () -> Collections.swap(state.attributeModifiers, currentIndex, currentIndex - 1)));
        upButton.active(currentIndex > 0);
        ButtonComponent downButton = UiFactory.button(
                ModifierField.DOWN.label(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> PanelBindings.mutateRefresh(
                        this.screen, () -> Collections.swap(state.attributeModifiers, currentIndex, currentIndex + 1)));
        downButton.active(currentIndex < state.attributeModifiers.size() - 1);
        ButtonComponent duplicateButton = UiFactory.button(
                ModifierField.DUPLICATE.label(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> PanelBindings.mutateRefresh(
                        this.screen, () -> state.attributeModifiers.add(currentIndex + 1, this.copyDraft(draft))));
        ButtonComponent resetButton = null;
        if (originalDraft != null) {
            resetButton = UiFactory.button(
                    ModifierField.RESET.label(),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> PanelBindings.mutateRefresh(
                            this.screen,
                            () -> state.attributeModifiers.set(currentIndex, this.copyDraft(originalDraft))));
        }
        card.child(UiFactory.actionButtonRow(
                upButton,
                downButton,
                duplicateButton,
                resetButton,
                UiFactory.negativeButton(
                        ModifierField.REMOVE.label(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> PanelBindings.mutateRefresh(
                                this.screen, () -> state.attributeModifiers.remove(currentIndex)))));
        return card;
    }

    private FlowLayout modifierIdRow(ItemEditorState.AttributeModifierDraft draft, boolean compactLayout) {
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        UIComponent modifierIdField =
                UiFactory.textBox(draft.modifierId, PanelBindings.text(this.screen, value -> draft.modifierId = value));
        modifierIdField.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        row.child(modifierIdField);

        ButtonComponent generateButton = UiFactory.actionToneButton(
                ModifierField.GENERATE.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                UiFactory.ActionTone.PICKER,
                button -> PanelBindings.mutateRefresh(
                        this.screen, () -> draft.modifierId = this.generatedModifierId(draft)));
        generateButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fill(MODIFIER_ID_GENERATE_PERCENT));
        row.child(generateButton);
        return UiFactory.field(ModifierField.ID.label(), Component.empty(), row);
    }

    private FlowLayout buildEffectiveAttributesSection() {
        ItemAttributeModifiers effectiveModifiers = this.screen
                .session()
                .previewStack()
                .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        FlowLayout section = UiFactory.section(ItemEditorText.tr("attributes.preview.title"), Component.empty());

        if (effectiveModifiers.modifiers().isEmpty()) {
            section.child(UiFactory.muted(ItemEditorText.tr("attributes.preview.none")));
            return section;
        }

        if (this.screen.session().minecraft().player != null) {
            section.child(UiFactory.muted(
                    ItemEditorText.tr(
                            "attributes.preview.player_base",
                            this.formatAmount(this.screen
                                    .session()
                                    .minecraft()
                                    .player
                                    .getAttributeBaseValue(Attributes.ATTACK_DAMAGE)),
                            this.formatAmount(this.screen
                                    .session()
                                    .minecraft()
                                    .player
                                    .getAttributeBaseValue(Attributes.ATTACK_SPEED))),
                    300));
        }

        for (ItemAttributeModifiers.Entry entry : effectiveModifiers.modifiers()) {
            section.child(this.buildAttributeSummaryCard(entry, ItemEditorText.str("attributes.preview.current")));
        }

        return section;
    }

    private String attributeSummary(ItemEditorState.AttributeModifierDraft draft) {
        String attribute = draft.attributeId == null || draft.attributeId.isBlank() ? "-" : draft.attributeId;
        String operation = this.safeOperationName(draft.operation);
        String slot =
                draft.slotGroup == null || draft.slotGroup.isBlank() ? EquipmentSlotGroup.ANY.name() : draft.slotGroup;
        String amount = draft.amount == null || draft.amount.isBlank() ? "0" : draft.amount;
        return attribute + " | " + operation + " | " + slot + " | " + amount;
    }

    private Component attributePreviewLine(
            ItemEditorState.AttributeModifierDraft draft, Registry<Attribute> attributeRegistry, int index) {
        if (draft.attributeId == null || draft.attributeId.isBlank()) {
            return null;
        }

        Holder<Attribute> attribute = RegistryUtil.resolveHolder(attributeRegistry, draft.attributeId);
        if (attribute == null) {
            return null;
        }

        double amount;
        try {
            amount = Double.parseDouble(draft.amount == null || draft.amount.isBlank() ? "0" : draft.amount.trim());
        } catch (NumberFormatException exception) {
            return null;
        }

        AttributeModifier.Operation operation;
        try {
            operation = AttributeModifier.Operation.valueOf(this.safeOperationName(draft.operation));
        } catch (IllegalArgumentException exception) {
            return null;
        }

        Identifier modifierId = this.modifierIdentifier(draft, index);
        if (modifierId == null) {
            return null;
        }

        AttributeModifier modifier = new AttributeModifier(modifierId, amount, operation);
        List<Component> previewLines = new ArrayList<>();
        draft.display.apply(previewLines::add, this.screen.session().minecraft().player, attribute, modifier);
        return previewLines.isEmpty()
                ? ItemEditorText.tr("attributes.preview.no_tooltip_change")
                : previewLines.getFirst();
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

    private ItemEditorState.AttributeModifierDraft copyDraft(ItemEditorState.AttributeModifierDraft source) {
        ItemEditorState.AttributeModifierDraft copy = new ItemEditorState.AttributeModifierDraft();
        copy.attributeId = source.attributeId;
        copy.modifierId = source.modifierId;
        copy.amount = source.amount;
        copy.operation = source.operation;
        copy.slotGroup = source.slotGroup;
        copy.display = source.display;
        copy.displayText = source.displayText.copy();
        copy.uiCollapsed = false;
        return copy;
    }

    private void resetAttributeModifiersFromOriginal(ItemEditorState state) {
        state.attributeModifiers.clear();
        ItemAttributeModifiers originalModifiers = this.screen
                .session()
                .originalStack()
                .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        originalModifiers
                .modifiers()
                .forEach(
                        entry -> state.attributeModifiers.add(ItemEditorState.AttributeModifierDraft.fromEntry(entry)));
    }

    private Map<String, ItemEditorState.AttributeModifierDraft> originalModifiersByIdentity(
            ItemAttributeModifiers modifiers) {
        Map<String, ItemEditorState.AttributeModifierDraft> byIdentity = new LinkedHashMap<>();
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            ItemEditorState.AttributeModifierDraft draft = ItemEditorState.AttributeModifierDraft.fromEntry(entry);
            byIdentity.put(this.draftIdentityKey(draft), draft);
        }
        return byIdentity;
    }

    private String draftKey(ItemEditorState.AttributeModifierDraft draft) {
        return draft.attributeId
                + "|" + draft.modifierId
                + "|" + draft.amount
                + "|" + draft.operation
                + "|" + draft.slotGroup
                + "|" + draft.display;
    }

    private String draftIdentityKey(ItemEditorState.AttributeModifierDraft draft) {
        return draft.attributeId + "|" + draft.modifierId + "|" + draft.slotGroup;
    }

    private FlowLayout buildAttributeSummaryCard(ItemAttributeModifiers.Entry entry, String sourceLabel) {
        FlowLayout card = UiFactory.subCard();
        List<Component> previewLines = new ArrayList<>();
        entry.display()
                .apply(
                        previewLines::add,
                        this.screen.session().minecraft().player,
                        entry.attribute(),
                        entry.modifier());

        if (previewLines.isEmpty()) {
            previewLines.add(Component.translatable(entry.attribute().value().getDescriptionId()));
        }

        for (Component line : previewLines) {
            card.child(UiFactory.bodyLabel(line).maxWidth(PREVIEW_LINE_MAX_WIDTH));
        }

        String attributeId = entry.attribute()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(ItemEditorText.str("attributes.preview.unbound"));
        card.child(UiFactory.muted(
                ItemEditorText.str(
                        "attributes.preview.source_line",
                        sourceLabel,
                        entry.slot().name(),
                        entry.modifier().operation().name(),
                        this.formatAmount(entry.modifier().amount())),
                NOTE_HINT_WIDTH_MEDIUM));
        card.child(UiFactory.muted(attributeId, NOTE_HINT_WIDTH_MEDIUM));
        return card;
    }

    private Identifier modifierIdentifier(ItemEditorState.AttributeModifierDraft draft, int index) {
        if (draft.modifierId == null || draft.modifierId.isBlank()) {
            return Identifier.fromNamespaceAndPath(ItemEditorClient.MOD_ID, "generated/" + index);
        }
        return Identifier.tryParse(draft.modifierId);
    }

    private String generatedModifierId(ItemEditorState.AttributeModifierDraft draft) {
        String attributePath =
                draft.attributeId == null || draft.attributeId.isBlank() ? "attribute" : draft.attributeId;
        String slotPath =
                draft.slotGroup == null || draft.slotGroup.isBlank() ? EquipmentSlotGroup.ANY.name() : draft.slotGroup;
        String path = "attributes/"
                + this.sanitizeIdentifierPath(attributePath.replace(':', '/'))
                + "/"
                + this.sanitizeIdentifierPath(slotPath.toLowerCase(Locale.ROOT));
        return Identifier.fromNamespaceAndPath(ItemEditorClient.MOD_ID, path).toString();
    }

    private String sanitizeIdentifierPath(String value) {
        StringBuilder builder = new StringBuilder();
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (int index = 0; index < lower.length(); index++) {
            char character = lower.charAt(index);
            if ((character >= 'a' && character <= 'z')
                    || (character >= '0' && character <= '9')
                    || character == '_'
                    || character == '-'
                    || character == '.'
                    || character == '/') {
                builder.append(character);
            } else {
                builder.append('_');
            }
        }
        return builder.isEmpty() ? "value" : builder.toString();
    }

    static String nextOperation(String operation) {
        AttributeModifier.Operation[] values = AttributeModifier.Operation.values();
        for (int index = 0; index < values.length; index++) {
            if (values[index].name().equals(operation)) return values[(index + 1) % values.length].name();
        }
        return AttributeModifier.Operation.ADD_VALUE.name();
    }

    private String safeOperationName(String operation) {
        return operation == null || operation.isBlank() ? AttributeModifier.Operation.ADD_VALUE.name() : operation;
    }

    private UiFactory.ActionTone operationTone(String operation) {
        return AttributeModifier.Operation.ADD_VALUE.name().equals(this.safeOperationName(operation))
                ? UiFactory.ActionTone.NEUTRAL
                : UiFactory.ActionTone.PICKER;
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
                    this.formatAmount(amount + playerBase));
        }

        if (Item.BASE_ATTACK_SPEED_ID.toString().equals(draft.modifierId)
                && draft.operation.equals(AttributeModifier.Operation.ADD_VALUE.name())) {
            double playerBase = this.screen.session().minecraft().player != null
                    ? this.screen.session().minecraft().player.getAttributeBaseValue(Attributes.ATTACK_SPEED)
                    : 4.0D;
            return ItemEditorText.str(
                    "attributes.preview.note.base_speed",
                    this.formatAmount(amount),
                    this.formatAmount(amount + playerBase));
        }

        return null;
    }

    private String formatAmount(double amount) {
        if (Math.rint(amount) == amount) {
            return Integer.toString((int) amount);
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private ButtonComponent introActionButton(
            Component fullText, UiFactory.ActionTone tone, Consumer<ButtonComponent> onPress) {
        return UiFactory.actionRowButton(fullText, UiFactory.ButtonTextPreset.STANDARD, tone, onPress);
    }

    private enum PanelField implements PanelSearchDeclaration {
        TITLE("attributes.modifiers.title"),
        RESET("common.reset"),
        ADD("attributes.modifiers.add"),
        EXPAND("common.expand_all"),
        COLLAPSE("common.collapse_all");

        private final String path;

        PanelField(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return this.path;
        }
    }

    private enum ModifierField implements PanelSearchDeclaration {
        ROW("attributes.modifier.row"),
        ATTRIBUTE("attributes.modifier.attribute"),
        AMOUNT("attributes.modifier.amount"),
        OPERATION("attributes.modifier.operation"),
        SLOT("attributes.modifier.slot"),
        ID("attributes.modifier.id"),
        GENERATE("attributes.modifier.generate"),
        DISPLAY("attributes.modifier.display"),
        OVERRIDE_TEXT("attributes.modifier.display.override"),
        UP("common.up"),
        DOWN("common.down"),
        DUPLICATE("common.duplicate"),
        RESET("attributes.modifier.reset_this"),
        REMOVE("common.remove");

        private final String path;

        ModifierField(String path) {
            this.path = path;
        }

        boolean appliesTo(ItemAttributeModifiers.Display.Type displayType) {
            return this != OVERRIDE_TEXT || displayType == ItemAttributeModifiers.Display.Type.OVERRIDE;
        }

        String scope(int index) {
            return modifierScope(index) + (this == OVERRIDE_TEXT ? ":override-text" : "");
        }

        String aliases() {
            return this == OVERRIDE_TEXT ? "custom tooltip text" : "";
        }

        @Override
        public String path() {
            return this.path;
        }
    }
}
