package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

public final class EntityVariantSpecialDataSection {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 620;
    private static final int ID_FIELD_WIDTH = 260;
    private static final int PICK_BUTTON_WIDTH = 110;

    private EntityVariantSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsEntityVariantData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.entity_variant.title"), Component.empty());
        section.id("entity-variants");
        FlowLayout card = UiFactory.subCard();
        fields(
                context,
                field -> card.child(textWithPicker(
                        context,
                        context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD),
                        field.text(),
                        field.value(),
                        field.setter(),
                        field.entries().get())));

        if (card.children().isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.entity_variant.none"), context.panelWidthHint()));
        }
        section.child(card);
        return section;
    }

    private record VariantField(String key, String value, Consumer<String> setter, Supplier<List<String>> entries)
            implements SpecialDataSearch.Field {}

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        fields(
                context,
                field -> result.addAll(SpecialDataSearch.targets(
                        context,
                        EditorCategory.SPECIAL_DATA,
                        "special.entity_variant.title",
                        "entity-variants",
                        () -> {},
                        field)));
        return result;
    }

    private static void fields(SpecialDataPanelContext context, Consumer<VariantField> fields) {
        ItemStack stack = context.originalStack();
        ItemEditorState.SpecialData special = context.special();
        String entityId = selectedEntityId(stack, special);

        declare(
                fields,
                show(
                        entityId,
                        "minecraft:axolotl",
                        stack.has(DataComponents.AXOLOTL_VARIANT),
                        special.bucketAxolotlVariant),
                "axolotl_variant",
                special.bucketAxolotlVariant,
                value -> special.bucketAxolotlVariant = value,
                () -> serializedValues(Axolotl.Variant.values()));
        declare(
                fields,
                show(entityId, "minecraft:cat", stack.has(DataComponents.CAT_VARIANT), special.entityCatVariant),
                "cat_variant",
                special.entityCatVariant,
                value -> special.entityCatVariant = value,
                () -> context.registryIds(Registries.CAT_VARIANT));
        declare(
                fields,
                show(entityId, "minecraft:cat", stack.has(DataComponents.CAT_COLLAR), special.entityCatCollar),
                "cat_collar",
                special.entityCatCollar,
                value -> special.entityCatCollar = value,
                EntityVariantSpecialDataSection::dyeColorValues);
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:chicken",
                        stack.has(DataComponents.CHICKEN_VARIANT) || stack.is(Items.EGG),
                        special.entityChickenVariant),
                "chicken_variant",
                special.entityChickenVariant,
                value -> special.entityChickenVariant = value,
                () -> context.registryIds(Registries.CHICKEN_VARIANT));
        declare(
                fields,
                show(entityId, "minecraft:cow", stack.has(DataComponents.COW_VARIANT), special.entityCowVariant),
                "cow_variant",
                special.entityCowVariant,
                value -> special.entityCowVariant = value,
                () -> context.registryIds(Registries.COW_VARIANT));
        declare(
                fields,
                show(entityId, "minecraft:fox", stack.has(DataComponents.FOX_VARIANT), special.entityFoxVariant),
                "fox_variant",
                special.entityFoxVariant,
                value -> special.entityFoxVariant = value,
                () -> serializedValues(Fox.Variant.values()));
        declare(
                fields,
                show(entityId, "minecraft:frog", stack.has(DataComponents.FROG_VARIANT), special.entityFrogVariant),
                "frog_variant",
                special.entityFrogVariant,
                value -> special.entityFrogVariant = value,
                () -> context.registryIds(Registries.FROG_VARIANT));
        declare(
                fields,
                show(entityId, "minecraft:horse", stack.has(DataComponents.HORSE_VARIANT), special.entityHorseVariant),
                "horse_variant",
                special.entityHorseVariant,
                value -> special.entityHorseVariant = value,
                () -> serializedValues(Variant.values()));
        declare(
                fields,
                showAny(
                        entityId,
                        List.of("minecraft:llama", "minecraft:trader_llama"),
                        stack.has(DataComponents.LLAMA_VARIANT),
                        special.entityLlamaVariant),
                "llama_variant",
                special.entityLlamaVariant,
                value -> special.entityLlamaVariant = value,
                () -> serializedValues(Llama.Variant.values()));
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:mooshroom",
                        stack.has(DataComponents.MOOSHROOM_VARIANT),
                        special.entityMooshroomVariant),
                "mooshroom_variant",
                special.entityMooshroomVariant,
                value -> special.entityMooshroomVariant = value,
                () -> serializedValues(MushroomCow.Variant.values()));
        declare(
                fields,
                stack.is(Items.PAINTING)
                        || stack.has(DataComponents.PAINTING_VARIANT)
                        || !special.paintingVariantId.isBlank(),
                "painting_variant",
                special.paintingVariantId,
                value -> special.paintingVariantId = value,
                () -> context.registryIds(Registries.PAINTING_VARIANT));
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:parrot",
                        stack.has(DataComponents.PARROT_VARIANT),
                        special.entityParrotVariant),
                "parrot_variant",
                special.entityParrotVariant,
                value -> special.entityParrotVariant = value,
                () -> serializedValues(Parrot.Variant.values()));
        declare(
                fields,
                show(entityId, "minecraft:pig", stack.has(DataComponents.PIG_VARIANT), special.entityPigVariant),
                "pig_variant",
                special.entityPigVariant,
                value -> special.entityPigVariant = value,
                () -> context.registryIds(Registries.PIG_VARIANT));
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:rabbit",
                        stack.has(DataComponents.RABBIT_VARIANT),
                        special.entityRabbitVariant),
                "rabbit_variant",
                special.entityRabbitVariant,
                value -> special.entityRabbitVariant = value,
                () -> serializedValues(Rabbit.Variant.values()));
        declare(
                fields,
                show(entityId, "minecraft:salmon", stack.has(DataComponents.SALMON_SIZE), special.bucketSalmonSize),
                "salmon_size",
                special.bucketSalmonSize,
                value -> special.bucketSalmonSize = value,
                () -> serializedValues(Salmon.Variant.values()));
        declare(
                fields,
                show(entityId, "minecraft:sheep", stack.has(DataComponents.SHEEP_COLOR), special.entitySheepColor),
                "sheep_color",
                special.entitySheepColor,
                value -> special.entitySheepColor = value,
                EntityVariantSpecialDataSection::dyeColorValues);
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:shulker",
                        stack.has(DataComponents.SHULKER_COLOR),
                        special.entityShulkerColor),
                "shulker_color",
                special.entityShulkerColor,
                value -> special.entityShulkerColor = value,
                EntityVariantSpecialDataSection::dyeColorValues);
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:tropical_fish",
                        stack.has(DataComponents.TROPICAL_FISH_PATTERN),
                        special.bucketTropicalPattern),
                "tropical_pattern",
                special.bucketTropicalPattern,
                value -> special.bucketTropicalPattern = value,
                () -> serializedValues(TropicalFish.Pattern.values()));
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:tropical_fish",
                        stack.has(DataComponents.TROPICAL_FISH_BASE_COLOR),
                        special.bucketTropicalBaseColor),
                "tropical_base_color",
                special.bucketTropicalBaseColor,
                value -> special.bucketTropicalBaseColor = value,
                EntityVariantSpecialDataSection::dyeColorValues);
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:tropical_fish",
                        stack.has(DataComponents.TROPICAL_FISH_PATTERN_COLOR),
                        special.bucketTropicalPatternColor),
                "tropical_pattern_color",
                special.bucketTropicalPatternColor,
                value -> special.bucketTropicalPatternColor = value,
                EntityVariantSpecialDataSection::dyeColorValues);
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:villager",
                        stack.has(DataComponents.VILLAGER_VARIANT),
                        special.entityVillagerVariant),
                "villager_variant",
                special.entityVillagerVariant,
                value -> special.entityVillagerVariant = value,
                () -> context.registryIds(Registries.VILLAGER_TYPE));
        declare(
                fields,
                show(entityId, "minecraft:wolf", stack.has(DataComponents.WOLF_VARIANT), special.entityWolfVariant),
                "wolf_variant",
                special.entityWolfVariant,
                value -> special.entityWolfVariant = value,
                () -> context.registryIds(Registries.WOLF_VARIANT));
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:wolf",
                        stack.has(DataComponents.WOLF_SOUND_VARIANT),
                        special.entityWolfSoundVariant),
                "wolf_sound_variant",
                special.entityWolfSoundVariant,
                value -> special.entityWolfSoundVariant = value,
                () -> context.registryIds(Registries.WOLF_SOUND_VARIANT));
        declare(
                fields,
                show(entityId, "minecraft:wolf", stack.has(DataComponents.WOLF_COLLAR), special.entityWolfCollar),
                "wolf_collar",
                special.entityWolfCollar,
                value -> special.entityWolfCollar = value,
                EntityVariantSpecialDataSection::dyeColorValues);
        declare(
                fields,
                show(
                        entityId,
                        "minecraft:zombie_nautilus",
                        stack.has(DataComponents.ZOMBIE_NAUTILUS_VARIANT),
                        special.entityZombieNautilusVariant),
                "zombie_nautilus_variant",
                special.entityZombieNautilusVariant,
                value -> special.entityZombieNautilusVariant = value,
                () -> context.registryIds(Registries.ZOMBIE_NAUTILUS_VARIANT));
    }

    private static void declare(
            Consumer<VariantField> fields,
            boolean visible,
            String key,
            String value,
            Consumer<String> setter,
            Supplier<List<String>> entries) {
        if (visible) {
            fields.accept(new VariantField("special.entity_variant." + key, value, setter, entries));
        }
    }

    private static FlowLayout textWithPicker(
            SpecialDataPanelContext context,
            boolean compactLayout,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries) {
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(
                UiFactory.textBox(value, text -> context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text))))
                        .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(ID_FIELD_WIDTH)));
        ButtonComponent pick = UiFactory.button(
                ItemEditorText.tr("common.pick"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.openSearchablePicker(
                        label.getString(),
                        "",
                        entries,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))));
        pick.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(PICK_BUTTON_WIDTH));
        row.child(pick);
        return UiFactory.field(label, Component.empty(), row);
    }

    private static boolean show(
            String selectedEntityId, String entityId, boolean componentPresent, String currentValue) {
        return Objects.equals(selectedEntityId, entityId)
                || componentPresent
                || (currentValue != null && !currentValue.isBlank());
    }

    private static boolean showAny(
            String selectedEntityId, List<String> entityIds, boolean componentPresent, String currentValue) {
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

    private static <T extends Enum<T> & StringRepresentable> List<String> serializedValues(T[] values) {
        return Arrays.stream(values).map(StringRepresentable::getSerializedName).toList();
    }

    private static List<String> dyeColorValues() {
        return Arrays.stream(DyeColor.values())
                .map(color -> color.name().toLowerCase(Locale.ROOT))
                .toList();
    }
}
