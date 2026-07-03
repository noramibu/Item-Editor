package me.noramibu.itemeditor.util;

import net.minecraft.core.RegistryAccess;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class RawAutocompleteUtil {
    private static final Pattern NAMESPACED_ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final Pattern SIMPLE_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]+$");
    private static final Pattern BARE_VALUE_PATTERN = Pattern.compile("^[A-Za-z0-9_.+-]+$");
    private static final Pattern SIMPLE_STRING_FIELD_PATTERN =
            Pattern.compile("(?:\"([A-Za-z0-9_.:-]+)\"|([A-Za-z0-9_.-]+))\\s*:\\s*\"([^\"]*)\"");
    private static final List<String> LITERAL_VALUES = List.of("true", "false", "null");
    private static final List<String> STRING_VALUE_TEMPLATES = List.of("\"\"");
    private RawAutocompleteUtil() {
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
            RawAutocompleteIndex index,
            String fallbackItemId
    ) {
        return suggest(rawText, caretIndex, registryAccess, index, fallbackItemId, List.of());
    }

    public static AutocompleteResult suggest(
            String rawText,
            int caretIndex,
            RegistryAccess registryAccess,
            RawAutocompleteIndex index,
            String fallbackItemId,
            List<String> lootTableIds
    ) {
        return RawAutocompleteHints.withExternalLootTableIds(
                lootTableIds,
                () -> suggestInternal(rawText, caretIndex, registryAccess, index, fallbackItemId)
        );
    }

    private static AutocompleteResult suggestInternal(
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
        RawCursorContext cursorContext = RawCursorContext.create(text, cursor);

        boolean insideQuote = cursorContext.insideString();
        int replaceStart = findTokenStart(text, cursor, insideQuote);
        String prefix = text.substring(replaceStart, cursor);
        String currentKey = cursorContext.currentKey().isBlank()
                ? effectiveIndex.lastObjectKeyAt(cursor)
                : cursorContext.currentKey();
        RawAutocompleteIndex.Context context = cursorContext.indexContext();
        String topLevelItemId = detectTopLevelItemId(text);
        String fallbackId = Objects.requireNonNullElse(fallbackItemId, "");
        if (topLevelItemId.isBlank() && !fallbackId.isBlank()) {
            topLevelItemId = fallbackId;
        }
        List<String> activeProfiles = RawAutocompleteHints.inferItemProfiles(topLevelItemId, registryAccess);
        List<String> profileComponents = RawAutocompleteHints.componentsForContext(
                topLevelItemId,
                activeProfiles,
                registryAccess
        );
        boolean keyPosition = cursorContext.slot() == RawCursorContext.Slot.OBJECT_KEY;
        if (cursorContext.slot() == RawCursorContext.Slot.UNKNOWN) {
            keyPosition = isLikelyObjectKeyPosition(text, cursor, insideQuote);
            keyPosition = refineKeyPosition(text, cursor, insideQuote, keyPosition, context);
        }
        if (!insideQuote && !keyPosition && looksLikeObjectKeyOnCurrentLine(text, cursor)) {
            keyPosition = true;
        }
        RawSuggestionBuilder output = new RawSuggestionBuilder();

        if (!shouldSuggestAtPosition(text, cursor, keyPosition, insideQuote, prefix)) {
            return AutocompleteResult.empty(cursor);
        }

        KeyCorrection correction = cursorContext.slot() == RawCursorContext.Slot.VALUE
                ? null
                : detectKeyCorrectionContext(text, cursor, keyPosition, insideQuote);
        if (correction != null) {
            UnaryOperator<String> keyInsert = insideQuote
                    ? quotedInsert(text, cursor, cursor)
                    : RawAutocompleteUtil::formatKeyInsert;
            UnaryOperator<String> dynamicKeyInsert = registryMapKeyInsert(
                    text,
                    cursor,
                    correction.replaceEnd(),
                    insideQuote,
                    context.containerKey(),
                    context.containerPath(),
                    keyInsert
            );
            RawSuggestionSources.Key keySources = RawSuggestionSources.key(
                    context,
                    topLevelItemId,
                    activeProfiles,
                    registryAccess
            );
            List<String> seenContainerKeys = effectiveIndex.seenKeysForContainer(context.containerKey());
            if (!isKnownContainerKey(
                    correction.keyPrefix(),
                    keySources.contextualKeys(),
                    keySources.catalogObjectKeys(),
                    keySources.componentNbtFields(),
                    keySources.dynamicKeyHints(),
                    seenContainerKeys
            )) {
                RawSuggestionBuilder correctionOutput = new RawSuggestionBuilder();
                RawSuggestionSources.addKeyMatches(
                        correctionOutput,
                        keySources,
                        seenContainerKeys,
                        correction.keyPrefix(),
                        itemStackKeyInsert(text, cursor, correction.replaceEnd(), insideQuote, context.containerPath()),
                        dynamicKeyInsert,
                        false
                );
                RawSuggestionSources.addKeyMatches(
                        correctionOutput,
                        keySources,
                        seenContainerKeys,
                        correction.keyPrefix(),
                        itemStackKeyInsert(text, cursor, correction.replaceEnd(), insideQuote, context.containerPath()),
                        dynamicKeyInsert,
                        true
                );
                correctionOutput.suppressEchoTypedKeySuggestion(correction.keyPrefix());
                AutocompleteResult correctionResult = autocompleteResult(
                        text,
                        cursor,
                        correction.replaceStart(),
                        correction.replaceEnd(),
                        correctionOutput.values(),
                        registryAccess,
                        RawAutocompleteHints.expectedModesForSlot(RawSlotType.OBJECT_KEY),
                        RawSlotType.OBJECT_KEY,
                        context.containerKey()
                );
                if (!correctionResult.suggestions().isEmpty()) {
                    return correctionResult;
                }
            }
        }

        if (keyPosition) {
            int keyReplaceEnd = keyReplaceEnd(text, cursor, insideQuote);
            UnaryOperator<String> keyInsert = insideQuote
                    ? quotedInsert(text, cursor, keyReplaceEnd)
                    : RawAutocompleteUtil::formatKeyInsert;
            RawSuggestionSources.Key keySources = RawSuggestionSources.key(
                    context,
                    topLevelItemId,
                    activeProfiles,
                    registryAccess
            );
            Map<String, String> siblingValues = localSiblingValues(text, cursor);
            List<String> siblingKeys = localSiblingKeys(text, cursor);
            List<String> siblingValueKeyHints = RawAutocompleteHints.objectKeyHintsForSiblingValues(
                    context.containerKey(),
                    context.containerPath(),
                    siblingValues
            );
            List<String> siblingKeyHints = RawAutocompleteHints.mergeUnique(
                    siblingValueKeyHints,
                    RawAutocompleteHints.mergeUnique(
                            siblingKeys,
                            RawAutocompleteHints.objectKeyHintsForSiblingKeys(siblingKeys)
                    )
            );
            List<String> validatedRegistryKeyHints = RawAutocompleteHints.validatedRegistryMapKeyHints(
                    text,
                    replaceStart,
                    keyReplaceEnd,
                    context.containerKey(),
                    context.containerPath(),
                    registryAccess
            );
            boolean focusedSiblingKeys = !siblingValueKeyHints.isEmpty();
            boolean strictContextKeys = RawAutocompleteHints.shouldUseStrictContextKeySuggestions(
                    context,
                    keySources.contextualKeys(),
                    keySources.catalogObjectKeys(),
                    keySources.componentNbtFields(),
                    RawAutocompleteHints.mergeUnique(
                            validatedRegistryKeyHints,
                            RawAutocompleteHints.mergeUnique(keySources.dynamicKeyHints(), siblingKeyHints)
                    ),
                    profileComponents
            );
            List<String> seenContainerKeys = effectiveIndex.seenKeysForContainer(context.containerKey());
            List<String> currentObjectKeys = currentObjectKeys(text, cursor, insideQuote);
            boolean completingQuotedKeyWithColon = insideQuote && quotedKeyHasColonAfterCursor(text, cursor);
            UnaryOperator<String> componentKeyInsert = context.inComponentsObject()
                    ? componentKeyInsert(text, cursor, keyReplaceEnd, insideQuote, topLevelItemId, registryAccess)
                    : null;
            UnaryOperator<String> itemStackKeyInsert = itemStackKeyInsert(
                    text,
                    cursor,
                    keyReplaceEnd,
                    insideQuote,
                    context.containerPath()
            );
            UnaryOperator<String> dynamicKeyInsert = registryMapKeyInsert(
                    text,
                    cursor,
                    keyReplaceEnd,
                    insideQuote,
                    context.containerKey(),
                    context.containerPath(),
                    keyInsert
            );
            if (context.inComponentsObject()) {
                output.addSuggestions(
                        profileComponents,
                        prefix,
                        SuggestionKind.SNIPPET,
                        componentKeyInsert,
                        0,
                        SuggestionSource.ITEM_PROFILE,
                        "item profile component");
                if (!prefix.isBlank()) {
                    output.addSuggestions(
                            RawAutocompleteHints.componentIds(registryAccess),
                            prefix,
                            SuggestionKind.SNIPPET,
                            componentKeyInsert,
                            1,
                            SuggestionSource.REGISTRY,
                            "component registry");
                    if (completingQuotedKeyWithColon) {
                        output.addSuggestions(
                                RawAutocompleteHints.componentIds(registryAccess),
                                prefix,
                                SuggestionKind.SNIPPET,
                                keyInsert,
                                1,
                                SuggestionSource.REGISTRY,
                                "component registry key");
                    }
                    output.addSuggestions(
                            RawAutocompleteHints.componentIds(registryAccess),
                            prefix,
                            SuggestionKind.KEY,
                            keyInsert,
                            1,
                            SuggestionSource.REGISTRY,
                            "component registry key");
                }
            } else if (context.inRootObject() || (!context.inObject() && context.containerKey().isBlank())) {
                output.addSuggestions(
                        RawAutocompleteHints.topLevelKeys(),
                        prefix,
                        SuggestionKind.SNIPPET,
                        itemStackKeyInsert,
                        0,
                        SuggestionSource.CATALOG,
                        "item stack key"
                );
            }
            if (!focusedSiblingKeys) {
                RawSuggestionSources.addKeyMatches(
                        output,
                        keySources,
                        List.of(),
                        prefix,
                        itemStackKeyInsert,
                        dynamicKeyInsert,
                        false
                );
            }
            output.addSuggestions(
                    validatedRegistryKeyHints,
                    prefix,
                    SuggestionKind.KEY,
                    dynamicKeyInsert,
                    0,
                    SuggestionSource.VALIDATED_REGISTRY,
                    "validated registry key");
            if (completingQuotedKeyWithColon) {
                output.addSuggestions(
                        validatedRegistryKeyHints,
                        prefix,
                        SuggestionKind.SNIPPET,
                        keyInsert,
                        0,
                        SuggestionSource.VALIDATED_REGISTRY,
                        "validated registry key completion");
            }
            output.addSuggestions(
                    siblingKeyHints,
                    prefix,
                    SuggestionKind.KEY,
                    itemStackKeyInsert,
                    0,
                    SuggestionSource.SIBLING,
                    "sibling context key"
            );
            if (!prefix.isBlank()) {
                if (!focusedSiblingKeys) {
                    output.addSuggestions(
                            seenContainerKeys,
                            prefix,
                            SuggestionKind.KEY,
                            itemStackKeyInsert,
                            1,
                            SuggestionSource.SEEN,
                            "seen in this container");
                }
                if (!strictContextKeys) {
                    output.addSuggestions(
                            effectiveIndex.seenKeys(),
                            prefix,
                            SuggestionKind.KEY,
                            keyInsert,
                            2,
                            SuggestionSource.SEEN,
                            "seen in document");
                }

                if (!focusedSiblingKeys) {
                    RawSuggestionSources.addKeyMatches(
                            output,
                            keySources,
                            seenContainerKeys,
                            prefix,
                            itemStackKeyInsert,
                            dynamicKeyInsert,
                            true
                    );
                }
                if (completingQuotedKeyWithColon) {
                    output.addSuggestions(
                            keySources.dynamicKeyHints(),
                            prefix,
                            SuggestionKind.SNIPPET,
                            keyInsert,
                            0,
                            SuggestionSource.REGISTRY,
                            "registry map key completion");
                }
                output.addNearestSuggestions(
                        validatedRegistryKeyHints,
                        prefix,
                        SuggestionKind.KEY,
                        dynamicKeyInsert,
                        0,
                        SuggestionSource.VALIDATED_REGISTRY,
                        "near validated registry key");
                if (completingQuotedKeyWithColon) {
                    output.addNearestSuggestions(
                            validatedRegistryKeyHints,
                            prefix,
                            SuggestionKind.SNIPPET,
                            keyInsert,
                            0,
                            SuggestionSource.VALIDATED_REGISTRY,
                            "near validated registry key completion");
                }
                    output.addNearestSuggestions(
                            siblingKeyHints,
                            prefix,
                            SuggestionKind.KEY,
                            itemStackKeyInsert,
                            0,
                            SuggestionSource.SIBLING,
                            "near sibling context key"
                    );
                if (!strictContextKeys) {
                    output.addNearestSuggestions(
                            effectiveIndex.seenKeys(),
                            prefix,
                            SuggestionKind.KEY,
                            keyInsert,
                            2,
                            SuggestionSource.SEEN,
                            "near seen document key");
                }
                if (context.inComponentsObject()) {
                    output.addNearestSuggestions(
                            profileComponents,
                            prefix,
                            SuggestionKind.SNIPPET,
                            componentKeyInsert,
                            0,
                            SuggestionSource.ITEM_PROFILE,
                            "near item profile component");
                    output.addNearestSuggestions(
                            RawAutocompleteHints.componentIds(registryAccess),
                            prefix,
                            SuggestionKind.SNIPPET,
                            componentKeyInsert,
                            1,
                            SuggestionSource.REGISTRY,
                            "near component registry");
                    if (completingQuotedKeyWithColon) {
                        output.addNearestSuggestions(
                                RawAutocompleteHints.componentIds(registryAccess),
                                prefix,
                                SuggestionKind.SNIPPET,
                                keyInsert,
                                1,
                                SuggestionSource.REGISTRY,
                                "near component registry key");
                    }
                    output.addNearestSuggestions(
                            RawAutocompleteHints.componentIds(registryAccess),
                            prefix,
                            SuggestionKind.KEY,
                            keyInsert,
                            1,
                            SuggestionSource.REGISTRY,
                            "near component registry key");
                }
            }
            if (!currentObjectKeys.isEmpty()) {
                output.suppressAlreadyPresentContainerKeys(currentObjectKeys);
            } else if (prefix.isBlank() && !insideArrayEntryObject(text, cursor)) {
                output.suppressAlreadyPresentContainerKeys(seenContainerKeys);
            }
            output.suppressEchoTypedKeySuggestion(prefix);
            if (!focusedSiblingKeys) {
                addKeyStructuralSuggestions(output, text, cursor, insideQuote, prefix);
            }
            output.suppressDuplicateLabels();
            return autocompleteResult(
                    text,
                    cursor,
                    replaceStart,
                    keyReplaceEnd,
                    output.values(),
                    registryAccess,
                    RawAutocompleteHints.expectedModesForSlot(RawSlotType.OBJECT_KEY),
                    RawSlotType.OBJECT_KEY,
                    currentKey
            );
        }

        if (cursorContext.slot() == RawCursorContext.Slot.AFTER_VALUE && !insideQuote && prefix.isBlank()) {
            addAfterValueStructuralSuggestions(output, text, cursor);
            return autocompleteResult(
                    text,
                    cursor,
                    replaceStart,
                    cursor,
                    output.values(),
                    registryAccess,
                    EnumSet.of(RawValueMode.NONE),
                    RawSlotType.VALUE_UNKNOWN,
                    currentKey
            );
        }

        RawSuggestionSources.Value valueSources = RawSuggestionSources.value(
                text,
                replaceStart,
                cursor,
                insideQuote,
                prefix,
                currentKey,
                context,
                localSiblingValues(text, cursor),
                activeProfiles,
                topLevelItemId,
                registryAccess
        );
        RawSlotType slotType = valueSources.slotType();
        EnumSet<RawValueMode> expectedModes = valueSources.expectedModes();
        List<String> typedCatalogValueHints = valueSources.typedCatalogHints();
        List<String> typedRegistryHints = valueSources.typedRegistryHints();
        BooleanInsertStyle booleanInsertStyle = detectBooleanInsertStyle(text, replaceStart, context.containerPath(), currentKey);
        int valueReplaceEnd = valueReplaceEnd(text, cursor, insideQuote);
        UnaryOperator<String> stringValueInsert = valueInsert(text, cursor, valueReplaceEnd, insideQuote);
        UnaryOperator<String> rawValueInsert = rawValueInsert(text, valueReplaceEnd);
        String specificValueSnippet = RawAutocompleteHints.valueSnippet(currentKey, topLevelItemId, registryAccess);
        boolean hasSpecificValueSnippet = !specificValueSnippet.isEmpty();
        boolean hasCompositeSpecificValueSnippet = specificValueSnippet.startsWith("{")
                || specificValueSnippet.startsWith("[");

        if (insideQuote) {
            output.addSuggestions(
                    typedRegistryHints,
                    prefix,
                    SuggestionKind.VALUE,
                    stringValueInsert,
                    0,
                    SuggestionSource.REGISTRY,
                    "registry value");
            output.addSuggestions(
                    typedCatalogValueHints,
                    prefix,
                    SuggestionKind.VALUE,
                    stringValueInsert,
                    1,
                    SuggestionSource.CATALOG,
                    "catalog value");
        } else {
            if (expectedModes.contains(RawValueMode.STRING)) {
                output.addSuggestions(
                        typedRegistryHints,
                        prefix,
                        SuggestionKind.VALUE,
                        stringValueInsert,
                        0,
                        SuggestionSource.REGISTRY,
                        "registry value");
                output.addSuggestions(
                        typedCatalogValueHints,
                        prefix,
                        SuggestionKind.VALUE,
                        stringValueInsert,
                        1,
                        SuggestionSource.CATALOG,
                        "catalog value");
                if (prefix.isBlank()) {
                    output.addSuggestions(
                            STRING_VALUE_TEMPLATES,
                            prefix,
                            SuggestionKind.STRUCTURAL,
                            rawValueInsert,
                            2,
                            SuggestionSource.STRUCTURAL,
                            "string template");
                }
            }
            if (expectedModes.contains(RawValueMode.BOOLEAN)) {
                addBooleanSuggestions(output, prefix, booleanInsertStyle, rawValueInsert);
            }
            if (expectedModes.contains(RawValueMode.NUMBER) && !hasCompositeSpecificValueSnippet) {
                output.addSuggestions(
                        valueSources.numberSuggestions(),
                        prefix,
                        SuggestionKind.VALUE,
                        rawValueInsert,
                        0,
                        SuggestionSource.CATALOG,
                        "number value");
            }
            if (expectedModes.contains(RawValueMode.LITERAL)) {
                output.addSuggestions(
                        LITERAL_VALUES,
                        prefix,
                        SuggestionKind.LITERAL,
                        rawValueInsert,
                        0,
                        SuggestionSource.LITERAL,
                        "literal value");
            }
            if (expectedModes.contains(RawValueMode.NONE)) {
                output.addSuggestions(
                        typedCatalogValueHints,
                        prefix,
                        SuggestionKind.SNIPPET,
                        rawValueInsert,
                        1,
                        SuggestionSource.CATALOG,
                        "catalog template");
            }
        }

        if (!prefix.isBlank()) {
            output.addNearestSuggestions(
                    typedRegistryHints,
                    prefix,
                    SuggestionKind.VALUE,
                    stringValueInsert,
                    0,
                    SuggestionSource.REGISTRY,
                    "near registry value");
            output.addNearestSuggestions(
                    typedCatalogValueHints,
                    prefix,
                    SuggestionKind.VALUE,
                    stringValueInsert,
                    1,
                    SuggestionSource.CATALOG,
                    "near catalog value");
        }
        addValueSnippetSuggestions(output, text, cursor, insideQuote, currentKey, prefix, topLevelItemId, registryAccess);
        boolean hasCompositeValueSnippet = expectedModes.contains(RawValueMode.NONE)
                && (!typedCatalogValueHints.isEmpty() || hasSpecificValueSnippet);
        if (shouldOfferGenericValueFallbacks(slotType, expectedModes) && !hasCompositeValueSnippet) {
            addValueStructuralSuggestions(output, text, cursor, insideQuote, prefix);
        }
        if (slotType == RawSlotType.VALUE_UNKNOWN
                && shouldOfferGenericValueFallbacks(slotType, expectedModes)
                && (!hasCompositeValueSnippet || hasSpecificValueSnippet)) {
            addValueSnippetSuggestions(
                    output,
                    text,
                    cursor,
                    insideQuote,
                    context.containerKey(),
                    prefix,
                    topLevelItemId,
                    registryAccess
            );
        }

        return autocompleteResult(
                text,
                cursor,
                replaceStart,
                valueReplaceEnd,
                output.values(),
                registryAccess,
                expectedModes,
                slotType,
                currentKey
        );
    }

    private static AutocompleteResult autocompleteResult(
            String text,
            int requestedCaret,
            int replaceStart,
            int replaceEnd,
            Collection<Suggestion> candidates,
            RegistryAccess registryAccess,
            EnumSet<RawValueMode> expectedModes,
            RawSlotType slotType,
            String currentKey
    ) {
        return new AutocompleteResult(
                requestedCaret,
                replaceStart,
                replaceEnd,
                RawSuggestionPipeline.limit(
                        candidates,
                        text,
                        replaceStart,
                        replaceEnd,
                        registryAccess,
                        expectedModes,
                        slotType
                ),
                currentKey
        );
    }

    public static String closingSuffix(String text) {
        Deque<Character> closers = new ArrayDeque<>();
        boolean unfinishedString = false;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, index + 1);
                if (quoteEnd < 0) {
                    unfinishedString = true;
                    break;
                }
                index = quoteEnd;
            } else if (value == '{') {
                closers.push('}');
            } else if (value == '[') {
                closers.push(']');
            } else if (!closers.isEmpty() && value == closers.peek()) {
                closers.pop();
            }
        }

        StringBuilder suffix = new StringBuilder();
        if (unfinishedString) {
            suffix.append('"');
        }
        while (!closers.isEmpty()) {
            suffix.append(closers.pop());
        }
        return suffix.toString();
    }

    private static boolean shouldOfferGenericValueFallbacks(
            RawSlotType slotType,
            EnumSet<RawValueMode> expectedModes
    ) {
        return slotType == RawSlotType.VALUE_UNKNOWN
                && (expectedModes == null || expectedModes.isEmpty() || expectedModes.contains(RawValueMode.NONE));
    }

    private static List<String> localSiblingKeys(String text, int cursor) {
        int objectStart = localObjectStart(text, cursor);
        if (objectStart < 0) {
            return List.of();
        }

        int objectEnd = localObjectEnd(text, cursor);
        int scanEnd = objectEnd < 0 ? text.length() : objectEnd;
        Set<String> keys = new LinkedHashSet<>();
        collectArrayEntryKeys(text, cursor, keys);
        collectObjectKeys(text, objectStart + 1, cursor, keys);
        collectObjectKeys(text, cursor, scanEnd, keys);
        return List.copyOf(keys);
    }

    private static List<String> currentObjectKeys(String text, int cursor, boolean insideQuote) {
        int objectStart = localObjectStart(text, cursor);
        if (objectStart < 0) {
            return List.of();
        }

        int objectEnd = localObjectEnd(text, cursor);
        int scanEnd = objectEnd < 0 ? text.length() : objectEnd;
        int afterCursor = cursor;
        if (insideQuote) {
            int quoteEnd = findStringEndOnLine(text, cursor);
            if (quoteEnd >= cursor) {
                afterCursor = quoteEnd + 1;
            }
        }
        Set<String> keys = new LinkedHashSet<>();
        collectObjectKeys(text, objectStart + 1, cursor, keys);
        collectObjectKeys(text, afterCursor, scanEnd, keys);
        return List.copyOf(keys);
    }

    private static boolean insideArrayEntryObject(String text, int cursor) {
        int arrayStart = localArrayStart(text, cursor);
        int objectStart = localObjectStart(text, cursor);
        return arrayStart >= 0 && objectStart > arrayStart;
    }

    private static Map<String, String> localSiblingValues(String text, int cursor) {
        int objectStart = localObjectStart(text, cursor);
        if (objectStart < 0) {
            return Map.of();
        }

        int objectEnd = localObjectEnd(text, cursor);
        int scanEnd = objectEnd < 0 ? text.length() : objectEnd;
        Map<String, String> values = new LinkedHashMap<>();
        collectArrayEntryValues(text, cursor, values);
        collectObjectValues(text, objectStart + 1, cursor, values);
        collectObjectValues(text, cursor, scanEnd, values);
        collectSimpleStringFieldValues(text, objectStart + 1, scanEnd, values);
        return Map.copyOf(values);
    }

    private static void collectArrayEntryKeys(String text, int cursor, Set<String> keys) {
        int arrayStart = localArrayStart(text, cursor);
        if (arrayStart < 0) {
            return;
        }

        int arrayEnd = localArrayEnd(text, arrayStart);
        int scanEnd = arrayEnd < 0 ? text.length() : arrayEnd;
        forEachShallowObject(text, arrayStart + 1, scanEnd, (start, end) -> collectObjectKeys(text, start + 1, end, keys));
    }

    private static void collectArrayEntryValues(String text, int cursor, Map<String, String> values) {
        int arrayStart = localArrayStart(text, cursor);
        if (arrayStart < 0) {
            return;
        }

        int arrayEnd = localArrayEnd(text, arrayStart);
        int scanEnd = arrayEnd < 0 ? text.length() : arrayEnd;
        forEachShallowObject(text, arrayStart + 1, scanEnd, (start, end) -> collectObjectValues(text, start + 1, end, values));
    }

    private static int localArrayStart(String text, int cursor) {
        List<DelimiterFrame> stack = new ArrayList<>();
        int end = Math.clamp(cursor, 0, text.length());
        for (int index = 0; index < end; index++) {
            char value = text.charAt(index);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, index + 1);
                if (quoteEnd < 0 || quoteEnd >= end) {
                    break;
                }
                index = quoteEnd;
            } else if (value == '{' || value == '[') {
                stack.add(new DelimiterFrame(value, index));
            } else if ((value == '}' || value == ']') && !stack.isEmpty()) {
                stack.removeLast();
            }
        }

        for (int index = stack.size() - 1; index >= 0; index--) {
            DelimiterFrame frame = stack.get(index);
            if (frame.value() == '[') {
                return frame.index();
            }
        }
        return -1;
    }

    private static int localArrayEnd(String text, int arrayStart) {
        if (arrayStart < 0 || arrayStart >= text.length() || text.charAt(arrayStart) != '[') {
            return -1;
        }

        int depth = 0;
        for (int index = arrayStart + 1; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, index + 1);
                if (quoteEnd < 0) {
                    break;
                }
                index = quoteEnd;
            } else if (value == '{' || value == '[') {
                depth++;
            } else if (value == '}' || value == ']') {
                if (depth == 0 && value == ']') {
                    return index;
                }
                depth = Math.max(0, depth - 1);
            }
        }
        return -1;
    }

    private static void forEachShallowObject(
            String text,
            int start,
            int end,
            ObjectRangeConsumer consumer
    ) {
        int depth = 0;
        int cursor = Math.clamp(start, 0, text.length());
        int safeEnd = Math.clamp(end, cursor, text.length());
        while (cursor < safeEnd) {
            char value = text.charAt(cursor);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, cursor + 1);
                if (quoteEnd < 0 || quoteEnd >= safeEnd) {
                    return;
                }
                cursor = quoteEnd + 1;
            } else if (value == '{' && depth == 0) {
                int objectEnd = matchingObjectEnd(text, cursor, safeEnd);
                if (objectEnd < 0) {
                    cursor++;
                } else {
                    consumer.accept(cursor, objectEnd);
                    cursor = objectEnd + 1;
                }
            } else {
                if (value == '[' || value == '{') {
                    depth++;
                } else if (value == ']' || value == '}') {
                    depth = Math.max(0, depth - 1);
                }
                cursor++;
            }
        }
    }

    private static int matchingObjectEnd(String text, int objectStart, int safeEnd) {
        int depth = 0;
        for (int cursor = objectStart; cursor < safeEnd; cursor++) {
            char value = text.charAt(cursor);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, cursor + 1);
                if (quoteEnd < 0 || quoteEnd >= safeEnd) {
                    return -1;
                }
                cursor = quoteEnd;
            } else if (value == '{') {
                depth++;
            } else if (value == '}') {
                depth--;
                if (depth == 0) {
                    return cursor;
                }
            }
        }
        return -1;
    }

    private static int localObjectStart(String text, int cursor) {
        if (text == null || text.isBlank()) {
            return -1;
        }

        List<Integer> objectStack = new ArrayList<>();
        int end = Math.clamp(cursor, 0, text.length());
        for (int index = 0; index < end; index++) {
            char value = text.charAt(index);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, index + 1);
                if (quoteEnd < 0 || quoteEnd >= end) {
                    break;
                }
                index = quoteEnd;
            } else if (value == '{') {
                objectStack.add(index);
            } else if (value == '}' && !objectStack.isEmpty()) {
                objectStack.removeLast();
            }
        }
        return objectStack.isEmpty() ? -1 : objectStack.getLast();
    }

    private static int localObjectEnd(String text, int cursor) {
        int depth = 0;
        for (int index = Math.clamp(cursor, 0, text.length()); index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, index + 1);
                if (quoteEnd < 0) {
                    break;
                }
                index = quoteEnd;
            } else if (value == '{') {
                depth++;
            } else if (value == '}') {
                if (depth == 0) {
                    return index;
                }
                depth--;
            }
        }
        return -1;
    }

    private static void collectObjectKeys(String text, int start, int end, Set<String> keys) {
        int depth = 0;
        int cursor = Math.clamp(start, 0, text.length());
        int safeEnd = Math.clamp(end, cursor, text.length());
        while (cursor < safeEnd) {
            char value = text.charAt(cursor);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, cursor + 1);
                if (quoteEnd < 0 || quoteEnd >= safeEnd) {
                    return;
                }
                int next = skipWhitespaceForward(text, quoteEnd + 1);
                if (depth == 0 && next < safeEnd && text.charAt(next) == ':') {
                    keys.add(text.substring(cursor + 1, quoteEnd).toLowerCase(Locale.ROOT));
                }
                cursor = quoteEnd + 1;
            } else if (value == '{' || value == '[') {
                depth++;
                cursor++;
            } else if (value == '}' || value == ']') {
                depth = Math.max(0, depth - 1);
                cursor++;
            } else if (depth == 0 && isObjectKeyAheadTokenCharacter(value)) {
                int tokenEnd = cursor + 1;
                while (tokenEnd < safeEnd && isObjectKeyAheadTokenCharacter(text.charAt(tokenEnd))) {
                    tokenEnd++;
                }
                int next = skipWhitespaceForward(text, tokenEnd);
                if (next < safeEnd && text.charAt(next) == ':') {
                    keys.add(text.substring(cursor, tokenEnd).toLowerCase(Locale.ROOT));
                }
                cursor = tokenEnd;
            } else {
                cursor++;
            }
        }
    }

    private static void collectObjectValues(String text, int start, int end, Map<String, String> values) {
        int depth = 0;
        int cursor = Math.clamp(start, 0, text.length());
        int safeEnd = Math.clamp(end, cursor, text.length());
        while (cursor < safeEnd) {
            char value = text.charAt(cursor);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, cursor + 1);
                if (quoteEnd < 0 || quoteEnd >= safeEnd) {
                    return;
                }
                int next = skipWhitespaceForward(text, quoteEnd + 1);
                if (depth == 0 && next < safeEnd && text.charAt(next) == ':') {
                    cursor = collectObjectValue(
                            text,
                            text.substring(cursor + 1, quoteEnd),
                            next + 1,
                            safeEnd,
                            values
                    );
                } else {
                    cursor = quoteEnd + 1;
                }
            } else if (value == '{' || value == '[') {
                depth++;
                cursor++;
            } else if (value == '}' || value == ']') {
                depth = Math.max(0, depth - 1);
                cursor++;
            } else if (depth == 0 && isObjectKeyAheadTokenCharacter(value)) {
                int tokenEnd = cursor + 1;
                while (tokenEnd < safeEnd && isObjectKeyAheadTokenCharacter(text.charAt(tokenEnd))) {
                    tokenEnd++;
                }
                int next = skipWhitespaceForward(text, tokenEnd);
                if (next < safeEnd && text.charAt(next) == ':') {
                    cursor = collectObjectValue(
                            text,
                            text.substring(cursor, tokenEnd),
                            next + 1,
                            safeEnd,
                            values
                    );
                } else {
                    cursor = tokenEnd;
                }
            } else {
                cursor++;
            }
        }
    }

    private static int collectObjectValue(
            String text,
            String key,
            int valueStart,
            int safeEnd,
            Map<String, String> values
    ) {
        int start = skipWhitespaceForward(text, valueStart);
        int end = shallowValueEnd(text, start, safeEnd);
        if (start < end && key != null && !key.isBlank()) {
            values.put(key.toLowerCase(Locale.ROOT), text.substring(start, end).trim());
        }
        return Math.max(end, valueStart);
    }

    private static int shallowValueEnd(String text, int start, int safeEnd) {
        int depth = 0;
        for (int cursor = Math.clamp(start, 0, text.length()); cursor < safeEnd; cursor++) {
            char value = text.charAt(cursor);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, cursor + 1);
                if (quoteEnd < 0 || quoteEnd >= safeEnd) {
                    return safeEnd;
                }
                cursor = quoteEnd;
            } else if (value == '{' || value == '[') {
                depth++;
            } else if (value == '}' || value == ']') {
                if (depth == 0) {
                    return cursor;
                }
                depth--;
            } else if (value == ',' && depth == 0) {
                return cursor;
            }
        }
        return safeEnd;
    }

    private static void collectSimpleStringFieldValues(
            String text,
            int start,
            int end,
            Map<String, String> values
    ) {
        int safeStart = Math.clamp(start, 0, text.length());
        int safeEnd = Math.clamp(end, safeStart, text.length());
        Matcher matcher = SIMPLE_STRING_FIELD_PATTERN.matcher(text.substring(safeStart, safeEnd));
        while (matcher.find()) {
            String key = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            values.putIfAbsent(key.toLowerCase(Locale.ROOT), "\"" + matcher.group(3) + "\"");
        }
    }

    private static boolean refineKeyPosition(
            String text,
            int cursor,
            boolean insideQuote,
            boolean currentGuess,
            RawAutocompleteIndex.Context context
    ) {
        if (insideQuote) {
            Boolean keyInString = classifyActiveStringAsKey(text, cursor);
            return Objects.requireNonNullElse(keyInString, currentGuess);
        }
        if (context != null && context.inObject() && isAfterObjectEntryBoundary(text, cursor)) {
            return true;
        }
        if (isLikelyValuePositionOutsideString(text, cursor)) {
            return false;
        }
        return currentGuess;
    }

    private static boolean isAfterObjectEntryBoundary(String text, int cursor) {
        int previous = skipWhitespaceBackward(text, cursor - 1);
        if (previous < 0) {
            return true;
        }
        char value = text.charAt(previous);
        return value == '{' || value == ',';
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

    private static void addBooleanSuggestions(
            RawSuggestionBuilder output,
            String prefix,
            BooleanInsertStyle style,
            UnaryOperator<String> insertMapper
    ) {
        switch (style) {
            case NBT_BYTE -> {
                output.addMappedSuggestion("true", insertMapper.apply("1b"), prefix);
                output.addMappedSuggestion("false", insertMapper.apply("0b"), prefix);
            }
            case TEXT -> {
                output.addMappedSuggestion("true", insertMapper.apply("true"), prefix);
                output.addMappedSuggestion("false", insertMapper.apply("false"), prefix);
            }
        }
    }

    private static BooleanInsertStyle detectBooleanInsertStyle(
            String text,
            int replaceStart,
            String containerPath,
            String currentKey
    ) {
        String path = RawAutocompleteHints.buildFullPath(containerPath, Objects.requireNonNullElse(currentKey, "").toLowerCase(Locale.ROOT));
        if (RawAutocompleteHints.isBooleanPath(path)) {
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

    private static UnaryOperator<String> componentKeyInsert(
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote,
            String itemId,
            RegistryAccess registryAccess
    ) {
        return componentId -> componentKeySnippetInsert(
                componentId,
                text,
                cursor,
                replaceEnd,
                insideQuote,
                itemId,
                registryAccess
        );
    }

    private static UnaryOperator<String> itemStackKeyInsert(
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote,
            String containerPath
    ) {
        return key -> itemStackKeySnippetInsert(key, text, cursor, replaceEnd, insideQuote, containerPath);
    }

    private static UnaryOperator<String> registryMapKeyInsert(
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote,
            String containerKey,
            String containerPath,
            UnaryOperator<String> keyInsert
    ) {
        String value = RawAutocompleteHints.registryMapKeySnippetValue(containerKey, containerPath);
        return value.isBlank()
                ? keyInsert
                : key -> keyValueSnippetInsert(key, value, text, cursor, replaceEnd, insideQuote);
    }

    private static String itemStackKeySnippetInsert(
            String key,
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote,
            String containerPath
    ) {
        String value = RawAutocompleteHints.keySnippetValue(key, containerPath);
        return value.isBlank()
                ? keyOnlyInsert(key, text, cursor, replaceEnd, insideQuote)
                : keyValueSnippetInsert(key, value, text, cursor, replaceEnd, insideQuote);
    }

    private static String componentKeySnippetInsert(
            String componentId,
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote,
            String itemId,
            RegistryAccess registryAccess
    ) {
        String value = RawAutocompleteHints.keySnippetValue(componentId, itemId, registryAccess);
        if (value.isBlank()) {
            value = RawAutocompleteHints.keyValidationPlaceholders(componentId, itemId, registryAccess).getFirst();
        }
        return keyValueSnippetInsert(componentId, value, text, cursor, replaceEnd, insideQuote);
    }

    private static String keyValueSnippetInsert(
            String key,
            String value,
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote
    ) {
        if (suffixStartsWithColon(text, replaceEnd)) {
            return keyOnlyInsert(key, text, cursor, replaceEnd, insideQuote);
        }
        return keyOnlyInsert(key, text, cursor, replaceEnd, insideQuote)
                + ": "
                + formatSnippetForIndent(value, text, replaceEnd)
                + keySnippetSuffix(text, replaceEnd);
    }

    private static String keyOnlyInsert(
            String key,
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote
    ) {
        return insideQuote
                ? quotedInsert(text, cursor, replaceEnd).apply(key)
                : formatKeyInsert(key);
    }

    static boolean suffixStartsWithColon(String text, int cursor) {
        int next = skipWhitespaceForward(text, cursor);
        return next < text.length() && text.charAt(next) == ':';
    }

    private static String keySnippetSuffix(String text, int cursor) {
        int next = skipWhitespaceForward(text, cursor);
        if (next >= text.length() || !looksLikeObjectKeyAhead(text, next)) {
            return "";
        }
        return ",\n" + currentLineIndent(text, cursor);
    }

    private static String currentLineIndent(String text, int cursor) {
        int lineStart = text.lastIndexOf('\n', Math.max(0, cursor - 1)) + 1;
        int index = lineStart;
        while (index < text.length()) {
            char value = text.charAt(index);
            if (value != ' ' && value != '\t') {
                break;
            }
            index++;
        }
        return text.substring(lineStart, index);
    }

    private static String formatSnippetForIndent(String snippet, String text, int cursor) {
        if (snippet == null || !shouldPrettyPrintSnippet(snippet)) {
            return snippet;
        }

        String indent = currentLineIndent(text, cursor);
        String indentUnit = detectIndentUnit(text, cursor);
        return prettyPrintSnippet(snippet, indent, indentUnit);
    }

    private static boolean shouldPrettyPrintSnippet(String snippet) {
        String trimmed = Objects.requireNonNullElse(snippet, "").trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return false;
        }
        return trimmed.contains("\n") || trimmed.contains(",") || trimmed.startsWith("[");
    }

    private static String detectIndentUnit(String text, int cursor) {
        String currentIndent = currentLineIndent(text, cursor);
        int lineStart = text.lastIndexOf('\n', Math.max(0, cursor - 1)) + 1;
        int previousEnd = Math.max(0, lineStart - 1);
        while (previousEnd > 0) {
            int previousStart = text.lastIndexOf('\n', previousEnd - 1) + 1;
            String previousIndent = currentLineIndent(text, previousStart);
            if (previousIndent.length() > currentIndent.length()
                    && previousIndent.startsWith(currentIndent)) {
                return previousIndent.substring(currentIndent.length());
            }
            previousEnd = previousStart - 1;
        }
        return "    ";
    }

    private static String prettyPrintSnippet(String snippet, String baseIndent, String indentUnit) {
        String compact = compactSnippet(snippet);
        StringBuilder out = new StringBuilder(compact.length() + 32);
        int level = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int index = 0; index < compact.length(); index++) {
            char value = compact.charAt(index);
            if (inString) {
                out.append(value);
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == '"') {
                    inString = false;
                }
                continue;
            }

            switch (value) {
                case '"' -> {
                    inString = true;
                    out.append(value);
                }
                case '{', '[' -> {
                    out.append(value);
                    if (!nextNonWhitespaceIs(compact, index + 1, value == '{' ? '}' : ']')) {
                        level++;
                        appendSnippetNewline(out, baseIndent, indentUnit, level);
                    }
                }
                case '}', ']' -> {
                    if (!previousNonWhitespaceIsOpening(out)) {
                        level = Math.max(0, level - 1);
                        appendSnippetNewline(out, baseIndent, indentUnit, level);
                    }
                    out.append(value);
                }
                case ',' -> {
                    out.append(value);
                    appendSnippetNewline(out, baseIndent, indentUnit, level);
                }
                case ':' -> {
                    out.append(": ");
                    index = skipSnippetSpaces(compact, index + 1) - 1;
                }
                default -> out.append(value);
            }
        }
        return out.toString();
    }

    private static String compactSnippet(String snippet) {
        StringBuilder out = new StringBuilder(snippet.length());
        boolean inString = false;
        boolean escaping = false;
        boolean pendingSpace = false;

        for (int index = 0; index < snippet.length(); index++) {
            char value = snippet.charAt(index);
            if (inString) {
                out.append(value);
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == '"') {
                    inString = false;
                }
                continue;
            }

            if (value == '"') {
                if (pendingSpace && needsSnippetSpaceBefore(out)) {
                    out.append(' ');
                }
                pendingSpace = false;
                inString = true;
                out.append(value);
            } else if (Character.isWhitespace(value)) {
                pendingSpace = true;
            } else {
                if (pendingSpace && needsSnippetSpaceBefore(out) && needsSnippetSpaceAfter(value)) {
                    out.append(' ');
                }
                pendingSpace = false;
                out.append(value);
            }
        }
        return out.toString().trim();
    }

    private static boolean needsSnippetSpaceBefore(StringBuilder out) {
        if (out.isEmpty()) {
            return false;
        }
        char previous = out.charAt(out.length() - 1);
        return previous != '{'
                && previous != '['
                && previous != '('
                && previous != ':'
                && previous != ',';
    }

    private static boolean needsSnippetSpaceAfter(char value) {
        return value != '}'
                && value != ']'
                && value != ')'
                && value != ':'
                && value != ',';
    }

    private static void appendSnippetNewline(
            StringBuilder out,
            String baseIndent,
            String indentUnit,
            int level
    ) {
        out.append('\n').append(baseIndent);
        out.append(indentUnit.repeat(Math.max(0, level)));
    }

    private static boolean nextNonWhitespaceIs(String text, int start, char expected) {
        int index = skipSnippetSpaces(text, start);
        return index < text.length() && text.charAt(index) == expected;
    }

    private static boolean previousNonWhitespaceIsOpening(StringBuilder out) {
        for (int index = out.length() - 1; index >= 0; index--) {
            char value = out.charAt(index);
            if (!Character.isWhitespace(value)) {
                return value == '{' || value == '[';
            }
        }
        return false;
    }

    private static int skipSnippetSpaces(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static void addValueSnippetSuggestions(
            RawSuggestionBuilder output,
            String text,
            int cursor,
            boolean insideQuote,
            String currentKey,
            String prefix,
            String itemId,
            RegistryAccess registryAccess
    ) {
        if (insideQuote) {
            return;
        }
        if (Objects.requireNonNullElse(currentKey, "").isBlank()) {
            return;
        }

        String normalizedKey = currentKey.toLowerCase(Locale.ROOT);
        String snippet = RawAutocompleteHints.valueSnippet(normalizedKey, itemId, registryAccess);
        if (snippet.isEmpty()) {
            return;
        }

        if (!prefix.isBlank()) {
            String normalizedPrefix = RawSuggestionBuilder.normalizeFuzzy(prefix);
            if (!RawSuggestionBuilder.normalizeFuzzy(snippet).startsWith(normalizedPrefix)
                    && !RawSuggestionBuilder.normalizeFuzzy(currentKey).startsWith(normalizedPrefix)) {
                return;
            }
        }

        String label = currentKey + " template";
        output.add(new Suggestion(
                label,
                formatSnippetForIndent(snippet, text, cursor),
                SuggestionKind.SNIPPET,
                0,
                0,
                SuggestionSource.CATALOG,
                "value template"
        ));
    }

    private static boolean shouldSuggestAtPosition(
            String text,
            int cursor,
            boolean keyPosition,
            boolean insideQuote,
            String prefix
    ) {
        if (insideQuote) {
            return !completedQuotedTokenAhead(text, cursor, prefix);
        }
        if (!Objects.requireNonNullElse(prefix, "").isBlank()) {
            return true;
        }
        if (completedBareTokenAhead(text, cursor)) {
            return false;
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

    private static boolean completedQuotedTokenAhead(String text, int cursor, String prefix) {
        if (!Objects.requireNonNullElse(prefix, "").isBlank()) {
            return false;
        }

        String safeText = Objects.requireNonNullElse(text, "");
        int safeCursor = Math.clamp(cursor, 0, safeText.length());
        if (safeCursor >= safeText.length() || !isTokenCharacter(safeText.charAt(safeCursor))) {
            return false;
        }

        int quoteEnd = findStringEndOnLine(safeText, safeCursor);
        return quoteEnd > safeCursor;
    }

    private static boolean completedBareTokenAhead(String text, int cursor) {
        String safeText = Objects.requireNonNullElse(text, "");
        int safeCursor = Math.clamp(cursor, 0, safeText.length());
        return safeCursor < safeText.length() && isTokenCharacter(safeText.charAt(safeCursor));
    }

    private static void addKeyStructuralSuggestions(
            RawSuggestionBuilder output,
            String text,
            int cursor,
            boolean insideQuote,
            String prefix
    ) {
        if (insideQuote) {
            return;
        }
        if (prefix.isBlank()) {
            output.add(new Suggestion("\"\":", "\"\": ", SuggestionKind.STRUCTURAL, 0, 0));
            output.add(new Suggestion("\"\"", "\"\"", SuggestionKind.STRUCTURAL, 1, 0));
        } else {
            int nextSameLine = skipWhitespaceForwardSameLine(text, cursor);
            if (nextSameLine < 0 || text.charAt(nextSameLine) != ':') {
                output.add(new Suggestion(":", ": ", SuggestionKind.STRUCTURAL, 0, 1));
            }
        }
    }

    private static void addValueStructuralSuggestions(
            RawSuggestionBuilder output,
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
            output.add(new Suggestion("\"\"", "\"\"", SuggestionKind.STRUCTURAL, 0, 0));
            output.add(new Suggestion("{}", "{}", SuggestionKind.STRUCTURAL, 1, 0));
            output.add(new Suggestion("[]", "[]", SuggestionKind.STRUCTURAL, 1, 0));
        }

        if (previousChar == '"' && nextChar != ':' && prefix.isBlank()) {
            output.add(new Suggestion(":", ": ", SuggestionKind.STRUCTURAL, 0, 0));
        }

        if (previousChar == '"' || Character.isDigit(previousChar) || previousChar == '}' || previousChar == ']') {
            addCommaSuggestionIfMissing(output, nextChar);
        }
    }

    private static void addAfterValueStructuralSuggestions(
            RawSuggestionBuilder output,
            String text,
            int cursor
    ) {
        int next = skipWhitespaceForward(text, cursor);
        char nextChar = next >= text.length() ? '\0' : text.charAt(next);
        if (nextChar != ',' && nextChar != '}' && nextChar != ']') {
            addCommaSuggestionIfMissing(output, nextChar);
        }
    }

    private static void addCommaSuggestionIfMissing(RawSuggestionBuilder output, char nextChar) {
        if (nextChar != ',') {
            output.add(new Suggestion(",", ",", SuggestionKind.STRUCTURAL, 0, 0));
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

        return firstChar == '"' || isTokenCharacter(firstChar);
    }

    private static int findLineKeySeparator(String text, int lineStart, int cursor) {
        int end = Math.clamp(cursor, lineStart, text.length());
        int separator = -1;
        for (int index = lineStart; index < end; index++) {
            char value = text.charAt(index);
            if (value == '"' || value == '\'') {
                int quoteEnd = findQuotedEnd(text, index + 1, value);
                if (quoteEnd < 0 || quoteEnd >= end) {
                    break;
                }
                index = quoteEnd;
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
            List<String> catalogObjectKeys,
            List<String> componentNbtFields,
            List<String> dynamicKeyHints,
            List<String> seenContainerKeys
    ) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return Stream.of(contextualKeys, catalogObjectKeys, componentNbtFields, dynamicKeyHints, seenContainerKeys)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .anyMatch(value -> value != null && value.equalsIgnoreCase(key));
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

        if (isTokenCharacter(value) || value == '"') {
            while (current >= 0) {
                char candidate = text.charAt(current);
                if (isTokenCharacter(candidate) || candidate == '"') {
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

    private static String detectTopLevelItemId(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int depth = 0;
        boolean expectingRootKey = false;
        boolean expectingRootValue = false;
        String pendingRootKey = "";

        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '"') {
                int quoteEnd = findStringEnd(text, index + 1);
                if (quoteEnd < 0) {
                    return "";
                }
                String stringToken = text.substring(index + 1, quoteEnd).replace("\\\"", "\"");
                String found = consumeTopLevelToken(
                        stringToken,
                        true,
                        expectingRootKey,
                        expectingRootValue,
                        pendingRootKey
                );
                if (!found.isEmpty()) {
                    return found;
                }
                if (depth == 1 && expectingRootKey) {
                    pendingRootKey = stringToken;
                    expectingRootKey = false;
                } else if (depth == 1 && expectingRootValue) {
                    pendingRootKey = "";
                    expectingRootValue = false;
                }
                index = quoteEnd;
                continue;
            }

            if (Character.isWhitespace(value)) {
                continue;
            }

            switch (value) {
                case '{', '[' -> {
                    if (depth == 1 && expectingRootValue) {
                        pendingRootKey = "";
                        expectingRootValue = false;
                    }
                    depth++;
                    if (depth == 1 && value == '{') {
                        expectingRootKey = true;
                    }
                }
                case '}', ']' -> {
                    if (depth > 0) {
                        depth--;
                    }
                }
                case ',' -> {
                    if (depth == 1) {
                        pendingRootKey = "";
                        expectingRootValue = false;
                        expectingRootKey = true;
                    }
                }
                case ':' -> {
                    if (depth == 1 && !pendingRootKey.isBlank()) {
                        expectingRootKey = false;
                        expectingRootValue = true;
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
                    String rawToken = text.substring(index, end);
                    String found = consumeTopLevelToken(
                            rawToken,
                            false,
                            depth == 1 && expectingRootKey,
                            depth == 1 && expectingRootValue,
                            pendingRootKey
                    );
                    if (!found.isEmpty()) {
                        return found;
                    }
                    if (depth == 1 && expectingRootKey) {
                        pendingRootKey = rawToken;
                        expectingRootKey = false;
                    } else if (depth == 1 && expectingRootValue) {
                        pendingRootKey = "";
                        expectingRootValue = false;
                    }
                    index = end - 1;
                }
            }
        }

        return "";
    }

    private static String consumeTopLevelToken(
            String token,
            boolean quoted,
            boolean expectingRootKey,
            boolean expectingRootValue,
            String pendingRootKey
    ) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (expectingRootKey || !expectingRootValue || !"id".equals(pendingRootKey)) {
            return "";
        }
        if (quoted && NAMESPACED_ID_PATTERN.matcher(token.toLowerCase(Locale.ROOT)).matches()) {
            return token.toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static boolean isParserTokenCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '-'
                || value == '.'
                || value == '+'
                || value == '/';
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
                || value == '#'
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

    private static int valueReplaceEnd(String text, int cursor, boolean insideQuote) {
        if (!insideQuote) {
            return cursor;
        }
        int quoteEnd = findStringEndOnLine(text, cursor);
        return quoteEnd >= cursor && needsValueCommaAfterCompletion(text, quoteEnd + 1)
                ? quoteEnd + 1
                : cursor;
    }

    private static int keyReplaceEnd(String text, int cursor, boolean insideQuote) {
        if (!insideQuote) {
            int end = cursor;
            while (end < text.length() && isObjectKeyAheadTokenCharacter(text.charAt(end))) {
                end++;
            }
            return end;
        }
        int quoteEnd = findStringEndOnLine(text, cursor);
        if (quoteEnd < cursor) {
            return cursor;
        }
        int next = skipWhitespaceForward(text, quoteEnd + 1);
        return next < text.length() && text.charAt(next) == ':' ? quoteEnd : quoteEnd + 1;
    }

    private static boolean quotedKeyHasColonAfterCursor(String text, int cursor) {
        int quoteEnd = findStringEndOnLine(text, cursor);
        if (quoteEnd < cursor) {
            return false;
        }
        int next = skipWhitespaceForward(text, quoteEnd + 1);
        return next < text.length() && text.charAt(next) == ':';
    }

    private static UnaryOperator<String> valueInsert(
            String text,
            int cursor,
            int replaceEnd,
            boolean insideQuote
    ) {
        UnaryOperator<String> formatter = insideQuote
                ? quotedInsert(text, cursor, replaceEnd)
                : RawAutocompleteUtil::formatValueInsert;
        return value -> appendMissingValueComma(formatter.apply(value), text, replaceEnd);
    }

    private static UnaryOperator<String> rawValueInsert(String text, int replaceEnd) {
        return value -> appendMissingValueComma(value, text, replaceEnd);
    }

    private static String appendMissingValueComma(
            String insert,
            String text,
            int cursor
    ) {
        if (insert == null || insert.endsWith(",") || !needsValueCommaAfterCompletion(text, cursor)) {
            return insert;
        }
        return insert + ",";
    }

    private static boolean needsValueCommaAfterCompletion(String text, int cursor) {
        if (text == null || text.isBlank()) {
            return false;
        }

        int next = skipWhitespaceForward(text, cursor);
        if (next >= text.length()) {
            return false;
        }
        char nextChar = text.charAt(next);
        if (nextChar == ',' || nextChar == '}' || nextChar == ']') {
            return false;
        }
        return looksLikeObjectKeyAhead(text, next);
    }

    private static boolean looksLikeObjectKeyAhead(String text, int start) {
        if (start < 0 || start >= text.length()) {
            return false;
        }

        char first = text.charAt(start);
        int tokenEnd;
        if (first == '"') {
            tokenEnd = findStringEnd(text, start + 1);
            if (tokenEnd < 0) {
                return false;
            }
            tokenEnd++;
        } else if (isObjectKeyAheadTokenCharacter(first)) {
            tokenEnd = start + 1;
            while (tokenEnd < text.length() && isObjectKeyAheadTokenCharacter(text.charAt(tokenEnd))) {
                tokenEnd++;
            }
        } else {
            return false;
        }

        int afterToken = skipWhitespaceForward(text, tokenEnd);
        return afterToken < text.length() && text.charAt(afterToken) == ':';
    }

    private static boolean isObjectKeyAheadTokenCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '.'
                || value == '-'
                || value == '/';
    }

    private static UnaryOperator<String> quotedInsert(String text, int cursor, int replaceEnd) {
        return value -> shouldAppendClosingQuote(text, cursor, replaceEnd)
                ? value + "\""
                : value;
    }

    private static boolean shouldAppendClosingQuote(String text, int cursor, int replaceEnd) {
        if (replaceEnd > cursor) {
            return replaceEnd >= text.length() || text.charAt(replaceEnd) != '"';
        }
        return !hasClosingQuoteAfterCursor(text, cursor);
    }

    private static boolean hasClosingQuoteAfterCursor(String text, int cursor) {
        int safeCursor = Math.clamp(cursor, 0, text.length());
        int quoteStart = text.lastIndexOf('"', Math.max(0, safeCursor - 1));
        return quoteStart >= 0 && findStringEndOnLine(text, safeCursor) > quoteStart;
    }

    private static boolean requiresQuotedKey(String key) {
        return key == null || !SIMPLE_KEY_PATTERN.matcher(key).matches();
    }

    private static boolean canBeBareToken(String value) {
        return value != null && BARE_VALUE_PATTERN.matcher(value).matches();
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
        return findQuotedEnd(text, start, '"');
    }

    private static int findStringEndOnLine(String text, int start) {
        int end = findStringEnd(text, start);
        if (end < 0) {
            return -1;
        }
        int lineStart = Math.clamp(start, 0, end);
        int newline = text.indexOf('\n', lineStart);
        int carriageReturn = text.indexOf('\r', lineStart);
        return (newline >= 0 && newline < end) || (carriageReturn >= 0 && carriageReturn < end) ? -1 : end;
    }

    private static int findQuotedEnd(String text, int start, char quote) {
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
            if (value == quote) {
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

    public record Suggestion(
            String label,
            String insertText,
            SuggestionKind kind,
            int matchRank,
            int contextRank,
            SuggestionSource source,
            int confidence,
            String reason
    ) {
        public Suggestion(
                String label,
                String insertText,
                SuggestionKind kind,
                int matchRank,
                int contextRank
        ) {
            this(
                    label,
                    insertText,
                    kind,
                    matchRank,
                    contextRank,
                    defaultSource(kind),
                    defaultConfidence(defaultSource(kind)),
                    ""
            );
        }

        public Suggestion(
                String label,
                String insertText,
                SuggestionKind kind,
                int matchRank,
                int contextRank,
                SuggestionSource source,
                String reason
        ) {
            this(label, insertText, kind, matchRank, contextRank, source, -1, reason);
        }

        public Suggestion {
            source = source == null ? defaultSource(kind) : source;
            confidence = confidence < 0
                    ? defaultConfidence(source)
                    : Math.clamp(confidence, 0, 100);
            reason = Objects.requireNonNullElse(reason, "");
        }

        private static SuggestionSource defaultSource(SuggestionKind kind) {
            if (kind == null) {
                return SuggestionSource.GENERATED;
            }
            return switch (kind) {
                case LITERAL -> SuggestionSource.LITERAL;
                case STRUCTURAL -> SuggestionSource.STRUCTURAL;
                case KEY, VALUE, SNIPPET -> SuggestionSource.CATALOG;
            };
        }

        private static int defaultConfidence(SuggestionSource source) {
            return source == null ? 40 : source.defaultConfidence();
        }
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

    public enum SuggestionSource {
        VALIDATED_REGISTRY(0, 96),
        ITEM_PROFILE(1, 88),
        REGISTRY(2, 82),
        SIBLING(3, 80),
        CATALOG(4, 70),
        LITERAL(5, 68),
        SEEN(6, 50),
        STRUCTURAL(7, 45),
        GENERATED(8, 40);

        private final int rank;
        private final int defaultConfidence;

        SuggestionSource(int rank, int defaultConfidence) {
            this.rank = rank;
            this.defaultConfidence = defaultConfidence;
        }

        public int rank() {
            return this.rank;
        }

        public int defaultConfidence() {
            return this.defaultConfidence;
        }
    }

    private enum BooleanInsertStyle {
        TEXT,
        NBT_BYTE
    }

    private record DelimiterFrame(char value, int index) {
    }

    @FunctionalInterface
    private interface ObjectRangeConsumer {
        void accept(int start, int end);
    }

    private record KeyCorrection(int replaceStart, int replaceEnd, String keyPrefix) {
    }

}
