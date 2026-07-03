package me.noramibu.itemeditor.util;

import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class RawSuggestionPipeline {

    private static final int MAX_RESULTS = 18;
    private static final int PARSE_FILTER_CANDIDATE_LIMIT = 64;
    private static final int PARSE_FILTER_MAX_TEXT_LENGTH = 24000;
    private static final int PARSE_FILTER_NEAR_CARET_WINDOW = 96;
    private static final int WEIGHT_TYPE_MATCH = 1000;
    private static final int WEIGHT_PARSER = 760;
    private static final int WEIGHT_PREFIX = 80;
    private static final int WEIGHT_RECENCY = 35;
    private static final int WEIGHT_CONTEXT = 120;
    private static final int WEIGHT_SOURCE = 70;
    private static final int WEIGHT_CONFIDENCE = 10;

    private RawSuggestionPipeline() {
    }

    static List<RawAutocompleteUtil.Suggestion> limit(
            Collection<RawAutocompleteUtil.Suggestion> source,
            String text,
            int replaceStart,
            int replaceEnd,
            RegistryAccess registryAccess,
            EnumSet<RawValueMode> expectedModes,
            RawSlotType slotType
    ) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        Comparator<RawAutocompleteUtil.Suggestion> baseComparator = RawSuggestionBuilder.QUALITY_COMPARATOR;
        List<RawAutocompleteUtil.Suggestion> preliminary = source.stream()
                .sorted(baseComparator)
                .limit(PARSE_FILTER_CANDIDATE_LIMIT)
                .toList();

        List<ParseFilteredSuggestion> accepted = canParseFilter(text, registryAccess)
                ? parseErrorGuidedFilter(preliminary, text, replaceStart, replaceEnd, registryAccess, baseComparator)
                : unfiltered(preliminary);
        if (accepted.isEmpty() && !preliminary.isEmpty()) {
            accepted = unfiltered(preliminary);
        }

        return rank(accepted, text, replaceStart, expectedModes, slotType, baseComparator).stream()
                .limit(MAX_RESULTS)
                .toList();
    }

    private static boolean canParseFilter(String text, RegistryAccess registryAccess) {
        return registryAccess != null && text != null && text.length() <= PARSE_FILTER_MAX_TEXT_LENGTH;
    }

    private static List<ParseFilteredSuggestion> unfiltered(
            List<RawAutocompleteUtil.Suggestion> suggestions
    ) {
        return suggestions.stream()
                .map(candidate -> new ParseFilteredSuggestion(candidate, 2, Integer.MAX_VALUE))
                .toList();
    }

    private static List<ParseFilteredSuggestion> parseErrorGuidedFilter(
            List<RawAutocompleteUtil.Suggestion> candidates,
            String text,
            int replaceStart,
            int replaceEnd,
            RegistryAccess registryAccess,
            Comparator<RawAutocompleteUtil.Suggestion> baseComparator
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        int safeStart = Math.clamp(replaceStart, 0, text.length());
        int safeEnd = Math.clamp(replaceEnd, safeStart, text.length());
        int baselineCursor = parseErrorCursor(text, RawItemDataUtil.parse(text, registryAccess));
        String prefix = text.substring(0, safeStart);
        String suffix = text.substring(safeEnd);

        List<ParseFilteredSuggestion> accepted = new ArrayList<>();
        for (RawAutocompleteUtil.Suggestion candidate : candidates) {
            ParseFilteredSuggestion parsed = parseCandidate(
                    candidate,
                    prefix,
                    suffix,
                    safeStart,
                    registryAccess,
                    baselineCursor
            );
            if (parsed != null) {
                accepted.add(parsed);
            }
        }

        accepted.sort(Comparator
                .comparingInt(ParseFilteredSuggestion::parseRank)
                .thenComparingInt(ParseFilteredSuggestion::distanceToTarget)
                .thenComparing(ParseFilteredSuggestion::suggestion, baseComparator));
        return accepted;
    }

    private static ParseFilteredSuggestion parseCandidate(
            RawAutocompleteUtil.Suggestion candidate,
            String prefix,
            String suffix,
            int safeStart,
            RegistryAccess registryAccess,
            int baselineCursor
    ) {
        if (candidate.source() == RawAutocompleteUtil.SuggestionSource.REGISTRY
                && "registry map key".equals(candidate.reason())) {
            return new ParseFilteredSuggestion(candidate, 2, Integer.MAX_VALUE);
        }

        ParseFilteredSuggestion bestError = null;
        for (String insert : validationInserts(candidate, suffix)) {
            String candidateText = prefix + insert + suffix;
            RawItemDataUtil.ParseResult parsed = RawItemDataUtil.parse(candidateText, registryAccess);
            if (parsed.success()) {
                return new ParseFilteredSuggestion(candidate, 0, 0);
            }
            if (suffix.isBlank()
                    && RawItemDataUtil.parse(candidateText + RawAutocompleteUtil.closingSuffix(candidateText), registryAccess).success()) {
                return new ParseFilteredSuggestion(candidate, 1, 0);
            }

            int errorCursor = parseErrorCursor(candidateText, parsed);
            if (errorCursor < 0) {
                continue;
            }

            int distanceToCaret = Math.abs(errorCursor - (safeStart + insert.length()));
            boolean nearCaret = distanceToCaret <= PARSE_FILTER_NEAR_CARET_WINDOW;
            boolean movedForward = baselineCursor < 0 ? nearCaret : errorCursor > baselineCursor;
            if (movedForward && nearCaret
                    && (bestError == null || distanceToCaret < bestError.distanceToTarget())) {
                bestError = new ParseFilteredSuggestion(candidate, 1, distanceToCaret);
            }
        }
        return bestError;
    }

    private static List<String> validationInserts(
            RawAutocompleteUtil.Suggestion candidate,
            String suffix
    ) {
        if (candidate.kind() != RawAutocompleteUtil.SuggestionKind.KEY
                || RawAutocompleteUtil.suffixStartsWithColon(suffix, 0)) {
            return List.of(candidate.insertText());
        }

        LinkedHashSet<String> inserts = new LinkedHashSet<>();
        inserts.add(candidate.insertText());
        for (String placeholder : RawAutocompleteHints.keyValidationPlaceholders(validationKey(candidate))) {
            inserts.add(candidate.insertText() + ": " + placeholder);
        }
        return List.copyOf(inserts);
    }

    private static String validationKey(RawAutocompleteUtil.Suggestion candidate) {
        String key = candidate.label() == null || candidate.label().isBlank()
                ? candidate.insertText()
                : candidate.label();
        return unquoteValidationKey(key).toLowerCase(Locale.ROOT);
    }

    private static String unquoteValidationKey(String key) {
        String trimmed = key == null ? "" : key.trim();
        if (trimmed.length() < 2) {
            return trimmed;
        }

        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return trimmed;
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
            if (text.charAt(index) == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }
        return -1;
    }

    private static List<RawAutocompleteUtil.Suggestion> rank(
            List<ParseFilteredSuggestion> candidates,
            String text,
            int replaceStart,
            EnumSet<RawValueMode> expectedModes,
            RawSlotType slotType,
            Comparator<RawAutocompleteUtil.Suggestion> fallbackComparator
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        return candidates.stream()
                .sorted(Comparator
                        .comparingInt((ParseFilteredSuggestion candidate) ->
                                weightedScore(candidate, text, replaceStart, expectedModes, slotType))
                        .reversed()
                        .thenComparing(ParseFilteredSuggestion::suggestion, fallbackComparator))
                .map(ParseFilteredSuggestion::suggestion)
                .toList();
    }

    private static int weightedScore(
            ParseFilteredSuggestion candidate,
            String text,
            int replaceStart,
            EnumSet<RawValueMode> expectedModes,
            RawSlotType slotType
    ) {
        RawAutocompleteUtil.Suggestion suggestion = candidate.suggestion();
        return (typeMatchScore(suggestion, expectedModes, slotType) * WEIGHT_TYPE_MATCH)
                + (parserScore(candidate) * WEIGHT_PARSER)
                + (Math.max(0, 4 - suggestion.matchRank()) * WEIGHT_PREFIX)
                + (recencyScore(text, replaceStart, suggestion) * WEIGHT_RECENCY)
                + (Math.max(0, 5 - suggestion.contextRank()) * WEIGHT_CONTEXT)
                + (Math.max(0, 8 - suggestion.source().rank()) * WEIGHT_SOURCE)
                + (suggestion.confidence() * WEIGHT_CONFIDENCE);
    }

    private static int parserScore(ParseFilteredSuggestion candidate) {
        return switch (candidate.parseRank()) {
            case 0 -> 8;
            case 1 -> 2;
            default -> 0;
        };
    }

    private static int typeMatchScore(
            RawAutocompleteUtil.Suggestion suggestion,
            EnumSet<RawValueMode> expectedModes,
            RawSlotType slotType
    ) {
        if (slotType == RawSlotType.OBJECT_KEY) {
            boolean keyCompletion = suggestion.kind() == RawAutocompleteUtil.SuggestionKind.KEY
                    || isComponentKeyValueSnippet(suggestion);
            return keyCompletion ? 3 : 0;
        }

        RawValueMode mode = RawValueClassifier.classify(suggestion.insertText());
        if (expectedModes != null && !expectedModes.isEmpty()
                && !expectedModes.contains(RawValueMode.NONE)) {
            if (expectedModes.contains(mode)) {
                return 3;
            }
            return 0;
        }

        return switch (suggestion.kind()) {
            case VALUE, LITERAL -> 2;
            case KEY -> 1;
            case STRUCTURAL, SNIPPET -> 0;
        };
    }

    private static boolean isComponentKeyValueSnippet(RawAutocompleteUtil.Suggestion suggestion) {
        if (suggestion.kind() != RawAutocompleteUtil.SuggestionKind.SNIPPET
                || suggestion.label() == null
                || !suggestion.label().contains(":")) {
            return false;
        }
        String insert = suggestion.insertText();
        return insert != null && insert.contains(suggestion.label()) && insert.contains(": ");
    }

    private static int recencyScore(
            String text,
            int replaceStart,
            RawAutocompleteUtil.Suggestion suggestion
    ) {
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
        if (distance <= 64) {
            return 4;
        }
        if (distance <= 160) {
            return 3;
        }
        if (distance <= 320) {
            return 2;
        }
        return 1;
    }

    private record ParseFilteredSuggestion(
            RawAutocompleteUtil.Suggestion suggestion,
            int parseRank,
            int distanceToTarget
    ) {
    }
}
