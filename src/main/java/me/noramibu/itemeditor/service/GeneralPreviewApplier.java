package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class GeneralPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        ItemEditorState state = context.state();
        ItemEditorState baselineState = context.baselineState();

        if (Objects.equals(state.customName, baselineState.customName)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.CUSTOM_NAME);
        } else if (state.customName.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.CUSTOM_NAME);
        } else {
            context.previewStack().set(DataComponents.CUSTOM_NAME, this.withPlainBaseline(TextComponentUtil.parseMarkup(state.customName)));
        }

        if (!Objects.equals(state.count, baselineState.count)) {
            Integer count = ValidationUtil.parseInt(state.count, ItemEditorText.str("general.stack_count"), 1, context.previewStack().getMaxStackSize(), context.messages());
            if (count != null) {
                context.previewStack().setCount(count);
            }
        }

        if (Objects.equals(state.maxDamage, baselineState.maxDamage) && Objects.equals(state.currentDamage, baselineState.currentDamage)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.MAX_DAMAGE);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.DAMAGE);
        } else {
            Integer maxDamage = state.maxDamage.isBlank()
                    ? null
                    : ValidationUtil.parseInt(state.maxDamage, ItemEditorText.str("general.max_damage"), 1, Integer.MAX_VALUE, context.messages());
            if (maxDamage != null) {
                context.previewStack().set(DataComponents.MAX_DAMAGE, maxDamage);
            } else if (state.maxDamage.isBlank()) {
                this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.MAX_DAMAGE);
            }

            if (!state.currentDamage.isBlank()) {
                int damageUpperBound = maxDamage != null ? maxDamage : Math.max(context.previewStack().getMaxDamage(), 1);
                Integer currentDamage = ValidationUtil.parseInt(state.currentDamage, ItemEditorText.str("general.current_damage"), 0, damageUpperBound, context.messages());
                if (currentDamage != null) {
                    context.previewStack().set(DataComponents.DAMAGE, currentDamage);
                }
            } else {
                this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.DAMAGE);
            }
        }

        if (Objects.equals(state.repairCost, baselineState.repairCost)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.REPAIR_COST);
        } else {
            Integer repairCost = state.repairCost.isBlank()
                    ? null
                    : ValidationUtil.parseInt(state.repairCost, ItemEditorText.str("general.repair_cost"), 0, Integer.MAX_VALUE, context.messages());
            if (repairCost != null) {
                context.previewStack().set(DataComponents.REPAIR_COST, repairCost);
            } else if (state.repairCost.isBlank()) {
                this.clearToPrototype(context.previewStack(), DataComponents.REPAIR_COST);
            }
        }

        if (state.unbreakable != baselineState.unbreakable) {
            if (state.unbreakable) {
                context.previewStack().set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            } else {
                this.clearToPrototype(context.previewStack(), DataComponents.UNBREAKABLE);
            }
        } else {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.UNBREAKABLE);
        }

        if (state.glintOverrideEnabled != baselineState.glintOverrideEnabled || state.glintOverride != baselineState.glintOverride) {
            if (state.glintOverrideEnabled) {
                context.previewStack().set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, state.glintOverride);
            } else {
                this.clearToPrototype(context.previewStack(), DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            }
        } else {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }

        if (Objects.equals(state.rarity, baselineState.rarity)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.RARITY);
        } else if (!state.rarity.isBlank()) {
            try {
                context.previewStack().set(DataComponents.RARITY, Rarity.valueOf(state.rarity));
            } catch (IllegalArgumentException exception) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.unknown_rarity", state.rarity)));
            }
        } else {
            this.clearToPrototype(context.previewStack(), DataComponents.RARITY);
        }

        if (Objects.equals(state.itemModelId, baselineState.itemModelId)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.ITEM_MODEL);
        } else if (state.itemModelId.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.ITEM_MODEL);
        } else {
            ResourceLocation itemModelId = IdFieldNormalizer.parse(state.itemModelId);
            if (itemModelId != null) {
                context.previewStack().set(DataComponents.ITEM_MODEL, itemModelId);
            } else {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.item_model_id")));
            }
        }

        if (this.sameCustomModel(state, baselineState)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.CUSTOM_MODEL_DATA);
        } else {
            CustomModelData merged = this.mergeCustomModelData(context.originalStack().get(DataComponents.CUSTOM_MODEL_DATA), state, context.messages());
            if (merged.floats().isEmpty() && merged.flags().isEmpty() && merged.strings().isEmpty() && merged.colors().isEmpty()) {
                this.clearToPrototype(context.previewStack(), DataComponents.CUSTOM_MODEL_DATA);
            } else {
                context.previewStack().set(DataComponents.CUSTOM_MODEL_DATA, merged);
            }
        }
    }

    private boolean sameCustomModel(ItemEditorState state, ItemEditorState baselineState) {
        return Objects.equals(state.customModelFloat, baselineState.customModelFloat)
                && state.customModelFlagEnabled == baselineState.customModelFlagEnabled
                && state.customModelFlag == baselineState.customModelFlag
                && Objects.equals(state.customModelString, baselineState.customModelString)
                && Objects.equals(state.customModelColor, baselineState.customModelColor);
    }

    private CustomModelData mergeCustomModelData(CustomModelData originalData, ItemEditorState state, List<ValidationMessage> messages) {
        List<Float> floats = new ArrayList<>(originalData != null ? originalData.floats() : List.of());
        List<Boolean> flags = new ArrayList<>(originalData != null ? originalData.flags() : List.of());
        List<String> strings = new ArrayList<>(originalData != null ? originalData.strings() : List.of());
        List<Integer> colors = new ArrayList<>(originalData != null ? originalData.colors() : List.of());

        if (!state.customModelFloat.isBlank()) {
            Float customModelFloat = ValidationUtil.parseFloat(state.customModelFloat, ItemEditorText.str("general.item_model.float"), messages);
            if (customModelFloat != null) {
                this.setFirstValue(floats, customModelFloat);
            }
        } else if (!floats.isEmpty()) {
            floats.removeFirst();
        }

        if (state.customModelFlagEnabled) {
            this.setFirstValue(flags, state.customModelFlag);
        } else if (!flags.isEmpty()) {
            flags.removeFirst();
        }

        if (!state.customModelString.isBlank()) {
            this.setFirstValue(strings, state.customModelString);
        } else if (!strings.isEmpty()) {
            strings.removeFirst();
        }

        if (!state.customModelColor.isBlank()) {
            Integer color = ValidationUtil.parseColor(state.customModelColor, ItemEditorText.str("general.item_model.color"), messages);
            if (color != null) {
                this.setFirstValue(colors, color);
            }
        } else if (!colors.isEmpty()) {
            colors.removeFirst();
        }

        return new CustomModelData(floats, flags, strings, colors);
    }

    private <T> void setFirstValue(List<T> values, T value) {
        while (values.isEmpty()) {
            values.add(value);
        }
        values.set(0, value);
    }
}
