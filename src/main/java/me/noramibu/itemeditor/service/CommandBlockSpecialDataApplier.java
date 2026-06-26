package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Objects;

final class CommandBlockSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        BlockEntityType<?> commandBlockType = this.resolveCommandBlockType(context);
        if (commandBlockType == null) {
            return;
        }

        if (this.sameCommandBlockData(context.special(), context.baselineSpecial())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }

        if (this.isCommandBlockDataDefault(context.special())
                && context.special().commandBlockOriginalTag.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }

        CompoundTag blockTag = context.special().commandBlockOriginalTag.copy();
        this.applyCommandText(blockTag, context.special());
        this.applyCustomName(blockTag, context.special());
        this.applyActivation(blockTag, context.special());
        if (!this.applyRuntimeState(blockTag, context)) {
            return;
        }

        if (blockTag.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }
        context.previewStack().set(
                DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(commandBlockType, blockTag)
        );
    }

    private void applyCommandText(CompoundTag blockTag, ItemEditorState.SpecialData special) {
        String command = rawOrEmpty(special.commandBlockCommand);
        if (isBlank(command)) {
            blockTag.remove("Command");
        } else {
            blockTag.putString("Command", command);
        }
    }

    private void applyCustomName(CompoundTag blockTag, ItemEditorState.SpecialData special) {
        String customName = rawOrEmpty(special.commandBlockCustomName);
        if (isBlank(customName)) {
            blockTag.remove("CustomName");
            return;
        }
        blockTag.store(
                "CustomName",
                ComponentSerialization.CODEC,
                TextComponentUtil.parseMarkup(customName)
        );
    }

    private void applyActivation(CompoundTag blockTag, ItemEditorState.SpecialData special) {
        setDefaultFalseBoolean(blockTag, "auto", special.commandBlockAuto);
        setDefaultFalseBoolean(blockTag, "powered", special.commandBlockPowered);
        setDefaultFalseBoolean(blockTag, "conditionMet", special.commandBlockConditionMet);
    }

    private boolean applyRuntimeState(CompoundTag blockTag, SpecialDataApplyContext context) {
        ItemEditorState.SpecialData special = context.special();
        setDefaultTrueBoolean(blockTag, "TrackOutput", special.commandBlockTrackOutput);
        setDefaultTrueBoolean(blockTag, "UpdateLastExecution", special.commandBlockUpdateLastExecution);

        if (!putSuccessCount(
                blockTag,
                special.commandBlockSuccessCount,
                ItemEditorText.str("special.command_block.success_count"),
                context
        )) {
            return false;
        }
        if (!putLastExecution(
                blockTag,
                special.commandBlockLastExecution,
                ItemEditorText.str("special.command_block.last_execution"),
                context
        )) {
            return false;
        }

        String lastOutput = rawOrEmpty(special.commandBlockLastOutput);
        if (isBlank(lastOutput)) {
            blockTag.remove("LastOutput");
        } else {
            blockTag.store(
                    "LastOutput",
                    ComponentSerialization.CODEC,
                    TextComponentUtil.parseMarkup(lastOutput)
            );
        }
        if (!special.commandBlockTrackOutput) {
            blockTag.remove("LastOutput");
        }
        if (!special.commandBlockUpdateLastExecution) {
            blockTag.remove("LastExecution");
        }
        return true;
    }

    private boolean putSuccessCount(
            CompoundTag blockTag,
            String raw,
            String label,
            SpecialDataApplyContext context
    ) {
        String value = rawOrEmpty(raw);
        if (isBlank(value)) {
            blockTag.remove("SuccessCount");
            return true;
        }
        Integer parsed = ValidationUtil.parseInt(
                value,
                label,
                0,
                Integer.MAX_VALUE,
                context.messages()
        );
        if (parsed == null) {
            return false;
        }
        blockTag.putInt("SuccessCount", parsed);
        return true;
    }

    private boolean putLastExecution(
            CompoundTag blockTag,
            String raw,
            String label,
            SpecialDataApplyContext context
    ) {
        String value = rawOrEmpty(raw);
        if (isBlank(value)) {
            blockTag.remove("LastExecution");
            return true;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed < -1L) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.range",
                        label,
                        -1L,
                        Long.MAX_VALUE
                )));
                return false;
            }
            blockTag.putLong("LastExecution", parsed);
            return true;
        } catch (NumberFormatException exception) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.whole_number",
                    label
            )));
            return false;
        }
    }

    private BlockEntityType<?> resolveCommandBlockType(SpecialDataApplyContext context) {
        if (context.previewStack().is(Items.COMMAND_BLOCK)
                || context.previewStack().is(Items.REPEATING_COMMAND_BLOCK)
                || context.previewStack().is(Items.CHAIN_COMMAND_BLOCK)) {
            return BlockEntityType.COMMAND_BLOCK;
        }

        TypedEntityData<BlockEntityType<?>> previewData = context.previewStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (previewData != null && previewData.type() == BlockEntityType.COMMAND_BLOCK) {
            return BlockEntityType.COMMAND_BLOCK;
        }

        TypedEntityData<BlockEntityType<?>> originalData = context.originalStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (originalData != null && originalData.type() == BlockEntityType.COMMAND_BLOCK) {
            return BlockEntityType.COMMAND_BLOCK;
        }

        return null;
    }

    private boolean sameCommandBlockData(
            ItemEditorState.SpecialData current,
            ItemEditorState.SpecialData baseline
    ) {
        return Objects.equals(current.commandBlockCommand, baseline.commandBlockCommand)
                && Objects.equals(current.commandBlockCustomName, baseline.commandBlockCustomName)
                && current.commandBlockAuto == baseline.commandBlockAuto
                && current.commandBlockPowered == baseline.commandBlockPowered
                && current.commandBlockConditionMet == baseline.commandBlockConditionMet
                && current.commandBlockTrackOutput == baseline.commandBlockTrackOutput
                && current.commandBlockUpdateLastExecution == baseline.commandBlockUpdateLastExecution
                && Objects.equals(current.commandBlockSuccessCount, baseline.commandBlockSuccessCount)
                && Objects.equals(current.commandBlockLastExecution, baseline.commandBlockLastExecution)
                && Objects.equals(current.commandBlockLastOutput, baseline.commandBlockLastOutput);
    }

    private boolean isCommandBlockDataDefault(ItemEditorState.SpecialData special) {
        return isBlank(special.commandBlockCommand)
                && isBlank(special.commandBlockCustomName)
                && !special.commandBlockAuto
                && !special.commandBlockPowered
                && !special.commandBlockConditionMet
                && special.commandBlockTrackOutput
                && special.commandBlockUpdateLastExecution
                && isBlank(special.commandBlockSuccessCount)
                && isBlank(special.commandBlockLastExecution)
                && isBlank(special.commandBlockLastOutput);
    }

    private static void setDefaultFalseBoolean(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        } else {
            tag.remove(key);
        }
    }

    private static void setDefaultTrueBoolean(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.remove(key);
        } else {
            tag.putBoolean(key, false);
        }
    }

    private static boolean isBlank(String raw) {
        return raw == null || raw.trim().isBlank();
    }

    private static String rawOrEmpty(String raw) {
        return raw == null ? "" : raw;
    }
}
