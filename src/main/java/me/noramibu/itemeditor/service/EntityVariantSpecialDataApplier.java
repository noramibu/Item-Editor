package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.EitherHolder;

import java.util.Locale;
import java.util.Objects;

final class EntityVariantSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        ItemEditorState.SpecialData current = context.special();
        ItemEditorState.SpecialData baseline = context.baselineSpecial();

        this.applyDyeComponent(context, DataComponents.CAT_COLLAR, current.entityCatCollar, baseline.entityCatCollar, "special.entity_variant.cat_collar");
        this.applyHolderComponent(context, DataComponents.CAT_VARIANT, Registries.CAT_VARIANT, current.entityCatVariant, baseline.entityCatVariant, "special.entity_variant.cat_variant");
        this.applyEitherHolderComponent(context, DataComponents.CHICKEN_VARIANT, Registries.CHICKEN_VARIANT, current.entityChickenVariant, baseline.entityChickenVariant, "special.entity_variant.chicken_variant");
        this.applyHolderComponent(context, DataComponents.COW_VARIANT, Registries.COW_VARIANT, current.entityCowVariant, baseline.entityCowVariant, "special.entity_variant.cow_variant");
        this.applyEnumComponent(context, DataComponents.FOX_VARIANT, Fox.Variant.values(), current.entityFoxVariant, baseline.entityFoxVariant, "special.entity_variant.fox_variant");
        this.applyHolderComponent(context, DataComponents.FROG_VARIANT, Registries.FROG_VARIANT, current.entityFrogVariant, baseline.entityFrogVariant, "special.entity_variant.frog_variant");
        this.applyEnumComponent(context, DataComponents.HORSE_VARIANT, Variant.values(), current.entityHorseVariant, baseline.entityHorseVariant, "special.entity_variant.horse_variant");
        this.applyEnumComponent(context, DataComponents.LLAMA_VARIANT, Llama.Variant.values(), current.entityLlamaVariant, baseline.entityLlamaVariant, "special.entity_variant.llama_variant");
        this.applyEnumComponent(context, DataComponents.MOOSHROOM_VARIANT, MushroomCow.Variant.values(), current.entityMooshroomVariant, baseline.entityMooshroomVariant, "special.entity_variant.mooshroom_variant");
        this.applyEnumComponent(context, DataComponents.PARROT_VARIANT, Parrot.Variant.values(), current.entityParrotVariant, baseline.entityParrotVariant, "special.entity_variant.parrot_variant");
        this.applyHolderComponent(context, DataComponents.PIG_VARIANT, Registries.PIG_VARIANT, current.entityPigVariant, baseline.entityPigVariant, "special.entity_variant.pig_variant");
        this.applyEnumComponent(context, DataComponents.RABBIT_VARIANT, Rabbit.Variant.values(), current.entityRabbitVariant, baseline.entityRabbitVariant, "special.entity_variant.rabbit_variant");
        this.applyDyeComponent(context, DataComponents.SHEEP_COLOR, current.entitySheepColor, baseline.entitySheepColor, "special.entity_variant.sheep_color");
        this.applyDyeComponent(context, DataComponents.SHULKER_COLOR, current.entityShulkerColor, baseline.entityShulkerColor, "special.entity_variant.shulker_color");
        this.applyHolderComponent(context, DataComponents.VILLAGER_VARIANT, Registries.VILLAGER_TYPE, current.entityVillagerVariant, baseline.entityVillagerVariant, "special.entity_variant.villager_variant");
        this.applyDyeComponent(context, DataComponents.WOLF_COLLAR, current.entityWolfCollar, baseline.entityWolfCollar, "special.entity_variant.wolf_collar");
        this.applyHolderComponent(context, DataComponents.WOLF_SOUND_VARIANT, Registries.WOLF_SOUND_VARIANT, current.entityWolfSoundVariant, baseline.entityWolfSoundVariant, "special.entity_variant.wolf_sound_variant");
        this.applyHolderComponent(context, DataComponents.WOLF_VARIANT, Registries.WOLF_VARIANT, current.entityWolfVariant, baseline.entityWolfVariant, "special.entity_variant.wolf_variant");
        this.applyEitherHolderComponent(context, DataComponents.ZOMBIE_NAUTILUS_VARIANT, Registries.ZOMBIE_NAUTILUS_VARIANT, current.entityZombieNautilusVariant, baseline.entityZombieNautilusVariant, "special.entity_variant.zombie_nautilus_variant");
    }

    private <T> void applyHolderComponent(
            SpecialDataApplyContext context,
            DataComponentType<Holder<T>> componentType,
            ResourceKey<Registry<T>> registryKey,
            String current,
            String baseline,
            String labelKey
    ) {
        if (Objects.equals(current, baseline)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), componentType);
            return;
        }
        if (current == null || current.isBlank()) {
            this.clearToPrototype(context.previewStack(), componentType);
            return;
        }

        Registry<T> registry = context.registryAccess().lookupOrThrow(registryKey);
        Holder<T> holder = RegistryUtil.resolveHolder(registry, current);
        if (holder == null) {
            this.reportMissing(labelKey, current, context);
            return;
        }
        context.previewStack().set(componentType, holder);
    }

    private <T> void applyEitherHolderComponent(
            SpecialDataApplyContext context,
            DataComponentType<EitherHolder<T>> componentType,
            ResourceKey<Registry<T>> registryKey,
            String current,
            String baseline,
            String labelKey
    ) {
        if (Objects.equals(current, baseline)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), componentType);
            return;
        }
        if (current == null || current.isBlank()) {
            this.clearToPrototype(context.previewStack(), componentType);
            return;
        }

        Registry<T> registry = context.registryAccess().lookupOrThrow(registryKey);
        Holder<T> holder = RegistryUtil.resolveHolder(registry, current);
        if (holder == null) {
            this.reportMissing(labelKey, current, context);
            return;
        }
        context.previewStack().set(componentType, new EitherHolder<>(holder));
    }

    private <T extends Enum<T> & StringRepresentable> void applyEnumComponent(
            SpecialDataApplyContext context,
            DataComponentType<T> componentType,
            T[] values,
            String current,
            String baseline,
            String labelKey
    ) {
        if (Objects.equals(current, baseline)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), componentType);
            return;
        }
        if (current == null || current.isBlank()) {
            this.clearToPrototype(context.previewStack(), componentType);
            return;
        }

        T parsed = parseStringRepresentable(values, current);
        if (parsed == null) {
            this.reportMissing(labelKey, current, context);
            return;
        }
        context.previewStack().set(componentType, parsed);
    }

    private void applyDyeComponent(
            SpecialDataApplyContext context,
            DataComponentType<DyeColor> componentType,
            String current,
            String baseline,
            String labelKey
    ) {
        if (Objects.equals(current, baseline)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), componentType);
            return;
        }
        if (current == null || current.isBlank()) {
            this.clearToPrototype(context.previewStack(), componentType);
            return;
        }

        DyeColor color = parseDyeColor(current);
        if (color == null) {
            this.reportMissing(labelKey, current, context);
            return;
        }
        context.previewStack().set(componentType, color);
    }

    private static <T extends Enum<T> & StringRepresentable> T parseStringRepresentable(T[] values, String raw) {
        String normalized = raw.trim();
        for (T value : values) {
            if (value.getSerializedName().equalsIgnoreCase(normalized)
                    || value.name().equalsIgnoreCase(normalized)) {
                return value;
            }
        }
        return null;
    }

    private static DyeColor parseDyeColor(String raw) {
        try {
            return DyeColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void reportMissing(String labelKey, String raw, SpecialDataApplyContext context) {
        context.messages().add(ValidationMessage.error(ItemEditorText.str(
                "validation.registry_missing",
                ItemEditorText.str(labelKey),
                raw
        )));
    }
}
