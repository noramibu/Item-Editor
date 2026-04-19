package me.noramibu.itemeditor.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RawAutocompleteSchema {

    private static final String VERSIONED_RESOURCE_TEMPLATE = "/data/itemeditor/mc_schemas/%s.json";
    private static final List<String> DEFAULT_TOP_LEVEL_KEYS = List.of("id", "count", "components");
    private static final String ALL_ITEMS_PROFILE_KEY = "all_items";

    private final List<String> topLevelKeys;
    private final List<String> componentIds;
    private final List<String> nbtKeyCandidates;
    private final Map<String, List<String>> objectKeyHints;
    private final Map<String, List<String>> valueHints;
    private final Map<String, ItemTypeProfile> itemTypeProfiles;

    private RawAutocompleteSchema(
            List<String> topLevelKeys,
            List<String> componentIds,
            List<String> nbtKeyCandidates,
            Map<String, List<String>> objectKeyHints,
            Map<String, List<String>> valueHints,
            Map<String, ItemTypeProfile> itemTypeProfiles
    ) {
        this.topLevelKeys = List.copyOf(topLevelKeys);
        this.componentIds = List.copyOf(componentIds);
        this.nbtKeyCandidates = List.copyOf(nbtKeyCandidates);
        this.objectKeyHints = immutableStringListMap(objectKeyHints);
        this.valueHints = immutableStringListMap(valueHints);
        this.itemTypeProfiles = Map.copyOf(itemTypeProfiles);
    }

    public static RawAutocompleteSchema load() {
        String currentVersion = currentVersionId();
        if (!currentVersion.isBlank()) {
            RawAutocompleteSchema versioned = loadFromResource(String.format(VERSIONED_RESOURCE_TEMPLATE, currentVersion));
            if (versioned != null) {
                return versioned;
            }
        }

        return empty();
    }

    private static String currentVersionId() {
        try {
            return SharedConstants.getCurrentVersion().id();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static RawAutocompleteSchema loadFromResource(String resourcePath) {
        try (InputStream stream = RawAutocompleteSchema.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> topLevel = readStringArray(root.getAsJsonArray("top_level_keys"));
            List<String> componentIds = readStringArray(root.getAsJsonArray("component_ids"));
            List<String> nbtKeys = readStringArray(root.getAsJsonArray("nbt_key_candidates"));
            Map<String, List<String>> objectHints = readMap(root.getAsJsonObject("object_key_hints"));
            Map<String, List<String>> valueHints = readMap(root.getAsJsonObject("value_hints"));
            Map<String, ItemTypeProfile> itemTypeProfiles = readItemTypeProfiles(root.getAsJsonObject("item_type_profiles"));

            if (topLevel.isEmpty()) {
                topLevel = DEFAULT_TOP_LEVEL_KEYS;
            }
            if (componentIds.isEmpty()) {
                componentIds = objectHints.getOrDefault("components", List.of());
            }
            if (itemTypeProfiles.isEmpty()) {
                itemTypeProfiles = defaultItemTypeProfiles(componentIds);
            }

            return new RawAutocompleteSchema(
                    topLevel,
                    componentIds,
                    nbtKeys,
                    objectHints,
                    valueHints,
                    itemTypeProfiles
            );
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static Map<String, ItemTypeProfile> defaultItemTypeProfiles(List<String> componentIds) {
        return Map.of(
                ALL_ITEMS_PROFILE_KEY,
                new ItemTypeProfile(
                        List.of(ALL_ITEMS_PROFILE_KEY),
                        componentIds,
                        Map.of()
                )
        );
    }

    public List<String> topLevelKeys() {
        return this.topLevelKeys;
    }

    public List<String> componentIds() {
        return this.componentIds;
    }

    public List<String> nbtKeyCandidates() {
        return this.nbtKeyCandidates;
    }

    public List<String> objectKeyHints(String key) {
        if (key == null || key.isBlank()) {
            return Collections.emptyList();
        }
        List<String> exact = this.objectKeyHints.get(key);
        if (exact != null) {
            return exact;
        }
        return this.objectKeyHints.getOrDefault(key.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public List<String> valueHints(String key) {
        if (key == null || key.isBlank()) {
            return Collections.emptyList();
        }
        List<String> exact = this.valueHints.get(key);
        if (exact != null) {
            return exact;
        }
        return this.valueHints.getOrDefault(key.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public List<String> componentsForProfiles(List<String> profileNames) {
        if (profileNames == null || profileNames.isEmpty()) {
            return this.componentIds;
        }

        Set<String> merged = new LinkedHashSet<>();
        for (String profileName : profileNames) {
            ItemTypeProfile profile = this.itemTypeProfiles.get(profileName);
            if (profile == null) {
                continue;
            }
            merged.addAll(profile.components());
        }
        if (merged.isEmpty()) {
            return this.componentIds;
        }
        return List.copyOf(merged);
    }

    public List<String> componentNbtFieldsForProfiles(String componentId, List<String> profileNames) {
        if (componentId == null || componentId.isBlank() || profileNames == null || profileNames.isEmpty()) {
            return List.of();
        }

        Set<String> merged = new LinkedHashSet<>();
        for (String profileName : profileNames) {
            ItemTypeProfile profile = this.itemTypeProfiles.get(profileName);
            if (profile == null) {
                continue;
            }
            merged.addAll(profile.componentNbtFields(componentId));
        }
        return List.copyOf(merged);
    }

    private static Map<String, List<String>> immutableStringListMap(Map<String, List<String>> source) {
        if (source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    private static Map<String, List<String>> readMap(JsonObject object) {
        if (object == null) {
            return Map.of();
        }
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            values.put(entry.getKey(), readStringArray(entry.getValue().getAsJsonArray()));
        }
        return values;
    }

    private static Map<String, ItemTypeProfile> readItemTypeProfiles(JsonObject object) {
        if (object == null) {
            return Map.of();
        }

        Map<String, ItemTypeProfile> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject profileObject = entry.getValue().getAsJsonObject();
            List<String> categories = readStringArray(profileObject.getAsJsonArray("categories"));
            List<String> components = readStringArray(profileObject.getAsJsonArray("components"));
            Map<String, List<String>> componentNbtFields = readMap(profileObject.getAsJsonObject("component_nbt_fields"));
            values.put(
                    entry.getKey(),
                    new ItemTypeProfile(categories, components, componentNbtFields)
            );
        }
        return values;
    }

    private static List<String> readStringArray(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                result.add(element.getAsString());
            }
        }
        return result;
    }

    private static RawAutocompleteSchema empty() {
        return new RawAutocompleteSchema(
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public record ItemTypeProfile(
            List<String> categories,
            List<String> components,
            Map<String, List<String>> componentNbtFields
    ) {
        public ItemTypeProfile {
            categories = List.copyOf(categories);
            components = List.copyOf(components);
            componentNbtFields = immutableStringListMap(componentNbtFields);
        }

        public List<String> componentNbtFields(String componentId) {
            if (componentId == null || componentId.isBlank()) {
                return List.of();
            }
            List<String> exact = this.componentNbtFields.get(componentId);
            if (exact != null) {
                return exact;
            }
            return this.componentNbtFields.getOrDefault(componentId.toLowerCase(Locale.ROOT), List.of());
        }
    }
}
