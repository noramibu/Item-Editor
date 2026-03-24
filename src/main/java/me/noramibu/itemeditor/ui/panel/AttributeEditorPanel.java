package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
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
import java.util.stream.Collectors;

public final class AttributeEditorPanel implements EditorPanel {

    private final ItemEditorScreen screen;

    public AttributeEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        Registry<Attribute> attributeRegistry = this.screen.session().registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
        List<String> attributeIds = RegistryUtil.ids(attributeRegistry);
        ItemAttributeModifiers prototypeModifiers = this.screen.session().originalStack().getPrototype().get(DataComponents.ATTRIBUTE_MODIFIERS);
        Set<String> prototypeKeys = prototypeModifiers == null
                ? Set.of()
                : prototypeModifiers.modifiers().stream().map(this::entryKey).collect(Collectors.toSet());

        FlowLayout root = UiFactory.column();
        root.child(this.buildEffectiveAttributesSection());

        FlowLayout intro = UiFactory.section(
                ItemEditorText.tr("attributes.modifiers.title"),
                Component.empty()
        );
        intro.child(UiFactory.button(ItemEditorText.tr("attributes.modifiers.reset"), button -> {
            PanelBindings.mutateRefresh(this.screen, () -> this.resetAttributeModifiersFromOriginal(state));
        }).horizontalSizing(Sizing.content()));
        intro.child(UiFactory.button(ItemEditorText.tr("attributes.modifiers.add"), button -> {
            PanelBindings.mutateRefresh(this.screen, () -> state.attributeModifiers.add(new ItemEditorState.AttributeModifierDraft()));
        }).horizontalSizing(Sizing.content()));
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
                card.child(UiFactory.muted(ItemEditorText.tr("attributes.modifier.built_in"), 220));
            }

            ButtonComponent attributeButton = UiFactory.button(
                    draft.attributeId.isBlank() ? ItemEditorText.str("attributes.modifier.select_attribute") : draft.attributeId,
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

            FlowLayout rowOne = UiFactory.row();
            rowOne.child(UiFactory.field(
                    ItemEditorText.tr("attributes.modifier.amount"),
                    Component.empty(),
                    UiFactory.textBox(draft.amount, PanelBindings.text(this.screen, value -> draft.amount = value)).horizontalSizing(Sizing.fixed(110))
            ));

            var operationButton = UiFactory.button(draft.operation, button -> this.screen.openDropdown(
                    button,
                    Arrays.asList(AttributeModifier.Operation.values()),
                    AttributeModifier.Operation::name,
                    operation -> PanelBindings.mutate(this.screen, () -> draft.operation = operation.name())
            ));
            rowOne.child(UiFactory.field(ItemEditorText.tr("attributes.modifier.operation"), Component.empty(), operationButton.horizontalSizing(Sizing.fixed(180))));

            var slotButton = UiFactory.button(draft.slotGroup, button -> this.screen.openDropdown(
                    button,
                    Arrays.asList(EquipmentSlotGroup.values()),
                    EquipmentSlotGroup::name,
                    slot -> PanelBindings.mutate(this.screen, () -> draft.slotGroup = slot.name())
            ));
            rowOne.child(UiFactory.field(ItemEditorText.tr("attributes.modifier.slot"), Component.empty(), slotButton.horizontalSizing(Sizing.fixed(160))));
            card.child(rowOne);

            card.child(UiFactory.field(
                    ItemEditorText.tr("attributes.modifier.id"),
                    Component.empty(),
                    UiFactory.textBox(draft.modifierId, PanelBindings.text(this.screen, value -> draft.modifierId = value)).horizontalSizing(Sizing.fixed(220))
            ));

            String vanillaTooltipNote = this.vanillaTooltipNote(draft);
            if (vanillaTooltipNote != null) {
                card.child(UiFactory.muted(vanillaTooltipNote, 520));
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
            card.child(UIComponents.label(line).maxWidth(520));
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
                320
        ));
        card.child(UiFactory.muted(attributeId, 320));
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
}
