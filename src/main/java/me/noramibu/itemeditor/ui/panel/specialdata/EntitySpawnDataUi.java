package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.service.EntitySpawnDataUtil;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

final class EntitySpawnDataUi {

    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int EQUIPMENT_SLOT_LABEL_WIDTH = 72;
    private static final int EQUIPMENT_SUMMARY_RESERVE = 112;

    private EntitySpawnDataUi() {
    }

    static UIComponent nameEditor(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            Component label,
            String placeholderKey,
            String colorTitleKey,
            String gradientTitleKey
    ) {
        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(draft.customName),
                Sizing.fill(100),
                UiFactory.fixed(NAME_EDITOR_HEIGHT),
                ItemEditorText.str(placeholderKey),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str(colorTitleKey),
                ItemEditorText.str(gradientTitleKey),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("special.spawn_egg.name.single_line")
                        : null,
                document -> context.mutate(() ->
                        draft.customName = TextComponentUtil.serializeEditorDocument(document))
        );

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(nameSection.toolbar());
        frame.child(nameSection.editor());
        frame.child(nameSection.validation());
        return UiFactory.field(label, Component.empty(), frame);
    }

    static FlowLayout flags(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft
    ) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.muted(ItemEditorText.tr("special.spawn_egg.flags")));

        addPackedRows(
                group,
                Math.max(1, context.panelWidthHint() - UiFactory.scaleProfile().padding() * 4),
                flag(context, "special.spawn_egg.no_ai", draft.noAi, value -> draft.noAi = value),
                flag(context, "special.spawn_egg.silent", draft.silent, value -> draft.silent = value),
                flag(context, "special.spawn_egg.no_gravity", draft.noGravity, value -> draft.noGravity = value),
                flag(context, "special.spawn_egg.glowing", draft.glowing, value -> draft.glowing = value),
                flag(context, "special.spawn_egg.invulnerable", draft.invulnerable,
                        value -> draft.invulnerable = value),
                flag(context, "special.spawn_egg.persistent", draft.persistenceRequired,
                        value -> draft.persistenceRequired = value),
                flag(context, "special.spawn_egg.name_visible", draft.customNameVisible,
                        value -> draft.customNameVisible = value)
        );
        return group;
    }

    static UIComponent health(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            boolean compactLayout
    ) {
        return health(
                context,
                draft.health,
                value -> draft.health = value,
                draft.entityId,
                draft.attributes,
                compactLayout
        );
    }

    static UIComponent health(
            SpecialDataPanelContext context,
            String health,
            Consumer<String> healthSetter,
            String entityId,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            boolean compactLayout
    ) {
        int columns = compactLayout ? 1 : 2;
        int fieldWidth = Math.max(1, context.panelWidthHint() / columns);
        int fieldFill = compactLayout ? 100 : 49;
        Component healthLabel = ItemEditorText.tr("special.spawn_egg.health");
        Component maxHealthLabel = ItemEditorText.tr("special.entity.max_health");
        int labelWidth = Math.max(textWidth(healthLabel), textWidth(maxHealthLabel));
        FlowLayout fields = compactLayout ? UiFactory.column() : UiFactory.row();
        FlowLayout healthField = compactValueField(
                healthLabel,
                UiFactory.textBox(health, context.bindText(healthSetter)),
                fieldWidth,
                labelWidth
        );
        healthField.horizontalSizing(Sizing.fill(fieldFill));
        fields.child(healthField);

        String maxHealthId = EntitySpawnDataUtil.maxHealthAttributeId();
        Map<String, Double> defaults = EntitySpawnDataUtil.defaultAttributeValues(
                context.screen().session().registryAccess(),
                entityId
        );
        String maxHealth = attributeBase(attributes, maxHealthId);
        if (defaults.containsKey(maxHealthId) || !maxHealth.isBlank()) {
            FlowLayout maxHealthField = compactValueField(
                    maxHealthLabel,
                    attributeInput(context, attributes, maxHealthId, defaults.get(maxHealthId)),
                    fieldWidth,
                    labelWidth
            );
            maxHealthField.horizontalSizing(Sizing.fill(fieldFill));
            fields.child(maxHealthField);
        }
        return fields;
    }

    static FlowLayout attributes(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft
    ) {
        return attributes(
                context,
                draft.entityId,
                draft.attributes,
                draft.uiAttributesCollapsed,
                () -> context.mutateRefresh(() -> draft.uiAttributesCollapsed = !draft.uiAttributesCollapsed),
                Set.of()
        );
    }

    static FlowLayout attributes(
            SpecialDataPanelContext context,
            String entityId,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            boolean collapsed,
            Runnable toggle,
            Set<String> excludedIds
    ) {
        FlowLayout group = UiFactory.column().gap(2);
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("category.attributes.title"))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.collapseToggleButton(collapsed, toggle));
        group.child(header);
        if (collapsed) {
            return group;
        }

        Map<String, Double> defaults = EntitySpawnDataUtil.defaultAttributeValues(
                context.screen().session().registryAccess(),
                entityId
        );
        Set<String> attributeIds = new LinkedHashSet<>(defaults.keySet());
        attributes.stream().map(attribute -> attribute.attributeId).forEach(attributeIds::add);
        attributeIds.remove(EntitySpawnDataUtil.maxHealthAttributeId());
        attributeIds.removeAll(excludedIds);
        if (attributeIds.isEmpty()) {
            group.child(UiFactory.muted(ItemEditorText.tr("common.none")));
            return group;
        }

        int columns = context.isCompactPanel(620) ? 1 : 2;
        int fieldWidth = Math.max(1, context.panelWidthHint() / columns);
        int fieldFill = columns == 1 ? 100 : 49;
        List<String> ids = List.copyOf(attributeIds);
        List<Component> labels = ids.stream().map(id -> attributeLabel(context, id)).toList();
        int labelWidth = labels.stream().mapToInt(EntitySpawnDataUi::textWidth).max().orElse(1);
        for (int offset = 0; offset < ids.size(); offset += columns) {
            FlowLayout row = UiFactory.row();
            for (int index = offset; index < Math.min(ids.size(), offset + columns); index++) {
                String attributeId = ids.get(index);
                row.child(compactValueField(
                        labels.get(index),
                        attributeInput(context, attributes, attributeId, defaults.get(attributeId)),
                        fieldWidth,
                        labelWidth
                ).horizontalSizing(Sizing.fill(fieldFill)));
            }
            group.child(row);
        }
        return group;
    }

    static boolean attributeSchemaChanged(
            SpecialDataPanelContext context,
            String previousEntityId,
            String nextEntityId
    ) {
        return !EntitySpawnDataUtil.defaultAttributeValues(
                context.screen().session().registryAccess(),
                previousEntityId
        ).equals(EntitySpawnDataUtil.defaultAttributeValues(
                context.screen().session().registryAccess(),
                nextEntityId
        ));
    }

    static FlowLayout equipment(
            SpecialDataPanelContext context,
            ItemEditorState.EntityEquipmentDraft draft,
            List<EquipmentSlot> slots,
            boolean editDropChances
    ) {
        FlowLayout group = UiFactory.column().gap(2);
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("common.equipment"))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.muted(ItemEditorText.tr(
                "special.entity.equipment.summary",
                equippedCount(draft, slots)
        )));
        header.child(UiFactory.collapseToggleButton(
                draft.uiCollapsed,
                () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
        ));
        group.child(header);
        if (draft.uiCollapsed) {
            return group;
        }

        for (EquipmentSlot slot : slots) {
            group.child(equipmentSlot(context, draft, slot, editDropChances));
        }
        return group;
    }

    private static FlowLayout equipmentSlot(
            SpecialDataPanelContext context,
            ItemEditorState.EntityEquipmentDraft draft,
            EquipmentSlot slot,
            boolean editDropChances
    ) {
        ItemStack stack = draft.stack(slot);
        FlowLayout group = UiFactory.column().gap(1);
        FlowLayout valueRow = UiFactory.row();
        valueRow.child(UiFactory.muted(ItemEditorText.tr(equipmentSlotKey(slot)), EQUIPMENT_SLOT_LABEL_WIDTH)
                .horizontalSizing(Sizing.fixed(EQUIPMENT_SLOT_LABEL_WIDTH)));
        if (!stack.isEmpty()) {
            valueRow.child(UIComponents.item(stack).showOverlay(false).setTooltipFromStack(true));
        }
        Component fullSummary = equipmentSummary(stack);
        int summaryWidth = Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(EQUIPMENT_SUMMARY_RESERVE));
        LabelComponent summary = UiFactory.muted(UiFactory.fitToWidth(fullSummary, summaryWidth), summaryWidth);
        summary.tooltip(List.of(fullSummary));
        summary.horizontalSizing(Sizing.expand(100));
        valueRow.child(summary);
        group.child(valueRow);

        ButtonComponent remove = UiFactory.negativeButton(
                ItemEditorText.tr("common.remove"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> draft.set(slot, ItemStack.EMPTY))
        );
        remove.active(!stack.isEmpty());
        FlowLayout actions = UiFactory.actionButtonRow(false,
                UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD, button ->
                        context.openSearchablePicker(
                                ItemEditorText.str("special.entity.equipment.picker_title"),
                                "",
                                context.itemIdsWithoutAir(),
                                id -> id,
                                id -> context.mutateRefresh(() -> draft.set(slot, stackForId(id)))
                        )
                ),
                context.storagePickButton(stackFromStorage -> draft.set(slot, stackFromStorage)),
                remove
        );
        if (editDropChances) {
            FlowLayout actionRow = UiFactory.row();
            actions.horizontalSizing(Sizing.fill(59));
            actionRow.child(actions);
            Component dropChanceLabel = ItemEditorText.tr("special.entity.equipment.drop_chance");
            actionRow.child(compactValueField(
                    dropChanceLabel,
                    UiFactory.textBox(
                            draft.dropChance(slot),
                            context.bindText(value -> draft.setDropChance(slot, value))
                    ),
                    Math.max(1, context.panelWidthHint() * 39 / 100),
                    textWidth(dropChanceLabel)
            ).horizontalSizing(Sizing.fill(39)));
            group.child(actionRow);
        } else {
            group.child(actions);
        }
        return group;
    }

    private static ItemStack stackForId(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(identifier)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static Component equipmentSummary(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemEditorText.tr("common.none");
        }
        return stack.getHoverName().copy().append(Component.literal(
                " | " + BuiltInRegistries.ITEM.getKey(stack.getItem())
        ));
    }

    private static Component attributeLabel(SpecialDataPanelContext context, String rawId) {
        Identifier identifier = IdFieldNormalizer.parse(rawId);
        if (identifier == null) {
            return Component.literal(rawId);
        }
        Registry<Attribute> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
        return registry.getOptional(identifier)
                .map(attribute -> Component.translatable(attribute.getDescriptionId()))
                .orElseGet(() -> Component.literal(rawId));
    }

    private static TextBoxComponent attributeInput(
            SpecialDataPanelContext context,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            String attributeId,
            Double defaultValue
    ) {
        TextBoxComponent input = UiFactory.textBox(
                attributeBase(attributes, attributeId),
                context.bindText(value -> setAttributeBase(attributes, attributeId, value))
        );
        if (defaultValue != null) {
            input.setHint(Component.literal(ValidationUtil.trimTrailingZeros(defaultValue)));
        }
        return input;
    }

    private static FlowLayout compactValueField(
            Component label,
            TextBoxComponent input,
            int availableWidth,
            int preferredLabelWidth
    ) {
        int gap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        int inputWidth = Math.min(
                Math.max(1, availableWidth / 2),
                Math.clamp(availableWidth / 3, 48, 96)
        );
        int labelWidth = Math.min(preferredLabelWidth, Math.max(1, availableWidth - inputWidth - gap));
        Component fittedLabel = UiFactory.fitToWidth(label, labelWidth);
        LabelComponent labelComponent = UiFactory.muted(fittedLabel, labelWidth);
        labelComponent.horizontalSizing(Sizing.fixed(labelWidth));
        if (!fittedLabel.getString().equals(label.getString())) {
            labelComponent.tooltip(List.of(label));
        }

        FlowLayout field = UiFactory.row().gap(gap);
        field.child(labelComponent);
        field.child(input.horizontalSizing(Sizing.fixed(inputWidth)));
        return field;
    }

    private static int textWidth(Component text) {
        return Minecraft.getInstance().font.width(text) + 4;
    }

    private static String attributeBase(
            List<ItemEditorState.EntityAttributeDraft> attributes,
            String attributeId
    ) {
        return attributes.stream()
                .filter(attribute -> attributeId.equals(attribute.attributeId))
                .map(attribute -> attribute.baseValue)
                .findFirst()
                .orElse("");
    }

    private static void setAttributeBase(
            List<ItemEditorState.EntityAttributeDraft> attributes,
            String attributeId,
            String value
    ) {
        String normalized = value == null ? "" : value.trim();
        ItemEditorState.EntityAttributeDraft existing = attributes.stream()
                .filter(attribute -> attributeId.equals(attribute.attributeId))
                .findFirst()
                .orElse(null);
        if (normalized.isBlank()) {
            attributes.removeIf(attribute -> attributeId.equals(attribute.attributeId));
        } else if (existing != null) {
            existing.baseValue = normalized;
        } else {
            ItemEditorState.EntityAttributeDraft attribute = new ItemEditorState.EntityAttributeDraft();
            attribute.attributeId = attributeId;
            attribute.baseValue = normalized;
            attributes.add(attribute);
        }
    }

    private static int equippedCount(
            ItemEditorState.EntityEquipmentDraft draft,
            List<EquipmentSlot> slots
    ) {
        return (int) slots.stream().filter(slot -> !draft.stack(slot).isEmpty()).count();
    }

    private static String equipmentSlotKey(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> "special.entity.equipment.slot.mainhand";
            case OFFHAND -> "special.entity.equipment.slot.offhand";
            case FEET -> "special.entity.equipment.slot.feet";
            case LEGS -> "special.entity.equipment.slot.legs";
            case CHEST -> "special.entity.equipment.slot.chest";
            case HEAD -> "special.entity.equipment.slot.head";
            case BODY -> "special.entity.equipment.slot.body";
            case SADDLE -> "special.entity.equipment.slot.saddle";
        };
    }

    private static UIComponent flag(
            SpecialDataPanelContext context,
            String labelKey,
            boolean checked,
            Consumer<Boolean> setter
    ) {
        Component label = ItemEditorText.tr(labelKey);
        int width = textWidth(label)
                + UiFactory.scaleProfile().controlHeight()
                + UiFactory.scaleProfile().tightSpacing();
        return UiFactory.checkbox(label, checked, value -> context.mutateRefresh(() -> setter.accept(value)))
                .horizontalSizing(Sizing.fixed(width));
    }

    private static void addPackedRows(FlowLayout group, int availableWidth, UIComponent... components) {
        int gap = Math.max(1, UiFactory.scaleProfile().spacing());
        int usedWidth = 0;
        FlowLayout row = UiFactory.row();
        for (UIComponent component : components) {
            int width = Math.min(availableWidth, component.horizontalSizing().get().value);
            int requiredWidth = usedWidth == 0 ? width : gap + width;
            if (usedWidth > 0 && usedWidth + requiredWidth > availableWidth) {
                group.child(row);
                row = UiFactory.row();
                usedWidth = 0;
                requiredWidth = width;
            }
            component.horizontalSizing(Sizing.fixed(width));
            row.child(component);
            usedWidth += requiredWidth;
        }
        group.child(row);
    }
}
