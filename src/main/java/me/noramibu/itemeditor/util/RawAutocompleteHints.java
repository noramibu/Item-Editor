package me.noramibu.itemeditor.util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.ItemEditorState;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

final class RawAutocompleteHints {
    private static final RawRuntimeSuggestionProvider RUNTIME = new RawRuntimeSuggestionProvider();
    private static final RawAutocompleteCatalog CATALOG = new RawAutocompleteCatalog();
    private static final ThreadLocal<List<String>> EXTERNAL_LOOT_TABLE_IDS = ThreadLocal.withInitial(List::of);
    private static final int RUNTIME_PROBE_MAX_TEXT_LENGTH = 20000;
    private static final int REGISTRY_BINDING_PROBE_MAX_TEXT_LENGTH = 18000;
    private static final int REGISTRY_BINDING_SAMPLE_LIMIT = 4;
    private static final int RUNTIME_PROBE_CACHE_LIMIT = 256;
    private static final int PATH_PROBE_CACHE_LIMIT = 384;
    private static final int REGISTRY_HINT_PROBE_CACHE_LIMIT = 384;
    private static final Object RUNTIME_PROBE_CACHE_LOCK = new Object();
    private static final Map<String, EnumSet<RawValueMode>> RUNTIME_PROBE_CACHE =
            new LinkedHashMap<>(RUNTIME_PROBE_CACHE_LIMIT + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, EnumSet<RawValueMode>> eldest) {
                    return this.size() > RUNTIME_PROBE_CACHE_LIMIT;
                }
            };
    private static final Map<String, EnumSet<RawValueMode>> PATH_PROBE_CACHE =
            new LinkedHashMap<>(PATH_PROBE_CACHE_LIMIT + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, EnumSet<RawValueMode>> eldest) {
                    return this.size() > PATH_PROBE_CACHE_LIMIT;
                }
            };
    private static final Map<String, List<RegistryHint>> REGISTRY_HINT_PROBE_CACHE =
            new LinkedHashMap<>(REGISTRY_HINT_PROBE_CACHE_LIMIT + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<RegistryHint>> eldest) {
                    return this.size() > REGISTRY_HINT_PROBE_CACHE_LIMIT;
                }
            };

    private static final List<String> INTEGER_NUMBER_VALUES =
            List.of("0", "1", "2", "5", "10", "20", "64", "128", "200");
    private static final List<String> FLOAT_NUMBER_VALUES =
            List.of("0.0f", "0.1f", "0.5f", "1.0f", "2.0f", "4.0f", "10.0f", "0.0d", "1.0d", "Infinityf");
    private static final List<String> NUMBER_VALUES =
            List.of("0", "1", "0b", "1b", "0s", "1s", "0L", "1L", "0.0f", "1.0f", "0.0d", "1.0d", "Infinityf", "NaNd");
    private static final List<String> BOOLEAN_VALUES = List.of("true", "false");
    private static final List<RegistryHint> VALUE_REGISTRY_HINTS = List.of(RegistryHint.values());
    private static final List<String> CONSUME_EFFECT_TYPES = ItemEditorState.ConsumableEffectDraft.ALL_TYPES;
    private static final List<String> CONSUMABLE_ANIMATIONS = List.of(
            "none",
            "eat",
            "drink",
            "block",
            "bow",
            "spear",
            "crossbow",
            "spyglass",
            "toot_horn",
            "brush",
            "bundle",
            "trident");
    private static final List<String> EQUIPMENT_SLOTS =
            List.of("mainhand", "offhand", "head", "chest", "legs", "feet", "body", "saddle");
    private static final List<String> LEGACY_ATTRIBUTE_OPERATIONS = List.of("0", "1", "2");
    private static final List<String> MODERN_ATTRIBUTE_OPERATIONS =
            List.of("add_value", "add_multiplied_base", "add_multiplied_total");
    private static final List<String> TEXT_COLORS = List.of(
            "black",
            "dark_blue",
            "dark_green",
            "dark_aqua",
            "dark_red",
            "dark_purple",
            "gold",
            "gray",
            "dark_gray",
            "blue",
            "green",
            "aqua",
            "red",
            "light_purple",
            "yellow",
            "white",
            "magenta");
    private static final List<String> COLOR_EXAMPLES = List.of("#ffffff", "#8219F3", "#ff0000", "#00ff00", "#0000ff");
    private static final List<String> HOVER_EVENT_ACTIONS = List.of("show_text", "show_item", "show_entity");
    private static final List<String> CLICK_EVENT_ACTIONS =
            List.of("open_url", "open_file", "run_command", "suggest_command", "change_page", "copy_to_clipboard");

    private static final Set<String> BOOLEAN_EXACT_KEYS = Set.of(
            "ambient",
            "can_always_eat",
            "custom_name_visible",
            "customnamevisible",
            "glowing",
            "has_consume_particles",
            "invulnerable",
            "auto",
            "conditionmet",
            "nogravity",
            "no_gravity",
            "powered",
            "resolved",
            "show_icon",
            "show_particles",
            "showicon",
            "showparticles",
            "silent",
            "trackoutput",
            "tracked",
            "updatelastexecution",
            "unbreakable");
    private static final Set<String> NUMERIC_EXACT_KEYS = Set.of(
            "amplifier",
            "base",
            "consume_seconds",
            "count",
            "custompotioncolor",
            "delay",
            "duration",
            "flight",
            "lvl",
            "maxnearbyentities",
            "maxspawndelay",
            "minspawndelay",
            "nutrition",
            "probability",
            "requiredplayerrange",
            "saturation",
            "seconds",
            "spawncount",
            "spawnrange",
            "successcount",
            "weight");
    private static final Set<String> STRING_EXACT_KEYS = Set.of(
            "animation",
            "attributename",
            "author",
            "color",
            "command",
            "dimension",
            "id",
            "name",
            "operation",
            "profession",
            "rarity",
            "slot",
            "sound",
            "text",
            "title",
            "translate",
            "type");
    private static final Set<String> COMPOSITE_KEY_PARTS = Set.of(
            "component",
            "data",
            "effects",
            "motion",
            "passengers",
            "position",
            "potentials",
            "rotation",
            "rules",
            "tag");
    private static final Set<String> COMPOSITE_EXACT_KEYS = Set.of(
            "click_event",
            "customname",
            "entity",
            "extra",
            "hover_event",
            "score",
            "spawn_data",
            "spawndata",
            "spawnpotentials",
            "tooltip_display",
            "with");

    private static final Map<String, String> VALUE_EXAMPLES = Map.ofEntries(
            Map.entry(
                    "minecraft:attribute_modifiers",
                    "[{type: \"minecraft:attack_damage\", amount: 1.0d, operation: \"add_value\", id: \"minecraft:modifier\"}]"),
            Map.entry("minecraft:banner_patterns", "[{pattern: \"minecraft:base\", color: \"white\"}]"),
            Map.entry("minecraft:charged_projectiles", "[{id: \"minecraft:arrow\", count: 1}]"),
            Map.entry(
                    "minecraft:consumable",
                    "{consume_seconds: 1.6f, animation: \"eat\", "
                            + "sound: \"minecraft:entity.generic.eat\", has_consume_particles: true, "
                            + "on_consume_effects: []}"),
            Map.entry("minecraft:custom_data", "{}"),
            Map.entry("minecraft:damage_type", "\"minecraft:in_fire\""),
            Map.entry("minecraft:dyed_color", "{rgb: 16777215}"),
            Map.entry("minecraft:enchantments", "{\"minecraft:sharpness\": 1}"),
            Map.entry("minecraft:entity_data", "{id: \"minecraft:zombie\"}"),
            Map.entry("minecraft:food", "{nutrition: 1, saturation: 0.1f, can_always_eat: false}"),
            Map.entry("minecraft:creative_slot_lock", "{}"),
            Map.entry("minecraft:glider", "{}"),
            Map.entry("minecraft:instrument", "\"minecraft:ponder_goat_horn\""),
            Map.entry("minecraft:intangible_projectile", "{}"),
            Map.entry("minecraft:jukebox_playable", "\"minecraft:chirp\""),
            Map.entry(
                    "minecraft:lodestone_tracker",
                    "{tracked: true, target: {dimension: \"minecraft:overworld\", pos: [0, 64, 0]}}"),
            Map.entry("minecraft:lore", "[\n  {\n    text: \"\"\n  }\n]"),
            Map.entry("minecraft:map_decorations", "{decorations: []}"),
            Map.entry("minecraft:potion_contents", "{}"),
            Map.entry("minecraft:profile", "{name: \"Steve\"}"),
            Map.entry("minecraft:block_entity_data", "{id: \"minecraft:chest\"}"),
            Map.entry("minecraft:bundle_contents", "[{id: \"minecraft:stone\", count: 1}]"),
            Map.entry("minecraft:use_cooldown", "{seconds: 1.0f}"),
            Map.entry("minecraft:use_effects", "{can_sprint: true, speed_multiplier: 1.0f, interact_vibrations: true}"),
            Map.entry("minecraft:use_remainder", "{id: \"minecraft:stick\", count: 1}"));
    private static final Map<String, String> VALUE_SNIPPETS = Map.of(
            "click_event", "{action: \"open_url\", url: \"https://example.com\"}",
            "hover_event", "{action: \"show_text\", value: {text: \"\"}}",
            "score", "{name: \"\", objective: \"\"}");

    private static final List<PathRule> PATH_RULES = List.of(
            PathRule.booleanRule("components", "minecraft:use_effects", "can_sprint"),
            PathRule.booleanRule("components", "minecraft:use_effects", "interact_vibrations"),
            PathRule.booleanRule("effects", "ambient"),
            PathRule.booleanRule("effects", "show_icon"),
            PathRule.booleanRule("effects", "show_particles"),
            PathRule.booleanRule("unbreakable"),
            PathRule.numberRule("blockentitytag", "base"),
            PathRule.numberRule("components", "minecraft:consumable", "consume_seconds"),
            PathRule.numberRule("components", "minecraft:food", "nutrition"),
            PathRule.numberRule("components", "minecraft:food", "saturation"),
            PathRule.numberRule("components", "minecraft:map_id"),
            PathRule.numberRule("components", "minecraft:use_cooldown", "seconds"),
            PathRule.numberRule("components", "minecraft:use_effects", "speed_multiplier"),
            PathRule.numberRule("custom_potion_effects", "amplifier"),
            PathRule.numberRule("custom_potion_effects", "duration"),
            PathRule.numberRule("custompotioneffects", "amplifier"),
            PathRule.numberRule("custompotioneffects", "duration"),
            PathRule.numberRule("custompotioneffects", "id"),
            PathRule.numberRule("effects", "amplifier"),
            PathRule.numberRule("effects", "duration"),
            PathRule.numberRule("enchantments", "lvl"),
            PathRule.numberRule("fireworks", "flight"),
            PathRule.numberRule("hover_event", "count"),
            PathRule.numberRule("click_event", "page"),
            PathRule.numberRule("on_consume_effects", "probability"),
            PathRule.numberRule("patterns", "color"),
            PathRule.stringRule("score", "name"),
            PathRule.stringRule("score", "objective"),
            PathRule.stringValues(EQUIPMENT_SLOTS, "attributemodifiers", "slot"),
            PathRule.stringValues(EQUIPMENT_SLOTS, "components", "minecraft:attribute_modifiers", "slot"),
            PathRule.stringValues(EQUIPMENT_SLOTS, "components", "minecraft:attribute_modifiers", "modifiers", "slot"),
            PathRule.stringValues(LEGACY_ATTRIBUTE_OPERATIONS, "attributemodifiers", "operation"),
            PathRule.stringValues(
                    MODERN_ATTRIBUTE_OPERATIONS, "components", "minecraft:attribute_modifiers", "operation"),
            PathRule.stringValues(mergeUnique(TEXT_COLORS, COLOR_EXAMPLES), "display", "color"),
            PathRule.stringValues(List.of("{\"text\":\"Item Name\",\"color\":\"#ffffff\"}"), "display", "name"),
            PathRule.stringValues(List.of("{\"text\":\"Lore line\"}"), "display", "lore"),
            PathRule.stringValues(CONSUMABLE_ANIMATIONS, "components", "minecraft:consumable", "animation"),
            PathRule.stringValues(CONSUME_EFFECT_TYPES, "on_consume_effects", "type"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_ID_ATTRIBUTE, RegistryHint.ATTRIBUTE, "attributemodifiers", "attributename"),
            PathRule.stringRegistry(RawSlotType.VALUE_STRING, RegistryHint.DIMENSION, "dimension"),
            PathRule.stringRegistry(RawSlotType.VALUE_STRING, RegistryHint.VILLAGER_PROFESSION, "profession"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.DIMENSION,
                    "components",
                    "minecraft:lodestone_tracker",
                    "target",
                    "dimension"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING, RegistryHint.INSTRUMENT, "components", "minecraft:instrument"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.PAINTING_VARIANT,
                    "components",
                    "minecraft:painting",
                    "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING, RegistryHint.CAT_VARIANT, "components", "minecraft:cat", "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.CAT_SOUND_VARIANT,
                    "components",
                    "minecraft:cat",
                    "sound_variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.CHICKEN_VARIANT,
                    "components",
                    "minecraft:chicken",
                    "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING, RegistryHint.COW_VARIANT, "components", "minecraft:cow", "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING, RegistryHint.FROG_VARIANT, "components", "minecraft:frog", "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING, RegistryHint.PIG_VARIANT, "components", "minecraft:pig", "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.VILLAGER_VARIANT,
                    "components",
                    "minecraft:villager",
                    "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING, RegistryHint.WOLF_VARIANT, "components", "minecraft:wolf", "variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.WOLF_SOUND_VARIANT,
                    "components",
                    "minecraft:wolf",
                    "sound_variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.ZOMBIE_NAUTILUS_VARIANT,
                    "components",
                    "minecraft:zombie",
                    "nautilus_variant"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_STRING,
                    RegistryHint.LOOT_TABLE,
                    "components",
                    "minecraft:container_loot",
                    "loot_table"),
            PathRule.stringRegistry(RawSlotType.VALUE_STRING, RegistryHint.LOOT_TABLE, "loot_table"),
            PathRule.stringRegistry(RawSlotType.VALUE_STRING, RegistryHint.LOOT_TABLE, "loottable"),
            new PathRule(
                    RawValueMode.STRING,
                    RawSlotType.VALUE_STRING,
                    List.of(),
                    null,
                    List.of(new String[] {"components", "minecraft:attribute_modifiers", "modifiers", "id"}),
                    ""),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_ID_SOUND,
                    RegistryHint.SOUND,
                    "components",
                    "minecraft:instrument",
                    "sound_event",
                    "sound_id"),
            PathRule.stringRegistry(RawSlotType.VALUE_ID_SOUND, RegistryHint.SOUND, "on_consume_effects", "sound"),
            PathRule.stringRegistry(RawSlotType.VALUE_ID_EFFECT, RegistryHint.EFFECT, "custom_potion_effects", "id"),
            PathRule.stringRegistry(RawSlotType.VALUE_ID_EFFECT, RegistryHint.EFFECT, "effects", "id"),
            PathRule.stringRegistry(RawSlotType.VALUE_ID_ENCHANTMENT, RegistryHint.ENCHANTMENT, "enchantments", "id"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_ID_BANNER_PATTERN, RegistryHint.BANNER_PATTERN, "patterns", "pattern"),
            PathRule.stringRegistry(
                    RawSlotType.VALUE_ID_BANNER_PATTERN,
                    RegistryHint.BANNER_PATTERN,
                    "components",
                    "minecraft:banner_patterns",
                    "pattern"),
            PathRule.effectRegistryContains(),
            PathRule.stringRule("attributemodifiers", "name"),
            PathRule.stringRule("click_event", "action"),
            PathRule.stringRule("click_event", "command"),
            PathRule.stringRule("click_event", "path"),
            PathRule.stringRule("click_event", "url"),
            PathRule.stringRule("click_event", "value"),
            PathRule.stringRule("display", "lore"),
            PathRule.stringRule("display", "name"),
            PathRule.stringRule("components", "minecraft:custom_name", "keybind"),
            PathRule.stringRule("components", "minecraft:custom_name", "nbt"),
            PathRule.stringRule("components", "minecraft:custom_name", "selector"),
            PathRule.stringRule("components", "minecraft:custom_name", "translate"),
            PathRule.stringRule("components", "minecraft:item_name", "keybind"),
            PathRule.stringRule("components", "minecraft:item_name", "nbt"),
            PathRule.stringRule("components", "minecraft:item_name", "selector"),
            PathRule.stringRule("components", "minecraft:item_name", "translate"),
            PathRule.stringRule("hover_event", "action"),
            PathRule.stringRule("hover_event", "id"),
            PathRule.stringRule("hover_event", "uuid"),
            PathRule.stringRule("on_consume_effects", "type"));
    private static final List<DirectRegistryRule> DIRECT_REGISTRY_RULES = List.of(
            new DirectRegistryRule(RegistryHint.DIMENSION, "dimension", "", "", "", "dimension"),
            new DirectRegistryRule(RegistryHint.VILLAGER_PROFESSION, "profession", "villager_profession", "", "", ""),
            DirectRegistryRule.contains(
                    RegistryHint.CAT_SOUND_VARIANT, "cat_sound_variant", "minecraft:cat/sound_variant"),
            DirectRegistryRule.contains(RegistryHint.CAT_VARIANT, "cat_variant", "minecraft:cat/variant"),
            DirectRegistryRule.contains(RegistryHint.CHICKEN_VARIANT, "chicken_variant", "minecraft:chicken/variant"),
            DirectRegistryRule.contains(RegistryHint.COW_VARIANT, "cow_variant", "minecraft:cow/variant"),
            DirectRegistryRule.contains(RegistryHint.FROG_VARIANT, "frog_variant", "minecraft:frog/variant"),
            DirectRegistryRule.contains(RegistryHint.PIG_VARIANT, "pig_variant", "minecraft:pig/variant"),
            DirectRegistryRule.contains(
                    RegistryHint.VILLAGER_VARIANT, "villager_variant", "minecraft:villager/variant"),
            DirectRegistryRule.contains(
                    RegistryHint.WOLF_SOUND_VARIANT, "wolf_sound_variant", "minecraft:wolf/sound_variant"),
            DirectRegistryRule.contains(RegistryHint.WOLF_VARIANT, "wolf_variant", "minecraft:wolf/variant"),
            DirectRegistryRule.contains(
                    RegistryHint.ZOMBIE_NAUTILUS_VARIANT,
                    "zombie_nautilus_variant",
                    "minecraft:zombie/nautilus_variant"),
            DirectRegistryRule.contains(
                    RegistryHint.PAINTING_VARIANT, "painting_variant", "minecraft:painting/variant"),
            new DirectRegistryRule(RegistryHint.INSTRUMENT, "minecraft:instrument", "", "instrument_id", "", ""));
    private static final List<RegistryKeywordRule> REGISTRY_KEYWORD_RULES = List.of(
            RegistryKeywordRule.of(RegistryHint.BANNER_PATTERN, "banner", "pattern"),
            RegistryKeywordRule.of(RegistryHint.DAMAGE_TYPE, "damage_type"),
            RegistryKeywordRule.of(RegistryHint.DIMENSION, "dimension"),
            RegistryKeywordRule.of(RegistryHint.INSTRUMENT, "instrument"),
            RegistryKeywordRule.of(RegistryHint.LOOT_TABLE, "loot"),
            RegistryKeywordRule.of(RegistryHint.MAP_DECORATION, "map", "decoration"),
            RegistryKeywordRule.of(RegistryHint.SONG, "song"),
            RegistryKeywordRule.of(RegistryHint.TRIM_MATERIAL, "trim", "material"),
            RegistryKeywordRule.of(RegistryHint.TRIM_PATTERN, "trim", "pattern"),
            RegistryKeywordRule.of(RegistryHint.ATTRIBUTE, "attribute"),
            RegistryKeywordRule.of(RegistryHint.COMPONENT, "component"),
            RegistryKeywordRule.of(RegistryHint.EFFECT, "effect"),
            RegistryKeywordRule.of(RegistryHint.ENCHANTMENT, "enchant"),
            RegistryKeywordRule.of(RegistryHint.ENTITY, "entity"),
            RegistryKeywordRule.of(RegistryHint.POTION, "potion"),
            RegistryKeywordRule.of(RegistryHint.SOUND, "sound"));

    private RawAutocompleteHints() {}

    static List<String> componentsForContext(String itemId, List<String> profiles, RegistryAccess registryAccess) {
        return CATALOG.componentsForContext(itemId, profiles, registryAccess);
    }

    static List<String> objectKeyHints(
            String key, String path, String itemId, List<String> profiles, RegistryAccess registryAccess) {
        return CATALOG.objectKeyHints(key, path, itemId, profiles, registryAccess);
    }

    static List<String> componentFieldHints(
            String componentId, String itemId, List<String> profiles, RegistryAccess registryAccess) {
        return CATALOG.componentFieldHints(componentId, itemId, profiles, registryAccess);
    }

    static List<String> componentIds(RegistryAccess registryAccess) {
        return CATALOG.componentIds(registryAccess);
    }

    static <T> T withExternalLootTableIds(List<String> lootTableIds, Supplier<T> action) {
        List<String> previous = EXTERNAL_LOOT_TABLE_IDS.get();
        EXTERNAL_LOOT_TABLE_IDS.set(cleanExternalIds(lootTableIds));
        try {
            return action.get();
        } finally {
            EXTERNAL_LOOT_TABLE_IDS.set(previous);
        }
    }

    static List<String> topLevelKeys() {
        return CATALOG.topLevelKeys();
    }

    static String valueSnippet(String key) {
        String normalized = normalize(key);
        return Objects.requireNonNullElse(VALUE_EXAMPLES.getOrDefault(normalized, VALUE_SNIPPETS.get(normalized)), "");
    }

    static String valueSnippet(String key, String itemId, RegistryAccess registryAccess) {
        String defaultComponentValue = CATALOG.defaultComponentValue(key, itemId, registryAccess);
        return defaultComponentValue.isBlank() ? valueSnippet(key) : defaultComponentValue;
    }

    static String keySnippetValue(String key) {
        String normalized = normalize(key);
        String snippet = valueSnippet(normalized);
        return switch (normalized) {
            case "components",
                    "minecraft:creative_slot_lock",
                    "minecraft:glider",
                    "minecraft:intangible_projectile",
                    "minecraft:unbreakable" -> "{}";
            case "count",
                    "minecraft:damage",
                    "minecraft:max_damage",
                    "minecraft:max_stack_size",
                    "minecraft:repair_cost" -> "1";
            case "id" -> "\"minecraft:stone\"";
            case "minecraft:custom_name", "minecraft:item_name" -> "{text: \"\"}";
            case "minecraft:lore" -> valueSnippet(normalized);
            case "minecraft:rarity" -> "\"common\"";
            case "minecraft:profile" -> "{name: \"Steve\"}";
            default -> {
                if (!snippet.isBlank()) {
                    yield snippet;
                }
                if (isBooleanLikeKey(normalized)) {
                    yield "false";
                }
                if (isNumericLikeKey(normalized)) {
                    yield "1";
                }
                if (isStringLikeKey(normalized)) {
                    yield "\"\"";
                }
                yield "";
            }
        };
    }

    static String keySnippetValue(String key, String containerPath) {
        String snippet = keySnippetValue(key);
        if (!snippet.isBlank()) {
            return snippet;
        }

        String path = buildFullPath(containerPath, key);
        if (isBooleanPath(path)) {
            return "false";
        }
        if (isNumericPath(path)) {
            return "1";
        }
        if (isStringPath(path)) {
            return "\"\"";
        }
        return "";
    }

    static String keySnippetValue(String key, String itemId, RegistryAccess registryAccess) {
        String snippet = keySnippetValue(key);
        if (!snippet.isBlank()) {
            return snippet;
        }
        return CATALOG.defaultComponentValue(key, itemId, registryAccess);
    }

    static List<String> keyValidationPlaceholders(String key) {
        String snippet = keySnippetValue(key);
        List<String> specific = snippet.isBlank() ? List.of() : List.of(snippet);
        return mergeUnique(specific, List.of("{}", "[]", "\"\"", "1", "false"));
    }

    static List<String> keyValidationPlaceholders(String key, String itemId, RegistryAccess registryAccess) {
        String defaultComponentValue = CATALOG.defaultComponentValue(key, itemId, registryAccess);
        return defaultComponentValue.isBlank()
                ? keyValidationPlaceholders(key)
                : mergeUnique(List.of(defaultComponentValue), keyValidationPlaceholders(key));
    }

    static List<String> integerNumberValues() {
        return INTEGER_NUMBER_VALUES;
    }

    static List<String> floatNumberValues() {
        return FLOAT_NUMBER_VALUES;
    }

    static List<String> numberSuggestionsForCurrentKey(String currentKey, String containerPath) {
        String key = normalize(currentKey);
        return isLikelyIntegerNumberKey(key, buildFullPath(containerPath, key)) ? INTEGER_NUMBER_VALUES : NUMBER_VALUES;
    }

    static List<String> filterNumericHintsForCurrentKey(List<String> values, String currentKey, String containerPath) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        String key = normalize(currentKey);
        if (!isLikelyIntegerNumberKey(key, buildFullPath(containerPath, key))) {
            return values;
        }

        return values.stream()
                .filter(value -> {
                    String normalized = normalize(value);
                    return normalized.matches("^-?\\d+$") || normalized.matches("^-?\\d+[bsil]$");
                })
                .toList();
    }

    static List<String> filterFloatHints(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(value -> {
                    String normalized = normalize(value);
                    return normalized.matches("^-?\\d+\\.\\d+(?:[fd])?$")
                            || normalized.matches("^-?\\d+(?:[fd])$")
                            || normalized.equals("infinityf")
                            || normalized.equals("infinityd")
                            || normalized.equals("nanf")
                            || normalized.equals("nand");
                })
                .toList();
    }

    static RawSlotType classifySlotType(
            String currentKey,
            String containerKey,
            String containerPath,
            boolean insideQuote,
            List<String> activeProfiles,
            String itemId,
            RegistryAccess registryAccess) {
        String key = normalize(currentKey);
        String container = normalize(containerKey);
        String path = buildFullPath(containerPath, key);
        RawSlotType idSlot = classifyIdSlot(key, container, path);
        if (idSlot == RawSlotType.VALUE_UNKNOWN && !valueSnippet(key).isBlank()) {
            return RawSlotType.VALUE_UNKNOWN;
        }
        if (insideQuote) {
            return idSlot == RawSlotType.VALUE_UNKNOWN ? RawSlotType.VALUE_STRING : idSlot;
        }
        if (idSlot != RawSlotType.VALUE_UNKNOWN) {
            return idSlot;
        }
        if (isBooleanPath(path) || isBooleanLikeKey(key)) {
            return RawSlotType.VALUE_BOOLEAN;
        }
        if (isTextComponentPath(path) && ("color".equals(key) || isStringLikeKey(key))) {
            return RawSlotType.VALUE_STRING;
        }
        if (isNumericPath(path) || isNumericLikeKey(key)) {
            return isLikelyIntegerNumberKey(key, path) ? RawSlotType.VALUE_INT : RawSlotType.VALUE_FLOAT;
        }
        if ("effects".equals(key)) {
            return RawSlotType.VALUE_UNKNOWN;
        }
        if (isStringPath(path) || isStringLikeKey(key)) {
            return RawSlotType.VALUE_STRING;
        }

        RawSlotType catalogType =
                catalogSlotTypeForPath(currentKey, containerPath, path, activeProfiles, itemId, registryAccess);
        if (catalogType != RawSlotType.VALUE_UNKNOWN) {
            return catalogType;
        }

        EnumSet<RawValueMode> pathModes = inferModesFromPath(path);
        if (pathModes.size() != 1) {
            return RawSlotType.VALUE_UNKNOWN;
        }
        return switch (pathModes.iterator().next()) {
            case BOOLEAN -> RawSlotType.VALUE_BOOLEAN;
            case NUMBER -> isLikelyIntegerNumberKey(key, path) ? RawSlotType.VALUE_INT : RawSlotType.VALUE_FLOAT;
            case STRING -> RawSlotType.VALUE_STRING;
            default -> RawSlotType.VALUE_UNKNOWN;
        };
    }

    static EnumSet<RawValueMode> expectedModesForSlot(RawSlotType slotType) {
        return switch (slotType) {
            case OBJECT_KEY, VALUE_UNKNOWN -> EnumSet.of(RawValueMode.NONE);
            case VALUE_BOOLEAN -> EnumSet.of(RawValueMode.BOOLEAN);
            case VALUE_INT, VALUE_FLOAT -> EnumSet.of(RawValueMode.NUMBER);
            case VALUE_ID_ATTRIBUTE,
                    VALUE_ID_BANNER_PATTERN,
                    VALUE_ID_BLOCK_ENTITY,
                    VALUE_ID_COMPONENT,
                    VALUE_ID_DAMAGE_TYPE,
                    VALUE_ID_EFFECT,
                    VALUE_ID_ENCHANTMENT,
                    VALUE_ID_ENTITY,
                    VALUE_ID_ITEM,
                    VALUE_ID_POTION,
                    VALUE_ID_SOUND,
                    VALUE_ID_TRIM_MATERIAL,
                    VALUE_ID_TRIM_PATTERN,
                    VALUE_STRING -> EnumSet.of(RawValueMode.STRING);
        };
    }

    static List<String> registryHintsForSlot(
            RawSlotType slotType,
            String currentKey,
            String containerKey,
            String containerPath,
            RegistryAccess registryAccess) {
        String key = normalize(currentKey);
        String path = buildFullPath(containerPath, key);
        List<String> tagHints = tagValueHints(key, path, registryAccess);
        if (isTagOnlyValuePath(key, path)) {
            return tagHints;
        }

        RegistryHint fixed =
                switch (slotType) {
                    case VALUE_ID_ATTRIBUTE -> RegistryHint.ATTRIBUTE;
                    case VALUE_ID_BANNER_PATTERN -> RegistryHint.BANNER_PATTERN;
                    case VALUE_ID_BLOCK_ENTITY -> RegistryHint.BLOCK_ENTITY;
                    case VALUE_ID_COMPONENT -> RegistryHint.COMPONENT;
                    case VALUE_ID_DAMAGE_TYPE -> RegistryHint.DAMAGE_TYPE;
                    case VALUE_ID_EFFECT -> RegistryHint.EFFECT;
                    case VALUE_ID_ENCHANTMENT -> RegistryHint.ENCHANTMENT;
                    case VALUE_ID_ENTITY -> RegistryHint.ENTITY;
                    case VALUE_ID_ITEM -> RegistryHint.ITEM;
                    case VALUE_ID_POTION -> RegistryHint.POTION;
                    case VALUE_ID_SOUND -> RegistryHint.SOUND;
                    case VALUE_ID_TRIM_MATERIAL -> RegistryHint.TRIM_MATERIAL;
                    case VALUE_ID_TRIM_PATTERN -> RegistryHint.TRIM_PATTERN;
                    default -> null;
                };
        return fixed == null
                ? guessRegistryIds(currentKey, containerKey, containerPath, registryAccess)
                : ids(fixed, registryAccess);
    }

    static List<String> validatedRegistryHintsForValue(
            String text,
            int replaceStart,
            int cursor,
            boolean insideQuote,
            String currentKey,
            String containerKey,
            String containerPath,
            RegistryAccess registryAccess) {
        if (registryAccess == null || text == null || text.length() > REGISTRY_BINDING_PROBE_MAX_TEXT_LENGTH) {
            return List.of();
        }

        for (RegistryHint hint : validatedRegistryCandidates(
                text, replaceStart, cursor, insideQuote, currentKey, containerKey, containerPath, registryAccess)) {
            List<String> values = ids(hint, registryAccess);
            if (!values.isEmpty()
                    && acceptsRegistryValue(text, replaceStart, cursor, insideQuote, values, registryAccess)) {
                return values;
            }
        }
        return List.of();
    }

    static List<String> contextualKeyHints(String containerKey, String containerPath) {
        String container = normalize(containerKey);
        if (container.isBlank()) {
            return List.of();
        }

        String path = normalize(containerPath);
        List<String> arrayEntryHints = arrayEntryKeyHints(container, path);
        if (!arrayEntryHints.isEmpty()) {
            return arrayEntryHints;
        }
        if (isItemStackObjectContext(container, path)) {
            return CATALOG.objectKeyHints("item", path, "", List.of(), null);
        }
        if (isMapDecorationContext(container, path)) {
            return CATALOG.objectKeyHints("map_decoration", path, "", List.of(), null);
        }
        if ("data".equals(container) && path.contains("/spawnpotentials/data")) {
            return CATALOG.objectKeyHints("spawndata", path, "", List.of(), null);
        }
        if (container.contains("block_entity_data")) {
            return CATALOG.objectKeyHints("block_entity_data", path, "", List.of(), null);
        }
        if ("entity".equals(container)
                && (path.contains("/spawndata/entity") || path.contains("/spawnpotentials/data/entity"))) {
            return mergeUnique(
                    CATALOG.objectKeyHints("entity", path, "", List.of(), null),
                    CATALOG.objectKeyHints("spawndata", path, "", List.of(), null));
        }
        if (container.contains("entity")) {
            return CATALOG.objectKeyHints("entity", path, "", List.of(), null);
        }
        if (container.contains("name") || container.contains("text")) {
            return CATALOG.objectKeyHints("text", path, "", List.of(), null);
        }
        return CATALOG.objectKeyHints(container, path, "", List.of(), null);
    }

    static List<String> objectKeyHintsForSiblingKeys(List<String> siblingKeys) {
        if (siblingKeys == null || siblingKeys.isEmpty()) {
            return List.of();
        }

        Set<String> keys = new LinkedHashSet<>();
        for (String siblingKey : siblingKeys) {
            if (siblingKey != null && !siblingKey.isBlank()) {
                keys.add(normalize(siblingKey));
            }
        }
        if (keys.isEmpty()) {
            return List.of();
        }
        if (keys.contains("id") || keys.contains("count") || keys.contains("components")) {
            return topLevelKeys();
        }
        if (keys.contains("text")
                || keys.contains("extra")
                || keys.contains("bold")
                || keys.contains("italic")
                || keys.contains("underlined")
                || keys.contains("strikethrough")
                || keys.contains("obfuscated")) {
            return CATALOG.objectKeyHints("text", "", "", List.of(), null);
        }
        if (keys.contains("amount") || keys.contains("operation") || keys.contains("slot")) {
            return CATALOG.objectKeyHints("attribute_modifier", "", "", List.of(), null);
        }
        if (keys.contains("pattern") && keys.contains("color")) {
            return CATALOG.objectKeyHints("banner_pattern", "", "", List.of(), null);
        }
        if (keys.contains("amplifier")
                || keys.contains("duration")
                || keys.contains("show_particles")
                || keys.contains("show_icon")) {
            return CATALOG.objectKeyHints("effects", "", "", List.of(), null);
        }
        if (keys.contains("probability") || keys.contains("sound")) {
            return CATALOG.objectKeyHints("on_consume_effects", "", "", List.of(), null);
        }
        return List.of();
    }

    static List<String> objectKeyHintsForSiblingValues(
            String containerKey, String containerPath, Map<String, String> siblingValues) {
        if (siblingValues == null || siblingValues.isEmpty()) {
            return List.of();
        }

        String container = normalize(containerKey);
        String path = normalize(containerPath);
        String action = normalizedSiblingValue(siblingValues, "action");
        if ("hover_event".equals(container) || path.endsWith("hover_event")) {
            return switch (action) {
                case "show_text" -> List.of("value");
                case "show_item" -> List.of("id", "count", "components");
                case "show_entity" -> List.of("id", "name", "uuid");
                default -> List.of();
            };
        }
        if ("click_event".equals(container) || path.endsWith("click_event")) {
            return switch (action) {
                case "open_url" -> List.of("url");
                case "open_file" -> List.of("path");
                case "run_command", "suggest_command" -> List.of("command");
                case "change_page" -> List.of("page");
                case "copy_to_clipboard" -> List.of("value");
                default -> List.of();
            };
        }
        return List.of();
    }

    private static List<String> arrayEntryKeyHints(String container, String path) {
        if (container.isBlank() || path.isBlank()) {
            return List.of();
        }
        if ("custom_effects".equals(container)
                && pathEndsWith(path, "components", "minecraft:potion_contents", "custom_effects")) {
            return CATALOG.objectKeyHints("custom_potion_effects", path, "", List.of(), null);
        }
        if ("modifiers".equals(container)
                && pathEndsWith(path, "components", "minecraft:attribute_modifiers", "modifiers")) {
            return CATALOG.objectKeyHints("attribute_modifier", path, "", List.of(), null);
        }
        if ("patterns".equals(container) && pathEndsWith(path, "components", "minecraft:banner_patterns", "patterns")) {
            return CATALOG.objectKeyHints("banner_pattern", path, "", List.of(), null);
        }
        if ("effects".equals(container)
                && (pathEndsWith(path, "components", "minecraft:suspicious_stew_effects", "effects")
                        || path.contains("/on_consume_effects/effects"))) {
            return CATALOG.objectKeyHints("effects", path, "", List.of(), null);
        }
        if ("explosions".equals(container) && pathEndsWith(path, "components", "minecraft:fireworks", "explosions")) {
            return CATALOG.objectKeyHints("explosions", path, "", List.of(), null);
        }
        if ("minecraft:container".equals(container) && pathEndsWith(path, "components", "minecraft:container")) {
            return CATALOG.objectKeyHints("container_entry", path, "", List.of(), null);
        }
        if ("minecraft:bundle_contents".equals(container)
                && pathEndsWith(path, "components", "minecraft:bundle_contents")) {
            return CATALOG.objectKeyHints("item", path, "", List.of(), null);
        }
        if ("minecraft:charged_projectiles".equals(container)
                && pathEndsWith(path, "components", "minecraft:charged_projectiles")) {
            return CATALOG.objectKeyHints("item", path, "", List.of(), null);
        }
        if ("items".equals(container)
                && (pathEndsWith(path, "components", "minecraft:container", "items")
                        || pathEndsWith(path, "components", "minecraft:bundle_contents", "items"))) {
            return CATALOG.objectKeyHints("items", path, "", List.of(), null);
        }
        if ("item".equals(container) && path.contains("minecraft:container")) {
            return CATALOG.objectKeyHints("item", path, "", List.of(), null);
        }
        return List.of();
    }

    private static boolean isItemStackObjectContext(String container, String path) {
        return "minecraft:use_remainder".equals(container)
                        && pathEndsWith(path, "components", "minecraft:use_remainder")
                || "item".equals(container)
                        && (path.contains("/entityplacer/item") || path.contains("/passengers/item"))
                || "value".equals(container) && path.endsWith("/hover_event/value");
    }

    private static boolean isMapDecorationContext(String container, String path) {
        return !container.isBlank()
                && path.contains("components/minecraft:map_decorations/")
                && !pathEndsWith(path, "components", "minecraft:map_decorations");
    }

    static boolean shouldUseStrictContextKeySuggestions(
            RawAutocompleteIndex.Context context,
            List<String> contextualKeys,
            List<String> catalogObjectKeys,
            List<String> componentNbtFields,
            List<String> dynamicKeyHints,
            List<String> profileComponents) {
        if (context == null) {
            return false;
        }
        return context.inRootObject()
                || context.inComponentsObject()
                || !Objects.requireNonNullElse(context.containerPath(), "").isBlank()
                || !Objects.requireNonNullElse(context.containerKey(), "").isBlank()
                || hasItems(contextualKeys)
                || hasItems(catalogObjectKeys)
                || hasItems(componentNbtFields)
                || hasItems(dynamicKeyHints)
                || hasItems(profileComponents);
    }

    static List<String> dynamicKeyHints(String containerKey, String containerPath, RegistryAccess registryAccess) {
        return registryMapKeys(containerKey, containerPath, registryAccess);
    }

    static String registryMapKeySnippetValue(String containerKey, String containerPath) {
        List<RegistryHint> hints = registryMapHints(containerKey, containerPath);
        if (hints.isEmpty()) {
            return "";
        }
        return hints.getFirst() == RegistryHint.ENCHANTMENT ? "1" : "";
    }

    static List<String> validatedRegistryMapKeyHints(
            String text,
            int replaceStart,
            int replaceEnd,
            String containerKey,
            String containerPath,
            RegistryAccess registryAccess) {
        if (registryAccess == null || text == null || text.length() > REGISTRY_BINDING_PROBE_MAX_TEXT_LENGTH) {
            return List.of();
        }

        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeEnd = Math.clamp(replaceEnd, safeStart, text.length());
        String prefix = text.substring(0, safeStart);
        String suffix = text.substring(safeEnd);
        for (RegistryHint hint : registryMapHints(containerKey, containerPath)) {
            List<String> ids = ids(hint, registryAccess);
            if (!ids.isEmpty() && registryMapKeyAccepted(prefix, suffix, ids, registryAccess)) {
                return ids;
            }
        }
        return List.of();
    }

    static List<String> contextualValueHints(String currentKey, String containerPath, RegistryAccess registryAccess) {
        String key = normalize(currentKey);
        if (key.isBlank()) {
            return List.of();
        }
        String path = buildFullPath(containerPath, key);
        if (isTextComponentPath(path) && textStyleBooleanKey(key)) {
            return BOOLEAN_VALUES;
        }
        List<String> debugStickProperties = debugStickStateProperties(key, path, registryAccess);
        if (!debugStickProperties.isEmpty()) {
            return debugStickProperties;
        }
        List<String> tagHints = tagValueHints(key, path, registryAccess);
        return switch (key) {
            case "color" -> mergeUnique(TEXT_COLORS, COLOR_EXAMPLES);
            case "effects" -> List.of("[{id: \"minecraft:speed\", amplifier: 0, duration: 200}]");
            case "sound" -> ids(RegistryHint.SOUND, registryAccess);
            case "action" -> {
                List<String> pathHints = pathValueHints(path, registryAccess);
                if (pathEndsWith(path, "hover_event", "action")) {
                    yield HOVER_EVENT_ACTIONS;
                }
                if (pathEndsWith(path, "click_event", "action")) {
                    yield CLICK_EVENT_ACTIONS;
                }
                yield mergeUnique(pathHints, tagHints);
            }
            default -> mergeUnique(pathValueHints(path, registryAccess), tagHints);
        };
    }

    static List<String> siblingValueHints(String currentKey, String containerPath, Map<String, String> siblingValues) {
        if (siblingValues == null || siblingValues.isEmpty()) {
            return List.of();
        }

        String key = normalize(currentKey);
        String path = normalize(containerPath);
        String type = normalizedSiblingValue(siblingValues, "type");
        String action = normalizedSiblingValue(siblingValues, "action");
        if ("effects".equals(key) && "minecraft:apply_effects".equals(type)) {
            return List.of("[{id: \"minecraft:speed\", amplifier: 0, duration: 200}]");
        }
        if ("sound".equals(key) && "minecraft:play_sound".equals(type)) {
            return List.of("minecraft:entity.generic.eat");
        }
        if ("operation".equals(key)
                && (siblingValues.containsKey("amount")
                        || siblingValues.containsKey("type")
                        || siblingValues.containsKey("attribute")
                        || siblingValues.containsKey("attributename")
                        || path.contains("attribute_modifiers"))) {
            return MODERN_ATTRIBUTE_OPERATIONS;
        }
        if ("value".equals(key) && "show_text".equals(action)) {
            return List.of("{text: \"\"}");
        }
        if ("name".equals(key) && "show_entity".equals(action)) {
            return List.of("{text: \"\"}");
        }
        String siblingExample = valueExampleFromSibling(siblingValues.get(key));
        if (!siblingExample.isBlank()) {
            return List.of(siblingExample);
        }
        return List.of();
    }

    static List<String> siblingRegistryValueHints(
            String currentKey, String containerPath, Map<String, String> siblingValues, RegistryAccess registryAccess) {
        if (siblingValues == null || siblingValues.isEmpty()) {
            return List.of();
        }

        String key = normalize(currentKey);
        String path = normalize(containerPath);
        String action = normalizedSiblingValue(siblingValues, "action");
        if ("id".equals(key) && path.endsWith("hover_event")) {
            if ("show_item".equals(action)) {
                return ids(RegistryHint.ITEM, registryAccess);
            }
            if ("show_entity".equals(action)) {
                return ids(RegistryHint.ENTITY, registryAccess);
            }
        }
        if (("id".equals(key) || "type".equals(key)) && siblingValues.containsKey("lvl")) {
            return ids(RegistryHint.ENCHANTMENT, registryAccess);
        }
        if (("attribute".equals(key) || "attributename".equals(key) || "type".equals(key))
                && (siblingValues.containsKey("amount")
                        || siblingValues.containsKey("operation")
                        || path.contains("attribute_modifiers"))) {
            return ids(RegistryHint.ATTRIBUTE, registryAccess);
        }
        if (("sound".equals(key) || "sound_id".equals(key))
                && (siblingValues.containsKey("range") || siblingValues.containsKey("use_duration"))) {
            return ids(RegistryHint.SOUND, registryAccess);
        }
        if ("blocks".equals(key)
                && (siblingValues.containsKey("speed") || siblingValues.containsKey("correct_for_drops"))) {
            return mergeUnique(ids(RegistryHint.BLOCK, registryAccess), tagIds(RegistryHint.BLOCK, registryAccess));
        }
        return List.of();
    }

    static EnumSet<RawValueMode> expectedValueModes(
            String currentKey, String containerPath, String prefix, boolean insideQuote) {
        if (insideQuote) {
            return EnumSet.of(RawValueMode.STRING);
        }

        String key = normalize(currentKey);
        String path = buildFullPath(containerPath, key);
        if ("effects".equals(key)) {
            return EnumSet.of(RawValueMode.NONE);
        }
        EnumSet<RawValueMode> modes = inferModesFromPath(path);
        if (!key.isBlank()) {
            addIf(modes, RawValueMode.BOOLEAN, isBooleanLikeKey(key));
            addIf(modes, RawValueMode.NUMBER, isNumericLikeKey(key));
            addIf(modes, RawValueMode.STRING, isStringLikeKey(key));
        }

        if (!modes.isEmpty()) {
            return modes;
        }

        String lowerPrefix = normalize(prefix);
        return lowerPrefix.startsWith("t") || lowerPrefix.startsWith("f") || lowerPrefix.startsWith("n")
                ? EnumSet.of(RawValueMode.LITERAL)
                : EnumSet.of(RawValueMode.NONE);
    }

    static EnumSet<RawValueMode> refineExpectedModesWithSiblingValues(
            String currentKey,
            String containerPath,
            Map<String, String> siblingValues,
            EnumSet<RawValueMode> expectedModes,
            boolean insideQuote) {
        if (insideQuote || siblingValues == null || siblingValues.isEmpty()) {
            return expectedModes;
        }

        String key = normalize(currentKey);
        String path = normalize(containerPath);
        String type = normalizedSiblingValue(siblingValues, "type");
        String action = normalizedSiblingValue(siblingValues, "action");
        if ("effects".equals(key) && "minecraft:apply_effects".equals(type)) {
            return EnumSet.of(RawValueMode.NONE);
        }
        if ("sound".equals(key) && "minecraft:play_sound".equals(type)) {
            return EnumSet.of(RawValueMode.STRING);
        }
        if ("operation".equals(key)
                && (siblingValues.containsKey("amount")
                        || siblingValues.containsKey("type")
                        || path.contains("attribute_modifiers"))) {
            return EnumSet.of(RawValueMode.STRING);
        }
        if ("components".equals(key) && "show_item".equals(action)
                || "name".equals(key) && "show_entity".equals(action)
                || "value".equals(key) && "show_text".equals(action)) {
            return EnumSet.of(RawValueMode.NONE);
        }
        if (("count".equals(key) && "show_item".equals(action))
                || ("page".equals(key) && path.endsWith("click_event"))) {
            return EnumSet.of(RawValueMode.NUMBER);
        }
        RawValueMode siblingMode = RawValueClassifier.classify(siblingValues.get(key));
        return siblingMode == RawValueMode.NONE || siblingMode == RawValueMode.LITERAL
                ? expectedModes
                : EnumSet.of(siblingMode);
    }

    static RawSlotType refineSlotTypeWithSiblingValues(
            RawSlotType slotType, String currentKey, String containerPath, Map<String, String> siblingValues) {
        if (slotType != RawSlotType.VALUE_UNKNOWN || siblingValues == null || siblingValues.isEmpty()) {
            return slotType;
        }

        String key = normalize(currentKey);
        String path = normalize(containerPath);
        if ("id".equals(key) && siblingValues.containsKey("count")) {
            return RawSlotType.VALUE_ID_ITEM;
        }
        if ("type".equals(key)
                && (siblingValues.containsKey("amount")
                        || siblingValues.containsKey("operation")
                        || siblingValues.containsKey("attribute")
                        || siblingValues.containsKey("attributename")
                        || path.contains("attribute_modifiers"))) {
            return RawSlotType.VALUE_ID_ATTRIBUTE;
        }
        if (("attribute".equals(key) || "attributename".equals(key))
                && (siblingValues.containsKey("amount")
                        || siblingValues.containsKey("operation")
                        || path.contains("attribute_modifiers"))) {
            return RawSlotType.VALUE_ID_ATTRIBUTE;
        }
        if ("id".equals(key) && siblingValues.containsKey("lvl")) {
            return RawSlotType.VALUE_ID_ENCHANTMENT;
        }
        if ("id".equals(key)
                && (siblingValues.containsKey("amplifier")
                        || siblingValues.containsKey("duration")
                        || siblingValues.containsKey("show_particles"))) {
            return RawSlotType.VALUE_ID_EFFECT;
        }
        if ("pattern".equals(key) && siblingValues.containsKey("color")) {
            return RawSlotType.VALUE_ID_BANNER_PATTERN;
        }
        return slotType;
    }

    static EnumSet<RawValueMode> refineExpectedModesWithRuntimeProbe(
            String text,
            int replaceStart,
            int cursor,
            String currentKey,
            String containerPath,
            RegistryAccess registryAccess,
            EnumSet<RawValueMode> expectedModes,
            boolean insideQuote) {
        if (insideQuote || text == null || text.length() > RUNTIME_PROBE_MAX_TEXT_LENGTH) {
            return expectedModes;
        }
        if (hasCompositeSnippet(currentKey)) {
            return expectedModes;
        }

        String pathCacheKey = runtimePathProbeCacheKey(currentKey, containerPath, registryAccess);
        if (!pathCacheKey.isBlank()) {
            synchronized (RUNTIME_PROBE_CACHE_LOCK) {
                EnumSet<RawValueMode> cached = PATH_PROBE_CACHE.get(pathCacheKey);
                if (cached != null) {
                    return mergeRuntimeModes(expectedModes, cached);
                }
            }
        }

        EnumSet<RawValueMode> runtimeModes = runtimeProbeModes(text, replaceStart, cursor, registryAccess);
        if (runtimeModes.isEmpty()) {
            return expectedModes;
        }

        if (!pathCacheKey.isBlank()) {
            synchronized (RUNTIME_PROBE_CACHE_LOCK) {
                PATH_PROBE_CACHE.put(pathCacheKey, EnumSet.copyOf(runtimeModes));
            }
        }
        return mergeRuntimeModes(expectedModes, runtimeModes);
    }

    private static EnumSet<RawValueMode> mergeRuntimeModes(
            EnumSet<RawValueMode> expectedModes, EnumSet<RawValueMode> runtimeModes) {
        if (expectedModes == null || expectedModes.isEmpty() || expectedModes.contains(RawValueMode.NONE)) {
            return runtimeModes;
        }
        EnumSet<RawValueMode> overlap = EnumSet.copyOf(expectedModes);
        overlap.retainAll(runtimeModes);
        return overlap.isEmpty() ? runtimeModes : overlap;
    }

    static List<String> filterValuesByModes(List<String> values, EnumSet<RawValueMode> expectedModes) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        if (expectedModes == null || expectedModes.isEmpty() || expectedModes.contains(RawValueMode.NONE)) {
            return values;
        }

        return values.stream()
                .filter(value -> {
                    RawValueMode mode = RawValueClassifier.classify(value);
                    return mode != RawValueMode.NONE && expectedModes.contains(mode);
                })
                .toList();
    }

    static String buildFullPath(String containerPath, String currentKey) {
        String container = normalize(containerPath);
        String key = normalize(currentKey);
        if (container.isBlank()) {
            return key;
        }
        return key.isBlank() ? container : container + "/" + key;
    }

    static boolean isBooleanPath(String path) {
        return pathHasMode(path, RawValueMode.BOOLEAN);
    }

    static List<String> inferItemProfiles(String itemId, RegistryAccess registryAccess) {
        return RUNTIME.itemProfiles(itemId, registryAccess);
    }

    static List<String> mergeUnique(List<String> primary, List<String> secondary) {
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

    private static RawSlotType catalogSlotTypeForPath(
            String currentKey,
            String containerPath,
            String fullPath,
            List<String> activeProfiles,
            String itemId,
            RegistryAccess registryAccess) {
        String key = normalize(currentKey);
        String componentId = activeComponentIdFromContainerPath(containerPath);
        if (key.isBlank() || componentId.isBlank()) {
            return RawSlotType.VALUE_UNKNOWN;
        }
        if (CATALOG.componentFieldHints(componentId, itemId, activeProfiles, registryAccess).stream()
                .noneMatch(field -> field != null && field.equalsIgnoreCase(key))) {
            return RawSlotType.VALUE_UNKNOWN;
        }

        RawSlotType idSlot = classifyIdSlot(key, componentId, fullPath);
        if (idSlot != RawSlotType.VALUE_UNKNOWN) {
            return idSlot;
        }
        if (isBooleanLikeKey(key)) {
            return RawSlotType.VALUE_BOOLEAN;
        }
        if (isNumericLikeKey(key)) {
            return isLikelyIntegerNumberKey(key, fullPath) ? RawSlotType.VALUE_INT : RawSlotType.VALUE_FLOAT;
        }
        if (!CATALOG.objectKeyHints(key, fullPath, itemId, activeProfiles, registryAccess)
                        .isEmpty()
                || isLikelyCompositeFieldName(key)) {
            return RawSlotType.VALUE_UNKNOWN;
        }
        return RawSlotType.VALUE_STRING;
    }

    private static RawSlotType classifyIdSlot(String key, String container, String path) {
        RawSlotType pathSlot = pathSlotType(path);
        if (pathSlot != RawSlotType.VALUE_UNKNOWN) {
            return pathSlot;
        }
        if (isNumericPath(path) || isBooleanPath(path)) {
            return RawSlotType.VALUE_UNKNOWN;
        }
        if (path.contains("trim") && path.contains("material")) {
            return RawSlotType.VALUE_ID_TRIM_MATERIAL;
        }
        if (path.contains("trim") && path.contains("pattern")) {
            return RawSlotType.VALUE_ID_TRIM_PATTERN;
        }
        if (path.contains("damage_type")) {
            return RawSlotType.VALUE_ID_DAMAGE_TYPE;
        }
        if (!"id".equals(key) && !key.endsWith("_id")) {
            return RawSlotType.VALUE_UNKNOWN;
        }
        if (isBlockEntityIdPath(container, path)) {
            return RawSlotType.VALUE_ID_BLOCK_ENTITY;
        }
        if (container.contains("effect")) {
            return RawSlotType.VALUE_ID_EFFECT;
        }
        if (container.contains("enchant")) {
            return RawSlotType.VALUE_ID_ENCHANTMENT;
        }
        if (container.contains("entity") || container.contains("spawn")) {
            return RawSlotType.VALUE_ID_ENTITY;
        }
        if (container.contains("component")) {
            return RawSlotType.VALUE_ID_COMPONENT;
        }
        if (container.contains("potion")) {
            return RawSlotType.VALUE_ID_POTION;
        }
        return RawSlotType.VALUE_ID_ITEM;
    }

    private static List<String> guessRegistryIds(
            String currentKey, String containerKey, String containerPath, RegistryAccess registryAccess) {
        String key = normalize(currentKey);
        if (key.isBlank()) {
            return List.of();
        }

        String container = normalize(containerKey);
        String path = buildFullPath(containerPath, key);
        List<String> pathHints = pathValueHints(path, registryAccess);
        if (!pathHints.isEmpty()) {
            return pathHints;
        }

        if ("slot".equals(key)) {
            return EQUIPMENT_SLOTS;
        }
        if ("type".equals(key) && "on_consume_effects".equals(container)) {
            return CONSUME_EFFECT_TYPES;
        }
        List<String> tagHints = tagValueHints(key, path, registryAccess);
        if (isTagOnlyValuePath(key, path)) {
            return tagHints;
        }
        List<String> registryIds = ids(registryCandidates(currentKey, containerKey, containerPath), registryAccess);
        return mergeUnique(registryIds, tagHints);
    }

    private static List<String> tagValueHints(String key, String path, RegistryAccess registryAccess) {
        if (key.isBlank() || path.isBlank()) {
            return List.of();
        }
        if (isBannerPatternTagPath(key, path)) {
            return tagIds(RegistryHint.BANNER_PATTERN, registryAccess);
        }
        if (isBlockTagPath(key, path)) {
            return tagIds(RegistryHint.BLOCK, registryAccess);
        }
        if (isDamageTypeTagPath(key, path)) {
            return tagIds(RegistryHint.DAMAGE_TYPE, registryAccess);
        }
        if (isEnchantmentTagPath(key, path)) {
            return tagIds(RegistryHint.ENCHANTMENT, registryAccess);
        }
        if (isEntityTagPath(key, path)) {
            return tagIds(RegistryHint.ENTITY, registryAccess);
        }
        if (isItemTagPath(key, path)) {
            return tagIds(RegistryHint.ITEM, registryAccess);
        }
        return List.of();
    }

    private static boolean isBannerPatternTagPath(String key, String path) {
        return "minecraft:provides_banner_patterns".equals(key)
                || pathEndsWith(path, "components", "minecraft:provides_banner_patterns");
    }

    private static boolean isTagOnlyValuePath(String key, String path) {
        return isBannerPatternTagPath(key, path)
                || isBlockTagPath(key, path)
                || isDamageTypeTagPath(key, path)
                || isEnchantmentTagPath(key, path)
                || isEntityTagPath(key, path)
                || isItemTagPath(key, path);
    }

    private static boolean isBlockTagPath(String key, String path) {
        return "blocks".equals(key)
                || key.endsWith("_blocks")
                || key.endsWith("_block")
                || pathEndsWith(path, "components", "minecraft:tool", "rules", "blocks");
    }

    private static boolean isDamageTypeTagPath(String key, String path) {
        return key.endsWith("_damage_types")
                || "damage_types".equals(key)
                || "bypassed_by".equals(key) && path.contains("damage_type");
    }

    private static boolean isEnchantmentTagPath(String key, String path) {
        return key.endsWith("_enchantments")
                || "enchantments".equals(key) && path.contains("tag")
                || path.contains("enchantment_tags");
    }

    private static boolean isEntityTagPath(String key, String path) {
        return key.endsWith("_entities")
                || key.endsWith("_entity_types")
                || "entity_types".equals(key)
                || path.contains("entity_type_tags");
    }

    private static boolean isItemTagPath(String key, String path) {
        return "items".equals(key)
                || key.endsWith("_items")
                || key.endsWith("_item")
                || pathEndsWith(path, "components", "minecraft:repairable", "items");
    }

    private static List<RegistryHint> registryCandidates(String currentKey, String containerKey, String containerPath) {
        String key = normalize(currentKey);
        if (key.isBlank()) {
            return List.of();
        }

        String container = normalize(containerKey);
        String path = buildFullPath(containerPath, key);
        List<RegistryHint> direct = directRegistryHintFromKeyAndPath(key, path);
        if (!direct.isEmpty()) {
            return direct;
        }
        if ("id".equals(key)) {
            if (isBlockEntityIdPath(container, path)) {
                return List.of(RegistryHint.BLOCK_ENTITY);
            }
            if (container.contains("effect") || path.contains("effects")) {
                return List.of(RegistryHint.EFFECT);
            }
            if (container.contains("enchant")) {
                return List.of(RegistryHint.ENCHANTMENT);
            }
            if (container.contains("entity") || path.contains("entity_data") || path.contains("spawn")) {
                return List.of(RegistryHint.ENTITY);
            }
            List<RegistryHint> semantic = semanticRegistryHintsFromText(container, true, true);
            if (!semantic.isEmpty()) {
                return semantic;
            }
            return List.of(RegistryHint.ITEM);
        }
        if (key.endsWith("item") || key.endsWith("item_id")) {
            return List.of(RegistryHint.ITEM);
        }
        if ("type".equals(key) && path.contains("minecraft:attribute_modifiers")) {
            return List.of(RegistryHint.ATTRIBUTE);
        }
        if ("type".equals(key) && path.contains("minecraft:map_decorations")) {
            return List.of(RegistryHint.MAP_DECORATION);
        }
        if (key.contains("damage_type") || path.contains("damage_type")) {
            return List.of(RegistryHint.DAMAGE_TYPE);
        }
        if (path.contains("hidden_components")) {
            return List.of(RegistryHint.COMPONENT);
        }
        if ("blocks".equals(key) || key.endsWith("_blocks")) {
            return List.of(RegistryHint.BLOCK);
        }
        if ("potion".equals(key) || key.endsWith("_potion")) {
            return List.of(RegistryHint.POTION);
        }
        if (key.contains("sound")) {
            return List.of(RegistryHint.SOUND);
        }
        if (key.contains("jukebox_playable") || path.contains("jukebox_playable")) {
            return List.of(RegistryHint.SONG);
        }
        if (key.contains("loot") && key.contains("table")
                || path.contains("loot_table")
                || path.contains("loottable")) {
            return List.of(RegistryHint.LOOT_TABLE);
        }
        if (key.contains("attribute")) {
            return List.of(RegistryHint.ATTRIBUTE);
        }
        if (key.contains("effect")) {
            return List.of(RegistryHint.EFFECT);
        }
        if (key.contains("enchant")) {
            return List.of(RegistryHint.ENCHANTMENT);
        }
        if (key.contains("entity")) {
            return List.of(RegistryHint.ENTITY);
        }
        if (key.contains("trim") && key.contains("material") || path.contains("trim") && key.contains("material")) {
            return List.of(RegistryHint.TRIM_MATERIAL);
        }
        if (key.contains("trim") && key.contains("pattern") || path.contains("trim") && key.contains("pattern")) {
            return List.of(RegistryHint.TRIM_PATTERN);
        }
        if (key.contains("banner") && key.contains("pattern") || path.contains("banner") && key.contains("pattern")) {
            return List.of(RegistryHint.BANNER_PATTERN);
        }
        if ("pattern".equals(key) && path.contains("minecraft:banner_patterns")) {
            return List.of(RegistryHint.BANNER_PATTERN);
        }
        List<RegistryHint> semantic = semanticRegistryHintsFromText(key, false, false);
        if (!semantic.isEmpty()) {
            return semantic;
        }
        RegistryHint fallback = registryHintFromKey(key);
        return fallback == null ? List.of() : List.of(fallback);
    }

    private static List<RegistryHint> validatedRegistryCandidates(
            String text,
            int replaceStart,
            int cursor,
            boolean insideQuote,
            String currentKey,
            String containerKey,
            String containerPath,
            RegistryAccess registryAccess) {
        String cacheKey = registryHintProbeCacheKey(
                text, replaceStart, cursor, insideQuote, currentKey, containerKey, containerPath, registryAccess);
        synchronized (RUNTIME_PROBE_CACHE_LOCK) {
            List<RegistryHint> cached = REGISTRY_HINT_PROBE_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        List<RegistryHint> explicit = registryCandidates(currentKey, containerKey, containerPath);
        List<RegistryHint> accepted =
                acceptedRegistryCandidates(explicit, text, replaceStart, cursor, insideQuote, registryAccess);
        if (accepted.isEmpty()
                && !acceptsArbitraryStringValue(text, replaceStart, cursor, insideQuote, registryAccess)) {
            List<RegistryHint> dynamic = acceptedRegistryCandidates(
                    VALUE_REGISTRY_HINTS, text, replaceStart, cursor, insideQuote, registryAccess);
            accepted = dynamic.size() > 3 ? List.of() : dynamic;
        }

        synchronized (RUNTIME_PROBE_CACHE_LOCK) {
            REGISTRY_HINT_PROBE_CACHE.put(cacheKey, accepted);
        }
        return accepted;
    }

    private static List<RegistryHint> acceptedRegistryCandidates(
            List<RegistryHint> candidates,
            String text,
            int replaceStart,
            int cursor,
            boolean insideQuote,
            RegistryAccess registryAccess) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<RegistryHint> accepted = new LinkedHashSet<>();
        for (RegistryHint hint : candidates) {
            List<String> values = ids(hint, registryAccess);
            if (!values.isEmpty()
                    && acceptsRegistryValue(text, replaceStart, cursor, insideQuote, values, registryAccess)) {
                accepted.add(hint);
            }
        }
        return List.copyOf(accepted);
    }

    private static boolean acceptsArbitraryStringValue(
            String text, int replaceStart, int cursor, boolean insideQuote, RegistryAccess registryAccess) {
        return acceptsValueProbe(
                text, replaceStart, cursor, insideQuote, "itemeditor:not_a_real_registry_value_999", registryAccess);
    }

    private static boolean acceptsRegistryValue(
            String text,
            int replaceStart,
            int cursor,
            boolean insideQuote,
            List<String> values,
            RegistryAccess registryAccess) {
        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeCursor = Math.clamp(cursor, safeStart, text.length());
        int checked = 0;
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            if (acceptsValueProbe(text, safeStart, safeCursor, insideQuote, value, registryAccess)) {
                return true;
            }

            checked++;
            if (checked >= REGISTRY_BINDING_SAMPLE_LIMIT) {
                return false;
            }
        }
        return false;
    }

    private static boolean acceptsValueProbe(
            String text,
            int replaceStart,
            int cursor,
            boolean insideQuote,
            String value,
            RegistryAccess registryAccess) {
        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeCursor = Math.clamp(cursor, safeStart, text.length());
        String prefix = text.substring(0, safeStart);
        String suffix = text.substring(safeCursor);
        String insert = insideQuote ? value : quoteProbeValue(value);
        return RawItemDataUtil.parse(prefix + insert + suffix, registryAccess).success();
    }

    private static String registryHintProbeCacheKey(
            String text,
            int replaceStart,
            int cursor,
            boolean insideQuote,
            String currentKey,
            String containerKey,
            String containerPath,
            RegistryAccess registryAccess) {
        TextWindow window = textWindow(text, replaceStart, cursor);
        return System.identityHashCode(registryAccess)
                + ":" + insideQuote
                + ":" + normalize(currentKey)
                + ":" + normalize(containerKey)
                + ":" + normalize(containerPath)
                + ":" + window.left()
                + ":" + window.right()
                + ":" + window.safeStart()
                + ":" + window.safeCursor()
                + ":" + window.value();
    }

    private static String quoteProbeValue(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static List<String> pathValueHints(String path, RegistryAccess registryAccess) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (PathRule rule : PATH_RULES) {
            if (rule.matches(path)) {
                values.addAll(rule.values(registryAccess));
            }
        }
        return List.copyOf(values);
    }

    private static List<String> debugStickStateProperties(
            String currentKey, String path, RegistryAccess registryAccess) {
        if (!pathEndsWith(path, "components", "minecraft:debug_stick_state", currentKey)) {
            return List.of();
        }
        return RUNTIME.blockStateProperties(currentKey, registryAccess);
    }

    private static List<RegistryHint> semanticRegistryHintsFromText(
            String text, boolean includeEntitySpawn, boolean includeBlockEntity) {
        String normalized = normalize(text);
        if (includeEntitySpawn && (normalized.contains("entity") || normalized.contains("spawn"))) {
            return List.of(RegistryHint.ENTITY);
        }
        if (includeBlockEntity && normalized.contains("block_entity")) {
            return List.of(RegistryHint.BLOCK_ENTITY);
        }
        RegistryHint hint = registryHintFromKey(normalized);
        if (hint != null) {
            return List.of(hint);
        }
        return List.of();
    }

    private static List<RegistryHint> directRegistryHintFromKeyAndPath(String key, String path) {
        for (DirectRegistryRule rule : DIRECT_REGISTRY_RULES) {
            if (rule.matches(key, path)) {
                return List.of(rule.hint());
            }
        }
        return List.of();
    }

    private static List<String> registryMapKeys(
            String containerKey, String containerPath, RegistryAccess registryAccess) {
        List<RegistryHint> hints = registryMapHints(containerKey, containerPath);
        if (hints.isEmpty()) {
            return List.of();
        }
        return ids(hints.getFirst(), registryAccess);
    }

    private static List<RegistryHint> registryMapHints(String containerKey, String containerPath) {
        String key = normalize(containerKey);
        String path = normalize(containerPath);
        if (key.endsWith("enchantments")
                || key.endsWith("stored_enchantments")
                || path.endsWith("/minecraft:enchantments")
                || path.endsWith("/minecraft:stored_enchantments")
                || path.endsWith("/minecraft:enchantments/levels")
                || path.endsWith("/minecraft:stored_enchantments/levels")) {
            return List.of(RegistryHint.ENCHANTMENT);
        }
        if ("modifiers".equals(key) && pathEndsWith(path, "components", "minecraft:attribute_modifiers", "modifiers")) {
            return List.of();
        }
        if ("minecraft:debug_stick_state".equals(key)
                || pathEndsWith(path, "components", "minecraft:debug_stick_state")) {
            return List.of(RegistryHint.BLOCK);
        }
        if (key.contains("attribute") || path.contains("attribute")) {
            return List.of(RegistryHint.ATTRIBUTE);
        }
        if ((key.contains("banner") && key.contains("pattern"))
                || (path.contains("banner") && path.contains("pattern"))) {
            return List.of(RegistryHint.BANNER_PATTERN);
        }
        return List.of();
    }

    private static boolean registryMapKeyAccepted(
            String prefix, String suffix, List<String> ids, RegistryAccess registryAccess) {
        int checked = 0;
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            for (String value : List.of("1", "{}", "[]", "\"\"")) {
                String candidate = prefix + quoteProbeValue(id) + ": " + value + suffix;
                if (RawItemDataUtil.parse(candidate, registryAccess).success()
                        || RawItemDataUtil.parse(
                                        candidate + RawAutocompleteUtil.closingSuffix(candidate), registryAccess)
                                .success()) {
                    return true;
                }
            }
            checked++;
            if (checked >= REGISTRY_BINDING_SAMPLE_LIMIT) {
                return false;
            }
        }
        return false;
    }

    private static EnumSet<RawValueMode> inferModesFromPath(String path) {
        EnumSet<RawValueMode> modes = EnumSet.noneOf(RawValueMode.class);
        for (PathRule rule : PATH_RULES) {
            if (rule.mode() != null && rule.matches(path)) {
                modes.add(rule.mode());
            }
        }
        return modes;
    }

    private static EnumSet<RawValueMode> runtimeProbeModes(
            String text, int replaceStart, int cursor, RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return EnumSet.noneOf(RawValueMode.class);
        }
        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeCursor = Math.clamp(cursor, safeStart, text.length());
        String cacheKey = runtimeProbeCacheKey(text, safeStart, safeCursor);
        synchronized (RUNTIME_PROBE_CACHE_LOCK) {
            EnumSet<RawValueMode> cached = RUNTIME_PROBE_CACHE.get(cacheKey);
            if (cached != null) {
                return EnumSet.copyOf(cached);
            }
        }

        String prefix = text.substring(0, safeStart);
        String suffix = text.substring(safeCursor);
        EnumSet<RawValueMode> modes = EnumSet.noneOf(RawValueMode.class);
        addIf(
                modes,
                RawValueMode.BOOLEAN,
                parsesWithProbe(prefix, suffix, "1b", registryAccess)
                        || parsesWithProbe(prefix, suffix, "true", registryAccess));
        addIf(
                modes,
                RawValueMode.NUMBER,
                parsesWithProbe(prefix, suffix, "1", registryAccess)
                        || parsesWithProbe(prefix, suffix, "1.0f", registryAccess));
        addIf(modes, RawValueMode.STRING, parsesWithProbe(prefix, suffix, "\"x\"", registryAccess));
        addIf(modes, RawValueMode.LITERAL, parsesWithProbe(prefix, suffix, "null", registryAccess));

        synchronized (RUNTIME_PROBE_CACHE_LOCK) {
            RUNTIME_PROBE_CACHE.put(cacheKey, EnumSet.copyOf(modes));
        }
        return modes;
    }

    private static boolean parsesWithProbe(
            String prefix, String suffix, String probeValue, RegistryAccess registryAccess) {
        return RawItemDataUtil.parse(prefix + probeValue + suffix, registryAccess)
                .success();
    }

    private static String runtimeProbeCacheKey(String text, int replaceStart, int cursor) {
        TextWindow window = textWindow(text, replaceStart, cursor);
        return window.left()
                + ":" + window.right()
                + ":" + window.safeStart()
                + ":" + window.safeCursor()
                + ":" + window.value();
    }

    private static TextWindow textWindow(String text, int replaceStart, int cursor) {
        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeCursor = Math.clamp(cursor, safeStart, text.length());
        int left = Math.max(0, safeStart - 80);
        int right = Math.min(text.length(), safeCursor + 80);
        return new TextWindow(safeStart, safeCursor, left, right, text.substring(left, right));
    }

    private record TextWindow(int safeStart, int safeCursor, int left, int right, String value) {}

    private static String runtimePathProbeCacheKey(
            String currentKey, String containerPath, RegistryAccess registryAccess) {
        String path = buildFullPath(containerPath, currentKey);
        return path.contains("components/") ? System.identityHashCode(registryAccess) + ":" + path : "";
    }

    private static boolean hasCompositeSnippet(String currentKey) {
        String snippet = valueSnippet(currentKey);
        return snippet.startsWith("{") || snippet.startsWith("[");
    }

    private static String normalizedSiblingValue(Map<String, String> siblingValues, String key) {
        String value = siblingValues.get(key);
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() >= 2
                && normalized.charAt(0) == '"'
                && normalized.charAt(normalized.length() - 1) == '"') {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String valueExampleFromSibling(String value) {
        String trimmed = Objects.requireNonNullElse(value, "").trim();
        if (trimmed.isBlank() || "null".equalsIgnoreCase(trimmed) || trimmed.length() > 180) {
            return "";
        }
        if (trimmed.startsWith("\"") && !trimmed.endsWith("\"")) {
            return "";
        }
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return trimmed;
    }

    private static String activeComponentIdFromContainerPath(String containerPath) {
        String[] segments = normalize(containerPath).split("/");
        for (int index = 0; index < segments.length - 1; index++) {
            if ("components".equals(segments[index])) {
                return segments[index + 1];
            }
        }
        return "";
    }

    private static boolean isLikelyCompositeFieldName(String key) {
        if (COMPOSITE_EXACT_KEYS.contains(key)) {
            return true;
        }
        for (String part : COMPOSITE_KEY_PARTS) {
            if (key.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLikelyIntegerNumberKey(String key, String path) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (pathEndsWith(path, "components", "minecraft:consumable", "consume_seconds")
                || pathEndsWith(path, "components", "minecraft:food", "nutrition")
                || pathEndsWith(path, "components", "minecraft:food", "saturation")
                || pathEndsWith(path, "on_consume_effects", "probability")) {
            return false;
        }
        return key.contains("amplifier")
                || key.contains("count")
                || key.contains("delay")
                || key.contains("duration")
                || key.contains("level")
                || key.contains("nutrition")
                || key.equals("page")
                || key.contains("range")
                || key.contains("repair")
                || key.startsWith("max")
                || key.startsWith("min");
    }

    private static boolean isBooleanLikeKey(String key) {
        return BOOLEAN_EXACT_KEYS.contains(key)
                || key.endsWith("enabled")
                || key.endsWith("glowing")
                || key.endsWith("hide_tooltip")
                || key.endsWith("resolved")
                || key.endsWith("trail")
                || key.endsWith("visible")
                || key.endsWith("twinkle")
                || key.startsWith("allow_")
                || key.startsWith("can_")
                || key.startsWith("has_")
                || key.startsWith("is_")
                || textStyleBooleanKey(key);
    }

    private static boolean textStyleBooleanKey(String key) {
        return key.endsWith("bold")
                || key.endsWith("italic")
                || key.endsWith("obfuscated")
                || key.endsWith("strikethrough")
                || key.endsWith("underlined");
    }

    private static boolean isNumericLikeKey(String key) {
        return NUMERIC_EXACT_KEYS.contains(key)
                || key.contains("amount")
                || key.contains("chance")
                || key.contains("count")
                || key.contains("delay")
                || key.contains("duration")
                || key.contains("level")
                || key.contains("range")
                || key.contains("scale")
                || key.contains("weight")
                || key.startsWith("max")
                || key.startsWith("min");
    }

    private static boolean isStringLikeKey(String key) {
        return STRING_EXACT_KEYS.contains(key)
                || key.endsWith("_id")
                || key.endsWith("_type")
                || key.contains("author")
                || key.contains("banner")
                || key.contains("color")
                || key.contains("effect")
                || key.contains("entity")
                || key.contains("generation")
                || key.contains("instrument")
                || key.contains("item")
                || key.contains("name")
                || key.contains("operation")
                || key.contains("potion")
                || key.contains("rarity")
                || key.contains("shape")
                || key.contains("slot")
                || key.contains("song")
                || key.contains("text")
                || key.contains("title")
                || key.contains("trim");
    }

    private static boolean isBlockEntityIdPath(String container, String path) {
        return "minecraft:block_entity_data".equals(container)
                        && pathEndsWith(path, "components", "minecraft:block_entity_data", "id")
                || pathEndsWith(path, "blockentitytag", "id");
    }

    private static RegistryHint registryHintFromKey(String key) {
        String normalized = normalize(key);
        for (RegistryKeywordRule rule : REGISTRY_KEYWORD_RULES) {
            if (rule.matches(normalized)) {
                return rule.hint();
            }
        }
        return null;
    }

    private static boolean isNumericPath(String path) {
        return pathHasMode(path, RawValueMode.NUMBER);
    }

    private static boolean isStringPath(String path) {
        return pathHasMode(path, RawValueMode.STRING);
    }

    private static boolean pathHasMode(String path, RawValueMode mode) {
        for (PathRule rule : PATH_RULES) {
            if (rule.mode() == mode && rule.matches(path)) {
                return true;
            }
        }
        return false;
    }

    private static RawSlotType pathSlotType(String path) {
        for (PathRule rule : PATH_RULES) {
            if (rule.slotType() != RawSlotType.VALUE_UNKNOWN && rule.matches(path)) {
                return rule.slotType();
            }
        }
        return RawSlotType.VALUE_UNKNOWN;
    }

    private static boolean isTextComponentPath(String path) {
        String normalized = normalize(path);
        return normalized.contains("minecraft:custom_name")
                || normalized.contains("minecraft:item_name")
                || normalized.contains("minecraft:lore")
                || normalized.contains("/display/name")
                || normalized.contains("/display/lore")
                || normalized.contains("/text");
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
            if (!normalize(segments[index]).equals(pathSegments[offset + index])) {
                return false;
            }
        }
        return true;
    }

    private static List<String> ids(RegistryHint hint, RegistryAccess registryAccess) {
        return hint.ids(registryAccess);
    }

    private static List<String> cleanExternalIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private static List<String> tagIds(RegistryHint hint, RegistryAccess registryAccess) {
        return hint.tagIds(registryAccess);
    }

    private static List<String> ids(List<RegistryHint> hints, RegistryAccess registryAccess) {
        if (hints == null || hints.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (RegistryHint hint : hints) {
            values.addAll(ids(hint, registryAccess));
        }
        return List.copyOf(values);
    }

    private static boolean hasItems(List<String> values) {
        return values != null && !values.isEmpty();
    }

    private static void addIf(EnumSet<RawValueMode> modes, RawValueMode mode, boolean condition) {
        if (condition) {
            modes.add(mode);
        }
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
    }

    private enum RegistryHint {
        ATTRIBUTE(Registries.ATTRIBUTE, BuiltInRegistries.ATTRIBUTE),
        BANNER_PATTERN(Registries.BANNER_PATTERN),
        BLOCK(Registries.BLOCK, BuiltInRegistries.BLOCK),
        BLOCK_ENTITY(Registries.BLOCK_ENTITY_TYPE, BuiltInRegistries.BLOCK_ENTITY_TYPE),
        CAT_SOUND_VARIANT(Registries.CAT_SOUND_VARIANT),
        CAT_VARIANT(Registries.CAT_VARIANT),
        CHICKEN_VARIANT(Registries.CHICKEN_VARIANT),
        COMPONENT(Registries.DATA_COMPONENT_TYPE, BuiltInRegistries.DATA_COMPONENT_TYPE),
        COW_VARIANT(Registries.COW_VARIANT),
        DAMAGE_TYPE(Registries.DAMAGE_TYPE),
        DIMENSION(Registries.DIMENSION),
        EFFECT(Registries.MOB_EFFECT, BuiltInRegistries.MOB_EFFECT),
        ENCHANTMENT(Registries.ENCHANTMENT),
        ENTITY(Registries.ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE),
        FROG_VARIANT(Registries.FROG_VARIANT),
        INSTRUMENT(Registries.INSTRUMENT),
        ITEM(Registries.ITEM, BuiltInRegistries.ITEM),
        LOOT_TABLE(Registries.LOOT_TABLE),
        MAP_DECORATION(Registries.MAP_DECORATION_TYPE),
        PAINTING_VARIANT(Registries.PAINTING_VARIANT),
        PIG_VARIANT(Registries.PIG_VARIANT),
        POTION(Registries.POTION, BuiltInRegistries.POTION),
        SONG(Registries.JUKEBOX_SONG),
        SOUND(Registries.SOUND_EVENT, BuiltInRegistries.SOUND_EVENT),
        TRIM_MATERIAL(Registries.TRIM_MATERIAL),
        TRIM_PATTERN(Registries.TRIM_PATTERN),
        VILLAGER_PROFESSION(Registries.VILLAGER_PROFESSION, BuiltInRegistries.VILLAGER_PROFESSION),
        VILLAGER_VARIANT(Registries.VILLAGER_TYPE, BuiltInRegistries.VILLAGER_TYPE),
        WOLF_SOUND_VARIANT(Registries.WOLF_SOUND_VARIANT),
        WOLF_VARIANT(Registries.WOLF_VARIANT),
        ZOMBIE_NAUTILUS_VARIANT(Registries.ZOMBIE_NAUTILUS_VARIANT);

        private final ResourceKey<? extends Registry<?>> registryKey;
        private final Registry<?> builtinFallback;

        RegistryHint(ResourceKey<? extends Registry<?>> registryKey) {
            this(registryKey, null);
        }

        RegistryHint(ResourceKey<? extends Registry<?>> registryKey, Registry<?> builtinFallback) {
            this.registryKey = registryKey;
            this.builtinFallback = builtinFallback;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private List<String> ids(RegistryAccess registryAccess) {
            List<String> ids = RUNTIME.registryIds(
                    registryAccess, (ResourceKey) this.registryKey, (Registry) this.builtinFallback);
            return this == LOOT_TABLE ? mergeUnique(ids, EXTERNAL_LOOT_TABLE_IDS.get()) : ids;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private List<String> tagIds(RegistryAccess registryAccess) {
            return RUNTIME.registryTagIds(
                    registryAccess, (ResourceKey) this.registryKey, (Registry) this.builtinFallback);
        }
    }

    private record DirectRegistryRule(
            RegistryHint hint,
            String exactKey,
            String keyContains,
            String keySuffix,
            String pathContains,
            String pathSuffix) {
        private DirectRegistryRule {
            exactKey = normalize(exactKey);
            keyContains = normalize(keyContains);
            keySuffix = normalize(keySuffix);
            pathContains = normalize(pathContains);
            pathSuffix = normalize(pathSuffix);
        }

        private static DirectRegistryRule contains(RegistryHint hint, String keyContains, String pathContains) {
            return new DirectRegistryRule(hint, "", keyContains, "", pathContains, "");
        }

        private boolean matches(String key, String path) {
            String normalizedKey = normalize(key);
            String normalizedPath = normalize(path);
            return (!this.exactKey.isBlank() && this.exactKey.equals(normalizedKey))
                    || (!this.keyContains.isBlank() && normalizedKey.contains(this.keyContains))
                    || (!this.keySuffix.isBlank() && normalizedKey.endsWith(this.keySuffix))
                    || (!this.pathContains.isBlank() && normalizedPath.contains(this.pathContains))
                    || (!this.pathSuffix.isBlank() && pathEndsWith(normalizedPath, this.pathSuffix));
        }
    }

    private record RegistryKeywordRule(RegistryHint hint, List<String> tokens) {
        private RegistryKeywordRule {
            tokens = tokens == null
                    ? List.of()
                    : tokens.stream()
                            .map(RawAutocompleteHints::normalize)
                            .filter(token -> !token.isBlank())
                            .toList();
        }

        private static RegistryKeywordRule of(RegistryHint hint, String... tokens) {
            return new RegistryKeywordRule(hint, List.of(tokens));
        }

        private boolean matches(String value) {
            String normalized = normalize(value);
            return !this.tokens.isEmpty() && this.tokens.stream().allMatch(normalized::contains);
        }
    }

    private record PathRule(
            RawValueMode mode,
            RawSlotType slotType,
            List<String> values,
            RegistryHint registryHint,
            List<String> suffix,
            String contains) {
        private PathRule {
            slotType = slotType == null ? RawSlotType.VALUE_UNKNOWN : slotType;
            values = values == null ? List.of() : values;
            suffix = suffix == null ? List.of() : suffix;
            contains = Objects.requireNonNullElse(contains, "");
        }

        private static PathRule booleanRule(String... suffix) {
            return mode(RawValueMode.BOOLEAN, suffix);
        }

        private static PathRule numberRule(String... suffix) {
            return mode(RawValueMode.NUMBER, suffix);
        }

        private static PathRule stringRule(String... suffix) {
            return mode(RawValueMode.STRING, suffix);
        }

        private static PathRule stringValues(List<String> values, String... suffix) {
            return new PathRule(RawValueMode.STRING, RawSlotType.VALUE_STRING, values, null, List.of(suffix), "");
        }

        private static PathRule stringRegistry(RawSlotType slotType, RegistryHint registryHint, String... suffix) {
            return new PathRule(RawValueMode.STRING, slotType, List.of(), registryHint, List.of(suffix), "");
        }

        private static PathRule effectRegistryContains() {
            return new PathRule(
                    RawValueMode.STRING,
                    RawSlotType.VALUE_ID_EFFECT,
                    List.of(),
                    RegistryHint.EFFECT,
                    List.of(),
                    "/on_consume_effects/effects/");
        }

        private static PathRule mode(RawValueMode mode, String... suffix) {
            return new PathRule(mode, RawSlotType.VALUE_UNKNOWN, List.of(), null, List.of(suffix), "");
        }

        private boolean matches(String path) {
            return this.contains.isBlank()
                    ? pathEndsWith(path, this.suffix.toArray(String[]::new))
                    : path.contains(this.contains);
        }

        private List<String> values(RegistryAccess registryAccess) {
            return this.registryHint == null ? this.values : ids(this.registryHint, registryAccess);
        }
    }
}
