package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.ItemEditorClient;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;
import java.util.Objects;

final class AttributesPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        if (this.sameAttributeModifiers(context.state().attributeModifiers, context.baselineState().attributeModifiers)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.ATTRIBUTE_MODIFIERS);
            return;
        }

        Registry<Attribute> attributeRegistry = context.registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        for (int index = 0; index < context.state().attributeModifiers.size(); index++) {
            ItemEditorState.AttributeModifierDraft draft = context.state().attributeModifiers.get(index);
            if (draft.attributeId.isBlank()) continue;

            Holder<Attribute> attribute = RegistryUtil.resolveHolder(attributeRegistry, draft.attributeId);
            if (attribute == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.unknown_attribute", draft.attributeId)));
                continue;
            }

            Double amount = ValidationUtil.parseDouble(draft.amount, ItemEditorText.str("attributes.modifier.amount"), context.messages());
            if (amount == null) continue;

            AttributeModifier.Operation operation;
            EquipmentSlotGroup slotGroup;
            try {
                operation = AttributeModifier.Operation.valueOf(draft.operation);
                slotGroup = EquipmentSlotGroup.valueOf(draft.slotGroup);
            } catch (IllegalArgumentException exception) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.attribute_operation")));
                continue;
            }

            Identifier modifierId;
            if (draft.modifierId.isBlank()) {
                modifierId = Identifier.fromNamespaceAndPath(ItemEditorClient.MOD_ID, "generated/" + index);
            } else {
                modifierId = Identifier.tryParse(draft.modifierId);
                if (modifierId == null) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.modifier_id", draft.modifierId)));
                    continue;
                }
            }

            builder.add(attribute, new AttributeModifier(modifierId, amount, operation), slotGroup);
        }

        ItemAttributeModifiers built = builder.build();
        if (built.modifiers().isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.ATTRIBUTE_MODIFIERS);
        } else {
            context.previewStack().set(DataComponents.ATTRIBUTE_MODIFIERS, built);
        }
    }

    private boolean sameAttributeModifiers(List<ItemEditorState.AttributeModifierDraft> current, List<ItemEditorState.AttributeModifierDraft> baseline) {
        return this.sameList(current, baseline, (left, right) -> Objects.equals(left.attributeId, right.attributeId)
                && Objects.equals(left.modifierId, right.modifierId)
                && Objects.equals(left.amount, right.amount)
                && Objects.equals(left.operation, right.operation)
                && Objects.equals(left.slotGroup, right.slotGroup));
    }
}
