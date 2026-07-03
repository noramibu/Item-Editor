package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
            context.previewStack().set(DataComponents.CUSTOM_NAME, this.rebuiltCustomName(state.customName));
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

        if (!Objects.equals(state.glintOverride, baselineState.glintOverride)) {
            this.applyGlintOverride(context, state.glintOverride);
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
            Identifier itemModelId = IdFieldNormalizer.parse(state.itemModelId);
            if (itemModelId != null) {
                context.previewStack().set(DataComponents.ITEM_MODEL, itemModelId);
            } else {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.item_model_id")));
            }
        }

        if (this.sameCustomModel(state, baselineState)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.CUSTOM_MODEL_DATA);
        } else {
            CustomModelData merged = this.mergeCustomModelData(state, context.messages());
            if (merged.floats().isEmpty() && merged.flags().isEmpty() && merged.strings().isEmpty() && merged.colors().isEmpty()) {
                this.clearToPrototype(context.previewStack(), DataComponents.CUSTOM_MODEL_DATA);
            } else {
                context.previewStack().set(DataComponents.CUSTOM_MODEL_DATA, merged);
            }
        }

        this.applyAdventurePredicate(
                context,
                state.canBreakBlockIds,
                baselineState.canBreakBlockIds,
                DataComponents.CAN_BREAK,
                ItemEditorText.str("general.adventure.can_break")
        );
        this.applyAdventurePredicate(
                context,
                state.canPlaceOnBlockIds,
                baselineState.canPlaceOnBlockIds,
                DataComponents.CAN_PLACE_ON,
                ItemEditorText.str("general.adventure.can_place_on")
        );
    }

    private Component rebuiltCustomName(String rawName) {
        Component compact = TextComponentUtil.compactStyleFlags(TextComponentUtil.parseMarkup(rawName));
        if (compact.getStyle().isItalic()) {
            return compact;
        }
        return compact.copy().withStyle(compact.getStyle().withItalic(false));
    }

    private void applyGlintOverride(ItemPreviewApplyContext context, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            return;
        }
        context.previewStack().set(
                DataComponents.ENCHANTMENT_GLINT_OVERRIDE,
                Boolean.parseBoolean(rawValue)
        );
    }

    private boolean sameCustomModel(ItemEditorState state, ItemEditorState baselineState) {
        return Objects.equals(state.customModelFloat, baselineState.customModelFloat)
                && Objects.equals(state.customModelFlags, baselineState.customModelFlags)
                && Objects.equals(state.customModelString, baselineState.customModelString)
                && Objects.equals(state.customModelColor, baselineState.customModelColor);
    }

    private CustomModelData mergeCustomModelData(ItemEditorState state, List<ValidationMessage> messages) {
        return new CustomModelData(
                this.parseCustomModelFloats(state.customModelFloat, messages),
                this.parseCustomModelFlags(state.customModelFlags, messages),
                splitCommaSeparated(state.customModelString),
                this.parseCustomModelColors(state.customModelColor, messages)
        );
    }

    private List<Float> parseCustomModelFloats(String raw, List<ValidationMessage> messages) {
        List<Float> values = new ArrayList<>();
        String fieldLabel = ItemEditorText.str("general.item_model.float");
        for (String part : splitCommaSeparated(raw)) {
            Float value = ValidationUtil.parseFloat(part, fieldLabel, messages);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private List<Boolean> parseCustomModelFlags(String raw, List<ValidationMessage> messages) {
        List<Boolean> values = new ArrayList<>();
        String fieldLabel = ItemEditorText.str("general.item_model.flag_value");
        for (String part : splitCommaSeparated(raw)) {
            if ("true".equalsIgnoreCase(part)) {
                values.add(Boolean.TRUE);
                continue;
            }
            if ("false".equalsIgnoreCase(part)) {
                values.add(Boolean.FALSE);
                continue;
            }
            messages.add(ValidationMessage.error(fieldLabel + " must be true or false: " + part));
        }
        return values;
    }

    private List<Integer> parseCustomModelColors(String raw, List<ValidationMessage> messages) {
        List<Integer> values = new ArrayList<>();
        String fieldLabel = ItemEditorText.str("general.item_model.color");
        for (String part : splitCommaSeparated(raw)) {
            Integer value = ValidationUtil.tryParseHexColor(part);
            if (value == null) {
                value = this.parseCustomModelDecimalColor(part, fieldLabel, messages);
            }
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private Integer parseCustomModelDecimalColor(
            String raw,
            String fieldLabel,
            List<ValidationMessage> messages
    ) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0 || value > 0xFFFFFF) {
                messages.add(ValidationMessage.error(fieldLabel + " must be a hex color or RGB integer: " + raw));
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            messages.add(ValidationMessage.error(fieldLabel + " must be a hex color or RGB integer: " + raw));
            return null;
        }
    }

    private static List<String> splitCommaSeparated(String raw) {
        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private void applyAdventurePredicate(
            ItemPreviewApplyContext context,
            List<String> stateBlocks,
            List<String> baselineBlocks,
            DataComponentType<AdventureModePredicate> componentType,
            String fieldLabel
    ) {
        if (Objects.equals(stateBlocks, baselineBlocks)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), componentType);
            return;
        }

        if (stateBlocks.isEmpty()) {
            this.clearToPrototype(context.previewStack(), componentType);
            return;
        }

        AdventureModePredicate predicate = this.buildAdventurePredicate(context, stateBlocks, fieldLabel);
        if (predicate != null) {
            context.previewStack().set(componentType, predicate);
        }
    }

    private AdventureModePredicate buildAdventurePredicate(
            ItemPreviewApplyContext context,
            List<String> blockIds,
            String fieldLabel
    ) {
        Registry<Block> blockRegistry;
        try {
            blockRegistry = context.registryAccess().lookupOrThrow(Registries.BLOCK);
        } catch (RuntimeException exception) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", fieldLabel)));
            return null;
        }

        List<BlockPredicate> predicates = new ArrayList<>();
        for (String blockId : blockIds) {
            var blockHolder = RegistryUtil.resolveHolder(blockRegistry, blockId);
            if (blockHolder == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("validation.registry_missing", fieldLabel, blockId)));
                continue;
            }
            predicates.add(new BlockPredicate(
                    Optional.of(HolderSet.direct(blockHolder)),
                    Optional.empty(),
                    Optional.empty(),
                    DataComponentMatchers.ANY
            ));
        }

        if (predicates.isEmpty()) {
            return null;
        }
        return new AdventureModePredicate(predicates);
    }
}
