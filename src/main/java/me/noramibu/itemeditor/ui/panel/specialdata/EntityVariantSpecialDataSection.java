package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorFieldReset;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.CompactFieldLayout;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import net.minecraft.core.component.DataComponentType;
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

    static final List<VariantDefinition> VARIANTS = List.of(
            new VariantDefinition(
                    "axolotl_variant",
                    DataComponents.AXOLOTL_VARIANT,
                    context -> serializedValues(Axolotl.Variant.values())),
            new VariantDefinition(
                    "cat_variant", DataComponents.CAT_VARIANT, context -> context.registryIds(Registries.CAT_VARIANT)),
            new VariantDefinition("cat_collar", DataComponents.CAT_COLLAR, context -> dyeColorValues()),
            new VariantDefinition(
                    "chicken_variant",
                    DataComponents.CHICKEN_VARIANT,
                    context -> context.registryIds(Registries.CHICKEN_VARIANT)),
            new VariantDefinition(
                    "cow_variant", DataComponents.COW_VARIANT, context -> context.registryIds(Registries.COW_VARIANT)),
            new VariantDefinition(
                    "fox_variant", DataComponents.FOX_VARIANT, context -> serializedValues(Fox.Variant.values())),
            new VariantDefinition(
                    "frog_variant",
                    DataComponents.FROG_VARIANT,
                    context -> context.registryIds(Registries.FROG_VARIANT)),
            new VariantDefinition(
                    "horse_variant", DataComponents.HORSE_VARIANT, context -> serializedValues(Variant.values())),
            new VariantDefinition(
                    "llama_variant", DataComponents.LLAMA_VARIANT, context -> serializedValues(Llama.Variant.values())),
            new VariantDefinition(
                    "mooshroom_variant",
                    DataComponents.MOOSHROOM_VARIANT,
                    context -> serializedValues(MushroomCow.Variant.values())),
            new VariantDefinition(
                    "painting_variant",
                    DataComponents.PAINTING_VARIANT,
                    context -> context.registryIds(Registries.PAINTING_VARIANT)),
            new VariantDefinition(
                    "parrot_variant",
                    DataComponents.PARROT_VARIANT,
                    context -> serializedValues(Parrot.Variant.values())),
            new VariantDefinition(
                    "pig_variant", DataComponents.PIG_VARIANT, context -> context.registryIds(Registries.PIG_VARIANT)),
            new VariantDefinition(
                    "rabbit_variant",
                    DataComponents.RABBIT_VARIANT,
                    context -> serializedValues(Rabbit.Variant.values())),
            new VariantDefinition(
                    "salmon_size", DataComponents.SALMON_SIZE, context -> serializedValues(Salmon.Variant.values())),
            new VariantDefinition("sheep_color", DataComponents.SHEEP_COLOR, context -> dyeColorValues()),
            new VariantDefinition("shulker_color", DataComponents.SHULKER_COLOR, context -> dyeColorValues()),
            new VariantDefinition(
                    "tropical_pattern",
                    DataComponents.TROPICAL_FISH_PATTERN,
                    context -> serializedValues(TropicalFish.Pattern.values())),
            new VariantDefinition(
                    "tropical_base_color", DataComponents.TROPICAL_FISH_BASE_COLOR, context -> dyeColorValues()),
            new VariantDefinition(
                    "tropical_pattern_color", DataComponents.TROPICAL_FISH_PATTERN_COLOR, context -> dyeColorValues()),
            new VariantDefinition(
                    "villager_variant",
                    DataComponents.VILLAGER_VARIANT,
                    context -> context.registryIds(Registries.VILLAGER_TYPE)),
            new VariantDefinition(
                    "wolf_variant",
                    DataComponents.WOLF_VARIANT,
                    context -> context.registryIds(Registries.WOLF_VARIANT)),
            new VariantDefinition(
                    "wolf_sound_variant",
                    DataComponents.WOLF_SOUND_VARIANT,
                    context -> context.registryIds(Registries.WOLF_SOUND_VARIANT)),
            new VariantDefinition("wolf_collar", DataComponents.WOLF_COLLAR, context -> dyeColorValues()),
            new VariantDefinition(
                    "zombie_nautilus_variant",
                    DataComponents.ZOMBIE_NAUTILUS_VARIANT,
                    context -> context.registryIds(Registries.ZOMBIE_NAUTILUS_VARIANT)));

    record VariantDefinition(
            String name, DataComponentType<?> component, Function<SpecialDataPanelContext, List<String>> entries) {
        boolean visible(ItemStack stack, String selectedEntity, String value) {
            String entity = entityForKey("special.entity_variant." + name);
            boolean present = stack.has(component);
            if (name.equals("painting_variant")) {
                return stack.is(Items.PAINTING) || present || !value.isBlank();
            }
            if (name.equals("chicken_variant")) present |= stack.is(Items.EGG);
            if (name.equals("llama_variant") && selectedEntity.equals("minecraft:trader_llama")) {
                selectedEntity = "minecraft:llama";
            }
            return show(selectedEntity, entity, present, value);
        }
    }

    private EntityVariantSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsEntityVariantData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        FlowLayout result = UiFactory.column();
        var groups = new LinkedHashSet<String>();
        fields(context, field -> groups.add(field.entityId()));
        for (String entityId : groups) {
            if (embedded(context, entityId)) continue;
            FlowLayout section = UiFactory.section(entityTitle(entityId), Component.empty());
            section.child(buildFields(context, entityId));
            result.child(section);
        }
        return result;
    }

    private record VariantField(String key, String value, Consumer<String> setter, Supplier<List<String>> entries)
            implements SpecialDataSearch.Field {
        String entityId() {
            return entityForKey(key);
        }
    }

    static String entityForKey(String key) {
        String suffix = key.substring("special.entity_variant.".length());
        if (suffix.startsWith("tropical_")) return "minecraft:tropical_fish";
        if (suffix.startsWith("zombie_nautilus_")) return "minecraft:zombie_nautilus";
        return "minecraft:" + suffix.substring(0, suffix.indexOf('_'));
    }

    private static Component entityTitle(String entityId) {
        return Component.translatable("entity." + entityId.replace(':', '.'));
    }

    private static boolean matches(VariantField field, String entityId) {
        return field.entityId().equals(entityId)
                || entityId.equals("minecraft:trader_llama") && field.entityId().equals("minecraft:llama");
    }

    private static boolean embedded(SpecialDataPanelContext context, String entityId) {
        String selected = selectedEntityId(context.originalStack(), context.special());
        return SpawnEggSpecialDataSection.supports(context.originalStack())
                && (entityId.equals(selected)
                        || entityId.equals("minecraft:llama") && selected.equals("minecraft:trader_llama"));
    }

    static long fieldCount(SpecialDataPanelContext context, String entityId, boolean configuredOnly) {
        var fields = new ArrayList<VariantField>();
        fields(context, field -> {
            if (matches(field, entityId) && (!configuredOnly || !field.value().isBlank())) fields.add(field);
        });
        return fields.size();
    }

    static FlowLayout buildFields(SpecialDataPanelContext context, String entityId) {
        FlowLayout result = UiFactory.column();
        String scopeEntity = entityId.equals("minecraft:trader_llama") ? "minecraft:llama" : entityId;
        result.id("entity-variants:" + scopeEntity);
        fields(context, field -> {
            if (matches(field, entityId))
                result.child(variantPicker(
                        context,
                        field.text(),
                        field.value(),
                        field.setter(),
                        field.entries().get()));
        });
        return result;
    }

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        fields(
                context,
                field -> result.addAll(SpecialDataSearch.targets(
                        context,
                        EditorCategory.SPECIAL_DATA,
                        List.of(entityTitle(field.entityId()).getString()),
                        "entity-variants:" + field.entityId(),
                        () -> {
                            if (embedded(context, field.entityId()))
                                context.special()
                                        .spawnEggEntity
                                        .uiExpandedTagGroups
                                        .add("entity");
                        },
                        field)));
        return result;
    }

    private static void fields(SpecialDataPanelContext context, Consumer<VariantField> fields) {
        ItemStack stack = context.originalStack();
        var state = context.screen().session().state();
        String entityId = selectedEntityId(stack, context.special());
        for (VariantDefinition definition : VARIANTS) {
            var binding = ItemEditorFieldReset.variant(definition.name());
            String value = binding.read().apply(state);
            if (definition.visible(stack, entityId, value)) {
                fields.accept(new VariantField(
                        "special.entity_variant." + definition.name(),
                        value,
                        selected -> binding.write().accept(state, selected),
                        () -> definition.entries().apply(context)));
            }
        }
    }

    private static FlowLayout variantPicker(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries) {
        ButtonComponent picker = PickerFieldFactory.clearableIdButton(context, value, entries, setter);
        return CompactFieldLayout.selectorRow(label, picker);
    }

    static boolean show(String selectedEntityId, String entityId, boolean componentPresent, String currentValue) {
        return (!entityId.equals("minecraft:horse") && Objects.equals(selectedEntityId, entityId))
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
