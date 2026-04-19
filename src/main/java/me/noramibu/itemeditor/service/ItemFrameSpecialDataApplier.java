package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.Objects;

final class ItemFrameSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (!this.supportsItemFrameData(context)) {
            return;
        }

        if (this.sameItemFrameData(context.special(), context.baselineSpecial())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }

        if (this.isItemFrameDataDefault(context.special())) {
            this.clearToPrototype(context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }

        CompoundTag entityTag = new CompoundTag();
        TypedEntityData<EntityType<?>> originalData = context.originalStack().get(DataComponents.ENTITY_DATA);
        if (isItemFrameType(originalData)) {
            entityTag = originalData.copyTagWithoutId();
        }

        setBooleanKey(entityTag, "Invisible", context.special().itemFrameInvisible);
        setBooleanKey(entityTag, "Fixed", context.special().itemFrameFixed);
        setBooleanKey(entityTag, "NoGravity", context.special().itemFrameNoGravity);
        setBooleanKey(entityTag, "Invulnerable", context.special().itemFrameInvulnerable);
        setBooleanKey(entityTag, "CustomNameVisible", context.special().itemFrameCustomNameVisible);
        this.applyCustomName(entityTag, context.special().itemFrameCustomName);
        this.putOptionalIntTag(
                entityTag,
                "ItemRotation",
                context.special().itemFrameRotation,
                ItemEditorText.str("special.item_frame.item_rotation"),
                0,
                7,
                context.messages()
        );
        this.putOptionalIntTag(
                entityTag,
                "Facing",
                context.special().itemFrameFacing,
                ItemEditorText.str("special.item_frame.facing"),
                0,
                5,
                context.messages()
        );
        this.applyDropChance(entityTag, context);

        if (entityTag.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }
        context.previewStack().set(
                DataComponents.ENTITY_DATA,
                TypedEntityData.of(resolveItemFrameType(context), entityTag)
        );
    }

    private void applyCustomName(CompoundTag entityTag, String rawName) {
        if (rawName == null || rawName.isBlank()) {
            entityTag.remove("CustomName");
            return;
        }
        entityTag.store(
                "CustomName",
                ComponentSerialization.CODEC,
                this.withPlainBaseline(TextComponentUtil.parseMarkup(rawName))
        );
    }

    private void applyDropChance(CompoundTag entityTag, SpecialDataApplyContext context) {
        String raw = context.special().itemFrameDropChance.trim();
        if (raw.isBlank()) {
            entityTag.remove("ItemDropChance");
            return;
        }

        Float value = ValidationUtil.parseFloat(
                raw,
                ItemEditorText.str("special.item_frame.item_drop_chance"),
                context.messages()
        );
        if (value == null) {
            return;
        }
        if (value < 0.0F || value > 1.0F) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.range",
                    ItemEditorText.str("special.item_frame.item_drop_chance"),
                    0,
                    1
            )));
            return;
        }
        entityTag.putFloat("ItemDropChance", value);
    }

    private boolean supportsItemFrameData(SpecialDataApplyContext context) {
        if (context.previewStack().is(Items.ITEM_FRAME) || context.previewStack().is(Items.GLOW_ITEM_FRAME)) {
            return true;
        }
        TypedEntityData<EntityType<?>> previewData = context.previewStack().get(DataComponents.ENTITY_DATA);
        if (isItemFrameType(previewData)) {
            return true;
        }
        TypedEntityData<EntityType<?>> originalData = context.originalStack().get(DataComponents.ENTITY_DATA);
        return isItemFrameType(originalData);
    }

    private static EntityType<?> resolveItemFrameType(SpecialDataApplyContext context) {
        TypedEntityData<EntityType<?>> previewData = context.previewStack().get(DataComponents.ENTITY_DATA);
        if (isItemFrameType(previewData)) {
            return previewData.type();
        }
        TypedEntityData<EntityType<?>> originalData = context.originalStack().get(DataComponents.ENTITY_DATA);
        if (isItemFrameType(originalData)) {
            return originalData.type();
        }
        if (context.previewStack().is(Items.GLOW_ITEM_FRAME) || context.originalStack().is(Items.GLOW_ITEM_FRAME)) {
            return EntityType.GLOW_ITEM_FRAME;
        }
        return EntityType.ITEM_FRAME;
    }

    private static boolean isItemFrameType(TypedEntityData<EntityType<?>> data) {
        return data != null
                && (data.type() == EntityType.ITEM_FRAME || data.type() == EntityType.GLOW_ITEM_FRAME);
    }

    private boolean sameItemFrameData(ItemEditorState.SpecialData current, ItemEditorState.SpecialData baseline) {
        return current.itemFrameInvisible == baseline.itemFrameInvisible
                && current.itemFrameFixed == baseline.itemFrameFixed
                && current.itemFrameNoGravity == baseline.itemFrameNoGravity
                && current.itemFrameInvulnerable == baseline.itemFrameInvulnerable
                && current.itemFrameCustomNameVisible == baseline.itemFrameCustomNameVisible
                && Objects.equals(current.itemFrameCustomName, baseline.itemFrameCustomName)
                && Objects.equals(current.itemFrameRotation, baseline.itemFrameRotation)
                && Objects.equals(current.itemFrameDropChance, baseline.itemFrameDropChance)
                && Objects.equals(current.itemFrameFacing, baseline.itemFrameFacing);
    }

    private boolean isItemFrameDataDefault(ItemEditorState.SpecialData special) {
        return !special.itemFrameInvisible
                && !special.itemFrameFixed
                && !special.itemFrameNoGravity
                && !special.itemFrameInvulnerable
                && !special.itemFrameCustomNameVisible
                && special.itemFrameCustomName.isBlank()
                && special.itemFrameRotation.isBlank()
                && special.itemFrameDropChance.isBlank()
                && special.itemFrameFacing.isBlank();
    }

    private static void setBooleanKey(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        } else {
            tag.remove(key);
        }
    }
}
