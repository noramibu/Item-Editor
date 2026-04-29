package me.noramibu.itemeditor.util;

import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class RawAutocompleteUtil {
    private static final int MAX_RESULTS = 18;
    private static final int MAX_NEAREST_CANDIDATES = 4;
    private static final int MAX_FUZZY_DISTANCE = 3;
    private static final int PARSE_FILTER_CANDIDATE_LIMIT = 64;
    private static final int PARSE_FILTER_MAX_TEXT_LENGTH = 24000;
    private static final int PARSE_FILTER_NEAR_CARET_WINDOW = 96;
    private static final int WEIGHT_TYPE_MATCH = 1000;
    private static final int WEIGHT_PARSER = 300;
    private static final int WEIGHT_PREFIX = 80;
    private static final int WEIGHT_RECENCY = 35;
    private static final int WEIGHT_SCHEMA = 20;
    private static final Pattern NAMESPACED_ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final Pattern NUMBER_TOKEN_PATTERN = Pattern.compile("^-?(?:\\d+|\\d+\\.\\d+)(?:[bBsSlLfFdD])?$");
    private static final List<String> LITERAL_VALUES = List.of("true", "false", "null");
    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");
    private static final List<String> STRUCTURAL_VALUE_TEMPLATES = List.of("\"\"", "{}", "[]");
    private static final List<String> INTEGER_NUMBER_VALUES = List.of("0", "1", "2", "5", "10", "20", "64", "128", "200");
    private static final List<String> FLOAT_NUMBER_VALUES = List.of(
            "0.0f",
            "0.1f",
            "0.5f",
            "1.0f",
            "2.0f",
            "4.0f",
            "10.0f",
            "0.0d",
            "1.0d",
            "Infinityf"
    );
    private static final List<String> NUMBER_VALUES = List.of(
            "0",
            "1",
            "0b",
            "1b",
            "0s",
            "1s",
            "0L",
            "1L",
            "0.0f",
            "1.0f",
            "0.0d",
            "1.0d",
            "Infinityf",
            "NaNd"
    );
    private static final RawRuntimeSuggestionProvider RUNTIME = new RawRuntimeSuggestionProvider();
    private static final List<String> SPAWNER_BLOCK_ENTITY_KEYS = List.of(
            "Delay",
            "MinSpawnDelay",
            "MaxSpawnDelay",
            "SpawnCount",
            "MaxNearbyEntities",
            "RequiredPlayerRange",
            "SpawnRange",
            "SpawnData",
            "SpawnPotentials",
            "id"
    );
    private static final List<String> ENTITY_COMMON_KEYS = List.of(
            "id",
            "CustomName",
            "CustomNameVisible",
            "Glowing",
            "NoGravity",
            "Silent",
            "Invulnerable",
            "AbsorptionAmount",
            "Health",
            "Motion",
            "Rotation",
            "Pos",
            "Tags",
            "Passengers"
    );
    private static final List<String> SPAWNER_ENTITY_KEYS = List.of(
            "id",
            "potion_duration_scale"
    );
    private static final List<String> FOOD_COMPONENT_KEYS = List.of(
            "nutrition",
            "saturation",
            "can_always_eat"
    );
    private static final List<String> CONSUMABLE_COMPONENT_KEYS = List.of(
            "consume_seconds",
            "animation",
            "sound",
            "has_consume_particles",
            "on_consume_effects"
    );
    private static final List<String> ON_CONSUME_EFFECT_ENTRY_KEYS = List.of(
            "type",
            "effects",
            "probability",
            "sound"
    );
    private static final List<String> EFFECT_INSTANCE_KEYS = List.of(
            "id",
            "amplifier",
            "duration",
            "ambient",
            "show_particles",
            "show_icon"
    );
    private static final List<String> CONSUME_EFFECT_TYPES = List.of(
            "minecraft:apply_effects",
            "minecraft:play_sound"
    );
    private static final List<String> LEGACY_ATTRIBUTE_MODIFIER_KEYS = List.of(
            "AttributeName",
            "Name",
            "Amount",
            "Operation",
            "Slot",
            "UUID"
    );
    private static final List<String> LEGACY_ENCHANTMENT_KEYS = List.of(
            "id",
            "lvl"
    );
    private static final List<String> LEGACY_DISPLAY_KEYS = List.of(
            "Name",
            "Lore"
    );
    private static final List<String> LEGACY_CUSTOM_POTION_EFFECT_KEYS = List.of(
            "Id",
            "Amplifier",
            "Duration",
            "Ambient",
            "ShowParticles",
            "ShowIcon"
    );
    private static final List<String> LEGACY_FIREWORK_KEYS = List.of(
            "Flight",
            "Explosions"
    );
    private static final List<String> LEGACY_BANNER_PATTERN_KEYS = List.of(
            "Pattern",
            "Color"
    );
    private static final List<String> EQUIPMENT_SLOT_VALUES = List.of(
            "mainhand",
            "offhand",
            "head",
            "chest",
            "legs",
            "feet",
            "hand",
            "armor",
            "body"
    );
    private static final List<String> LEGACY_ATTRIBUTE_OPERATION_VALUES = List.of("0", "1", "2");
    private static final List<String> MODERN_ATTRIBUTE_OPERATION_VALUES = List.of(
            "add_value",
            "add_multiplied_base",
            "add_multiplied_total"
    );
    private static final Set<String> BOOLEAN_EXACT_KEYS = Set.of(
            "can_always_eat",
            "has_consume_particles",
            "show_icon",
            "show_particles",
            "ambient",
            "tracked",
            "customnamevisible",
            "custom_name_visible",
            "glowing",
            "nogravity",
            "no_gravity",
            "silent",
            "invulnerable",
            "resolved",
            "unbreakable",
            "showparticles",
            "showicon"
    );
    private static final Set<String> NUMERIC_EXACT_KEYS = Set.of(
            "count",
            "nutrition",
            "saturation",
            "consume_seconds",
            "seconds",
            "duration",
            "amplifier",
            "probability",
            "weight",
            "delay",
            "maxspawncount",
            "minspawndelay",
            "maxspawndelay",
            "maxnearbyentities",
            "requiredplayerrange",
            "spawnrange",
            "spawncount",
            "lvl",
            "base",
            "flight",
            "custompotioncolor"
    );
    private static final Set<String> STRING_EXACT_KEYS = Set.of(
            "id",
            "type",
            "animation",
            "sound",
            "dimension",
            "name",
            "title",
            "author",
            "text",
            "translate",
            "attributename",
            "color",
            "rarity",
            "operation",
            "slot"
    );
    private static final Map<String, String> VALUE_SNIPPETS = Map.of(
            "minecraft:food", "{nutrition: 1, saturation: 0.1f, can_always_eat: false}",
            "minecraft:consumable", "{consume_seconds: 1.6f, animation: \"eat\", sound: \"minecraft:entity.generic.eat\", has_consume_particles: true, on_consume_effects: []}",
            "minecraft:use_effects", "{can_sprint: true, movement_speed_modifier: 1.0f, remove_effects: []}",
            "minecraft:use_remainder", "{id: \"minecraft:stick\", count: 1}",
            "minecraft:use_cooldown", "{seconds: 1.0f}",
            "minecraft:charged_projectiles", "[{id: \"minecraft:arrow\", count: 1}]",
            "minecraft:map_decorations", "{decorations: []}",
            "minecraft:lodestone_tracker", "{tracked: true, target: {dimension: \"minecraft:overworld\", pos: [0, 64, 0]}}"
    );
    private static final List<List<String>> BOOLEAN_PATH_SUFFIXES = List.of(
            List.of("components", "minecraft:food", "can_always_eat"),
            List.of("components", "minecraft:consumable", "has_consume_particles"),
            List.of("effects", "show_icon"),
            List.of("effects", "show_particles"),
            List.of("effects", "ambient"),
            List.of("unbreakable")
    );
    private static final List<List<String>> NUMERIC_PATH_SUFFIXES = List.of(
            List.of("components", "minecraft:food", "nutrition"),
            List.of("components", "minecraft:food", "saturation"),
            List.of("components", "minecraft:consumable", "consume_seconds"),
            List.of("components", "minecraft:use_cooldown", "seconds"),
            List.of("on_consume_effects", "probability"),
            List.of("effects", "duration"),
            List.of("effects", "amplifier"),
            List.of("attributemodifiers", "amount"),
            List.of("attributemodifiers", "operation"),
            List.of("enchantments", "lvl"),
            List.of("custompotioneffects", "amplifier"),
            List.of("custompotioneffects", "duration"),
            List.of("custom_potion_effects", "amplifier"),
            List.of("custom_potion_effects", "duration"),
            List.of("patterns", "color"),
            List.of("blockentitytag", "base"),
            List.of("fireworks", "flight")
    );
    private static final List<List<String>> NUMERIC_MODE_ONLY_PATH_SUFFIXES = List.of(
            List.of("custompotioneffects", "id")
    );
    private static final List<List<String>> STRING_PATH_SUFFIXES = List.of(
            List.of("components", "minecraft:consumable", "animation"),
            List.of("components", "minecraft:consumable", "sound"),
            List.of("on_consume_effects", "type"),
            List.of("on_consume_effects", "sound"),
            List.of("effects", "id"),
            List.of("attributemodifiers", "attributename"),
            List.of("attributemodifiers", "name"),
            List.of("attributemodifiers", "slot"),
            List.of("enchantments", "id"),
            List.of("custom_potion_effects", "id"),
            List.of("display", "name"),
            List.of("display", "lore"),
            List.of("patterns", "pattern")
    );
    private static final RawAutocompleteSchema SCHEMA = RawAutocompleteSchema.load();
    private static final int RUNTIME_PROBE_MAX_TEXT_LENGTH = 20000;
    private static final int RUNTIME_PROBE_CACHE_LIMIT = 256;
    private static final Object RUNTIME_PROBE_CACHE_LOCK = new Object();
    private static final Map<String, EnumSet<ValueMode>> RUNTIME_PROBE_CACHE = new LinkedHashMap<>(RUNTIME_PROBE_CACHE_LIMIT + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, EnumSet<ValueMode>> eldest) {
            return this.size() > RUNTIME_PROBE_CACHE_LIMIT;
        }
    };

    private RawAutocompleteUtil() {
    }

    public static AutocompleteResult suggest(String rawText, int caretIndex, RegistryAccess registryAccess) {
        RawAutocompleteIndex index = RawAutocompleteIndex.create(rawText);
        return suggest(rawText, caretIndex, registryAccess, index, "");
    }

    public static AutocompleteResult suggest(
            String rawText,
            int caretIndex,
            RegistryAccess registryAccess,
            String fallbackItemId
    ) {
        RawAutocompleteIndex index = RawAutocompleteIndex.create(rawText);
        return suggest(rawText, caretIndex, registryAccess, index, fallbackItemId);
    }

    public static AutocompleteResult suggest(
            String rawText,
            int caretIndex,
            RegistryAccess registryAccess,
            RawAutocompleteIndex index
    ) {
        return suggest(rawText, caretIndex, registryAccess, index, "");
    }

    public static AutocompleteResult suggest(
            String rawText,
            int caretIndex,
            RegistryAccess registryAccess,
            RawAutocompleteIndex index,
            String fallbackItemId
    ) {
        String text = Objects.requireNonNullElse(rawText, "");
        int cursor = Math.clamp(caretIndex, 0, text.length());
        RawAutocompleteIndex effectiveIndex = index == null || !index.matches(text)
                ? RawAutocompleteIndex.create(text)
                : index;

        boolean insideQuote = effectiveIndex.insideStringAt(cursor);
        int replaceStart = findTokenStart(text, cursor, insideQuote);
        String prefix = text.substring(replaceStart, cursor);
        String currentKey = effectiveIndex.lastObjectKeyAt(cursor);
        RawAutocompleteIndex.Context context = effectiveIndex.contextAt(cursor);
        String topLevelItemId = detectTopLevelItemId(text);
        String fallbackId = Objects.requireNonNullElse(fallbackItemId, "");
        if (topLevelItemId.isBlank() && !fallbackId.isBlank()) {
            topLevelItemId = fallbackId;
        }
        List<String> activeProfiles = inferItemProfiles(topLevelItemId, registryAccess);
        List<String> profileComponents = SCHEMA.componentsForProfiles(activeProfiles);
        boolean keyPosition = isLikelyObjectKeyPosition(text, cursor, insideQuote);
        keyPosition = refineKeyPosition(text, cursor, insideQuote, keyPosition);
        if (!insideQuote && !keyPosition && looksLikeObjectKeyOnCurrentLine(text, cursor)) {
            keyPosition = true;
        }
        String inferredLineKey = inferCurrentLineObjectKey(text, cursor);
        if (!keyPosition && !inferredLineKey.isBlank()) {
            currentKey = inferredLineKey;
        }
        Map<String, Suggestion> output = new HashMap<>();

        if (!shouldSuggestAtPosition(text, cursor, keyPosition, insideQuote, prefix)) {
            return AutocompleteResult.empty(cursor);
        }

        KeyCorrection correction = detectKeyCorrectionContext(text, cursor, keyPosition, insideQuote);
        if (correction != null) {
            UnaryOperator<String> keyInsert = insideQuote ? UnaryOperator.identity() : RawAutocompleteUtil::formatKeyInsert;
            List<String> contextualKeys = contextualKeyHints(context.containerKey(), context.containerPath());
            List<String> schemaObjectKeys = SCHEMA.objectKeyHints(context.containerKey());
            List<String> componentNbtFields = SCHEMA.componentNbtFieldsForProfiles(context.containerKey(), activeProfiles);
            List<String> dynamicKeyHints = dynamicKeyHints(context.containerKey(), context.containerPath(), registryAccess);
            List<String> mapIdKeyHints = mapIdKeyHints(context.containerKey(), context.containerPath(), registryAccess);
            List<String> seenContainerKeys = effectiveIndex.seenKeysForContainer(context.containerKey());
            if (!isKnownContainerKey(
                    correction.keyPrefix(),
                    contextualKeys,
                    schemaObjectKeys,
                    componentNbtFields,
                    dynamicKeyHints,
                    seenContainerKeys
            )) {
                Map<String, Suggestion> correctionOutput = new HashMap<>();
                addSuggestions(correctionOutput, contextualKeys, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addSuggestions(correctionOutput, schemaObjectKeys, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 1);
                addSuggestions(correctionOutput, componentNbtFields, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addSuggestions(correctionOutput, dynamicKeyHints, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addSuggestions(correctionOutput, mapIdKeyHints, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addSuggestions(correctionOutput, seenContainerKeys, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 1);
                addNearestSuggestions(correctionOutput, contextualKeys, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(correctionOutput, schemaObjectKeys, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 1);
                addNearestSuggestions(correctionOutput, componentNbtFields, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(correctionOutput, dynamicKeyHints, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(correctionOutput, mapIdKeyHints, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(correctionOutput, seenContainerKeys, correction.keyPrefix(), SuggestionKind.KEY, keyInsert, 1);
                suppressEchoTypedKeySuggestion(correctionOutput, correction.keyPrefix());
                List<Suggestion> correctionSuggestions = limit(
                        correctionOutput.values(),
                        text,
                        correction.replaceStart(),
                        correction.replaceEnd(),
                        registryAccess,
                        expectedModesForSlot(SlotType.OBJECT_KEY),
                        SlotType.OBJECT_KEY
                );
                if (!correctionSuggestions.isEmpty()) {
                    return new AutocompleteResult(
                            cursor,
                            correction.replaceStart(),
                            correction.replaceEnd(),
                            correctionSuggestions,
                            context.containerKey()
                    );
                }
            }
        }

        if (keyPosition) {
            UnaryOperator<String> keyInsert = insideQuote ? UnaryOperator.identity() : RawAutocompleteUtil::formatKeyInsert;
            List<String> contextualKeys = contextualKeyHints(context.containerKey(), context.containerPath());
            List<String> schemaObjectKeys = SCHEMA.objectKeyHints(context.containerKey());
            List<String> componentNbtFields = SCHEMA.componentNbtFieldsForProfiles(context.containerKey(), activeProfiles);
            List<String> dynamicKeyHints = dynamicKeyHints(context.containerKey(), context.containerPath(), registryAccess);
            List<String> mapIdKeyHints = mapIdKeyHints(context.containerKey(), context.containerPath(), registryAccess);
            boolean strictContextKeys = shouldUseStrictContextKeySuggestions(
                    context,
                    contextualKeys,
                    schemaObjectKeys,
                    componentNbtFields,
                    dynamicKeyHints,
                    profileComponents
            );
            if (context.inComponentsObject()) {
                addSuggestions(output, profileComponents, prefix, SuggestionKind.KEY, keyInsert, 0);
                if (!prefix.isBlank()) {
                    addSuggestions(output, SCHEMA.componentIds(), prefix, SuggestionKind.KEY, keyInsert, 1);
                    addSuggestions(output, RUNTIME.componentIds(registryAccess), prefix, SuggestionKind.KEY, keyInsert, 2);
                }
            } else if (context.inRootObject()) {
                addSuggestions(output, SCHEMA.topLevelKeys(), prefix, SuggestionKind.KEY, keyInsert, 0);
            }
            addSuggestions(output, contextualKeys, prefix, SuggestionKind.KEY, keyInsert, 0);
            addSuggestions(output, schemaObjectKeys, prefix, SuggestionKind.KEY, keyInsert, 1);
            addSuggestions(output, componentNbtFields, prefix, SuggestionKind.KEY, keyInsert, 0);
            addSuggestions(output, dynamicKeyHints, prefix, SuggestionKind.KEY, keyInsert, 0);
            addSuggestions(output, mapIdKeyHints, prefix, SuggestionKind.KEY, keyInsert, 0);
            if (!prefix.isBlank()) {
                addSuggestions(output, effectiveIndex.seenKeysForContainer(context.containerKey()), prefix, SuggestionKind.KEY, keyInsert, 1);
                if (!strictContextKeys) {
                    addSuggestions(output, SCHEMA.nbtKeyCandidates(), prefix, SuggestionKind.KEY, keyInsert, 4);
                    addSuggestions(output, effectiveIndex.seenKeys(), prefix, SuggestionKind.KEY, keyInsert, 2);
                }

                addNearestSuggestions(output, contextualKeys, prefix, SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(output, schemaObjectKeys, prefix, SuggestionKind.KEY, keyInsert, 1);
                addNearestSuggestions(output, componentNbtFields, prefix, SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(output, dynamicKeyHints, prefix, SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(output, mapIdKeyHints, prefix, SuggestionKind.KEY, keyInsert, 0);
                addNearestSuggestions(output, effectiveIndex.seenKeysForContainer(context.containerKey()), prefix, SuggestionKind.KEY, keyInsert, 1);
                if (!strictContextKeys) {
                    addNearestSuggestions(output, effectiveIndex.seenKeys(), prefix, SuggestionKind.KEY, keyInsert, 2);
                }
                if (context.inComponentsObject()) {
                    addNearestSuggestions(output, profileComponents, prefix, SuggestionKind.KEY, keyInsert, 0);
                    addNearestSuggestions(output, SCHEMA.componentIds(), prefix, SuggestionKind.KEY, keyInsert, 1);
                }
            }
            if (prefix.isBlank()) {
                suppressAlreadyPresentContainerKeys(output, effectiveIndex.seenKeysForContainer(context.containerKey()));
            }
            suppressEchoTypedKeySuggestion(output, prefix);
            if (!strictContextKeys) {
                addKeySnippetSuggestions(output, text, cursor, insideQuote, context.containerKey(), prefix);
            }
            addKeyStructuralSuggestions(output, text, cursor, insideQuote, prefix);
            List<Suggestion> keySuggestions = limit(
                    output.values(),
                    text,
                    replaceStart,
                    cursor,
                    registryAccess,
                    expectedModesForSlot(SlotType.OBJECT_KEY),
                    SlotType.OBJECT_KEY
            );
            return new AutocompleteResult(
                    cursor,
                    replaceStart,
                    cursor,
                    keySuggestions,
                    currentKey
            );
        }

        SlotType slotType = classifySlotType(
                currentKey,
                context.containerKey(),
                context.containerPath(),
                insideQuote,
                activeProfiles
        );
        EnumSet<ValueMode> expectedModes = expectedModesForSlot(slotType);
        if (slotType == SlotType.VALUE_UNKNOWN) {
            expectedModes = expectedValueModes(currentKey, context.containerKey(), context.containerPath(), prefix, insideQuote);
            expectedModes = refineExpectedModesWithRuntimeProbe(
                    text,
                    replaceStart,
                    cursor,
                    registryAccess,
                    expectedModes,
                    insideQuote
            );
        }
        List<String> schemaValueHints = mergeUnique(
                schemaValueHints(currentKey, context.containerKey()),
                contextualValueHints(currentKey, context.containerPath())
        );
        List<String> registryHints = registryHintsForSlot(
                slotType,
                currentKey,
                context.containerKey(),
                context.containerPath(),
                registryAccess
        );
        List<String> typedSchemaValueHints = filterValuesByModes(schemaValueHints, expectedModes);
        List<String> typedRegistryHints = filterValuesByModes(registryHints, expectedModes);
        BooleanInsertStyle booleanInsertStyle = detectBooleanInsertStyle(text, replaceStart, context.containerPath(), currentKey);
        switch (slotType) {
            case VALUE_INT -> {
                typedSchemaValueHints = filterNumericHintsForCurrentKey(typedSchemaValueHints, currentKey, context.containerPath());
                typedRegistryHints = filterNumericHintsForCurrentKey(typedRegistryHints, currentKey, context.containerPath());
            }
            case VALUE_FLOAT -> {
                typedSchemaValueHints = filterFloatHints(typedSchemaValueHints);
                typedRegistryHints = filterFloatHints(typedRegistryHints);
            }
            default -> {
                if (expectedModes.contains(ValueMode.NUMBER)) {
                    typedSchemaValueHints = filterNumericHintsForCurrentKey(typedSchemaValueHints, currentKey, context.containerPath());
                    typedRegistryHints = filterNumericHintsForCurrentKey(typedRegistryHints, currentKey, context.containerPath());
                }
            }
        }

        if (insideQuote) {
            if ("components".equalsIgnoreCase(context.containerKey())) {
                if (!prefix.isBlank()) {
                    addSuggestions(output, profileComponents, prefix, SuggestionKind.VALUE, UnaryOperator.identity(), 0);
                    addSuggestions(output, SCHEMA.componentIds(), prefix, SuggestionKind.VALUE, UnaryOperator.identity(), 1);
                    addSuggestions(output, RUNTIME.componentIds(registryAccess), prefix, SuggestionKind.VALUE, UnaryOperator.identity(), 2);
                }
            } else {
                if (!prefix.isBlank()) {
                    addSuggestions(output, typedRegistryHints, prefix, SuggestionKind.VALUE, UnaryOperator.identity(), 0);
                }
            }
            addSuggestions(output, typedSchemaValueHints, prefix, SuggestionKind.VALUE, UnaryOperator.identity(), 1);
        } else {
            if (expectedModes.contains(ValueMode.STRING)) {
                if (!prefix.isBlank()) {
                    addSuggestions(output, typedRegistryHints, prefix, SuggestionKind.VALUE, RawAutocompleteUtil::formatValueInsert, 0);
                }
                addSuggestions(output, typedSchemaValueHints, prefix, SuggestionKind.VALUE, RawAutocompleteUtil::formatValueInsert, 1);
                if (slotType == SlotType.VALUE_UNKNOWN && prefix.isBlank()) {
                    addSuggestions(output, STRUCTURAL_VALUE_TEMPLATES, prefix, SuggestionKind.STRUCTURAL, UnaryOperator.identity(), 2);
                }
            }
            if (expectedModes.contains(ValueMode.BOOLEAN)) {
                addBooleanSuggestions(output, prefix, booleanInsertStyle);
            }
            if (expectedModes.contains(ValueMode.NUMBER)) {
                List<String> numberSuggestions = switch (slotType) {
                    case VALUE_INT -> INTEGER_NUMBER_VALUES;
                    case VALUE_FLOAT -> FLOAT_NUMBER_VALUES;
                    default -> numberSuggestionsForCurrentKey(currentKey, context.containerPath());
                };
                addSuggestions(output, numberSuggestions, prefix, SuggestionKind.VALUE, UnaryOperator.identity(), 0);
            }
            if (expectedModes.contains(ValueMode.LITERAL)) {
                addSuggestions(output, LITERAL_VALUES, prefix, SuggestionKind.LITERAL, UnaryOperator.identity(), 0);
            }
        }

        if (!prefix.isBlank()) {
            addNearestSuggestions(output, typedRegistryHints, prefix, SuggestionKind.VALUE, insideQuote ? UnaryOperator.identity() : RawAutocompleteUtil::formatValueInsert, 0);
            addNearestSuggestions(output, typedSchemaValueHints, prefix, SuggestionKind.VALUE, insideQuote ? UnaryOperator.identity() : RawAutocompleteUtil::formatValueInsert, 1);
            if ("components".equalsIgnoreCase(context.containerKey()) && expectedModes.contains(ValueMode.STRING)) {
                addNearestSuggestions(output, profileComponents, prefix, SuggestionKind.VALUE, insideQuote ? UnaryOperator.identity() : RawAutocompleteUtil::formatValueInsert, 0);
                addNearestSuggestions(output, SCHEMA.componentIds(), prefix, SuggestionKind.VALUE, insideQuote ? UnaryOperator.identity() : RawAutocompleteUtil::formatValueInsert, 1);
            }
        }
        addValueStructuralSuggestions(output, text, cursor, insideQuote, prefix);
        if (slotType == SlotType.VALUE_UNKNOWN && !isPrimitiveOnlyMode(expectedModes)) {
            addValueSnippetSuggestions(output, insideQuote, currentKey, prefix);
        }

        List<Suggestion> valueSuggestions = limit(
                output.values(),
                text,
                replaceStart,
                cursor,
                registryAccess,
                expectedModes,
                slotType
        );
        return new AutocompleteResult(
                cursor,
                replaceStart,
                cursor,
                valueSuggestions,
                currentKey
        );
    }

    private static boolean isPrimitiveOnlyMode(EnumSet<ValueMode> expectedModes) {
        return expectedModes != null
                && !expectedModes.isEmpty()
                && !expectedModes.contains(ValueMode.NONE)
                && !expectedModes.contains(ValueMode.STRING);
    }

    private static List<String> numberSuggestionsForCurrentKey(String currentKey, String containerPath) {
        String key = Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT);
        String path = buildFullPath(containerPath, key);
        if (isLikelyIntegerNumberKey(key, path)) {
            return INTEGER_NUMBER_VALUES;
        }
        return NUMBER_VALUES;
    }

    private static List<String> filterNumericHintsForCurrentKey(List<String> values, String currentKey, String containerPath) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        String key = Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT);
        String path = buildFullPath(containerPath, key);
        if (!isLikelyIntegerNumberKey(key, path)) {
            return values;
        }
        return integerNumericHints(values);
    }

    private static List<String> integerNumericHints(List<String> values) {
        List<String> filtered = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.matches("^-?\\d+$")) {
                filtered.add(value);
                continue;
            }
            if (normalized.matches("^-?\\d+[bsil]$")) {
                filtered.add(value);
            }
        }
        return filtered;
    }

    private static boolean isLikelyIntegerNumberKey(String key, String path) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (pathEndsWith(path, "components", "minecraft:food", "nutrition")
                || pathEndsWith(path, "on_consume_effects", "probability")
                || pathEndsWith(path, "components", "minecraft:food", "saturation")
                || pathEndsWith(path, "components", "minecraft:consumable", "consume_seconds")) {
            return false;
        }
        return key.contains("count")
                || key.contains("delay")
                || key.contains("range")
                || key.contains("duration")
                || key.contains("level")
                || key.contains("amplifier")
                || key.contains("nutrition")
                || key.contains("repair")
                || key.startsWith("max")
                || key.startsWith("min");
    }

    private static boolean refineKeyPosition(String text, int cursor, boolean insideQuote, boolean currentGuess) {
        if (insideQuote) {
            Boolean keyInString = classifyActiveStringAsKey(text, cursor);
            if (keyInString != null) {
                return keyInString;
            }
            return currentGuess;
        }
        if (isLikelyValuePositionOutsideString(text, cursor)) {
            return false;
        }
        return currentGuess;
    }

    private static Boolean classifyActiveStringAsKey(String text, int cursor) {
        int quoteStart = text.lastIndexOf('"', Math.max(0, cursor - 1));
        if (quoteStart < 0) {
            return null;
        }
        int previous = skipWhitespaceBackward(text, quoteStart - 1);
        if (previous < 0) {
            return null;
        }
        char beforeQuote = text.charAt(previous);
        return switch (beforeQuote) {
            case ':' -> false;
            case '{', ',' -> true;
            default -> null;
        };
    }

    private static boolean isLikelyValuePositionOutsideString(String text, int cursor) {
        int current = skipWhitespaceBackward(text, cursor - 1);
        if (current < 0) {
            return false;
        }

        char value = text.charAt(current);
        switch (value) {
            case ':':
                return true;
            case '{', '[', ',':
                return false;
            case '"', ']', '}':
                break;
            default:
                if (!isTokenCharacter(value) && !Character.isDigit(value)) {
                    return false;
                }
                break;
        }
        while (current >= 0) {
            char candidate = text.charAt(current);
            if (candidate == '"' || isTokenCharacter(candidate) || Character.isDigit(candidate)) {
                current--;
                continue;
            }
            break;
        }
        current = skipWhitespaceBackward(text, current);
        return current >= 0 && text.charAt(current) == ':';
    }

    private static List<Suggestion> limit(
            Collection<Suggestion> source,
            String text,
            int replaceStart,
            int replaceEnd,
            RegistryAccess registryAccess,
            EnumSet<ValueMode> expectedModes,
            SlotType slotType
    ) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        Comparator<Suggestion> baseComparator = Comparator
                .comparing((Suggestion suggestion) -> suggestion.kind().priority())
                .thenComparingInt(Suggestion::contextRank)
                .thenComparingInt(Suggestion::matchRank)
                .thenComparingInt(suggestion -> suggestion.insertText().length())
                .thenComparing(Suggestion::insertText);

        List<Suggestion> preliminary = source.stream()
                .sorted(baseComparator)
                .limit(PARSE_FILTER_CANDIDATE_LIMIT)
                .toList();

        boolean onlyKeySuggestions = preliminary.stream().allMatch(suggestion -> suggestion.kind() == SuggestionKind.KEY);

        boolean canParseFilter = registryAccess != null
                && text != null
                && text.length() <= PARSE_FILTER_MAX_TEXT_LENGTH;

        List<ParseFilteredSuggestion> parseAccepted;
        if (onlyKeySuggestions) {
            parseAccepted = preliminary.stream()
                    .map(candidate -> new ParseFilteredSuggestion(candidate, 2, Integer.MAX_VALUE))
                    .toList();
        } else if (canParseFilter) {
            parseAccepted = parseErrorGuidedFilter(preliminary, text, replaceStart, replaceEnd, registryAccess, baseComparator);
            if (parseAccepted.isEmpty()) {
                parseAccepted = preliminary.stream()
                        .map(candidate -> new ParseFilteredSuggestion(candidate, 2, Integer.MAX_VALUE))
                        .toList();
            }
        } else {
            parseAccepted = preliminary.stream()
                    .map(candidate -> new ParseFilteredSuggestion(candidate, 2, Integer.MAX_VALUE))
                    .toList();
        }

        List<Suggestion> ranked = rankSuggestions(
                parseAccepted,
                text,
                replaceStart,
                expectedModes,
                slotType,
                baseComparator
        );
        return ranked.stream()
                .limit(MAX_RESULTS)
                .toList();
    }

    private static List<ParseFilteredSuggestion> parseErrorGuidedFilter(
            List<Suggestion> candidates,
            String text,
            int replaceStart,
            int replaceEnd,
            RegistryAccess registryAccess,
            Comparator<Suggestion> baseComparator
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeEnd = Math.clamp(replaceEnd, safeStart, text.length());

        RawItemDataUtil.ParseResult baselineResult = RawItemDataUtil.parse(text, registryAccess);
        int baselineCursor = parseErrorCursor(text, baselineResult);

        List<ParseFilteredSuggestion> accepted = new ArrayList<>();
        for (Suggestion candidate : candidates) {
            String candidateText = text.substring(0, safeStart)
                    + candidate.insertText()
                    + text.substring(safeEnd);
            RawItemDataUtil.ParseResult parsed = RawItemDataUtil.parse(candidateText, registryAccess);
            if (parsed.success()) {
                accepted.add(new ParseFilteredSuggestion(candidate, 0, 0));
                continue;
            }

            int errorCursor = parseErrorCursor(candidateText, parsed);
            if (errorCursor < 0) {
                continue;
            }

            int targetCursor = safeStart + candidate.insertText().length();
            int distanceToCaret = Math.abs(errorCursor - targetCursor);
            boolean nearCaret = distanceToCaret <= PARSE_FILTER_NEAR_CARET_WINDOW;
            boolean movedForward = baselineCursor < 0 ? nearCaret : errorCursor > baselineCursor;
            if (movedForward && nearCaret) {
                accepted.add(new ParseFilteredSuggestion(candidate, 1, distanceToCaret));
            }
        }

        if (accepted.isEmpty()) {
            return List.of();
        }

        accepted.sort(Comparator
                .comparingInt(ParseFilteredSuggestion::parseRank)
                .thenComparingInt(ParseFilteredSuggestion::distanceToTarget)
                .thenComparing(ParseFilteredSuggestion::suggestion, baseComparator));
        return accepted;
    }

    private static List<Suggestion> rankSuggestions(
            List<ParseFilteredSuggestion> candidates,
            String text,
            int replaceStart,
            EnumSet<ValueMode> expectedModes,
            SlotType slotType,
            Comparator<Suggestion> fallbackComparator
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<WeightedSuggestion> weighted = new ArrayList<>(candidates.size());
        for (ParseFilteredSuggestion candidate : candidates) {
            Suggestion suggestion = candidate.suggestion();
            int score = weightedScoreForSuggestion(
                    suggestion,
                    candidate,
                    text,
                    replaceStart,
                    expectedModes,
                    slotType
            );
            weighted.add(new WeightedSuggestion(suggestion, score));
        }

        weighted.sort(Comparator
                .comparingInt(WeightedSuggestion::score).reversed()
                .thenComparing(WeightedSuggestion::suggestion, fallbackComparator));

        return weighted.stream()
                .map(WeightedSuggestion::suggestion)
                .toList();
    }

    private static int weightedScoreForSuggestion(
            Suggestion suggestion,
            ParseFilteredSuggestion parseCandidate,
            String text,
            int replaceStart,
            EnumSet<ValueMode> expectedModes,
            SlotType slotType
    ) {
        int typeMatch = typeMatchScore(suggestion, expectedModes, slotType);
        int parserScore = parserScore(parseCandidate);
        int prefixScore = Math.max(0, 4 - suggestion.matchRank());
        int recencyScore = recencyScore(text, replaceStart, suggestion);
        int schemaScore = Math.max(0, 5 - suggestion.contextRank());
        return (typeMatch * WEIGHT_TYPE_MATCH)
                + (parserScore * WEIGHT_PARSER)
                + (prefixScore * WEIGHT_PREFIX)
                + (recencyScore * WEIGHT_RECENCY)
                + (schemaScore * WEIGHT_SCHEMA);
    }

    private static int parserScore(ParseFilteredSuggestion parseCandidate) {
        return switch (parseCandidate.parseRank()) {
            case 0 -> 3;
            case 1 -> 2;
            default -> 1;
        };
    }

    private static int typeMatchScore(Suggestion suggestion, EnumSet<ValueMode> expectedModes, SlotType slotType) {
        if (slotType == SlotType.OBJECT_KEY) {
            return suggestion.kind() == SuggestionKind.KEY ? 3 : 0;
        }

        ValueMode mode = classifyValueMode(suggestion.insertText());
        if (expectedModes != null && !expectedModes.isEmpty() && !expectedModes.contains(ValueMode.NONE)) {
            if (expectedModes.contains(mode)) {
                return 3;
            }
            if (expectedModes.contains(ValueMode.NUMBER) && mode == ValueMode.STRING && suggestion.insertText().matches("^-?\\d+(?:\\.\\d+)?[bBsSlLfFdD]?$")) {
                return 2;
            }
            return 0;
        }

        return switch (suggestion.kind()) {
            case VALUE, LITERAL -> 2;
            case KEY -> 1;
            case STRUCTURAL, SNIPPET -> 0;
        };
    }

    private static int recencyScore(String text, int replaceStart, Suggestion suggestion) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int safeStart = Math.clamp(replaceStart, 0, text.length());
        String haystack = text.substring(0, safeStart).toLowerCase(Locale.ROOT);
        String needle = (suggestion.label() == null || suggestion.label().isBlank()
                ? suggestion.insertText()
                : suggestion.label()).toLowerCase(Locale.ROOT);
        if (needle.isBlank()) {
            return 0;
        }
        int index = haystack.lastIndexOf(needle);
        if (index < 0) {
            return 0;
        }
        int distance = safeStart - index;
        if (distance <= 64) return 4;
        if (distance <= 160) return 3;
        if (distance <= 320) return 2;
        return 1;
    }

    private static int parseErrorCursor(String text, RawItemDataUtil.ParseResult parsed) {
        if (parsed == null || parsed.success() || !parsed.hasPosition()) {
            return -1;
        }
        return cursorFromLineColumn(text, parsed.line(), parsed.column());
    }

    private static int cursorFromLineColumn(String text, int line, int column) {
        if (text == null || line <= 0 || column <= 0) {
            return -1;
        }

        int currentLine = 1;
        int currentColumn = 1;
        for (int index = 0; index <= text.length(); index++) {
            if (currentLine == line && currentColumn == column) {
                return index;
            }
            if (index == text.length()) {
                break;
            }
            char value = text.charAt(index);
            if (value == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }
        return -1;
    }

    private static void addBooleanSuggestions(
            Map<String, Suggestion> output,
            String prefix,
            BooleanInsertStyle style
    ) {
        switch (style) {
            case NBT_BYTE -> {
                addMappedSuggestion(output, "true", "1b", prefix);
                addMappedSuggestion(output, "false", "0b", prefix);
            }
            case TEXT -> {
                addMappedSuggestion(output, "true", "true", prefix);
                addMappedSuggestion(output, "false", "false", prefix);
            }
        }
    }

    private static void addMappedSuggestion(
            Map<String, Suggestion> output,
            String label,
            String insertText,
            String prefix
    ) {
        int labelRank = matchRank(label, prefix);
        int insertRank = matchRank(insertText, prefix);
        int rank;
        if (labelRank < 0 && insertRank < 0) {
            return;
        }
        if (labelRank < 0) {
            rank = insertRank;
        } else if (insertRank < 0) {
            rank = labelRank;
        } else {
            rank = Math.min(labelRank, insertRank);
        }
        upsertSuggestion(output, new Suggestion(label, insertText, SuggestionKind.LITERAL, rank, 0));
    }

    private static BooleanInsertStyle detectBooleanInsertStyle(
            String text,
            int replaceStart,
            String containerPath,
            String currentKey
    ) {
        String path = buildFullPath(containerPath, Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT));
        if (isBooleanPath(path)) {
            return BooleanInsertStyle.NBT_BYTE;
        }
        if (text == null || text.isBlank()) {
            return BooleanInsertStyle.NBT_BYTE;
        }

        int safeStart = Math.clamp(replaceStart, 0, text.length());
        String sample = text.substring(Math.max(0, safeStart - 3000), safeStart).toLowerCase(Locale.ROOT);
        boolean hasByteBool = sample.matches("(?s).*\\b(?:0b|1b)\\b.*");
        boolean hasTextBool = sample.matches("(?s).*\\b(?:true|false)\\b.*");
        if (hasByteBool && !hasTextBool) {
            return BooleanInsertStyle.NBT_BYTE;
        }
        if (hasTextBool && !hasByteBool) {
            return BooleanInsertStyle.TEXT;
        }
        return BooleanInsertStyle.NBT_BYTE;
    }

    private static void addSuggestions(
            Map<String, Suggestion> output,
            List<String> values,
            String prefix,
            SuggestionKind kind
    ) {
        addSuggestions(output, values, prefix, kind, UnaryOperator.identity(), 3);
    }

    private static void addSuggestions(
            Map<String, Suggestion> output,
            List<String> values,
            String prefix,
            SuggestionKind kind,
            UnaryOperator<String> insertMapper
    ) {
        addSuggestions(output, values, prefix, kind, insertMapper, 3);
    }

    private static void addSuggestions(
            Map<String, Suggestion> output,
            List<String> values,
            String prefix,
            SuggestionKind kind,
            UnaryOperator<String> insertMapper,
            int contextRank
    ) {
        if (values == null || values.isEmpty()) {
            return;
        }

        boolean blankPrefix = prefix.isBlank();
        if (blankPrefix && kind == SuggestionKind.VALUE && values.size() > 96) {
            return;
        }

        if (blankPrefix && values.size() > 256) {
            int max = 64;
            for (int index = 0; index < max; index++) {
                String value = values.get(index);
                String insertText = insertMapper.apply(value);
                upsertSuggestion(output, new Suggestion(value, insertText, kind, 3, contextRank));
            }
            return;
        }

        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        boolean strictPrefix = values.size() > 2500 && lowerPrefix.length() < 2;
        int accepted = 0;
        for (String value : values) {
            if (strictPrefix && !lowerPrefix.isBlank() && !value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                continue;
            }

            int rank = matchRank(value, prefix);
            if (rank < 0) {
                continue;
            }

            String insertText = insertMapper.apply(value);
            upsertSuggestion(output, new Suggestion(value, insertText, kind, rank, contextRank));

            accepted++;
            if (values.size() > 2500 && accepted >= 96) {
                break;
            }
        }
    }

    private static void addNearestSuggestions(
            Map<String, Suggestion> output,
            List<String> values,
            String prefix,
            SuggestionKind kind,
            UnaryOperator<String> insertMapper,
            int contextRank
    ) {
        if (values == null || values.isEmpty() || prefix.isBlank()) {
            return;
        }

        String normalizedPrefix = normalizeFuzzy(prefix);
        if (normalizedPrefix.length() < 3) {
            return;
        }
        if (normalizedPrefix.length() > 12) {
            return;
        }
        if (values.size() > 4000) {
            return;
        }
        char firstPrefixChar = normalizedPrefix.charAt(0);

        List<FuzzyCandidate> nearest = new ArrayList<>(MAX_NEAREST_CANDIDATES);
        for (String value : values) {
            String normalizedValue = normalizeFuzzy(value);
            if (normalizedValue.isEmpty() || normalizedValue.charAt(0) != firstPrefixChar) {
                continue;
            }
            int distance = boundedLevenshtein(normalizedPrefix, normalizedValue);
            if (distance < 0) {
                continue;
            }
            addNearestCandidate(nearest, new FuzzyCandidate(value, distance));
        }

        for (FuzzyCandidate candidate : nearest) {
            String insertText = insertMapper.apply(candidate.value());
            int rank = 3 + candidate.distance();
            upsertSuggestion(output, new Suggestion(candidate.value(), insertText, kind, rank, contextRank));
        }
    }

    private static void addKeySnippetSuggestions(
            Map<String, Suggestion> output,
            String text,
            int cursor,
            boolean insideQuote,
            String containerKey,
            String prefix
    ) {
        if (insideQuote) {
            return;
        }
        if (!prefix.isBlank()) {
            return;
        }

        int previous = skipWhitespaceBackward(text, cursor - 1);
        if (previous < 0) {
            upsertSuggestion(output, new Suggestion("\"components\": {}", "\"components\": {}", SuggestionKind.SNIPPET, 0, 0));
            upsertSuggestion(output, new Suggestion("\"id\": \"minecraft:stone\"", "\"id\": \"minecraft:stone\"", SuggestionKind.SNIPPET, 1, 0));
            return;
        }
        char previousChar = text.charAt(previous);
        if (previousChar == '{' || previousChar == ',') {
            upsertSuggestion(output, new Suggestion("\"id\": \"minecraft:stone\"", "\"id\": \"minecraft:stone\"", SuggestionKind.SNIPPET, 1, 1));
            if (Objects.requireNonNullElse(containerKey, "").isBlank()) {
                upsertSuggestion(output, new Suggestion("\"components\": {}", "\"components\": {}", SuggestionKind.SNIPPET, 0, 0));
            }
        }
    }

    private static void addValueSnippetSuggestions(
            Map<String, Suggestion> output,
            boolean insideQuote,
            String currentKey,
            String prefix
    ) {
        if (insideQuote) {
            return;
        }
        if (Objects.requireNonNullElse(currentKey, "").isBlank()) {
            return;
        }

        String normalizedKey = currentKey.toLowerCase(Locale.ROOT);
        String snippet = Objects.requireNonNullElse(VALUE_SNIPPETS.get(normalizedKey), "");
        if (snippet.isEmpty()) {
            return;
        }

        if (!prefix.isBlank()) {
            String normalizedPrefix = normalizeFuzzy(prefix);
            if (!normalizeFuzzy(snippet).startsWith(normalizedPrefix) && !normalizeFuzzy(currentKey).startsWith(normalizedPrefix)) {
                return;
            }
        }

        String label = currentKey + " template";
        upsertSuggestion(output, new Suggestion(label, snippet, SuggestionKind.SNIPPET, 0, 0));
    }

    private static String normalizeFuzzy(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder normalized = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toLowerCase(value.charAt(index));
            if (Character.isLetterOrDigit(current) || current == '_' || current == '-' || current == ':' || current == '.' || current == '/') {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }

    private static int boundedLevenshtein(String left, String right) {
        if (left.equals(right)) {
            return 0;
        }
        if (left.isBlank()) {
            return right.length() <= MAX_FUZZY_DISTANCE ? right.length() : -1;
        }
        if (right.isBlank()) {
            return left.length() <= MAX_FUZZY_DISTANCE ? left.length() : -1;
        }
        if (Math.abs(left.length() - right.length()) > MAX_FUZZY_DISTANCE) {
            return -1;
        }

        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int col = 0; col <= right.length(); col++) {
            previous[col] = col;
        }

        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            int rowBest = current[0];
            char leftChar = left.charAt(row - 1);
            for (int col = 1; col <= right.length(); col++) {
                int cost = leftChar == right.charAt(col - 1) ? 0 : 1;
                int deletion = previous[col] + 1;
                int insertion = current[col - 1] + 1;
                int substitution = previous[col - 1] + cost;
                int distance = Math.min(Math.min(deletion, insertion), substitution);
                current[col] = distance;
                if (distance < rowBest) {
                    rowBest = distance;
                }
            }
            if (rowBest > MAX_FUZZY_DISTANCE) {
                return -1;
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        int result = previous[right.length()];
        return result <= MAX_FUZZY_DISTANCE ? result : -1;
    }

    private static void addNearestCandidate(List<FuzzyCandidate> nearest, FuzzyCandidate candidate) {
        for (FuzzyCandidate existing : nearest) {
            if (existing.value().equals(candidate.value())) {
                return;
            }
        }

        nearest.add(candidate);
        nearest.sort(Comparator
                .comparingInt(FuzzyCandidate::distance)
                .thenComparingInt(existing -> existing.value().length())
                .thenComparing(FuzzyCandidate::value));
        if (nearest.size() > MAX_NEAREST_CANDIDATES) {
            nearest.removeLast();
        }
    }

    private static void upsertSuggestion(Map<String, Suggestion> output, Suggestion candidate) {
        String dedupeKey = candidate.kind().name() + "|" + candidate.insertText();
        Suggestion existing = output.get(dedupeKey);
        if (existing == null || compareQuality(candidate, existing) < 0) {
            output.put(dedupeKey, candidate);
        }
    }

    private static int compareQuality(Suggestion left, Suggestion right) {
        int kindCompare = Integer.compare(left.kind().priority(), right.kind().priority());
        if (kindCompare != 0) {
            return kindCompare;
        }
        int contextCompare = Integer.compare(left.contextRank(), right.contextRank());
        if (contextCompare != 0) {
            return contextCompare;
        }
        int rankCompare = Integer.compare(left.matchRank(), right.matchRank());
        if (rankCompare != 0) {
            return rankCompare;
        }
        int lengthCompare = Integer.compare(left.insertText().length(), right.insertText().length());
        if (lengthCompare != 0) {
            return lengthCompare;
        }
        return left.insertText().compareTo(right.insertText());
    }

    private static void suppressAlreadyPresentContainerKeys(
            Map<String, Suggestion> output,
            List<String> seenKeysForContainer
    ) {
        if (output == null || output.isEmpty() || seenKeysForContainer == null || seenKeysForContainer.isEmpty()) {
            return;
        }

        Set<String> seenNormalized = new HashSet<>();
        for (String key : seenKeysForContainer) {
            if (key == null || key.isBlank()) {
                continue;
            }
            seenNormalized.add(key.toLowerCase(Locale.ROOT));
        }
        if (seenNormalized.isEmpty()) {
            return;
        }

        output.entrySet().removeIf(entry -> {
            Suggestion suggestion = entry.getValue();
            if (suggestion.kind() != SuggestionKind.KEY) {
                return false;
            }
            String normalized = normalizeSuggestedKey(suggestion.insertText());
            return !normalized.isBlank() && seenNormalized.contains(normalized);
        });
    }

    private static String normalizeSuggestedKey(String insertText) {
        if (insertText == null || insertText.isBlank()) {
            return "";
        }
        String value = insertText.trim();
        if (value.endsWith(":")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            value = value.substring(1, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static void suppressEchoTypedKeySuggestion(Map<String, Suggestion> output, String typedPrefix) {
        if (output == null || output.isEmpty() || typedPrefix == null || typedPrefix.isBlank()) {
            return;
        }

        String normalizedTyped = normalizeSuggestedKey(typedPrefix);
        if (normalizedTyped.isBlank()) {
            return;
        }

        boolean hasLongerPrefixCandidate = output.values().stream()
                .filter(suggestion -> suggestion.kind() == SuggestionKind.KEY)
                .map(Suggestion::insertText)
                .map(RawAutocompleteUtil::normalizeSuggestedKey)
                .anyMatch(normalized -> normalized.startsWith(normalizedTyped) && normalized.length() > normalizedTyped.length());
        if (!hasLongerPrefixCandidate) {
            return;
        }

        output.entrySet().removeIf(entry -> {
            Suggestion suggestion = entry.getValue();
            if (suggestion.kind() != SuggestionKind.KEY) {
                return false;
            }
            String normalizedInsert = normalizeSuggestedKey(suggestion.insertText());
            String normalizedLabel = normalizeSuggestedKey(suggestion.label());
            return normalizedInsert.equals(normalizedTyped) || normalizedLabel.equals(normalizedTyped);
        });
    }

    private static int matchRank(String value, String prefix) {
        if (prefix.isBlank()) {
            return 3;
        }

        String lowerValue = value.toLowerCase(Locale.ROOT);
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowerValue.equals(lowerPrefix)) {
            return 0;
        }
        if (lowerValue.startsWith(lowerPrefix)) {
            return 1;
        }
        int namespaceRank = namespaceLocalMatchRank(lowerValue, lowerPrefix);
        if (namespaceRank >= 0) {
            return namespaceRank;
        }
        if (lowerPrefix.length() >= 2 && lowerValue.contains(lowerPrefix)) {
            return 2;
        }
        return -1;
    }

    private static int namespaceLocalMatchRank(String lowerValue, String lowerPrefix) {
        int valueSeparator = lowerValue.indexOf(':');
        if (valueSeparator < 0) {
            return -1;
        }
        String localValue = lowerValue.substring(valueSeparator + 1);
        String localPrefix = lowerPrefix;
        int prefixSeparator = lowerPrefix.indexOf(':');
        if (prefixSeparator >= 0 && prefixSeparator + 1 < lowerPrefix.length()) {
            localPrefix = lowerPrefix.substring(prefixSeparator + 1);
        }
        if (localPrefix.isBlank()) {
            return -1;
        }
        if (localValue.equals(localPrefix)) {
            return 0;
        }
        if (localValue.startsWith(localPrefix)) {
            return 1;
        }
        if (localPrefix.length() >= 2 && localValue.contains(localPrefix)) {
            return 2;
        }
        return -1;
    }

    private static boolean shouldSuggestAtPosition(
            String text,
            int cursor,
            boolean keyPosition,
            boolean insideQuote,
            String prefix
    ) {
        if (insideQuote) {
            return true;
        }
        if (!Objects.requireNonNullElse(prefix, "").isBlank()) {
            return true;
        }

        int previous = skipWhitespaceBackwardSameLine(text, cursor - 1);
        if (previous < 0) {
            previous = skipWhitespaceBackward(text, cursor - 1);
        }
        if (previous < 0) {
            return true;
        }
        char previousChar = text.charAt(previous);
        if (keyPosition) {
            return switch (previousChar) {
                case '{', ',', '"' -> true;
                default -> isTokenCharacter(previousChar) || Character.isDigit(previousChar);
            };
        }
        return switch (previousChar) {
            case '{', '[', ':', ',', '"', ']', '}' -> true;
            default -> isTokenCharacter(previousChar) || Character.isDigit(previousChar);
        };
    }

    private static void addKeyStructuralSuggestions(
            Map<String, Suggestion> output,
            String text,
            int cursor,
            boolean insideQuote,
            String prefix
    ) {
        if (insideQuote) {
            return;
        }
        if (prefix.isBlank()) {
            upsertSuggestion(output, new Suggestion("\"\":", "\"\": ", SuggestionKind.STRUCTURAL, 0, 0));
            upsertSuggestion(output, new Suggestion("\"\"", "\"\"", SuggestionKind.STRUCTURAL, 1, 0));
        } else {
            int nextSameLine = skipWhitespaceForwardSameLine(text, cursor);
            if (nextSameLine < 0 || text.charAt(nextSameLine) != ':') {
                upsertSuggestion(output, new Suggestion(":", ": ", SuggestionKind.STRUCTURAL, 0, 1));
            }
        }
    }

    private static void addValueStructuralSuggestions(
            Map<String, Suggestion> output,
            String text,
            int cursor,
            boolean insideQuote,
            String prefix
    ) {
        if (insideQuote) {
            return;
        }

        int previous = skipWhitespaceBackward(text, cursor - 1);
        char previousChar = previous < 0 ? '\0' : text.charAt(previous);
        int next = skipWhitespaceForward(text, cursor);
        char nextChar = next >= text.length() ? '\0' : text.charAt(next);

        if (previousChar == ':' && prefix.isBlank()) {
            upsertSuggestion(output, new Suggestion("\"\"", "\"\"", SuggestionKind.STRUCTURAL, 0, 0));
            upsertSuggestion(output, new Suggestion("{}", "{}", SuggestionKind.STRUCTURAL, 1, 0));
            upsertSuggestion(output, new Suggestion("[]", "[]", SuggestionKind.STRUCTURAL, 1, 0));
        }

        if (previousChar == '"' && nextChar != ':' && prefix.isBlank()) {
            upsertSuggestion(output, new Suggestion(":", ": ", SuggestionKind.STRUCTURAL, 0, 0));
        }

        if (previousChar == '"' || Character.isDigit(previousChar) || previousChar == '}' || previousChar == ']') {
            if (nextChar != ',') {
                upsertSuggestion(output, new Suggestion(",", ",", SuggestionKind.STRUCTURAL, 0, 0));
            }
        }
    }

    private static int skipWhitespaceBackwardSameLine(String text, int index) {
        int cursor = index;
        while (cursor >= 0) {
            char value = text.charAt(cursor);
            if (value == '\n' || value == '\r') {
                return -1;
            }
            if (!Character.isWhitespace(value)) {
                return cursor;
            }
            cursor--;
        }
        return -1;
    }

    private static int skipWhitespaceForwardSameLine(String text, int index) {
        int cursor = Math.max(0, index);
        while (cursor < text.length()) {
            char value = text.charAt(cursor);
            if (value == '\n' || value == '\r') {
                return -1;
            }
            if (!Character.isWhitespace(value)) {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    private static KeyCorrection detectKeyCorrectionContext(
            String text,
            int cursor,
            boolean keyPosition,
            boolean insideQuote
    ) {
        if (text == null || text.isBlank() || keyPosition || insideQuote) {
            return null;
        }

        int lineStart = text.lastIndexOf('\n', Math.max(0, cursor - 1)) + 1;
        if (lineStart >= text.length()) {
            return null;
        }

        int separator = findLineKeySeparator(text, lineStart, cursor);
        if (separator < 0) {
            return null;
        }

        int keyEnd = skipWhitespaceBackward(text, separator - 1);
        if (keyEnd < lineStart) {
            return null;
        }
        int keyStart = keyEnd;
        while (keyStart > lineStart && isKeyCorrectionChar(text.charAt(keyStart - 1))) {
            keyStart--;
        }
        if (keyStart > keyEnd) {
            return null;
        }

        String keyPrefix = text.substring(keyStart, keyEnd + 1);
        if (keyPrefix.isBlank()) {
            return null;
        }

        return new KeyCorrection(keyStart, keyEnd + 1, keyPrefix);
    }

    private static boolean looksLikeObjectKeyOnCurrentLine(String text, int cursor) {
        if (text == null || text.isBlank()) {
            return false;
        }

        int safeCursor = Math.clamp(cursor, 0, text.length());
        int lineStart = text.lastIndexOf('\n', Math.max(0, safeCursor - 1)) + 1;
        if (lineStart >= safeCursor) {
            return false;
        }

        if (findLineKeySeparator(text, lineStart, safeCursor) >= 0) {
            return false;
        }

        int first = skipWhitespaceForwardSameLine(text, lineStart);
        if (first < 0 || first >= safeCursor) {
            return false;
        }

        char firstChar = text.charAt(first);
        if (firstChar == '}' || firstChar == ']' || firstChar == ',' || firstChar == ':') {
            return false;
        }

        return firstChar == '"' || isContextTokenCharacter(firstChar);
    }

    private static String inferCurrentLineObjectKey(String text, int cursor) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int safeCursor = Math.clamp(cursor, 0, text.length());
        int lineStart = text.lastIndexOf('\n', Math.max(0, safeCursor - 1)) + 1;
        if (lineStart >= text.length()) {
            return "";
        }

        int separator = findLineKeySeparator(text, lineStart, safeCursor);
        if (separator < 0) {
            return "";
        }

        int keyEnd = skipWhitespaceBackward(text, separator - 1);
        if (keyEnd < lineStart) {
            return "";
        }

        if (text.charAt(keyEnd) == '"') {
            int keyStartQuote = findUnescapedQuoteStart(text, keyEnd - 1, lineStart);
            if (keyStartQuote < 0 || keyStartQuote + 1 > keyEnd) {
                return "";
            }
            return text.substring(keyStartQuote + 1, keyEnd);
        }

        int keyStart = keyEnd;
        while (keyStart > lineStart && isKeyCorrectionChar(text.charAt(keyStart - 1))) {
            keyStart--;
        }
        if (keyStart > keyEnd) {
            return "";
        }

        return text.substring(keyStart, keyEnd + 1);
    }

    private static int findUnescapedQuoteStart(String text, int fromIndex, int limitInclusive) {
        for (int index = fromIndex; index >= limitInclusive; index--) {
            if (text.charAt(index) != '"') {
                continue;
            }
            int slashCount = 0;
            int check = index - 1;
            while (check >= limitInclusive && text.charAt(check) == '\\') {
                slashCount++;
                check--;
            }
            if ((slashCount & 1) == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int findLineKeySeparator(String text, int lineStart, int cursor) {
        boolean inString = false;
        boolean escaping = false;
        char quote = '\0';
        int end = Math.clamp(cursor, lineStart, text.length());
        int separator = -1;
        for (int index = lineStart; index < end; index++) {
            char value = text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (value == '\\') {
                    escaping = true;
                    continue;
                }
                if (value == quote) {
                    inString = false;
                    quote = '\0';
                }
                continue;
            }
            if (value == '"' || value == '\'') {
                inString = true;
                quote = value;
                continue;
            }
            if (value == ':') {
                separator = index;
                continue;
            }
            if (value == '\n' || value == '\r') {
                break;
            }
        }
        return separator;
    }

    private static boolean isKeyCorrectionChar(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == ':'
                || value == '.'
                || value == '-';
    }

    private static boolean isKnownContainerKey(
            String key,
            List<String> contextualKeys,
            List<String> schemaObjectKeys,
            List<String> componentNbtFields,
            List<String> dynamicKeyHints,
            List<String> seenContainerKeys
    ) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return containsKeyIgnoreCase(contextualKeys, key)
                || containsKeyIgnoreCase(schemaObjectKeys, key)
                || containsKeyIgnoreCase(componentNbtFields, key)
                || containsKeyIgnoreCase(dynamicKeyHints, key)
                || containsKeyIgnoreCase(seenContainerKeys, key);
    }

    private static boolean containsKeyIgnoreCase(List<String> values, String key) {
        if (values == null || values.isEmpty() || key == null || key.isBlank()) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLikelyObjectKeyPosition(String text, int cursor, boolean insideQuote) {
        if (insideQuote) {
            return isLikelyObjectKey(text, cursor);
        }
        if (cursor >= 0 && cursor < text.length() && text.charAt(cursor) == ':') {
            return true;
        }
        return isLikelyObjectKeyOutsideString(text, cursor);
    }

    private static boolean isLikelyObjectKeyOutsideString(String text, int cursor) {
        int current = skipWhitespaceBackward(text, cursor - 1);
        if (current < 0) {
            return true;
        }

        char value = text.charAt(current);
        switch (value) {
            case '{', ',' -> {
                return true;
            }
            case ':' -> {
                return false;
            }
            default -> {
            }
        }

        if (isContextTokenCharacter(value) || value == '"') {
            while (current >= 0) {
                char candidate = text.charAt(current);
                if (isContextTokenCharacter(candidate) || candidate == '"') {
                    current--;
                    continue;
                }
                break;
            }
            current = skipWhitespaceBackward(text, current);
            if (current < 0) {
                return true;
            }
            char beforeToken = text.charAt(current);
            return beforeToken == '{' || beforeToken == ',';
        }

        return false;
    }

    private static List<String> guessRegistryIds(String currentKey, String containerKey, String containerPath, RegistryAccess registryAccess) {
        if (currentKey == null || currentKey.isBlank()) {
            return List.of();
        }
        String key = currentKey.toLowerCase(Locale.ROOT);
        String container = Objects.requireNonNullElse(containerKey, "").toLowerCase(Locale.ROOT);
        String path = buildFullPath(containerPath, key);

        if (pathEndsWith(path, "effects", "id") || path.contains("/on_consume_effects/effects/")) {
            return RUNTIME.effectIds(registryAccess);
        }
        if (pathEndsWith(path, "on_consume_effects", "type")) {
            return CONSUME_EFFECT_TYPES;
        }
        if (pathEndsWith(path, "components", "minecraft:consumable", "animation")) {
            return List.of("eat", "drink", "block", "bow", "spear", "crossbow", "spyglass", "horn");
        }
        if (pathEndsWith(path, "components", "minecraft:food", "can_always_eat")
                || pathEndsWith(path, "components", "minecraft:consumable", "has_consume_particles")) {
            return BOOLEAN_VALUES;
        }
        if (pathEndsWith(path, "attributemodifiers", "attributename")) {
            return RUNTIME.attributeIds(registryAccess);
        }
        if (pathEndsWith(path, "attributemodifiers", "slot")) {
            return EQUIPMENT_SLOT_VALUES;
        }
        if (pathEndsWith(path, "enchantments", "id")) {
            return RUNTIME.enchantmentIds(registryAccess);
        }
        if (pathEndsWith(path, "custom_potion_effects", "id")) {
            return RUNTIME.effectIds(registryAccess);
        }
        if (pathEndsWith(path, "patterns", "pattern")) {
            return RUNTIME.bannerPatternIds(registryAccess);
        }
        if (pathEndsWith(path, "components", "minecraft:consumable", "sound")
                || pathEndsWith(path, "on_consume_effects", "sound")) {
            return RUNTIME.soundIds(registryAccess);
        }

        if ("id".equals(key)) {
            if (container.contains("effect")) {
                return RUNTIME.effectIds(registryAccess);
            }
            if (container.contains("enchant")) {
                return RUNTIME.enchantmentIds(registryAccess);
            }
            List<String> semanticIds = semanticIdsFromText(container, registryAccess, true, true);
            if (!semanticIds.isEmpty()) {
                return semanticIds;
            }
            return RUNTIME.itemIds(registryAccess);
        }
        if (key.endsWith("item") || key.endsWith("item_id")) {
            return RUNTIME.itemIds(registryAccess);
        }
        if ("type".equals(key) && "on_consume_effects".equals(container)) {
            return CONSUME_EFFECT_TYPES;
        }
        if ("entity".equals(key) || key.endsWith("entity_id") || key.contains("entity")) {
            return RUNTIME.entityIds(registryAccess);
        }
        if (key.contains("component")) {
            return RUNTIME.componentIds(registryAccess);
        }
        if (key.contains("enchant")) {
            return RUNTIME.enchantmentIds(registryAccess);
        }
        if (key.contains("effect")) {
            return RUNTIME.effectIds(registryAccess);
        }
        if (key.contains("potion")) {
            return RUNTIME.potionIds(registryAccess);
        }
        List<String> semanticIds = semanticIdsFromText(key, registryAccess, false, false);
        if (!semanticIds.isEmpty()) {
            return semanticIds;
        }
        if (key.contains("attribute")) {
            return RUNTIME.attributeIds(registryAccess);
        }
        if (key.contains("sound")) {
            return RUNTIME.soundIds(registryAccess);
        }
        if ("slot".equals(key)) {
            return EQUIPMENT_SLOT_VALUES;
        }
        return List.of();
    }

    private static SlotType classifySlotType(
            String currentKey,
            String containerKey,
            String containerPath,
            boolean insideQuote,
            List<String> activeProfiles
    ) {
        String key = Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT);
        String container = Objects.requireNonNullElse(containerKey, "").toLowerCase(Locale.ROOT);
        String path = buildFullPath(containerPath, key);

        SlotType idSlot = classifyIdSlot(key, container, path);
        if (insideQuote) {
            return idSlot == SlotType.VALUE_UNKNOWN ? SlotType.VALUE_STRING : idSlot;
        }
        if (idSlot != SlotType.VALUE_UNKNOWN) {
            return idSlot;
        }

        if (isBooleanPath(path) || isBooleanLikeKey(key)) {
            return SlotType.VALUE_BOOLEAN;
        }
        if (isNumericPath(path) || isNumericLikeKey(key)) {
            return isLikelyIntegerNumberKey(key, path) ? SlotType.VALUE_INT : SlotType.VALUE_FLOAT;
        }
        if (isStringPath(path) || isStringLikeKey(key)) {
            return SlotType.VALUE_STRING;
        }

        SlotType schemaPathType = schemaSlotTypeForPath(currentKey, containerPath, path, activeProfiles);
        if (schemaPathType != SlotType.VALUE_UNKNOWN) {
            return schemaPathType;
        }

        EnumSet<ValueMode> pathModes = inferModesFromPath(path);
        if (pathModes.size() == 1) {
            ValueMode mode = pathModes.iterator().next();
            return switch (mode) {
                case BOOLEAN -> SlotType.VALUE_BOOLEAN;
                case NUMBER -> isLikelyIntegerNumberKey(key, path) ? SlotType.VALUE_INT : SlotType.VALUE_FLOAT;
                case STRING -> SlotType.VALUE_STRING;
                default -> SlotType.VALUE_UNKNOWN;
            };
        }

        return SlotType.VALUE_UNKNOWN;
    }

    private static SlotType schemaSlotTypeForPath(
            String currentKey,
            String containerPath,
            String fullPath,
            List<String> activeProfiles
    ) {
        if (currentKey == null || currentKey.isBlank() || activeProfiles == null || activeProfiles.isEmpty()) {
            return SlotType.VALUE_UNKNOWN;
        }

        String componentId = activeComponentIdFromContainerPath(containerPath);
        if (componentId.isBlank()) {
            return SlotType.VALUE_UNKNOWN;
        }

        List<String> knownFields = SCHEMA.componentNbtFieldsForProfiles(componentId, activeProfiles);
        if (knownFields.isEmpty()) {
            return SlotType.VALUE_UNKNOWN;
        }

        String key = currentKey.toLowerCase(Locale.ROOT);
        boolean knownField = knownFields.stream().anyMatch(field -> field != null && field.equalsIgnoreCase(key));
        if (!knownField) {
            return SlotType.VALUE_UNKNOWN;
        }

        SlotType idSlot = classifyIdSlot(key, componentId, fullPath);
        if (idSlot != SlotType.VALUE_UNKNOWN) {
            return idSlot;
        }

        if (isBooleanLikeKey(key)) {
            return SlotType.VALUE_BOOLEAN;
        }
        if (isNumericLikeKey(key)) {
            return isLikelyIntegerNumberKey(key, fullPath) ? SlotType.VALUE_INT : SlotType.VALUE_FLOAT;
        }

        if (!SCHEMA.objectKeyHints(key).isEmpty() || isLikelyCompositeFieldName(key)) {
            return SlotType.VALUE_UNKNOWN;
        }

        return SlotType.VALUE_STRING;
    }

    private static String activeComponentIdFromContainerPath(String containerPath) {
        if (containerPath == null || containerPath.isBlank()) {
            return "";
        }
        String[] segments = containerPath.toLowerCase(Locale.ROOT).split("/");
        for (int index = 0; index < segments.length - 1; index++) {
            if ("components".equals(segments[index])) {
                return segments[index + 1];
            }
        }
        return "";
    }

    private static boolean isLikelyCompositeFieldName(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return key.contains("data")
                || key.contains("tag")
                || key.contains("component")
                || key.contains("effects")
                || key.contains("potentials")
                || key.contains("rules")
                || key.contains("passengers")
                || key.contains("motion")
                || key.contains("rotation")
                || key.contains("position")
                || key.equals("entity")
                || key.equals("spawn_data")
                || key.equals("spawndata")
                || key.equals("spawnpotentials")
                || key.equals("customname")
                || key.equals("tooltip_display");
    }

    private static SlotType classifyIdSlot(String key, String container, String path) {
        if (pathEndsWith(path, "effects", "id") || path.contains("/on_consume_effects/effects/")) {
            return SlotType.VALUE_ID_EFFECT;
        }
        if (pathEndsWith(path, "enchantments", "id")) {
            return SlotType.VALUE_ID_ENCHANTMENT;
        }
        if (pathEndsWith(path, "patterns", "pattern")) {
            return SlotType.VALUE_ID_BANNER_PATTERN;
        }
        if (pathEndsWith(path, "components", "minecraft:consumable", "sound")
                || pathEndsWith(path, "on_consume_effects", "sound")) {
            return SlotType.VALUE_ID_SOUND;
        }
        if (pathEndsWith(path, "attributemodifiers", "attributename")) {
            return SlotType.VALUE_ID_ATTRIBUTE;
        }
        if (pathEndsWith(path, "custom_potion_effects", "id")
                || pathEndsWith(path, "components", "minecraft:potion_contents", "potion")) {
            return SlotType.VALUE_ID_POTION;
        }
        if (path.contains("trim") && path.contains("material")) {
            return SlotType.VALUE_ID_TRIM_MATERIAL;
        }
        if (path.contains("trim") && path.contains("pattern")) {
            return SlotType.VALUE_ID_TRIM_PATTERN;
        }

        if (!"id".equals(key) && !key.endsWith("_id")) {
            return SlotType.VALUE_UNKNOWN;
        }
        if (container.contains("effect")) {
            return SlotType.VALUE_ID_EFFECT;
        }
        if (container.contains("enchant")) {
            return SlotType.VALUE_ID_ENCHANTMENT;
        }
        if (container.contains("entity") || container.contains("spawn")) {
            return SlotType.VALUE_ID_ENTITY;
        }
        if (container.contains("component")) {
            return SlotType.VALUE_ID_COMPONENT;
        }
        if (container.contains("potion")) {
            return SlotType.VALUE_ID_POTION;
        }
        return SlotType.VALUE_ID_ITEM;
    }

    private static EnumSet<ValueMode> expectedModesForSlot(SlotType slotType) {
        return switch (slotType) {
            case OBJECT_KEY, VALUE_UNKNOWN -> EnumSet.of(ValueMode.NONE);
            case VALUE_BOOLEAN -> EnumSet.of(ValueMode.BOOLEAN);
            case VALUE_INT, VALUE_FLOAT -> EnumSet.of(ValueMode.NUMBER);
            case VALUE_ID_EFFECT,
                    VALUE_ID_ENCHANTMENT,
                    VALUE_ID_ITEM,
                    VALUE_ID_ENTITY,
                    VALUE_ID_COMPONENT,
                    VALUE_ID_POTION,
                    VALUE_ID_BANNER_PATTERN,
                    VALUE_ID_TRIM_MATERIAL,
                    VALUE_ID_TRIM_PATTERN,
                    VALUE_ID_SOUND,
                    VALUE_ID_ATTRIBUTE,
                    VALUE_STRING -> EnumSet.of(ValueMode.STRING);
        };
    }

    private static List<String> registryHintsForSlot(
            SlotType slotType,
            String currentKey,
            String containerKey,
            String containerPath,
            RegistryAccess registryAccess
    ) {
        return switch (slotType) {
            case VALUE_ID_EFFECT -> RUNTIME.effectIds(registryAccess);
            case VALUE_ID_ENCHANTMENT -> RUNTIME.enchantmentIds(registryAccess);
            case VALUE_ID_ITEM -> RUNTIME.itemIds(registryAccess);
            case VALUE_ID_ENTITY -> RUNTIME.entityIds(registryAccess);
            case VALUE_ID_COMPONENT -> RUNTIME.componentIds(registryAccess);
            case VALUE_ID_POTION -> RUNTIME.potionIds(registryAccess);
            case VALUE_ID_BANNER_PATTERN -> RUNTIME.bannerPatternIds(registryAccess);
            case VALUE_ID_TRIM_MATERIAL -> RUNTIME.trimMaterialIds(registryAccess);
            case VALUE_ID_TRIM_PATTERN -> RUNTIME.trimPatternIds(registryAccess);
            case VALUE_ID_SOUND -> RUNTIME.soundIds(registryAccess);
            case VALUE_ID_ATTRIBUTE -> RUNTIME.attributeIds(registryAccess);
            case VALUE_BOOLEAN,
                    VALUE_INT,
                    VALUE_FLOAT,
                    VALUE_STRING,
                    VALUE_UNKNOWN,
                    OBJECT_KEY -> guessRegistryIds(currentKey, containerKey, containerPath, registryAccess);
        };
    }

    private static List<String> filterFloatHints(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> filtered = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.matches("^-?\\d+\\.\\d+(?:[fd])?$")
                    || normalized.matches("^-?\\d+(?:[fd])$")
                    || normalized.equals("infinityf")
                    || normalized.equals("infinityd")
                    || normalized.equals("nanf")
                    || normalized.equals("nand")) {
                filtered.add(value);
            }
        }
        return filtered;
    }

    private static boolean isBooleanPath(String path) {
        return pathEndsWithAny(path, BOOLEAN_PATH_SUFFIXES);
    }

    private static boolean isNumericPath(String path) {
        return pathEndsWithAny(path, NUMERIC_PATH_SUFFIXES);
    }

    private static boolean isStringPath(String path) {
        return pathEndsWithAny(path, STRING_PATH_SUFFIXES);
    }

    private static List<String> semanticIdsFromText(
            String text,
            RegistryAccess registryAccess,
            boolean includeEntitySpawn,
            boolean includeBlockEntity
    ) {
        if (text.contains("trim") && text.contains("material")) {
            return RUNTIME.trimMaterialIds(registryAccess);
        }
        if (text.contains("trim") && text.contains("pattern")) {
            return RUNTIME.trimPatternIds(registryAccess);
        }
        if (text.contains("banner") && text.contains("pattern")) {
            return RUNTIME.bannerPatternIds(registryAccess);
        }
        if (text.contains("song")) {
            return RUNTIME.songIds(registryAccess);
        }
        if (text.contains("instrument")) {
            return RUNTIME.instrumentIds(registryAccess);
        }
        if (includeEntitySpawn && (text.contains("entity") || text.contains("spawn"))) {
            return RUNTIME.entityIds(registryAccess);
        }
        if (includeBlockEntity && text.contains("block_entity")) {
            return RUNTIME.blockEntityIds(registryAccess);
        }
        return List.of();
    }

    private static List<String> contextualKeyHints(String containerKey, String containerPath) {
        String container = Objects.requireNonNullElse(containerKey, "").toLowerCase(Locale.ROOT);
        if (container.isBlank()) {
            return List.of();
        }

        String path = Objects.requireNonNullElse(containerPath, "").toLowerCase(Locale.ROOT);
        if ("entity".equals(container) && (path.contains("/spawndata/entity") || path.contains("/spawnpotentials/data/entity"))) {
            return mergeUnique(ENTITY_COMMON_KEYS, SPAWNER_ENTITY_KEYS);
        }
        if (container.contains("block_entity_data")) {
            return SPAWNER_BLOCK_ENTITY_KEYS;
        }
        return switch (container) {
            case "minecraft:enchantments", "minecraft:stored_enchantments" -> List.of();
            case "spawndata" -> List.of("entity", "equipment");
            case "spawnpotentials" -> List.of("data", "weight");
            case "custom_spawn_rules" -> List.of("block_light_limit", "sky_light_limit");
            case "block_light_limit", "sky_light_limit" -> List.of("min_inclusive", "max_inclusive");
            case "minecraft:food" -> FOOD_COMPONENT_KEYS;
            case "minecraft:consumable" -> CONSUMABLE_COMPONENT_KEYS;
            case "on_consume_effects" -> ON_CONSUME_EFFECT_ENTRY_KEYS;
            case "effects" -> EFFECT_INSTANCE_KEYS;
            case "attributemodifiers" -> LEGACY_ATTRIBUTE_MODIFIER_KEYS;
            case "enchantments", "storedenchantments", "stored_enchantments" -> LEGACY_ENCHANTMENT_KEYS;
            case "display" -> LEGACY_DISPLAY_KEYS;
            case "custompotioneffects", "custom_potion_effects" -> LEGACY_CUSTOM_POTION_EFFECT_KEYS;
            case "fireworks" -> LEGACY_FIREWORK_KEYS;
            case "patterns" -> LEGACY_BANNER_PATTERN_KEYS;
            default -> {
                if (container.contains("entity")) {
                    yield ENTITY_COMMON_KEYS;
                }
                if (container.contains("name") || container.contains("text")) {
                    yield List.of("text", "color", "translate", "with", "extra", "bold", "italic", "underlined", "strikethrough", "obfuscated");
                }
                yield List.of();
            }
        };
    }

    private static boolean shouldUseStrictContextKeySuggestions(
            RawAutocompleteIndex.Context context,
            List<String> contextualKeys,
            List<String> schemaObjectKeys,
            List<String> componentNbtFields,
            List<String> dynamicKeyHints,
            List<String> profileComponents
    ) {
        if (context == null) {
            return false;
        }
        if (context.inRootObject() || context.inComponentsObject()) {
            return true;
        }
        if (context.containerPath() != null && !context.containerPath().isBlank()) {
            return true;
        }
        if (context.containerKey() != null && !context.containerKey().isBlank()) {
            return true;
        }
        return hasItems(contextualKeys)
                || hasItems(schemaObjectKeys)
                || hasItems(componentNbtFields)
                || hasItems(dynamicKeyHints)
                || hasItems(profileComponents);
    }

    private static boolean hasItems(List<String> values) {
        return values != null && !values.isEmpty();
    }

    private static List<String> dynamicKeyHints(String containerKey, String containerPath, RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return List.of();
        }
        String key = Objects.requireNonNullElse(containerKey, "").toLowerCase(Locale.ROOT);
        String path = Objects.requireNonNullElse(containerPath, "").toLowerCase(Locale.ROOT);
        if (key.endsWith("enchantments")
                || key.endsWith("stored_enchantments")
                || path.endsWith("/minecraft:enchantments")
                || path.endsWith("/minecraft:stored_enchantments")) {
            return RUNTIME.enchantmentIds(registryAccess);
        }
        return List.of();
    }

    private static List<String> mapIdKeyHints(String containerKey, String containerPath, RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return List.of();
        }
        String key = Objects.requireNonNullElse(containerKey, "").toLowerCase(Locale.ROOT);
        String path = Objects.requireNonNullElse(containerPath, "").toLowerCase(Locale.ROOT);
        if ("minecraft:stored_enchantments".equals(key)
                || "minecraft:enchantments".equals(key)
                || path.endsWith("/minecraft:stored_enchantments")
                || path.endsWith("/minecraft:enchantments")) {
            return RUNTIME.enchantmentIds(registryAccess);
        }
        return List.of();
    }

    private static List<String> schemaValueHints(String currentKey, String containerKey) {
        String normalized = Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT);
        if ("id".equals(normalized) || normalized.endsWith("_id")) {
            return List.of();
        }

        List<String> keyHints = SCHEMA.valueHints(currentKey);
        List<String> containerHints = SCHEMA.valueHints(containerKey);
        return mergeUnique(keyHints, containerHints);
    }

    private static List<String> contextualValueHints(String currentKey, String containerPath) {
        if (currentKey == null || currentKey.isBlank()) {
            return List.of();
        }

        String key = currentKey.toLowerCase(Locale.ROOT);
        String path = buildFullPath(containerPath, key);

        if (pathEndsWith(path, "attributemodifiers", "slot")
                || pathEndsWith(path, "components", "minecraft:attribute_modifiers", "slot")) {
            return EQUIPMENT_SLOT_VALUES;
        }
        if (pathEndsWith(path, "attributemodifiers", "operation")) {
            return LEGACY_ATTRIBUTE_OPERATION_VALUES;
        }
        if (pathEndsWith(path, "components", "minecraft:attribute_modifiers", "operation")) {
            return MODERN_ATTRIBUTE_OPERATION_VALUES;
        }
        if (pathEndsWith(path, "display", "color")) {
            return List.of("#ffffff", "#8219F3", "#ff0000", "#00ff00", "#0000ff");
        }
        if (pathEndsWith(path, "display", "name")) {
            return List.of("{\"text\":\"Item Name\",\"color\":\"#ffffff\"}");
        }
        if (pathEndsWith(path, "display", "lore")) {
            return List.of("{\"text\":\"Lore line\"}");
        }
        return List.of();
    }

    private static List<String> mergeUnique(List<String> primary, List<String> secondary) {
        if (primary == null || primary.isEmpty()) {
            return Objects.requireNonNullElse(secondary, List.of());
        }
        if (secondary == null || secondary.isEmpty()) {
            return primary;
        }

        List<String> merged = new ArrayList<>(primary.size() + secondary.size());
        merged.addAll(primary);
        for (String value : secondary) {
            if (!merged.contains(value)) {
                merged.add(value);
            }
        }
        return merged;
    }

    private static EnumSet<ValueMode> expectedValueModes(
            String currentKey,
            String containerKey,
            String containerPath,
            String prefix,
            boolean insideQuote
    ) {
        if (insideQuote) {
            return EnumSet.of(ValueMode.STRING);
        }

        EnumSet<ValueMode> modes = EnumSet.noneOf(ValueMode.class);
        String key = Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT);
        String path = buildFullPath(containerPath, key);
        EnumSet<ValueMode> pathModes = inferModesFromPath(path);
        if (!pathModes.isEmpty()) {
            modes.addAll(pathModes);
        }
        if (!key.isBlank()) {
            if (isBooleanLikeKey(key)) {
                modes.add(ValueMode.BOOLEAN);
            }
            if (isNumericLikeKey(key)) {
                modes.add(ValueMode.NUMBER);
            }
            if (isStringLikeKey(key)) {
                modes.add(ValueMode.STRING);
            }
        }

        EnumSet<ValueMode> schemaModes = inferModesFromHints(schemaValueHints(currentKey, containerKey));
        if (modes.isEmpty()) {
            modes.addAll(schemaModes);
        } else if (!schemaModes.isEmpty()) {
            EnumSet<ValueMode> overlap = EnumSet.copyOf(modes);
            overlap.retainAll(schemaModes);
            if (!overlap.isEmpty()) {
                modes = overlap;
            }
        }

        if (!modes.isEmpty()) {
            return modes;
        }

        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowerPrefix.startsWith("t") || lowerPrefix.startsWith("f") || lowerPrefix.startsWith("n")) {
            return EnumSet.of(ValueMode.LITERAL);
        }
        return EnumSet.of(ValueMode.NONE);
    }

    private static EnumSet<ValueMode> refineExpectedModesWithRuntimeProbe(
            String text,
            int replaceStart,
            int cursor,
            RegistryAccess registryAccess,
            EnumSet<ValueMode> expectedModes,
            boolean insideQuote
    ) {
        if (insideQuote || text == null || text.length() > RUNTIME_PROBE_MAX_TEXT_LENGTH) {
            return expectedModes;
        }

        EnumSet<ValueMode> runtimeModes = runtimeProbeModes(text, replaceStart, cursor, registryAccess);
        if (runtimeModes.isEmpty()) {
            return expectedModes;
        }

        if (expectedModes == null || expectedModes.isEmpty() || expectedModes.contains(ValueMode.NONE)) {
            return runtimeModes;
        }

        EnumSet<ValueMode> overlap = EnumSet.copyOf(expectedModes);
        overlap.retainAll(runtimeModes);
        if (!overlap.isEmpty()) {
            return overlap;
        }
        return runtimeModes;
    }

    private static EnumSet<ValueMode> runtimeProbeModes(
            String text,
            int replaceStart,
            int cursor,
            RegistryAccess registryAccess
    ) {
        if (registryAccess == null) {
            return EnumSet.noneOf(ValueMode.class);
        }
        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeCursor = Math.clamp(cursor, safeStart, text.length());
        String cacheKey = runtimeProbeCacheKey(text, safeStart, safeCursor);
        synchronized (RUNTIME_PROBE_CACHE_LOCK) {
            EnumSet<ValueMode> cached = RUNTIME_PROBE_CACHE.get(cacheKey);
            if (cached != null) {
                return EnumSet.copyOf(cached);
            }
        }

        String prefix = text.substring(0, safeStart);
        String suffix = text.substring(safeCursor);
        EnumSet<ValueMode> modes = EnumSet.noneOf(ValueMode.class);
        if (parsesWithProbe(prefix, suffix, "1b", registryAccess) || parsesWithProbe(prefix, suffix, "true", registryAccess)) {
            modes.add(ValueMode.BOOLEAN);
        }
        if (parsesWithProbe(prefix, suffix, "1", registryAccess) || parsesWithProbe(prefix, suffix, "1.0f", registryAccess)) {
            modes.add(ValueMode.NUMBER);
        }
        if (parsesWithProbe(prefix, suffix, "\"x\"", registryAccess)) {
            modes.add(ValueMode.STRING);
        }
        if (parsesWithProbe(prefix, suffix, "null", registryAccess)) {
            modes.add(ValueMode.LITERAL);
        }

        synchronized (RUNTIME_PROBE_CACHE_LOCK) {
            RUNTIME_PROBE_CACHE.put(cacheKey, modes.isEmpty() ? EnumSet.noneOf(ValueMode.class) : EnumSet.copyOf(modes));
        }
        return modes;
    }

    private static boolean parsesWithProbe(String prefix, String suffix, String probeValue, RegistryAccess registryAccess) {
        String candidate = prefix + probeValue + suffix;
        RawItemDataUtil.ParseResult parsed = RawItemDataUtil.parse(candidate, registryAccess);
        return parsed.success();
    }

    private static String runtimeProbeCacheKey(String text, int replaceStart, int cursor) {
        int left = Math.max(0, replaceStart - 80);
        int right = Math.min(text.length(), cursor + 80);
        String window = text.substring(left, right);
        return left + ":" + right + ":" + replaceStart + ":" + cursor + ":" + window;
    }

    private static EnumSet<ValueMode> inferModesFromPath(String path) {
        if (path == null || path.isBlank()) {
            return EnumSet.noneOf(ValueMode.class);
        }

        if (isNumericPath(path) || pathEndsWithAny(path, NUMERIC_MODE_ONLY_PATH_SUFFIXES)) {
            return EnumSet.of(ValueMode.NUMBER);
        }

        if (isBooleanPath(path)) {
            return EnumSet.of(ValueMode.BOOLEAN);
        }

        if (isStringPath(path)) {
            return EnumSet.of(ValueMode.STRING);
        }

        return EnumSet.noneOf(ValueMode.class);
    }

    private static EnumSet<ValueMode> inferModesFromHints(List<String> hints) {
        EnumSet<ValueMode> modes = EnumSet.noneOf(ValueMode.class);
        if (hints == null || hints.isEmpty()) {
            return modes;
        }

        for (String hint : hints) {
            ValueMode mode = classifyValueMode(hint);
            if (mode != ValueMode.NONE) {
                modes.add(mode);
            }
        }
        return modes;
    }

    private static List<String> filterValuesByModes(List<String> values, EnumSet<ValueMode> expectedModes) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        if (expectedModes == null || expectedModes.isEmpty() || expectedModes.contains(ValueMode.NONE)) {
            return values;
        }

        List<String> filtered = new ArrayList<>(values.size());
        for (String value : values) {
            ValueMode mode = classifyValueMode(value);
            if (mode != ValueMode.NONE && expectedModes.contains(mode)) {
                filtered.add(value);
            }
        }
        return filtered;
    }

    private static String buildFullPath(String containerPath, String currentKey) {
        String container = Objects.requireNonNullElse(containerPath, "").toLowerCase(Locale.ROOT);
        String key = Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT);
        if (container.isBlank()) {
            return key;
        }
        if (key.isBlank()) {
            return container;
        }
        return container + "/" + key;
    }

    private static boolean pathEndsWith(String path, String... segments) {
        if (path == null || path.isBlank() || segments == null || segments.length == 0) {
            return false;
        }
        String[] pathSegments = path.toLowerCase(Locale.ROOT).split("/");
        if (pathSegments.length < segments.length) {
            return false;
        }
        int offset = pathSegments.length - segments.length;
        for (int index = 0; index < segments.length; index++) {
            String expected = Objects.requireNonNullElse(segments[index], "").toLowerCase(Locale.ROOT);
            if (!expected.equals(pathSegments[offset + index])) {
                return false;
            }
        }
        return true;
    }

    private static boolean pathEndsWithAny(String path, List<List<String>> suffixes) {
        if (suffixes == null || suffixes.isEmpty()) {
            return false;
        }
        for (List<String> suffix : suffixes) {
            if (pathEndsWith(path, suffix.toArray(String[]::new))) {
                return true;
            }
        }
        return false;
    }

    private static ValueMode classifyValueMode(String value) {
        if (value == null || value.isBlank()) {
            return ValueMode.NONE;
        }

        String normalized = value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "true", "false" -> {
                return ValueMode.BOOLEAN;
            }
            case "null" -> {
                return ValueMode.LITERAL;
            }
            case "infinityf", "infinityd", "nanf", "nand", "nan" -> {
                return ValueMode.NUMBER;
            }
            default -> {
            }
        }
        if (NUMBER_TOKEN_PATTERN.matcher(normalized).matches()) {
            return ValueMode.NUMBER;
        }
        if (normalized.startsWith("{") || normalized.startsWith("[")) {
            return ValueMode.NONE;
        }
        return ValueMode.STRING;
    }

    private static String detectTopLevelItemId(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        List<IdScanFrame> stack = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;

        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    token.append(value);
                    continue;
                }
                if (value == '\\') {
                    escaping = true;
                    token.append(value);
                    continue;
                }
                if (value == '"') {
                    inString = false;
                    String found = consumeToken(stack, token.toString(), true);
                    token.setLength(0);
                    if (!found.isEmpty()) {
                        return found;
                    }
                    continue;
                }
                token.append(value);
                continue;
            }

            if (Character.isWhitespace(value)) {
                continue;
            }

            switch (value) {
                case '"' -> {
                    inString = true;
                    token.setLength(0);
                }
                case '{' -> {
                    consumeCompositeValue(stack);
                    stack.add(IdScanFrame.objectFrame());
                }
                case '[' -> {
                    consumeCompositeValue(stack);
                    stack.add(IdScanFrame.arrayFrame());
                }
                case '}', ']' -> {
                    if (!stack.isEmpty()) {
                        stack.removeLast();
                    }
                }
                case ',' -> {
                    IdScanFrame frame = topFrame(stack);
                    if (frame != null && frame.object) {
                        frame.resetForNextPair();
                    }
                }
                case ':' -> {
                    IdScanFrame frame = topFrame(stack);
                    if (frame != null && frame.object) {
                        frame.afterColon();
                    }
                }
                default -> {
                    if (!isParserTokenCharacter(value)) {
                        continue;
                    }

                    int end = index + 1;
                    while (end < text.length() && isParserTokenCharacter(text.charAt(end))) {
                        end++;
                    }
                    String found = consumeToken(stack, text.substring(index, end), false);
                    if (!found.isEmpty()) {
                        return found;
                    }
                    index = end - 1;
                }
            }
        }

        return "";
    }

    private static void consumeCompositeValue(List<IdScanFrame> stack) {
        IdScanFrame parent = topFrame(stack);
        if (parent != null && parent.object && parent.awaitingValue()) {
            parent.consumeValue();
        }
    }

    private static String consumeToken(List<IdScanFrame> stack, String token, boolean quoted) {
        if (token == null || token.isBlank()) {
            return "";
        }

        IdScanFrame frame = topFrame(stack);
        if (frame == null || !frame.object) {
            return "";
        }

        if (frame.expectingKey) {
            frame.pendingKey = token;
            frame.expectingKey = false;
            frame.expectingValue = false;
            return "";
        }

        if (frame.pendingKey == null || !frame.awaitingValue()) {
            return "";
        }

        String pendingKey = frame.pendingKey;
        frame.consumeValue();
        if (stack.size() == 1
                && "id".equals(pendingKey)
                && quoted
                && NAMESPACED_ID_PATTERN.matcher(token.toLowerCase(Locale.ROOT)).matches()) {
            return token.toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static IdScanFrame topFrame(List<IdScanFrame> stack) {
        return stack.isEmpty() ? null : stack.getLast();
    }

    private static boolean isParserTokenCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '-'
                || value == '.'
                || value == '+'
                || value == '/';
    }

    private static List<String> inferItemProfiles(String itemId, RegistryAccess registryAccess) {
        return RUNTIME.itemProfiles(itemId, registryAccess);
    }

    private static boolean isBooleanLikeKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (BOOLEAN_EXACT_KEYS.contains(key)) {
            return true;
        }
        return key.endsWith("visible")
                || key.endsWith("resolved")
                || key.endsWith("enabled")
                || key.endsWith("trail")
                || key.endsWith("twinkle")
                || key.endsWith("obfuscated")
                || key.endsWith("italic")
                || key.endsWith("bold")
                || key.endsWith("underlined")
                || key.endsWith("strikethrough")
                || key.endsWith("glowing")
                || key.endsWith("hide_tooltip")
                || key.startsWith("allow_")
                || key.startsWith("can_")
                || key.startsWith("has_")
                || key.startsWith("is_");
    }

    private static boolean isNumericLikeKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (NUMERIC_EXACT_KEYS.contains(key)) {
            return true;
        }
        return key.contains("count")
                || key.contains("amount")
                || key.contains("delay")
                || key.contains("range")
                || key.contains("chance")
                || key.contains("duration")
                || key.contains("scale")
                || key.contains("weight")
                || key.contains("level")
                || key.startsWith("max")
                || key.startsWith("min");
    }

    private static boolean isStringLikeKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (STRING_EXACT_KEYS.contains(key)) {
            return true;
        }
        return "id".equals(key)
                || key.endsWith("_id")
                || key.endsWith("_type")
                || key.contains("item")
                || key.contains("entity")
                || key.contains("effect")
                || key.contains("potion")
                || key.contains("trim")
                || key.contains("banner")
                || key.contains("song")
                || key.contains("instrument")
                || key.contains("name")
                || key.contains("title")
                || key.contains("author")
                || key.contains("text")
                || key.contains("color")
                || key.contains("rarity")
                || key.contains("operation")
                || key.contains("slot")
                || key.contains("shape")
                || key.contains("generation");
    }

    private static int findTokenStart(String text, int cursor, boolean insideQuote) {
        int index = cursor;
        while (index > 0) {
            char previous = text.charAt(index - 1);
            if (!insideQuote && previous == ':') {
                break;
            }
            if (!isTokenCharacter(previous)) {
                break;
            }
            index--;
        }
        if (insideQuote) {
            int quoteStart = text.lastIndexOf('"', cursor - 1);
            if (quoteStart >= 0 && index < quoteStart + 1) {
                return quoteStart + 1;
            }
        }
        return index;
    }

    private static boolean isTokenCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == ':'
                || value == '.'
                || value == '-'
                || value == '/';
    }

    private static boolean isContextTokenCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == ':'
                || value == '.'
                || value == '-'
                || value == '/';
    }

    private static String formatKeyInsert(String key) {
        return requiresQuotedKey(key) ? quote(key) : key;
    }

    private static String formatValueInsert(String value) {
        return canBeBareToken(value) ? value : quote(value);
    }

    private static boolean requiresQuotedKey(String key) {
        for (int index = 0; index < key.length(); index++) {
            char value = key.charAt(index);
            if (!Character.isLetterOrDigit(value) && value != '_' && value != '-' && value != '.') {
                return true;
            }
        }
        return false;
    }

    private static boolean canBeBareToken(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.' && c != '+') {
                return false;
            }
        }
        return true;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static boolean isLikelyObjectKey(String text, int cursor) {
        int quoteStart = text.lastIndexOf('"', cursor - 1);
        if (quoteStart < 0) {
            return false;
        }

        int quoteEnd = findStringEnd(text, quoteStart + 1);
        if (quoteEnd > quoteStart) {
            int next = skipWhitespaceForward(text, quoteEnd + 1);
            if (next < text.length() && text.charAt(next) == ':') {
                return true;
            }
        }

        int previous = skipWhitespaceBackward(text, quoteStart - 1);
        return previous >= 0 && (text.charAt(previous) == '{' || text.charAt(previous) == ',');
    }

    private static int findStringEnd(String text, int start) {
        boolean escaping = false;
        for (int index = start; index < text.length(); index++) {
            char value = text.charAt(index);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (value == '\\') {
                escaping = true;
                continue;
            }
            if (value == '"') {
                return index;
            }
        }
        return -1;
    }

    private static int skipWhitespaceForward(String text, int index) {
        int cursor = Math.max(0, index);
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static int skipWhitespaceBackward(String text, int index) {
        if (text.isEmpty()) {
            return -1;
        }
        int cursor = index;
        if (cursor >= text.length()) {
            cursor = text.length() - 1;
        }
        while (cursor >= 0 && Character.isWhitespace(text.charAt(cursor))) {
            cursor--;
        }
        return cursor;
    }

    public record AutocompleteResult(int requestedCaret, int replaceStart, int replaceEnd, List<Suggestion> suggestions, String currentKey) {
        public static AutocompleteResult empty(int caretIndex) {
            return new AutocompleteResult(caretIndex, caretIndex, caretIndex, List.of(), null);
        }
    }

    public record Suggestion(String label, String insertText, SuggestionKind kind, int matchRank, int contextRank) {
    }

    public enum SuggestionKind {
        SNIPPET(0),
        STRUCTURAL(1),
        KEY(2),
        VALUE(3),
        LITERAL(4);

        private final int priority;

        SuggestionKind(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return this.priority;
        }
    }

    private enum ValueMode {
        STRING,
        BOOLEAN,
        NUMBER,
        LITERAL,
        NONE
    }

    private enum SlotType {
        OBJECT_KEY,
        VALUE_BOOLEAN,
        VALUE_INT,
        VALUE_FLOAT,
        VALUE_ID_EFFECT,
        VALUE_ID_ENCHANTMENT,
        VALUE_ID_ITEM,
        VALUE_ID_ENTITY,
        VALUE_ID_COMPONENT,
        VALUE_ID_POTION,
        VALUE_ID_BANNER_PATTERN,
        VALUE_ID_TRIM_MATERIAL,
        VALUE_ID_TRIM_PATTERN,
        VALUE_ID_SOUND,
        VALUE_ID_ATTRIBUTE,
        VALUE_STRING,
        VALUE_UNKNOWN
    }

    private enum BooleanInsertStyle {
        TEXT,
        NBT_BYTE
    }

    private record ParseFilteredSuggestion(Suggestion suggestion, int parseRank, int distanceToTarget) {
    }

    private record WeightedSuggestion(Suggestion suggestion, int score) {
    }

    private record FuzzyCandidate(String value, int distance) {
    }

    private record KeyCorrection(int replaceStart, int replaceEnd, String keyPrefix) {
    }

    private static final class IdScanFrame {
        private final boolean object;
        private boolean expectingKey;
        private boolean expectingValue;
        private String pendingKey;

        private IdScanFrame(boolean object) {
            this.object = object;
            this.expectingKey = object;
            this.expectingValue = false;
            this.pendingKey = null;
        }

        public static IdScanFrame objectFrame() {
            return new IdScanFrame(true);
        }

        public static IdScanFrame arrayFrame() {
            return new IdScanFrame(false);
        }

        public void afterColon() {
            if (!this.object || this.pendingKey == null) {
                return;
            }
            this.expectingValue = true;
        }

        public boolean awaitingValue() {
            return this.pendingKey != null && this.expectingValue;
        }

        public void consumeValue() {
            this.pendingKey = null;
            this.expectingValue = false;
            this.expectingKey = false;
        }

        public void resetForNextPair() {
            if (!this.object) {
                return;
            }
            this.pendingKey = null;
            this.expectingValue = false;
            this.expectingKey = true;
        }
    }
}
