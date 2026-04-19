package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.Objects;

final class ArmorStandSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    private static final float MIN_SCALE = 0.01F;
    private static final float MAX_SCALE = 10.0F;

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (!this.supportsArmorStandData(context)) {
            return;
        }

        if (this.sameArmorStandData(context.special(), context.baselineSpecial())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }

        if (this.isArmorStandDataDefault(context.special())) {
            this.clearToPrototype(context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }

        CompoundTag entityTag = new CompoundTag();
        TypedEntityData<EntityType<?>> originalData = context.originalStack().get(DataComponents.ENTITY_DATA);
        if (originalData != null && originalData.type() == EntityType.ARMOR_STAND) {
            entityTag = originalData.copyTagWithoutId();
        }

        setBooleanKey(entityTag, "Small", context.special().armorStandSmall);
        setBooleanKey(entityTag, "ShowArms", context.special().armorStandShowArms);
        setBooleanKey(entityTag, "NoBasePlate", context.special().armorStandNoBasePlate);
        setBooleanKey(entityTag, "Invisible", context.special().armorStandInvisible);
        setBooleanKey(entityTag, "NoGravity", context.special().armorStandNoGravity);
        setBooleanKey(entityTag, "Invulnerable", context.special().armorStandInvulnerable);
        setBooleanKey(entityTag, "CustomNameVisible", context.special().armorStandCustomNameVisible);
        setBooleanKey(entityTag, "Marker", context.special().armorStandMarker);
        this.applyCustomName(entityTag, context.special().armorStandCustomName);
        this.putOptionalIntTag(
                entityTag,
                "DisabledSlots",
                context.special().armorStandDisabledSlots,
                ItemEditorText.str("special.armor_stand.disabled_slots"),
                0,
                Integer.MAX_VALUE,
                context.messages()
        );

        this.applyPose(entityTag, context);
        this.applyScale(entityTag, context);

        if (entityTag.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }
        context.previewStack().set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityType.ARMOR_STAND, entityTag));
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

    private boolean supportsArmorStandData(SpecialDataApplyContext context) {
        if (context.previewStack().is(Items.ARMOR_STAND)) {
            return true;
        }
        TypedEntityData<EntityType<?>> previewData = context.previewStack().get(DataComponents.ENTITY_DATA);
        if (previewData != null && previewData.type() == EntityType.ARMOR_STAND) {
            return true;
        }
        TypedEntityData<EntityType<?>> originalData = context.originalStack().get(DataComponents.ENTITY_DATA);
        return originalData != null && originalData.type() == EntityType.ARMOR_STAND;
    }

    private void applyPose(CompoundTag entityTag, SpecialDataApplyContext context) {
        PoseValue head = this.parsePosePart(context, "special.armor_stand.part.head", context.special().armorStandPose.head, 0.0F, 0.0F);
        PoseValue body = this.parsePosePart(context, "special.armor_stand.part.body", context.special().armorStandPose.body, 0.0F, 0.0F);
        PoseValue leftArm = this.parsePosePart(context, "special.armor_stand.part.left_arm", context.special().armorStandPose.leftArm, -10.0F, -10.0F);
        PoseValue rightArm = this.parsePosePart(context, "special.armor_stand.part.right_arm", context.special().armorStandPose.rightArm, -15.0F, 10.0F);
        PoseValue leftLeg = this.parsePosePart(context, "special.armor_stand.part.left_leg", context.special().armorStandPose.leftLeg, -1.0F, -1.0F);
        PoseValue rightLeg = this.parsePosePart(context, "special.armor_stand.part.right_leg", context.special().armorStandPose.rightLeg, 1.0F, 1.0F);

        if (head == null || body == null || leftArm == null || rightArm == null || leftLeg == null || rightLeg == null) {
            return;
        }

        if (head.isDefault(0.0F, 0.0F)
                && body.isDefault(0.0F, 0.0F)
                && leftArm.isDefault(-10.0F, -10.0F)
                && rightArm.isDefault(-15.0F, 10.0F)
                && leftLeg.isDefault(-1.0F, -1.0F)
                && rightLeg.isDefault(1.0F, 1.0F)) {
            entityTag.remove("Pose");
            return;
        }

        CompoundTag poseTag = new CompoundTag();
        poseTag.put("Head", head.toTag());
        poseTag.put("Body", body.toTag());
        poseTag.put("LeftArm", leftArm.toTag());
        poseTag.put("RightArm", rightArm.toTag());
        poseTag.put("LeftLeg", leftLeg.toTag());
        poseTag.put("RightLeg", rightLeg.toTag());
        entityTag.put("Pose", poseTag);
    }

    private void applyScale(CompoundTag entityTag, SpecialDataApplyContext context) {
        String raw = context.special().armorStandScale.trim();
        if (raw.isBlank()) {
            this.removeScaleAttribute(entityTag);
            return;
        }

        Float parsed = ValidationUtil.parseFloat(
                raw,
                ItemEditorText.str("special.armor_stand.scale"),
                context.messages()
        );
        if (parsed == null) {
            return;
        }
        if (parsed < MIN_SCALE || parsed > MAX_SCALE) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.range",
                    ItemEditorText.str("special.armor_stand.scale"),
                    MIN_SCALE,
                    MAX_SCALE
            )));
            return;
        }
        this.setScaleAttribute(entityTag, parsed);
    }

    private void removeScaleAttribute(CompoundTag entityTag) {
        ListTag attributes = entityTag.getListOrEmpty("attributes");
        ListTag filtered = new ListTag();
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attributeTag = attributes.getCompoundOrEmpty(index);
            if (!"minecraft:scale".equals(attributeTag.getStringOr("id", ""))) {
                filtered.add(attributeTag.copy());
            }
        }

        if (filtered.isEmpty()) {
            entityTag.remove("attributes");
        } else {
            entityTag.put("attributes", filtered);
        }
    }

    private void setScaleAttribute(CompoundTag entityTag, float scale) {
        ListTag attributes = entityTag.getListOrEmpty("attributes");
        boolean updated = false;
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attributeTag = attributes.getCompoundOrEmpty(index);
            if (!"minecraft:scale".equals(attributeTag.getStringOr("id", ""))) {
                continue;
            }
            attributeTag.putDouble("base", scale);
            attributes.set(index, attributeTag);
            updated = true;
            break;
        }

        if (!updated) {
            CompoundTag scaleTag = new CompoundTag();
            scaleTag.putString("id", "minecraft:scale");
            scaleTag.putDouble("base", scale);
            attributes.add(scaleTag);
        }

        entityTag.put("attributes", attributes);
    }

    private PoseValue parsePosePart(
            SpecialDataApplyContext context,
            String fieldPrefix,
            ItemEditorState.RotationDraft draft,
            float defaultX,
            float defaultZ
    ) {
        Float x = this.parseAxis(context, fieldPrefix + ".x", draft.x, defaultX);
        Float y = this.parseAxis(context, fieldPrefix + ".y", draft.y, 0.0F);
        Float z = this.parseAxis(context, fieldPrefix + ".z", draft.z, defaultZ);
        if (x == null || y == null || z == null) {
            return null;
        }
        return new PoseValue(x, y, z);
    }

    private Float parseAxis(
            SpecialDataApplyContext context,
            String fieldKey,
            String raw,
            float fallback
    ) {
        String normalized = raw == null ? "" : raw.trim();
        if (normalized.isBlank()) {
            return fallback;
        }
        return ValidationUtil.parseFloat(normalized, ItemEditorText.str(fieldKey), context.messages());
    }

    private boolean sameArmorStandData(ItemEditorState.SpecialData current, ItemEditorState.SpecialData baseline) {
        return current.armorStandSmall == baseline.armorStandSmall
                && current.armorStandShowArms == baseline.armorStandShowArms
                && current.armorStandNoBasePlate == baseline.armorStandNoBasePlate
                && current.armorStandInvisible == baseline.armorStandInvisible
                && current.armorStandNoGravity == baseline.armorStandNoGravity
                && current.armorStandInvulnerable == baseline.armorStandInvulnerable
                && current.armorStandCustomNameVisible == baseline.armorStandCustomNameVisible
                && current.armorStandMarker == baseline.armorStandMarker
                && Objects.equals(current.armorStandCustomName, baseline.armorStandCustomName)
                && Objects.equals(current.armorStandDisabledSlots, baseline.armorStandDisabledSlots)
                && Objects.equals(current.armorStandScale, baseline.armorStandScale)
                && this.sameRotation(current.armorStandPose.head, baseline.armorStandPose.head)
                && this.sameRotation(current.armorStandPose.body, baseline.armorStandPose.body)
                && this.sameRotation(current.armorStandPose.leftArm, baseline.armorStandPose.leftArm)
                && this.sameRotation(current.armorStandPose.rightArm, baseline.armorStandPose.rightArm)
                && this.sameRotation(current.armorStandPose.leftLeg, baseline.armorStandPose.leftLeg)
                && this.sameRotation(current.armorStandPose.rightLeg, baseline.armorStandPose.rightLeg);
    }

    private boolean sameRotation(ItemEditorState.RotationDraft current, ItemEditorState.RotationDraft baseline) {
        return Objects.equals(current.x, baseline.x)
                && Objects.equals(current.y, baseline.y)
                && Objects.equals(current.z, baseline.z);
    }

    private boolean isArmorStandDataDefault(ItemEditorState.SpecialData special) {
        return !special.armorStandSmall
                && !special.armorStandShowArms
                && !special.armorStandNoBasePlate
                && !special.armorStandInvisible
                && !special.armorStandNoGravity
                && !special.armorStandInvulnerable
                && !special.armorStandCustomNameVisible
                && !special.armorStandMarker
                && special.armorStandCustomName.isBlank()
                && special.armorStandDisabledSlots.isBlank()
                && special.armorStandScale.isBlank()
                && this.isDefaultRotation(special.armorStandPose.head, "0", "0")
                && this.isDefaultRotation(special.armorStandPose.body, "0", "0")
                && this.isDefaultRotation(special.armorStandPose.leftArm, "-10", "-10")
                && this.isDefaultRotation(special.armorStandPose.rightArm, "-15", "10")
                && this.isDefaultRotation(special.armorStandPose.leftLeg, "-1", "-1")
                && this.isDefaultRotation(special.armorStandPose.rightLeg, "1", "1");
    }

    private boolean isDefaultRotation(ItemEditorState.RotationDraft rotation, String defaultX, String defaultZ) {
        return equalsFloat(rotation.x, defaultX)
                && equalsFloat(rotation.y, "0")
                && equalsFloat(rotation.z, defaultZ);
    }

    private static boolean equalsFloat(String value, String defaultValue) {
        float fallback = parseDefaultFloat(defaultValue, 0.0F);
        float parsed = parseDefaultFloat(value, fallback);
        return parsed == fallback;
    }

    private static float parseDefaultFloat(String value, float fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void setBooleanKey(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        } else {
            tag.remove(key);
        }
    }

    private record PoseValue(float x, float y, float z) {
        ListTag toTag() {
            ListTag list = new ListTag();
            list.add(FloatTag.valueOf(this.x));
            list.add(FloatTag.valueOf(this.y));
            list.add(FloatTag.valueOf(this.z));
            return list;
        }

        boolean isDefault(float defaultX, float defaultZ) {
            return this.x == defaultX && this.y == 0.0F && this.z == defaultZ;
        }
    }
}
