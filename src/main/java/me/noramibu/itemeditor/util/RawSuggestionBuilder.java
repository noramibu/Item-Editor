package me.noramibu.itemeditor.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

final class RawSuggestionBuilder {

    private static final int MAX_NEAREST_CANDIDATES = 4;
    private static final int MAX_FUZZY_DISTANCE = 3;
    static final Comparator<RawAutocompleteUtil.Suggestion> QUALITY_COMPARATOR = Comparator
            .comparing((RawAutocompleteUtil.Suggestion suggestion) -> suggestion.kind().priority())
            .thenComparingInt(suggestion -> suggestion.source().rank())
            .thenComparingInt(suggestion -> -suggestion.confidence())
            .thenComparingInt(RawAutocompleteUtil.Suggestion::contextRank)
            .thenComparingInt(RawAutocompleteUtil.Suggestion::matchRank)
            .thenComparingInt(suggestion -> suggestion.insertText().length())
            .thenComparing(RawAutocompleteUtil.Suggestion::insertText);

    private final Map<String, RawAutocompleteUtil.Suggestion> suggestions = new HashMap<>();

    Collection<RawAutocompleteUtil.Suggestion> values() {
        return this.suggestions.values();
    }

    void add(RawAutocompleteUtil.Suggestion suggestion) {
        String dedupeKey = suggestion.kind().name() + "|" + suggestion.insertText();
        RawAutocompleteUtil.Suggestion existing = this.suggestions.get(dedupeKey);
        if (existing == null || QUALITY_COMPARATOR.compare(suggestion, existing) < 0) {
            this.suggestions.put(dedupeKey, suggestion);
        }
    }

    void addSuggestions(
            List<String> values,
            String prefix,
            RawAutocompleteUtil.SuggestionKind kind,
            UnaryOperator<String> insertMapper,
            int contextRank
    ) {
        addSuggestions(
                values,
                prefix,
                kind,
                insertMapper,
                contextRank,
                null,
                ""
        );
    }

    void addSuggestions(
            List<String> values,
            String prefix,
            RawAutocompleteUtil.SuggestionKind kind,
            UnaryOperator<String> insertMapper,
            int contextRank,
            RawAutocompleteUtil.SuggestionSource source,
            String reason
    ) {
        if (values == null || values.isEmpty()) {
            return;
        }

        boolean blankPrefix = prefix.isBlank();
        if (blankPrefix && kind == RawAutocompleteUtil.SuggestionKind.VALUE && values.size() > 96) {
            return;
        }

        if (blankPrefix && values.size() > 256) {
            int max = 64;
            for (int index = 0; index < max; index++) {
                String value = values.get(index);
                String insertText = insertMapper.apply(value);
                addSuggestion(value, insertText, kind, 3, contextRank, source, reason);
            }
            return;
        }

        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        boolean strictPrefix = values.size() > 2500 && lowerPrefix.length() < 2;
        int accepted = 0;
        for (String value : values) {
            int rank = matchRank(value, prefix);
            if (strictPrefix && rank < 0) {
                continue;
            }

            if (rank < 0) {
                continue;
            }

            String insertText = insertMapper.apply(value);
            addSuggestion(value, insertText, kind, rank, contextRank, source, reason);

            accepted++;
            if (values.size() > 2500 && accepted >= 96) {
                break;
            }
        }
    }

    void addNearestSuggestions(
            List<String> values,
            String prefix,
            RawAutocompleteUtil.SuggestionKind kind,
            UnaryOperator<String> insertMapper,
            int contextRank
    ) {
        addNearestSuggestions(
                values,
                prefix,
                kind,
                insertMapper,
                contextRank,
                null,
                ""
        );
    }

    void addNearestSuggestions(
            List<String> values,
            String prefix,
            RawAutocompleteUtil.SuggestionKind kind,
            UnaryOperator<String> insertMapper,
            int contextRank,
            RawAutocompleteUtil.SuggestionSource source,
            String reason
    ) {
        if (values == null || values.isEmpty() || prefix.isBlank()) {
            return;
        }

        String normalizedPrefix = normalizeFuzzy(prefix);
        if (normalizedPrefix.length() < 3 || normalizedPrefix.length() > 12 || values.size() > 4000) {
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
            if (distance >= 0) {
                addNearestCandidate(nearest, new FuzzyCandidate(value, distance));
            }
        }

        for (FuzzyCandidate candidate : nearest) {
            String insertText = insertMapper.apply(candidate.value());
            int rank = 3 + candidate.distance();
            addSuggestion(
                    candidate.value(),
                    insertText,
                    kind,
                    rank,
                    contextRank,
                    source,
                    reason);
        }
    }

    void addMappedSuggestion(String label, String insertText, String prefix) {
        int labelRank = matchRank(label, prefix);
        int insertRank = matchRank(insertText, prefix);
        int rank = labelRank < 0 ? insertRank : insertRank < 0 ? labelRank : Math.min(labelRank, insertRank);
        if (rank < 0) {
            return;
        }
        add(new RawAutocompleteUtil.Suggestion(
                label,
                insertText,
                RawAutocompleteUtil.SuggestionKind.LITERAL,
                rank,
                0,
                RawAutocompleteUtil.SuggestionSource.LITERAL,
                "literal alias"
        ));
    }

    private void addSuggestion(
            String label,
            String insertText,
            RawAutocompleteUtil.SuggestionKind kind,
            int matchRank,
            int contextRank,
            RawAutocompleteUtil.SuggestionSource source,
            String reason
    ) {
        add(new RawAutocompleteUtil.Suggestion(
                label,
                insertText,
                kind,
                matchRank,
                contextRank,
                source,
                reason));
    }

