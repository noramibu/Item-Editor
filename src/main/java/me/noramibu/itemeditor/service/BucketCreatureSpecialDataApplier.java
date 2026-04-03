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
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BUCKET_ENTITY_DATA);
            return;
        }

        this.applyBucketEntityData(context);
    }

    private void applyBucketEntityData(SpecialDataApplyContext context) {
        boolean hasAnyEntry = context.special().bucketNoAi
                || context.special().bucketSilent
                || context.special().bucketNoGravity
                || context.special().bucketGlowing
                || context.special().bucketInvulnerable
                || !context.special().bucketPuffState.isBlank()
                || !context.special().bucketAxolotlVariant.isBlank()
                || !context.special().bucketSalmonSize.isBlank()
                || !context.special().bucketTropicalPattern.isBlank()
                || !context.special().bucketTropicalBaseColor.isBlank()
                || !context.special().bucketTropicalPatternColor.isBlank()
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

        this.applyAxolotlVariant(bucketTag, context.special().bucketAxolotlVariant, context.messages());
        this.applySalmonVariant(bucketTag, context.special().bucketSalmonSize, context.messages());
        this.applyTropicalVariant(
                bucketTag,
                context.special().bucketTropicalPattern,
                context.special().bucketTropicalBaseColor,
                context.special().bucketTropicalPatternColor,
                context.messages()
        );

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

    private static Axolotl.Variant parseAxolotlVariant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (Axolotl.Variant candidate : Axolotl.Variant.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(raw) || candidate.name().equalsIgnoreCase(raw)) {
                return candidate;
            }
        }
        return null;
    }

    private static Salmon.Variant parseSalmonVariant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (Salmon.Variant candidate : Salmon.Variant.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(raw) || candidate.name().equalsIgnoreCase(raw)) {
                return candidate;
            }
        }
        return null;
    }

    private void applyAxolotlVariant(CompoundTag bucketTag, String rawValue, java.util.List<ValidationMessage> messages) {
        String raw = rawValue == null ? "" : rawValue.trim();
        if (raw.isBlank()) {
            bucketTag.remove("Variant");
            return;
        }

        Axolotl.Variant variant = parseAxolotlVariant(raw);
        if (variant == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.axolotl_variant"),
                    raw
            )));
            return;
        }
        bucketTag.putInt("Variant", variant.getId());
    }

    private void applySalmonVariant(CompoundTag bucketTag, String rawValue, java.util.List<ValidationMessage> messages) {
        String raw = rawValue == null ? "" : rawValue.trim();
        if (raw.isBlank()) {
            bucketTag.remove("type");
            return;
        }

        Salmon.Variant variant = parseSalmonVariant(raw);
        if (variant == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.salmon_size"),
                    raw
            )));
            return;
        }
        bucketTag.putString("type", variant.getSerializedName());
    }

    private void applyTropicalVariant(
            CompoundTag bucketTag,
            String patternRawValue,
            String baseColorRawValue,
            String patternColorRawValue,
            java.util.List<ValidationMessage> messages
    ) {
        String patternRaw = patternRawValue == null ? "" : patternRawValue.trim();
        String baseRaw = baseColorRawValue == null ? "" : baseColorRawValue.trim();
        String patternColorRaw = patternColorRawValue == null ? "" : patternColorRawValue.trim();

        if (patternRaw.isBlank() && baseRaw.isBlank() && patternColorRaw.isBlank()) {
            bucketTag.remove("BucketVariantTag");
            return;
        }

        int existingVariant = bucketTag.contains("BucketVariantTag", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                ? bucketTag.getInt("BucketVariantTag")
                : 0;

        TropicalFish.Pattern pattern = patternRaw.isBlank()
                ? TropicalFish.getPattern(existingVariant)
                : this.parseTropicalPattern(patternRaw, messages);
        DyeColor baseColor = baseRaw.isBlank()
                ? TropicalFish.getBaseColor(existingVariant)
                : parseDyeColor(baseRaw);
        DyeColor patternColor = patternColorRaw.isBlank()
                ? TropicalFish.getPatternColor(existingVariant)
                : parseDyeColor(patternColorRaw);

        if (pattern == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.tropical_pattern"),
                    patternRaw
            )));
            return;
        }
        if (baseColor == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.tropical_base_color"),
                    baseRaw
            )));
            return;
        }
        if (patternColor == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.bucket.tropical_pattern_color"),
                    patternColorRaw
            )));
            return;
        }

        bucketTag.putInt("BucketVariantTag", packTropicalVariant(pattern, baseColor, patternColor));
    }

    private TropicalFish.Pattern parseTropicalPattern(String raw, java.util.List<ValidationMessage> messages) {
        for (TropicalFish.Pattern candidate : TropicalFish.Pattern.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(raw) || candidate.name().equalsIgnoreCase(raw)) {
                return candidate;
            }
        }
        return null;
    }

    private static int packTropicalVariant(TropicalFish.Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
        return (pattern.getPackedId() & 0xFFFF)
                | ((baseColor.getId() & 0xFF) << 16)
                | ((patternColor.getId() & 0xFF) << 24);
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
