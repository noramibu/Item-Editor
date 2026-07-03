package me.noramibu.itemeditor.util;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class RawAutocompleteCatalog {

    private static final List<String> TOP_LEVEL_KEYS = List.of("id", "count", "components");
    private static final Map<String, List<String>> OBJECT_KEY_HINTS = createObjectKeyHints();
    private static final Map<String, List<String>> PROFILE_COMPONENTS = Map.ofEntries(
            Map.entry("all_items", List.of("minecraft:custom_name", "minecraft:item_name", "minecraft:lore")),
            Map.entry("food_items", List.of("minecraft:food", "minecraft:consumable")),
            Map.entry("durable_items", List.of("minecraft:damage", "minecraft:max_damage", "minecraft:repair_cost")),
            Map.entry("tools", List.of("minecraft:tool")),
            Map.entry("weapons", List.of("minecraft:weapon", "minecraft:enchantments")),
            Map.entry("spears", List.of("minecraft:weapon")),
            Map.entry("armor", List.of("minecraft:equippable")),
            Map.entry("trimmable_armor", List.of("minecraft:trim")),
            Map.entry("books", List.of("minecraft:writable_book_content", "minecraft:written_book_content")),
            Map.entry("banners", List.of("minecraft:banner_patterns")),
            Map.entry("bundles", List.of("minecraft:bundle_contents")),
            Map.entry("shulker_boxes", List.of("minecraft:container")),
            Map.entry("containers", List.of("minecraft:container")),
            Map.entry("block_entity_items", List.of("minecraft:block_entity_data")),
            Map.entry("spawn_eggs", List.of("minecraft:entity_data")),
            Map.entry("bucket_items", List.of("minecraft:bucket_entity_data")),
            Map.entry("buckets", List.of("minecraft:bucket_entity_data")),
            Map.entry("potions", List.of("minecraft:potion_contents", "minecraft:suspicious_stew_effects")),
            Map.entry("fireworks", List.of("minecraft:fireworks", "minecraft:firework_explosion")),
            Map.entry("maps", List.of("minecraft:map_id", "minecraft:map_decorations")),
            Map.entry("music_discs", List.of("minecraft:jukebox_playable")),
            Map.entry("goat_horns", List.of("minecraft:instrument")),
            Map.entry("heads", List.of("minecraft:profile"))
    );

    private final RawRuntimeSuggestionProvider runtime = new RawRuntimeSuggestionProvider();
    private final RawCodecShapeIndex codecShapes = new RawCodecShapeIndex();
    private final Map<String, DefaultComponentData> defaultComponentDataCache = new LinkedHashMap<>();

    List<String> topLevelKeys() {
        return TOP_LEVEL_KEYS;
    }

    List<String> componentIds(RegistryAccess registryAccess) {
        return this.runtime.registryIds(
                effectiveRegistryAccess(registryAccess),
                Registries.DATA_COMPONENT_TYPE,
                BuiltInRegistries.DATA_COMPONENT_TYPE
        );
    }

    List<String> componentsForContext(
            String itemId,
            List<String> profiles,
            RegistryAccess registryAccess
    ) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(defaultComponentIds(itemId, registryAccess));
        result.addAll(profileRelevantComponentIds(profiles));
        return List.copyOf(result);
    }

    List<String> objectKeyHints(
            String key,
            String path,
            String itemId,
            List<String> profiles,
            RegistryAccess registryAccess
    ) {
        String normalizedKey = normalizeLookupKey(key);
        if (normalizedKey.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (isComponentId(normalizedKey, registryAccess)) {
            result.addAll(componentFieldHints(normalizedKey, itemId, profiles, registryAccess));
        }
        result.addAll(OBJECT_KEY_HINTS.getOrDefault(normalizedKey, List.of()));

        String normalizedPath = Objects.requireNonNullElse(path, "").toLowerCase(Locale.ROOT);
        if (normalizedPath.endsWith("/entity")
                || normalizedPath.contains("/spawndata/entity")
                || normalizedPath.contains("/spawnpotentials/data/entity")) {
            result.addAll(OBJECT_KEY_HINTS.getOrDefault("entity", List.of()));
        }
        return List.copyOf(result);
    }

    List<String> componentFieldHints(
            String componentId,
            String itemId,
            List<String> profiles,
            RegistryAccess registryAccess
    ) {
        String normalizedComponentId = normalizeComponentId(componentId);
        if (normalizedComponentId.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(defaultComponentData(itemId, registryAccess)
                .fields()
                .getOrDefault(normalizedComponentId, List.of()));
        result.addAll(this.codecShapes.fieldsFor(
                normalizedComponentId,
                componentType(normalizedComponentId, registryAccess)
        ));
        result.addAll(profileFieldHints(normalizedComponentId, profiles));
        return List.copyOf(result);
    }

    String defaultComponentValue(String componentId, String itemId, RegistryAccess registryAccess) {
        String normalizedComponentId = normalizeComponentId(componentId);
        if (normalizedComponentId.isBlank()) {
            return "";
        }
        return defaultComponentData(itemId, registryAccess)
                .values()
                .getOrDefault(normalizedComponentId, "");
    }

    private List<String> defaultComponentIds(String itemId, RegistryAccess registryAccess) {
        String normalizedItemId = normalizeComponentId(itemId);
        if (normalizedItemId.isBlank()) {
            return componentIdsFrom(DataComponents.COMMON_ITEM_COMPONENTS);
        }
        return defaultComponentData(normalizedItemId, registryAccess).ids();
    }

    private DefaultComponentData defaultComponentData(String itemId, RegistryAccess registryAccess) {
        String normalizedItemId = normalizeComponentId(itemId);
        if (normalizedItemId.isBlank()) {
            return DefaultComponentData.EMPTY;
        }

        String cacheKey = normalizedItemId + "@" + System.identityHashCode(registryAccess);
        DefaultComponentData cached = this.defaultComponentDataCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Item item = itemById(normalizedItemId, registryAccess);
        ItemStack stack;
        try {
            stack = item == null ? null : new ItemStack(item);
        } catch (RuntimeException ignored) {
            stack = null;
        }
        if (stack == null) {
            return new DefaultComponentData(componentIdsFrom(DataComponents.COMMON_ITEM_COMPONENTS), Map.of(), Map.of());
        }

        DynamicOps<Tag> ops = effectiveRegistryAccess(registryAccess)
                .createSerializationContext(NbtOps.INSTANCE);
        List<String> ids = componentIdsFrom(stack.getComponents());
        Map<String, List<String>> fields = new LinkedHashMap<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (TypedDataComponent<?> component : stack.getComponents()) {
            Identifier componentId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            if (componentId == null) {
                continue;
            }
            component.encodeValue(ops)
                    .result()
                    .ifPresent(tag -> addDefaultComponentData(componentId.toString(), tag, fields, values));
        }

        DefaultComponentData data = new DefaultComponentData(ids, Map.copyOf(fields), Map.copyOf(values));
        putBounded(this.defaultComponentDataCache, cacheKey, data);
        return data;
    }

    private static void addDefaultComponentData(
            String componentId,
            Tag tag,
            Map<String, List<String>> fields,
            Map<String, String> values
    ) {
        List<String> keys = keysFromEncodedValue(tag);
        if (!keys.isEmpty()) {
            fields.put(componentId, keys);
        }

        String value = tag.toString();
        if (!value.isBlank() && value.length() <= 240) {
            values.put(componentId, value);
        }
    }

    private List<String> profileRelevantComponentIds(List<String> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        Set<String> normalizedProfiles = new LinkedHashSet<>();
        for (String profile : profiles) {
            if (profile != null && !profile.isBlank()) {
                normalizedProfiles.add(profile.toLowerCase(Locale.ROOT));
            }
        }

        for (String profile : normalizedProfiles) {
            result.addAll(PROFILE_COMPONENTS.getOrDefault(profile, List.of()));
        }

        return List.copyOf(result);
    }

    private List<String> profileFieldHints(String componentId, List<String> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String profile : profiles) {
            if ("heads".equals(profile) && "minecraft:profile".equals(componentId)) {
                result.addAll(List.of("name", "id", "properties"));
            } else if ("spawn_eggs".equals(profile) && "minecraft:entity_data".equals(componentId)) {
                result.addAll(OBJECT_KEY_HINTS.getOrDefault("entity", List.of()));
            } else if ("block_entity_items".equals(profile)
                    && "minecraft:block_entity_data".equals(componentId)) {
                result.addAll(OBJECT_KEY_HINTS.getOrDefault("block_entity_data", List.of()));
            }
        }
        return List.copyOf(result);
    }

    private boolean isComponentId(String key, RegistryAccess registryAccess) {
        return componentType(key, registryAccess) != null;
    }

    private DataComponentType<?> componentType(String componentId, RegistryAccess registryAccess) {
        Identifier id = Identifier.tryParse(normalizeComponentId(componentId));
        if (id == null) {
            return null;
        }

        try {
            Registry<DataComponentType<?>> registry = effectiveRegistryAccess(registryAccess)
                    .lookupOrThrow(Registries.DATA_COMPONENT_TYPE);
            var holder = registry.get(id).orElse(null);
            if (holder != null) {
                return holder.value();
            }
        } catch (RuntimeException ignored) {
        }
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(id).orElse(null);
    }

    private Item itemById(String itemId, RegistryAccess registryAccess) {
        Identifier id = Identifier.tryParse(normalizeComponentId(itemId));
        if (id == null) {
            return null;
        }

        try {
            Registry<Item> registry = effectiveRegistryAccess(registryAccess)
                    .lookupOrThrow(Registries.ITEM);
            var holder = registry.get(id).orElse(null);
            if (holder != null) {
                return holder.value();
            }
        } catch (RuntimeException ignored) {
        }
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    private static List<String> componentIdsFrom(DataComponentMap components) {
        if (components == null || components.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>(components.size());
        for (DataComponentType<?> type : components.keySet()) {
            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id != null) {
                result.add(id.toString());
            }
        }
        return List.copyOf(result);
    }

    private static List<String> keysFromEncodedValue(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            return List.copyOf(compoundTag.keySet());
        }
        if (tag instanceof ListTag listTag && !listTag.isEmpty()) {
            Tag first = listTag.getFirst();
            if (first instanceof CompoundTag compoundTag) {
                return List.copyOf(compoundTag.keySet());
            }
        }
        return List.of();
    }

    private static RegistryAccess effectiveRegistryAccess(RegistryAccess registryAccess) {
        return registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
    }

    private static String normalizeLookupKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeComponentId(String componentId) {
        String normalized = Objects.requireNonNullElse(componentId, "").trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.contains(":")) {
            return normalized;
        }
        return "minecraft:" + normalized;
    }

    private static <T> void putBounded(Map<String, T> cache, String key, T value) {
        if (cache.size() > 128) {
            String firstKey = cache.keySet().iterator().next();
            cache.remove(firstKey);
        }
        cache.put(key, value);
    }

    private record DefaultComponentData(
            List<String> ids,
            Map<String, List<String>> fields,
            Map<String, String> values
    ) {
        private static final DefaultComponentData EMPTY = new DefaultComponentData(List.of(), Map.of(), Map.of());
    }

    private static Map<String, List<String>> createObjectKeyHints() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("entity", List.of(
                "id",
                "CustomName",
                "CustomNameVisible",
                "Glowing",
                "NoAI",
                "NoGravity",
                "Silent",
                "Invulnerable",
                "Health",
                "Motion",
                "Rotation",
                "Pos",
                "Tags",
                "Passengers",
                "TileEntityData"
        ));
        values.put("block_entity_data", List.of(
                "id",
                "Items",
                "LootTable",
                "LootTableSeed",
                "SpawnData",
                "SpawnPotentials",
                "Delay",
                "MinSpawnDelay",
                "MaxSpawnDelay",
                "SpawnCount",
                "MaxNearbyEntities",
                "RequiredPlayerRange",
                "SpawnRange",
                "conditionMet",
                "auto",
                "powered",
                "Command",
                "SuccessCount",
                "TrackOutput",
                "UpdateLastExecution"
        ));
        values.put("spawndata", List.of("entity", "equipment"));
        values.put("spawnpotentials", List.of("data", "weight"));
        values.put("minecraft:custom_data", List.of("BlockEntityTag"));
        values.put("custom_spawn_rules", List.of("block_light_limit", "sky_light_limit"));
        values.put("block_light_limit", List.of("min_inclusive", "max_inclusive"));
        values.put("sky_light_limit", List.of("min_inclusive", "max_inclusive"));
        values.put("map_decoration", List.of("type", "x", "z", "rotation"));
        values.put("on_consume_effects", List.of("type", "effects", "probability", "diameter", "sound"));
        values.put("effects", List.of(
                "id",
                "amplifier",
                "duration",
                "ambient",
                "show_particles",
                "show_icon"
        ));
        values.put("properties", List.of("name", "value", "signature"));
        values.put("rules", List.of("blocks", "speed", "correct_for_drops"));
        values.put("target", List.of("dimension", "pos"));
        values.put("display", List.of("Name", "Lore"));
        values.put("attribute_modifier", List.of("type", "amount", "operation", "id", "slot"));
        values.put("banner_pattern", List.of("pattern", "color"));
        values.put("score", List.of("name", "objective"));
        values.put("patterns", List.of("Pattern", "Color"));
        values.put("attributemodifiers", List.of("AttributeName", "Name", "Amount", "Operation", "Slot", "UUID"));
        values.put("enchantments", List.of("id", "lvl"));
        values.put("storedenchantments", List.of("id", "lvl"));
        values.put("stored_enchantments", List.of("id", "lvl"));
        values.put("custompotioneffects", List.of(
                "Id",
                "Amplifier",
                "Duration",
                "Ambient",
                "ShowParticles",
                "ShowIcon"
        ));
        values.put("custom_potion_effects", List.of(
                "id",
                "amplifier",
                "duration",
                "ambient",
                "show_particles",
                "show_icon"
        ));
        values.put("fireworks", List.of("Flight", "Explosions"));
        values.put("explosions", List.of("shape", "colors", "fade_colors", "has_trail", "has_twinkle"));
        values.put("container_entry", List.of("slot", "item"));
        values.put("item", TOP_LEVEL_KEYS);
        values.put("items", TOP_LEVEL_KEYS);
        values.put("text", List.of(
                "text",
                "color",
                "translate",
                "with",
                "extra",
                "score",
                "selector",
                "keybind",
                "nbt",
                "bold",
                "italic",
                "underlined",
                "strikethrough",
                "obfuscated",
                "font",
                "insertion",
                "hover_event",
                "click_event"
        ));
        values.put("hover_event", List.of("action", "value", "id", "count", "components", "name", "uuid"));
        values.put("click_event", List.of("action", "url", "path", "command", "page", "value"));
        return Map.copyOf(values);
    }
}