    void suppressAlreadyPresentContainerKeys(List<String> seenKeysForContainer) {
        if (this.suggestions.isEmpty() || seenKeysForContainer == null || seenKeysForContainer.isEmpty()) {
            return;
        }

        Set<String> seenNormalized = new HashSet<>();
        for (String key : seenKeysForContainer) {
            if (key != null && !key.isBlank()) {
                seenNormalized.add(key.toLowerCase(Locale.ROOT));
            }
        }
        if (seenNormalized.isEmpty()) {
            return;
        }

        this.suggestions.entrySet().removeIf(entry -> {
            RawAutocompleteUtil.Suggestion suggestion = entry.getValue();
            if (suggestion.kind() != RawAutocompleteUtil.SuggestionKind.KEY
                    && suggestion.kind() != RawAutocompleteUtil.SuggestionKind.SNIPPET) {
                return false;
            }
            String normalized = normalizeSuggestedKey(suggestion.insertText());
            return !normalized.isBlank() && seenNormalized.contains(normalized);
        });
    }

    void suppressEchoTypedKeySuggestion(String typedPrefix) {
        if (this.suggestions.isEmpty() || typedPrefix == null || typedPrefix.isBlank()) {
            return;
        }

        String normalizedTyped = normalizeSuggestedKey(typedPrefix);
        if (normalizedTyped.isBlank()) {
            return;
        }

        boolean hasLongerPrefixCandidate = this.suggestions.values().stream()
                .filter(suggestion -> suggestion.kind() == RawAutocompleteUtil.SuggestionKind.KEY)
                .map(RawAutocompleteUtil.Suggestion::insertText)
                .map(RawSuggestionBuilder::normalizeSuggestedKey)
                .anyMatch(normalized -> normalized.startsWith(normalizedTyped)
                        && normalized.length() > normalizedTyped.length());
        if (!hasLongerPrefixCandidate) {
            return;
        }

        this.suggestions.entrySet().removeIf(entry -> {
            RawAutocompleteUtil.Suggestion suggestion = entry.getValue();
            if (suggestion.kind() != RawAutocompleteUtil.SuggestionKind.KEY) {
                return false;
            }
            String normalizedInsert = normalizeSuggestedKey(suggestion.insertText());
            String normalizedLabel = normalizeSuggestedKey(suggestion.label());
            return normalizedInsert.equals(normalizedTyped) || normalizedLabel.equals(normalizedTyped);
        });
    }

    void suppressDuplicateLabels() {
        if (this.suggestions.size() < 2) {
            return;
        }

        Map<String, RawAutocompleteUtil.Suggestion> bestByLabel = new HashMap<>();
        for (RawAutocompleteUtil.Suggestion suggestion : this.suggestions.values()) {
            String label = normalizeSuggestedKey(suggestion.label());
            if (label.isBlank()) {
                continue;
            }

            RawAutocompleteUtil.Suggestion existing = bestByLabel.get(label);
            if (existing == null || QUALITY_COMPARATOR.compare(suggestion, existing) < 0) {
                bestByLabel.put(label, suggestion);
            }
        }

        this.suggestions.entrySet().removeIf(entry -> {
            String label = normalizeSuggestedKey(entry.getValue().label());
            return !label.isBlank() && bestByLabel.get(label) != entry.getValue();
        });
    }

    static int matchRank(String value, String prefix) {
        if (prefix.isBlank()) {
            return 3;
        }

        String lowerValue = value.toLowerCase(Locale.ROOT);
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowerPrefix.startsWith("#")) {
            lowerPrefix = lowerPrefix.substring(1);
        }
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

    static String normalizeFuzzy(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder normalized = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toLowerCase(value.charAt(index));
            if (Character.isLetterOrDigit(current)
                    || current == '_'
                    || current == '-'
                    || current == ':'
                    || current == '.'
                    || current == '/') {
                normalized.append(current);
            }
        }
        return normalized.toString();
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

    private static String normalizeSuggestedKey(String insertText) {
        if (insertText == null || insertText.isBlank()) {
            return "";
        }
        String value = keyPart(insertText.trim());
        if (value.endsWith(":")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            value = value.substring(1, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String keyPart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.charAt(0) == '"') {
            int quoteEnd = quotedEnd(value);
            if (quoteEnd > 0) {
                int next = quoteEnd + 1;
                while (next < value.length() && Character.isWhitespace(value.charAt(next))) {
                    next++;
                }
                if (next < value.length() && value.charAt(next) == ':') {
                    return value.substring(0, quoteEnd + 1);
                }
            }
            return value;
        }

        int colon = keyValueColon(value);
        return colon < 0 ? value : value.substring(0, colon);
    }

    private static int keyValueColon(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != ':') {
                continue;
            }
            int next = index + 1;
            if (next >= value.length()) {
                return index;
            }
            char after = value.charAt(next);
            if (Character.isWhitespace(after)
                    || after == '"'
                    || after == '\''
                    || after == '{'
                    || after == '['
                    || after == '-'
                    || Character.isDigit(after)) {
                return index;
            }
        }
        return -1;
    }

    private static int quotedEnd(String value) {
        boolean escaping = false;
        for (int index = 1; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaping) {
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                return index;
            }
        }
        return -1;
    }

    private record FuzzyCandidate(String value, int distance) {
    }
}
