package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

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

        this.applySerializedEnumComponent(
                context,
                context.special().bucketAxolotlVariant,
                DataComponents.AXOLOTL_VARIANT,
                "special.bucket.axolotl_variant",
                Axolotl.Variant.values(),
                Axolotl.Variant::getSerializedName
        );
        this.applySerializedEnumComponent(
                context,
                context.special().bucketSalmonSize,
                DataComponents.SALMON_SIZE,
                "special.bucket.salmon_size",
                Salmon.Variant.values(),
                Salmon.Variant::getSerializedName
        );
        this.applySerializedEnumComponent(
                context,
                context.special().bucketTropicalPattern,
                DataComponents.TROPICAL_FISH_PATTERN,
                "special.bucket.tropical_pattern",
                TropicalFish.Pattern.values(),
                TropicalFish.Pattern::getSerializedName
        );
        this.applyDyeColorComponent(
                context,
                context.special().bucketTropicalBaseColor,
                DataComponents.TROPICAL_FISH_BASE_COLOR,
                "special.bucket.tropical_base_color"
        );
        this.applyDyeColorComponent(
                context,
                context.special().bucketTropicalPatternColor,
                DataComponents.TROPICAL_FISH_PATTERN_COLOR,
                "special.bucket.tropical_pattern_color"
        );
        this.applyBucketEntityData(context);
    }

    private void applyDyeColorComponent(
            SpecialDataApplyContext context,
            String rawValue,
            DataComponentType<DyeColor> component,
            String labelKey
    ) {
        String raw = rawValue.trim();
        if (raw.isBlank()) {
            this.clearToPrototype(context.previewStack(), component);
            return;
        }

        DyeColor color = parseDyeColor(raw);
        if (color == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str(labelKey),
                    raw
            )));
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), component);
            return;
        }
        context.previewStack().set(component, color);
    }

    private void applyBucketEntityData(SpecialDataApplyContext context) {
        var special = context.special();
        boolean hasAnyEntry = special.bucketNoAi
                || special.bucketSilent
                || special.bucketNoGravity
                || special.bucketGlowing
                || special.bucketInvulnerable
                || !special.bucketPuffState.isBlank()
                || !special.bucketHealth.isBlank()
                || !special.bucketAge.isBlank()
                || special.bucketAgeLocked
                || !special.bucketHuntingCooldown.isBlank();
        if (!hasAnyEntry) {
            this.clearToPrototype(context.previewStack(), DataComponents.BUCKET_ENTITY_DATA);
            return;
        }

        CompoundTag bucketTag = new CompoundTag();
        CustomData originalData = context.originalStack().get(DataComponents.BUCKET_ENTITY_DATA);
        if (originalData != null) {
            bucketTag = originalData.copyTag();
        }

        NbtTagUtil.setBooleanKey(bucketTag, "NoAI", special.bucketNoAi);
        NbtTagUtil.setBooleanKey(bucketTag, "Silent", special.bucketSilent);
        NbtTagUtil.setBooleanKey(bucketTag, "NoGravity", special.bucketNoGravity);
        NbtTagUtil.setBooleanKey(bucketTag, "Glowing", special.bucketGlowing);
        NbtTagUtil.setBooleanKey(bucketTag, "Invulnerable", special.bucketInvulnerable);
        this.putOptionalIntTag(
                bucketTag,
                "PuffState",
                special.bucketPuffState,
                ItemEditorText.str("special.bucket.puffer_state"),
                0,
                2,
                context.messages()
        );
        this.putOptionalIntTag(
                bucketTag,
                "Age",
                special.bucketAge,
                ItemEditorText.str("special.bucket.age"),
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                context.messages()
        );
        NbtTagUtil.setBooleanKey(bucketTag, "AgeLocked", special.bucketAgeLocked);
        this.putHuntingCooldownTag(
                bucketTag,
                special.bucketHuntingCooldown,
                context.messages()
        );

        String healthRaw = special.bucketHealth.trim();
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

    private void putHuntingCooldownTag(
            CompoundTag tag,
            String raw,
            List<ValidationMessage> messages
    ) {
        String normalized = raw.trim();
        String fieldName = ItemEditorText.str("special.bucket.hunting_cooldown");
        if (normalized.isBlank()) {
            tag.remove("HuntingCooldown");
            return;
        }

        try {
            long value = Long.parseLong(normalized);
            if (value < 0L) {
                messages.add(ValidationMessage.error(ItemEditorText.str("validation.range", fieldName, 0L, Long.MAX_VALUE)));
                return;
            }
            tag.putLong("HuntingCooldown", value);
        } catch (NumberFormatException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str("validation.whole_number", fieldName)));
        }
    }

    private static DyeColor parseDyeColor(String raw) {
        try {
            return DyeColor.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private <T extends Enum<T>> void applySerializedEnumComponent(
            SpecialDataApplyContext context,
            String rawValue,
            DataComponentType<T> component,
            String labelKey,
            T[] values,
            Function<T, String> serializedName
    ) {
        String raw = rawValue.trim();
        if (raw.isBlank()) {
            this.clearToPrototype(context.previewStack(), component);
            return;
        }

        T parsed = parseSerializedEnum(raw, values, serializedName);
        if (parsed == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str(labelKey),
                    raw
            )));
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), component);
            return;
        }
        context.previewStack().set(component, parsed);
    }

    private static <T extends Enum<T>> T parseSerializedEnum(
            String raw,
            T[] values,
            Function<T, String> serializedName
    ) {
        for (T candidate : values) {
            if (serializedName.apply(candidate).equalsIgnoreCase(raw) || candidate.name().equalsIgnoreCase(raw)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean sameBucketCreatureData(SpecialDataApplyContext context) {
        var current = context.special();
        var baseline = context.baselineSpecial();
        return Objects.equals(current.bucketAxolotlVariant, baseline.bucketAxolotlVariant)
                && Objects.equals(current.bucketSalmonSize, baseline.bucketSalmonSize)
                && Objects.equals(current.bucketTropicalPattern, baseline.bucketTropicalPattern)
                && Objects.equals(current.bucketTropicalBaseColor, baseline.bucketTropicalBaseColor)
                && Objects.equals(current.bucketTropicalPatternColor, baseline.bucketTropicalPatternColor)
                && Objects.equals(current.bucketPuffState, baseline.bucketPuffState)
                && current.bucketNoAi == baseline.bucketNoAi
                && current.bucketSilent == baseline.bucketSilent
                && current.bucketNoGravity == baseline.bucketNoGravity
                && current.bucketGlowing == baseline.bucketGlowing
                && current.bucketInvulnerable == baseline.bucketInvulnerable
                && Objects.equals(current.bucketHealth, baseline.bucketHealth)
                && Objects.equals(current.bucketAge, baseline.bucketAge)
                && current.bucketAgeLocked == baseline.bucketAgeLocked
                && Objects.equals(current.bucketHuntingCooldown, baseline.bucketHuntingCooldown);
    }
}
