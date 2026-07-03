package me.noramibu.itemeditor.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class RawCodecShapeIndex {

    private static final int CACHE_LIMIT = 256;
    private static final int MAX_REFLECTION_DEPTH = 8;
    private static final int MAX_KEYS = 32;

    private final Map<String, List<String>> cache = new LinkedHashMap<>(CACHE_LIMIT + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest) {
            return this.size() > CACHE_LIMIT;
        }
    };

    List<String> fieldsFor(String componentId, DataComponentType<?> type) {
        if (type == null) {
            return List.of();
        }

        String cacheKey = Objects.requireNonNullElse(componentId, "")
                + "@"
                + System.identityHashCode(type);
        List<String> cached = this.cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<String> fields = discoverFields(type);
        this.cache.put(cacheKey, fields);
        return fields;
    }

    private static List<String> discoverFields(DataComponentType<?> type) {
        Codec<?> codec;
        try {
            codec = type.codec();
        } catch (RuntimeException ignored) {
            return List.of();
        }
        if (codec == null) {
            return List.of();
        }

        return findFields(codec, new IdentityHashMap<>(), 0);
    }

    private static List<String> findFields(
            Object value,
            IdentityHashMap<Object, Boolean> seen,
            int depth
    ) {
        if (value == null || depth > MAX_REFLECTION_DEPTH) {
            return List.of();
        }
        if (seen.put(value, Boolean.TRUE) != null) {
            return List.of();
        }

        if (value instanceof MapCodec<?> mapCodec) {
            return mapCodecKeys(mapCodec);
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                List<String> fields = findFields(entry, seen, depth + 1);
                if (!fields.isEmpty()) {
                    return fields;
                }
            }
            return List.of();
        }
        if (value instanceof Map<?, ?> map) {
            for (Object entry : map.values()) {
                List<String> fields = findFields(entry, seen, depth + 1);
                if (!fields.isEmpty()) {
                    return fields;
                }
            }
            return List.of();
        }
        if (value instanceof Optional<?> optional) {
            return optional
                    .map(entry -> findFields(entry, seen, depth + 1))
                    .orElse(List.of());
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                List<String> fields = findFields(Array.get(value, index), seen, depth + 1);
                if (!fields.isEmpty()) {
                    return fields;
                }
            }
            return List.of();
        }
        if (!shouldInspectFields(value.getClass())) {
            return List.of();
        }

        for (Field field : value.getClass().getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object nested = fieldValue(field, value);
            if (nested != null) {
                List<String> fields = findFields(nested, seen, depth + 1);
                if (!fields.isEmpty()) {
                    return fields;
                }
            }
        }
        return List.of();
    }

    private static List<String> mapCodecKeys(MapCodec<?> codec) {
        try {
            return codec.keys(JsonOps.INSTANCE)
                    .map(element -> element == null || !element.isJsonPrimitive()
                            ? ""
                            : Objects.requireNonNullElse(element.getAsString(), "")
                                    .trim()
                                    .toLowerCase(Locale.ROOT))
                    .filter(key -> !key.isBlank())
                    .distinct()
                    .limit(MAX_KEYS)
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static Object fieldValue(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (RuntimeException | ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean shouldInspectFields(Class<?> type) {
        if (type == null || type.isPrimitive() || type.isEnum()) {
            return false;
        }

        String name = type.getName();
        return name.startsWith("com.mojang.serialization.")
                || name.startsWith("com.mojang.datafixers.")
                || name.startsWith("net.minecraft.");
    }
}
