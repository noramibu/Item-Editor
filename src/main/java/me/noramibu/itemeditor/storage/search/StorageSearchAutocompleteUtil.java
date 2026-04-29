package me.noramibu.itemeditor.storage.search;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class StorageSearchAutocompleteUtil {

    private StorageSearchAutocompleteUtil() {
    }

    public static Completion complete(String rawQuery, String[] tokens) {
        String query = rawQuery == null ? "" : rawQuery;
        int splitIndex = query.lastIndexOf(' ') + 1;
        String base = query.substring(0, splitIndex);
        String prefix = query.substring(splitIndex);
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        String[] suggestions = Arrays.stream(tokens)
                .filter(token -> normalizedPrefix.isBlank() || token.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toArray(String[]::new);
        return new Completion(base, prefix, suggestions);
    }

    public record Completion(
            String base,
            String prefix,
            String[] suggestions
    ) {
        public List<String> withBase() {
            return Arrays.stream(this.suggestions)
                    .map(token -> this.base + token)
                    .toList();
        }
    }
}
