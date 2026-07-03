package me.noramibu.itemeditor.util;

import net.minecraft.core.RegistryAccess;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

final class RawSuggestionSources {

    private RawSuggestionSources() {
    }

    static Key key(
            RawAutocompleteIndex.Context context,
            String itemId,
            List<String> activeProfiles,
            RegistryAccess registryAccess
    ) {
        return new Key(
                RawAutocompleteHints.contextualKeyHints(context.containerKey(), context.containerPath()),
                RawAutocompleteHints.objectKeyHints(
                        context.containerKey(),
                        context.containerPath(),
                        itemId,
                        activeProfiles,
                        registryAccess
                ),
                RawAutocompleteHints.componentFieldHints(
                        context.containerKey(),
                        itemId,
                        activeProfiles,
                        registryAccess
                ),
                RawAutocompleteHints.dynamicKeyHints(
                        context.containerKey(),
                        context.containerPath(),
                        registryAccess
                )
        );
    }

    static Value value(
            String text,
            int replaceStart,
            int cursor,
            boolean insideQuote,
            String prefix,
            String currentKey,
            RawAutocompleteIndex.Context context,
            Map<String, String> siblingValues,
            List<String> activeProfiles,
            String itemId,
            RegistryAccess registryAccess
    ) {
        RawSlotType slotType = RawAutocompleteHints.classifySlotType(
                currentKey,
                context.containerKey(),
                context.containerPath(),
                insideQuote,
                activeProfiles,
                itemId,
                registryAccess
        );
        slotType = RawAutocompleteHints.refineSlotTypeWithSiblingValues(
                slotType,
                currentKey,
                context.containerPath(),
                siblingValues
        );
        EnumSet<RawValueMode> expectedModes = RawAutocompleteHints.expectedModesForSlot(slotType);
        if (slotType == RawSlotType.VALUE_UNKNOWN) {
            expectedModes = RawAutocompleteHints.expectedValueModes(
                    currentKey,
                    context.containerPath(),
                    prefix,
                    insideQuote
            );
            expectedModes = RawAutocompleteHints.refineExpectedModesWithSiblingValues(
                    currentKey,
                    context.containerPath(),
                    siblingValues,
                    expectedModes,
                    insideQuote
            );
            expectedModes = RawAutocompleteHints.refineExpectedModesWithRuntimeProbe(
                    text,
                    replaceStart,
                    cursor,
                    currentKey,
                    context.containerPath(),
                    registryAccess,
                    expectedModes,
                    insideQuote
            );
        }

        List<String> catalogHints = RawAutocompleteHints.contextualValueHints(
                currentKey,
                context.containerPath(),
                registryAccess
        );
        catalogHints = RawAutocompleteHints.mergeUnique(
                RawAutocompleteHints.siblingValueHints(currentKey, context.containerPath(), siblingValues),
                catalogHints
        );
        List<String> registryHints = RawAutocompleteHints.registryHintsForSlot(
                slotType,
                currentKey,
                context.containerKey(),
                context.containerPath(),
                registryAccess
        );
        registryHints = RawAutocompleteHints.mergeUnique(
                RawAutocompleteHints.siblingRegistryValueHints(
                        currentKey,
                        context.containerPath(),
                        siblingValues,
                        registryAccess
                ),
                registryHints
        );
        List<String> validatedRegistryHints = RawAutocompleteHints.validatedRegistryHintsForValue(
                text,
                replaceStart,
                cursor,
                insideQuote,
                currentKey,
                context.containerKey(),
                context.containerPath(),
                registryAccess
        );
        if (!validatedRegistryHints.isEmpty()) {
            registryHints = validatedRegistryHints;
        }

        List<String> typedCatalogHints = RawAutocompleteHints.filterValuesByModes(
                catalogHints,
                expectedModes
        );
        List<String> typedRegistryHints = RawAutocompleteHints.filterValuesByModes(
                registryHints,
                expectedModes
        );
        if (slotType == RawSlotType.VALUE_FLOAT) {
            typedCatalogHints = RawAutocompleteHints.filterFloatHints(typedCatalogHints);
            typedRegistryHints = RawAutocompleteHints.filterFloatHints(typedRegistryHints);
        } else if (slotType == RawSlotType.VALUE_INT || expectedModes.contains(RawValueMode.NUMBER)) {
            typedCatalogHints = RawAutocompleteHints.filterNumericHintsForCurrentKey(
                    typedCatalogHints,
                    currentKey,
                    context.containerPath()
            );
            typedRegistryHints = RawAutocompleteHints.filterNumericHintsForCurrentKey(
                    typedRegistryHints,
                    currentKey,
                    context.containerPath()
            );
        }

        List<String> numberSuggestions = switch (slotType) {
            case VALUE_INT -> RawAutocompleteHints.integerNumberValues();
            case VALUE_FLOAT -> RawAutocompleteHints.floatNumberValues();
            default -> RawAutocompleteHints.numberSuggestionsForCurrentKey(currentKey, context.containerPath());
        };
        return new Value(slotType, expectedModes, typedCatalogHints, typedRegistryHints, numberSuggestions);
    }

    static void addKeyMatches(
            RawSuggestionBuilder output,
            Key sources,
            List<String> seenContainerKeys,
            String prefix,
            UnaryOperator<String> itemStackKeyInsert,
            UnaryOperator<String> dynamicKeyInsert,
            boolean nearest
    ) {
        addKeySource(
                output,
                sources.contextualKeys(),
                prefix,
                itemStackKeyInsert,
                0,
                RawAutocompleteUtil.SuggestionSource.CATALOG,
                "context key",
                nearest);
        addKeySource(
                output,
                sources.catalogObjectKeys(),
                prefix,
                itemStackKeyInsert,
                1,
                RawAutocompleteUtil.SuggestionSource.CATALOG,
                "catalog key",
                nearest);
        addKeySource(
                output,
                sources.componentNbtFields(),
                prefix,
                itemStackKeyInsert,
                0,
                RawAutocompleteUtil.SuggestionSource.CATALOG,
                "component field",
                nearest);
        addKeySource(
                output,
                sources.dynamicKeyHints(),
                prefix,
                dynamicKeyInsert,
                0,
                RawAutocompleteUtil.SuggestionSource.REGISTRY,
                "registry map key",
                nearest);
        addKeySource(
                output,
                seenContainerKeys,
                prefix,
                itemStackKeyInsert,
                1,
                RawAutocompleteUtil.SuggestionSource.SEEN,
                "seen in this container",
                nearest);
    }

    private static void addKeySource(
            RawSuggestionBuilder output,
            List<String> values,
            String prefix,
            UnaryOperator<String> keyInsert,
            int rank,
            RawAutocompleteUtil.SuggestionSource source,
            String reason,
            boolean nearest
    ) {
        if (nearest) {
            output.addNearestSuggestions(
                    values,
                    prefix,
                    RawAutocompleteUtil.SuggestionKind.KEY,
                    keyInsert,
                    rank,
                    source,
                    reason);
        } else {
            output.addSuggestions(
                    values,
                    prefix,
                    RawAutocompleteUtil.SuggestionKind.KEY,
                    keyInsert,
                    rank,
                    source,
                    reason);
        }
    }

    record Key(
            List<String> contextualKeys,
            List<String> catalogObjectKeys,
            List<String> componentNbtFields,
            List<String> dynamicKeyHints
    ) {
    }

    record Value(
            RawSlotType slotType,
            EnumSet<RawValueMode> expectedModes,
            List<String> typedCatalogHints,
            List<String> typedRegistryHints,
            List<String> numberSuggestions
    ) {
    }

}
