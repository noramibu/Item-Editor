package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.NbtCompatUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.SignText;

import java.util.List;
import java.util.Objects;

final class SignSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (this.sameSignData(context.special().sign, context.baselineSpecial().sign)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }

        if (this.isSignDataDefault(context.special().sign)) {
            this.clearToPrototype(context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }

        if (!this.isSignItem(context.previewStack()) && !this.isSignItem(context.originalStack())) {
            return;
        }

        CompoundTag blockEntityTag = new CompoundTag();
        CustomData blockEntityData = context.originalStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            blockEntityTag = blockEntityData.copyTag();
        }

        NbtCompatUtil.store(blockEntityTag, "front_text", SignText.DIRECT_CODEC, this.buildSignText(context.special().sign.front, context.messages()));
        NbtCompatUtil.store(blockEntityTag, "back_text", SignText.DIRECT_CODEC, this.buildSignText(context.special().sign.back, context.messages()));
        blockEntityTag.putBoolean("is_waxed", context.special().sign.waxed);

        context.previewStack().set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
    }

    private boolean isSignItem(ItemStack stack) {
        return stack.getItem() instanceof SignItem || stack.getItem() instanceof HangingSignItem;
    }

    private SignText buildSignText(ItemEditorState.SignSideDraft sideDraft, List<ValidationMessage> messages) {
        SignText signText = new SignText();
        DyeColor color = DyeColor.BLACK;

        if (!sideDraft.color.isBlank()) {
            try {
                color = DyeColor.valueOf(sideDraft.color);
            } catch (IllegalArgumentException exception) {
                messages.add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.sign.color"),
                        sideDraft.color
                )));
            }
        }

        signText = signText.setColor(color).setHasGlowingText(sideDraft.glowing);
        for (int index = 0; index < 4; index++) {
            String line = index < sideDraft.lines.size() ? Objects.toString(sideDraft.lines.get(index), "") : "";
            signText = signText.setMessage(index, this.withPlainBaseline(TextComponentUtil.parseMarkup(line)));
        }
        return signText;
    }

    private boolean isSignDataDefault(ItemEditorState.SignData signData) {
        return !signData.waxed
                && this.isSignSideDefault(signData.front)
                && this.isSignSideDefault(signData.back);
    }

    private boolean isSignSideDefault(ItemEditorState.SignSideDraft sideDraft) {
        if (!DyeColor.BLACK.name().equals(sideDraft.color) || sideDraft.glowing) {
            return false;
        }
        for (String line : sideDraft.lines) {
            if (line != null && !line.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean sameSignData(ItemEditorState.SignData current, ItemEditorState.SignData baseline) {
        return current.waxed == baseline.waxed
                && this.sameSignSide(current.front, baseline.front)
                && this.sameSignSide(current.back, baseline.back);
    }

    private boolean sameSignSide(ItemEditorState.SignSideDraft current, ItemEditorState.SignSideDraft baseline) {
        if (!Objects.equals(current.color, baseline.color) || current.glowing != baseline.glowing) {
            return false;
        }

        for (int index = 0; index < 4; index++) {
            String left = index < current.lines.size() ? Objects.toString(current.lines.get(index), "") : "";
            String right = index < baseline.lines.size() ? Objects.toString(baseline.lines.get(index), "") : "";
            if (!Objects.equals(left, right)) {
                return false;
            }
        }

        return true;
    }
}
