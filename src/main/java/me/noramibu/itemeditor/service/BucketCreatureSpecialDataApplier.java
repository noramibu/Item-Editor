package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;
import java.util.Objects;

final class BucketCreatureSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (this.sameBucketCreatureData(context)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.AXOLOTL_VARIANT);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.SALMON_SIZE);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TROPICAL_FISH_PATTERN);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TROPICAL_FISH_BASE_COLOR);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TROPICAL_FISH_PATTERN_COLOR);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BUCKET_ENTITY_DATA);
            return;
        }

        this.applyAxolotlVariant(context);
        this.applySalmonSize(context);
        this.applyTropicalPattern(context);
        this.applyTropicalBaseColor(context);
        this.applyTropicalPatternColor(context);
        this.applyBucketEntityData(context);
    }

    private void applyAxolotlVariant(SpecialDataApplyContext context) {
        String raw = context.special().bucketAxolotlVariant.trim();
        if (raw.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.AXOLOTL_VARIANT);
            return;
        }

        Axolotl.Variant variant = null;
        for (Axolotl.Variant candidate : Axolotl.Variant.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(raw) || candidate.name().equalsIgnoreCase(raw)) {
                variant = candidate;
                break;
            }
        }

        if (variant == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.axolotl_variant"),
                    raw
            )));
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.AXOLOTL_VARIANT);
            return;
        }
        context.previewStack().set(DataComponents.AXOLOTL_VARIANT, variant);
    }

    private void applySalmonSize(SpecialDataApplyContext context) {
        String raw = context.special().bucketSalmonSize.trim();
        if (raw.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.SALMON_SIZE);
            return;
        }

        Salmon.Variant variant = null;
        for (Salmon.Variant candidate : Salmon.Variant.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(raw) || candidate.name().equalsIgnoreCase(raw)) {
                variant = candidate;
                break;
            }
        }

        if (variant == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.salmon_size"),
                    raw
            )));
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.SALMON_SIZE);
            return;
        }
        context.previewStack().set(DataComponents.SALMON_SIZE, variant);
    }

    private void applyTropicalPattern(SpecialDataApplyContext context) {
        String raw = context.special().bucketTropicalPattern.trim();
        if (raw.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.TROPICAL_FISH_PATTERN);
            return;
        }

        TropicalFish.Pattern pattern = null;
        for (TropicalFish.Pattern candidate : TropicalFish.Pattern.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(raw) || candidate.name().equalsIgnoreCase(raw)) {
                pattern = candidate;
                break;
            }
        }

        if (pattern == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.tropical_pattern"),
                    raw
            )));
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TROPICAL_FISH_PATTERN);
            return;
        }
        context.previewStack().set(DataComponents.TROPICAL_FISH_PATTERN, pattern);
    }

    private void applyTropicalBaseColor(SpecialDataApplyContext context) {
        String raw = context.special().bucketTropicalBaseColor.trim();
        if (raw.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.TROPICAL_FISH_BASE_COLOR);
            return;
        }

        DyeColor color = parseDyeColor(raw);
        if (color == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.tropical_base_color"),
                    raw
            )));
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TROPICAL_FISH_BASE_COLOR);
            return;
        }
        context.previewStack().set(DataComponents.TROPICAL_FISH_BASE_COLOR, color);
    }

    private void applyTropicalPatternColor(SpecialDataApplyContext context) {
        String raw = context.special().bucketTropicalPatternColor.trim();
        if (raw.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.TROPICAL_FISH_PATTERN_COLOR);
            return;
        }

        DyeColor color = parseDyeColor(raw);
        if (color == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.tropical_pattern_color"),
                    raw
            )));
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TROPICAL_FISH_PATTERN_COLOR);
            return;
        }
        context.previewStack().set(DataComponents.TROPICAL_FISH_PATTERN_COLOR, color);
    }

    private void applyBucketEntityData(SpecialDataApplyContext context) {
        boolean hasAnyEntry = context.special().bucketNoAi
                || context.special().bucketSilent
                || context.special().bucketNoGravity
                || context.special().bucketGlowing
                || context.special().bucketInvulnerable
                || !context.special().bucketPuffState.isBlank()
                || !context.special().bucketHealth.isBlank();
        if (!hasAnyEntry) {
            this.clearToPrototype(context.previewStack(), DataComponents.BUCKET_ENTITY_DATA);
            return;
        }

        CompoundTag bucketTag = new CompoundTag();
        CustomData originalData = context.originalStack().get(DataComponents.BUCKET_ENTITY_DATA);
        if (originalData != null) {
            bucketTag = originalData.copyTag();
        }

        setBooleanKey(bucketTag, "NoAI", context.special().bucketNoAi);
        setBooleanKey(bucketTag, "Silent", context.special().bucketSilent);
        setBooleanKey(bucketTag, "NoGravity", context.special().bucketNoGravity);
        setBooleanKey(bucketTag, "Glowing", context.special().bucketGlowing);
        setBooleanKey(bucketTag, "Invulnerable", context.special().bucketInvulnerable);
        this.putOptionalIntTag(
                bucketTag,
                "PuffState",
                context.special().bucketPuffState,
                ItemEditorText.str("special.bucket.puffer_state"),
                0,
                2,
                context.messages()
        );

        String healthRaw = context.special().bucketHealth.trim();
        if (healthRaw.isBlank()) {
            bucketTag.remove("Health");
        } else {
            Float health = ValidationUtil.parseFloat(healthRaw, ItemEditorText.str("special.bucket.health"), context.messages());
            if (health != null) {
                if (health <= 0.0F || health > 2048.0F) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str("validation.range", ItemEditorText.str("special.bucket.health"), "0.01", "2048.0")));
                } else {
                    bucketTag.putFloat("Health", health);
                }
            }
        }

        if (bucketTag.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BUCKET_ENTITY_DATA);
            return;
        }
        context.previewStack().set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(bucketTag));
    }

    private static void setBooleanKey(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        } else {
            tag.remove(key);
        }
    }

    private static DyeColor parseDyeColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return DyeColor.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean sameBucketCreatureData(SpecialDataApplyContext context) {
        return Objects.equals(context.special().bucketAxolotlVariant, context.baselineSpecial().bucketAxolotlVariant)
                && Objects.equals(context.special().bucketSalmonSize, context.baselineSpecial().bucketSalmonSize)
                && Objects.equals(context.special().bucketTropicalPattern, context.baselineSpecial().bucketTropicalPattern)
                && Objects.equals(context.special().bucketTropicalBaseColor, context.baselineSpecial().bucketTropicalBaseColor)
                && Objects.equals(context.special().bucketTropicalPatternColor, context.baselineSpecial().bucketTropicalPatternColor)
                && Objects.equals(context.special().bucketPuffState, context.baselineSpecial().bucketPuffState)
                && context.special().bucketNoAi == context.baselineSpecial().bucketNoAi
                && context.special().bucketSilent == context.baselineSpecial().bucketSilent
                && context.special().bucketNoGravity == context.baselineSpecial().bucketNoGravity
                && context.special().bucketGlowing == context.baselineSpecial().bucketGlowing
                && context.special().bucketInvulnerable == context.baselineSpecial().bucketInvulnerable
                && Objects.equals(context.special().bucketHealth, context.baselineSpecial().bucketHealth);
    }
}
