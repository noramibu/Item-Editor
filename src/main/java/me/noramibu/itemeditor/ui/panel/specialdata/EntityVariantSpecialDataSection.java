package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public final class EntityVariantSpecialDataSection {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 620;
    private static final int ID_FIELD_WIDTH = 260;
    private static final int PICK_BUTTON_WIDTH = 110;

    private EntityVariantSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsEntityVariantData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemStack stack = context.originalStack();
        ItemEditorState.SpecialData special = context.special();
        String entityId = selectedEntityId(stack, special);
        boolean compactLayout = LayoutModeUtil.isCompactPanel(
                context.guiScale(),
                context.panelWidthHint(),
                COMPACT_LAYOUT_WIDTH_THRESHOLD
        );

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.entity_variant.title"), Component.empty());
        FlowLayout card = UiFactory.subCard();

        addIf(card, show(entityId, "minecraft:axolotl", stack.has(DataComponents.AXOLOTL_VARIANT), special.bucketAxolotlVariant), () ->
                variantField(
                        context,
                        compactLayout,
                        ItemEditorText.tr("special.entity_variant.axolotl_variant"),
                        special.bucketAxolotlVariant,
                        value -> special.bucketAxolotlVariant = value,
                        serializedValues(Axolotl.Variant.values())
                ));
        addIf(card, show(entityId, "minecraft:cat", stack.has(DataComponents.CAT_VARIANT), special.entityCatVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.cat_variant"), special.entityCatVariant, value -> special.entityCatVariant = value, registryIds(context, Registries.CAT_VARIANT)));
        addIf(card, show(entityId, "minecraft:cat", stack.has(DataComponents.CAT_COLLAR), special.entityCatCollar), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.cat_collar"), special.entityCatCollar, value -> special.entityCatCollar = value, dyeColorValues()));
        addIf(card, show(entityId, "minecraft:chicken", stack.has(DataComponents.CHICKEN_VARIANT) || stack.is(Items.EGG), special.entityChickenVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.chicken_variant"), special.entityChickenVariant, value -> special.entityChickenVariant = value, registryIds(context, Registries.CHICKEN_VARIANT)));
        addIf(card, show(entityId, "minecraft:cow", stack.has(DataComponents.COW_VARIANT), special.entityCowVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.cow_variant"), special.entityCowVariant, value -> special.entityCowVariant = value, registryIds(context, Registries.COW_VARIANT)));
        addIf(card, show(entityId, "minecraft:fox", stack.has(DataComponents.FOX_VARIANT), special.entityFoxVariant), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.fox_variant"), special.entityFoxVariant, value -> special.entityFoxVariant = value, serializedValues(Fox.Variant.values())));
        addIf(card, show(entityId, "minecraft:frog", stack.has(DataComponents.FROG_VARIANT), special.entityFrogVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.frog_variant"), special.entityFrogVariant, value -> special.entityFrogVariant = value, registryIds(context, Registries.FROG_VARIANT)));
        addIf(card, show(entityId, "minecraft:horse", stack.has(DataComponents.HORSE_VARIANT), special.entityHorseVariant), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.horse_variant"), special.entityHorseVariant, value -> special.entityHorseVariant = value, serializedValues(Variant.values())));
        addIf(card, showAny(entityId, List.of("minecraft:llama", "minecraft:trader_llama"), stack.has(DataComponents.LLAMA_VARIANT), special.entityLlamaVariant), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.llama_variant"), special.entityLlamaVariant, value -> special.entityLlamaVariant = value, serializedValues(Llama.Variant.values())));
        addIf(card, show(entityId, "minecraft:mooshroom", stack.has(DataComponents.MOOSHROOM_VARIANT), special.entityMooshroomVariant), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.mooshroom_variant"), special.entityMooshroomVariant, value -> special.entityMooshroomVariant = value, serializedValues(MushroomCow.Variant.values())));
        addIf(card, stack.is(Items.PAINTING) || stack.has(DataComponents.PAINTING_VARIANT) || !special.paintingVariantId.isBlank(), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.painting_variant"), special.paintingVariantId, value -> special.paintingVariantId = value, registryIds(context, Registries.PAINTING_VARIANT)));
        addIf(card, show(entityId, "minecraft:parrot", stack.has(DataComponents.PARROT_VARIANT), special.entityParrotVariant), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.parrot_variant"), special.entityParrotVariant, value -> special.entityParrotVariant = value, serializedValues(Parrot.Variant.values())));
        addIf(card, show(entityId, "minecraft:pig", stack.has(DataComponents.PIG_VARIANT), special.entityPigVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.pig_variant"), special.entityPigVariant, value -> special.entityPigVariant = value, registryIds(context, Registries.PIG_VARIANT)));
        addIf(card, show(entityId, "minecraft:rabbit", stack.has(DataComponents.RABBIT_VARIANT), special.entityRabbitVariant), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.rabbit_variant"), special.entityRabbitVariant, value -> special.entityRabbitVariant = value, serializedValues(Rabbit.Variant.values())));
        addIf(card, show(entityId, "minecraft:salmon", stack.has(DataComponents.SALMON_SIZE), special.bucketSalmonSize), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.salmon_size"), special.bucketSalmonSize, value -> special.bucketSalmonSize = value, serializedValues(Salmon.Variant.values())));
        addIf(card, show(entityId, "minecraft:sheep", stack.has(DataComponents.SHEEP_COLOR), special.entitySheepColor), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.sheep_color"), special.entitySheepColor, value -> special.entitySheepColor = value, dyeColorValues()));
        addIf(card, show(entityId, "minecraft:shulker", stack.has(DataComponents.SHULKER_COLOR), special.entityShulkerColor), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.shulker_color"), special.entityShulkerColor, value -> special.entityShulkerColor = value, dyeColorValues()));
        addIf(card, show(entityId, "minecraft:tropical_fish", stack.has(DataComponents.TROPICAL_FISH_PATTERN), special.bucketTropicalPattern), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.tropical_pattern"), special.bucketTropicalPattern, value -> special.bucketTropicalPattern = value, serializedValues(TropicalFish.Pattern.values())));
        addIf(card, show(entityId, "minecraft:tropical_fish", stack.has(DataComponents.TROPICAL_FISH_BASE_COLOR), special.bucketTropicalBaseColor), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.tropical_base_color"), special.bucketTropicalBaseColor, value -> special.bucketTropicalBaseColor = value, dyeColorValues()));
        addIf(card, show(entityId, "minecraft:tropical_fish", stack.has(DataComponents.TROPICAL_FISH_PATTERN_COLOR), special.bucketTropicalPatternColor), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.tropical_pattern_color"), special.bucketTropicalPatternColor, value -> special.bucketTropicalPatternColor = value, dyeColorValues()));
        addIf(card, show(entityId, "minecraft:villager", stack.has(DataComponents.VILLAGER_VARIANT), special.entityVillagerVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.villager_variant"), special.entityVillagerVariant, value -> special.entityVillagerVariant = value, registryIds(context, Registries.VILLAGER_TYPE)));
        addIf(card, show(entityId, "minecraft:wolf", stack.has(DataComponents.WOLF_VARIANT), special.entityWolfVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.wolf_variant"), special.entityWolfVariant, value -> special.entityWolfVariant = value, registryIds(context, Registries.WOLF_VARIANT)));
        addIf(card, show(entityId, "minecraft:wolf", stack.has(DataComponents.WOLF_SOUND_VARIANT), special.entityWolfSoundVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.wolf_sound_variant"), special.entityWolfSoundVariant, value -> special.entityWolfSoundVariant = value, registryIds(context, Registries.WOLF_SOUND_VARIANT)));
        addIf(card, show(entityId, "minecraft:wolf", stack.has(DataComponents.WOLF_COLLAR), special.entityWolfCollar), () ->
                variantField(context, compactLayout, ItemEditorText.tr("special.entity_variant.wolf_collar"), special.entityWolfCollar, value -> special.entityWolfCollar = value, dyeColorValues()));
        addIf(card, show(entityId, "minecraft:zombie_nautilus", stack.has(DataComponents.ZOMBIE_NAUTILUS_VARIANT), special.entityZombieNautilusVariant), () ->
                idField(context, compactLayout, ItemEditorText.tr("special.entity_variant.zombie_nautilus_variant"), special.entityZombieNautilusVariant, value -> special.entityZombieNautilusVariant = value, registryIds(context, Registries.ZOMBIE_NAUTILUS_VARIANT)));

        if (card.children().isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.entity_variant.none"), context.panelWidthHint()));
        }
        section.child(card);
        return section;
    }

    private static FlowLayout idField(
            SpecialDataPanelContext context,
            boolean compactLayout,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries
    ) {
        return textWithPicker(context, compactLayout, label, value, setter, entries);
    }

    private static FlowLayout variantField(
            SpecialDataPanelContext context,
            boolean compactLayout,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries
    ) {
        return textWithPicker(context, compactLayout, label, value, setter, entries);
    }

    private static FlowLayout textWithPicker(
            SpecialDataPanelContext context,
            boolean compactLayout,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries
    ) {
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.textBox(value, text -> context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text))))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(ID_FIELD_WIDTH)));
        ButtonComponent pick = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.openSearchablePicker(
                        label.getString(),
                        "",
                        entries,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        );
        pick.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(PICK_BUTTON_WIDTH));
        row.child(pick);
        return UiFactory.field(label, Component.empty(), row);
    }

    private static void addIf(FlowLayout card, boolean condition, VariantControlBuilder builder) {
        if (condition) {
            card.child(builder.build());
        }
    }

    private static boolean show(String selectedEntityId, String entityId, boolean componentPresent, String currentValue) {
        return Objects.equals(selectedEntityId, entityId)
                || componentPresent
                || (currentValue != null && !currentValue.isBlank());
    }

    private static boolean showAny(
            String selectedEntityId,
            List<String> entityIds,
            boolean componentPresent,
            String currentValue
    ) {
        return entityIds.contains(selectedEntityId)
                || componentPresent
                || (currentValue != null && !currentValue.isBlank());
    }

    private static String selectedEntityId(ItemStack stack, ItemEditorState.SpecialData special) {
        if (special.spawnEggEntity.entityId != null && !special.spawnEggEntity.entityId.isBlank()) {
            return IdFieldNormalizer.normalize(special.spawnEggEntity.entityId);
        }
        if (stack.getItem() instanceof SpawnEggItem spawnEggItem) {
            EntityType<?> type = spawnEggItem.getType(stack);
            if (type == null) {
                return "";
            }
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            return id.toString();
        }
        return "";
    }

    private static <T extends @NotNull Object> List<String> registryIds(
            SpecialDataPanelContext context,
            ResourceKey<? extends Registry<T>> registryKey
    ) {
        Registry<T> registry = context.screen().session().registryAccess().lookupOrThrow(registryKey);
        return RegistryUtil.ids(registry);
    }

    private static <T extends Enum<T> & StringRepresentable> List<String> serializedValues(T[] values) {
        return Arrays.stream(values)
                .map(StringRepresentable::getSerializedName)
                .toList();
    }

    private static List<String> dyeColorValues() {
        return Arrays.stream(DyeColor.values())
                .map(color -> color.name().toLowerCase(Locale.ROOT))
                .toList();
    }

    private interface VariantControlBuilder {
        UIComponent build();
    }
}
