package me.noramibu.itemeditor.editor;

import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

public final class ItemEditorChangeSet {
    private static final String ITEM_ID = "itemeditor:item";
    private static final String COUNT_ID = "itemeditor:count";

    private final List<Change> changes;

    private ItemEditorChangeSet(List<Change> changes) {
        this.changes = changes;
    }

    public static ItemEditorChangeSet between(ItemStack original, ItemStack current, RegistryAccess registryAccess) {
        return between(original, current, registryAccess, true);
    }

    public static ItemEditorChangeSet detect(ItemStack original, ItemStack current, RegistryAccess registryAccess) {
        return between(original, current, registryAccess, false);
    }

    private static ItemEditorChangeSet between(
            ItemStack original, ItemStack current, RegistryAccess registryAccess, boolean includeValues) {
        List<Change> changes = new ArrayList<>();
        Identifier originalItem = BuiltInRegistries.ITEM.getKey(original.getItem());
        Identifier currentItem = BuiltInRegistries.ITEM.getKey(current.getItem());
        if (!Objects.equals(originalItem, currentItem)) {
            changes.add(new Change(
                    ITEM_ID, EditorCategory.GENERAL, value(originalItem), value(currentItem), ChangeKind.MODIFIED));
        }
        if (original.getCount() != current.getCount()) {
            changes.add(new Change(
                    COUNT_ID,
                    EditorCategory.GENERAL,
                    Integer.toString(original.getCount()),
                    Integer.toString(current.getCount()),
                    ChangeKind.MODIFIED));
        }

        Set<DataComponentType<?>> types =
                new LinkedHashSet<>(original.getComponents().keySet());
        types.addAll(current.getComponents().keySet());
        DynamicOps<Tag> ops = includeValues ? registryAccess.createSerializationContext(NbtOps.INSTANCE) : null;
        for (DataComponentType<?> type : types) {
            Object before = original.get(type);
            Object after = current.get(type);
            if (equivalent(type, before, after)) {
                continue;
            }
            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id == null) {
                continue;
            }
            ChangeKind kind = classify(type, before, after);
            changes.add(new Change(
                    id.toString(),
                    categoryFor(id.toString()),
                    includeValues && !semanticallyAbsent(type, before) ? encode(type, before, ops) : "",
                    includeValues && !semanticallyAbsent(type, after) ? encode(type, after, ops) : "",
                    kind));
        }
        changes.sort(Comparator.comparing(Change::category).thenComparing(Change::id));
        return new ItemEditorChangeSet(List.copyOf(changes));
    }

    public List<Change> forCategory(EditorCategory category) {
        if (category == EditorCategory.RAW_EDITOR) {
            return this.changes;
        }
        return this.changes.stream()
                .filter(change -> change.category() == category)
                .toList();
    }

    public int count(EditorCategory category) {
        return this.forCategory(category).size();
    }

    public int size() {
        return this.changes.size();
    }

    static boolean equivalent(DataComponentType<?> type, Object before, Object after) {
        if (Objects.equals(before, after)) {
            return true;
        }
        return semanticallyAbsent(type, before) && semanticallyAbsent(type, after);
    }

    static ChangeKind classify(DataComponentType<?> type, Object before, Object after) {
        if (semanticallyAbsent(type, before)) {
            return ChangeKind.ADDED;
        }
        if (semanticallyAbsent(type, after)) {
            return ChangeKind.REMOVED;
        }
        return ChangeKind.MODIFIED;
    }

    private static boolean semanticallyAbsent(DataComponentType<?> type, Object value) {
        return value == null
                || type == DataComponents.LORE
                        && value instanceof ItemLore lore
                        && lore.lines().isEmpty()
                || type == DataComponents.ATTRIBUTE_MODIFIERS
                        && value instanceof ItemAttributeModifiers modifiers
                        && modifiers.modifiers().isEmpty();
    }

    private static String encode(DataComponentType<?> type, Object value, DynamicOps<Tag> ops) {
        if (value == null) {
            return "";
        }
        return encodeUnchecked(type, value, ops);
    }

    @SuppressWarnings("unchecked")
    private static <T> String encodeUnchecked(DataComponentType<T> type, Object value, DynamicOps<Tag> ops) {
        return TypedDataComponent.createUnchecked(type, (T) value)
                .encodeValue(ops)
                .result()
                .map(Tag::toString)
                .orElseGet(() -> String.valueOf(value));
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    static EditorCategory categoryFor(String id) {
        return switch (id) {
            case "minecraft:custom_name",
                    "minecraft:max_stack_size",
                    "minecraft:max_damage",
                    "minecraft:damage",
                    "minecraft:repair_cost",
                    "minecraft:unbreakable",
                    "minecraft:enchantment_glint_override",
                    "minecraft:rarity",
                    "minecraft:item_model",
                    "minecraft:custom_model_data",
                    "minecraft:can_break",
                    "minecraft:can_place_on" -> EditorCategory.GENERAL;
            case "minecraft:lore", "minecraft:dyed_color", "minecraft:trim" -> EditorCategory.DISPLAY;
            case "minecraft:attribute_modifiers" -> EditorCategory.ATTRIBUTES;
            case "minecraft:enchantments", "minecraft:stored_enchantments" -> EditorCategory.ENCHANTMENTS;
            case "minecraft:tooltip_display" -> EditorCategory.FLAGS;
            case "minecraft:written_book_content", "minecraft:writable_book_content" -> EditorCategory.BOOK;
            case "minecraft:weapon",
                    "minecraft:tool",
                    "minecraft:attack_range",
                    "minecraft:damage_resistant",
                    "minecraft:blocks_attacks",
                    "minecraft:death_protection",
                    "minecraft:piercing_weapon",
                    "minecraft:kinetic_weapon",
                    "minecraft:swing_animation" -> EditorCategory.COMBAT;
            case "minecraft:entity_data",
                    "minecraft:block_entity_data",
                    "minecraft:bucket_entity_data",
                    "minecraft:banner_patterns",
                    "minecraft:base_color",
                    "minecraft:fireworks",
                    "minecraft:firework_explosion",
                    "minecraft:potion_contents",
                    "minecraft:suspicious_stew_effects",
                    "minecraft:profile",
                    "minecraft:instrument",
                    "minecraft:map_id",
                    "minecraft:map_decorations",
                    "minecraft:lodestone_tracker",
                    "minecraft:container",
                    "minecraft:bundle_contents",
                    "minecraft:pot_decorations" -> EditorCategory.SPECIAL_DATA;
            default -> EditorCategory.COMPONENTS;
        };
    }

    public record Change(String id, EditorCategory category, String before, String after, ChangeKind kind) {}

    public enum ChangeKind {
        ADDED,
        MODIFIED,
        REMOVED
    }
}
