package me.noramibu.itemeditor.storage.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StorageSearchParser {

    private StorageSearchParser() {
    }

    public static StorageSearchQuery parse(String rawQuery) {
        StorageSearchQuery query = new StorageSearchQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }

        for (String token : tokenize(rawQuery)) {
            if (token.isBlank()) {
                continue;
            }
            String lowered = token.toLowerCase(Locale.ROOT);
            if (lowered.startsWith("item:")) {
                String value = cleanValue(token.substring(5));
                if (!value.isBlank()) {
                    query.itemTokens.add(value.toLowerCase(Locale.ROOT));
                }
                continue;
            }
            if (lowered.startsWith("name:")) {
                String value = cleanValue(token.substring(5));
                if (!value.isBlank()) {
                    query.nameTokens.add(value.toLowerCase(Locale.ROOT));
                }
                continue;
            }
            if (lowered.startsWith("lore:")) {
                String value = cleanValue(token.substring(5));
                if (!value.isBlank()) {
                    query.loreTokens.add(value.toLowerCase(Locale.ROOT));
                }
                continue;
            }
            if (lowered.startsWith("before:")) {
                Long duration = parseDurationMillis(token.substring(7));
                if (duration != null) {
                    query.beforeDurationsMs.add(duration);
                }
                continue;
            }
            if (lowered.startsWith("after:")) {
                Long duration = parseDurationMillis(token.substring(6));
                if (duration != null) {
                    query.afterDurationsMs.add(duration);
                }
                continue;
            }
            if (lowered.startsWith("amount:")) {
                StorageSearchQuery.NumericFilter filter = parseNumericFilter(token.substring(7));
                if (filter != null) {
                    query.amountFilters.add(filter);
                }
                continue;
            }
            if (lowered.startsWith("a:")) {
                StorageSearchQuery.NumericFilter filter = parseNumericFilter(token.substring(2));
                if (filter != null) {
                    query.amountFilters.add(filter);
                }
                continue;
            }
            if (lowered.startsWith("size:")) {
                StorageSearchQuery.NumericFilter filter = parseNumericFilter(token.substring(5));
                if (filter != null) {
                    query.nbtSizeFilters.add(filter);
                }
                continue;
            }
            if (lowered.startsWith("bytes:")) {
                StorageSearchQuery.NumericFilter filter = parseNumericFilter(token.substring(6));
                if (filter != null) {
                    query.nbtSizeFilters.add(filter);
                }
                continue;
            }

            String cleaned = cleanValue(token).toLowerCase(Locale.ROOT);
            if (looksLikeItemKey(cleaned)) {
                query.itemTokens.add(cleaned);
            } else if (!cleaned.isBlank()) {
                query.freeTokens.add(cleaned);
            }
        }
        return query;
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quote = '\0';

        for (int index = 0; index < query.length(); index++) {
            char value = query.charAt(index);
            if (inQuotes) {
                if (value == quote) {
                    inQuotes = false;
                    quote = '\0';
                } else {
                    current.append(value);
                }
                continue;
            }

            if (Character.isWhitespace(value)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (value == '"' || value == '\'') {
                inQuotes = true;
                quote = value;
                continue;
            }
            current.append(value);
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static Long parseDurationMillis(String raw) {
        String value = cleanValue(raw).trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2) {
            return null;
        }
        char unit = value.charAt(value.length() - 1);
        String numeric = value.substring(0, value.length() - 1).trim();
        long count;
        try {
            count = Long.parseLong(numeric);
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (count <= 0L) {
            return null;
        }
        return switch (unit) {
            case 'm' -> count * 60_000L;
            case 'h' -> count * 3_600_000L;
            case 'd' -> count * 86_400_000L;
            default -> null;
        };
    }

    private static StorageSearchQuery.NumericFilter parseNumericFilter(String raw) {
        String value = cleanValue(raw).trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return null;
        }

        if (value.startsWith(">=")) {
            Integer n = parsePositiveInt(value.substring(2));
            return n == null ? null : new StorageSearchQuery.NumericFilter(StorageSearchQuery.Mode.GTE, n, n);
        }
        if (value.startsWith("<=")) {
            Integer n = parsePositiveInt(value.substring(2));
            return n == null ? null : new StorageSearchQuery.NumericFilter(StorageSearchQuery.Mode.LTE, n, n);
        }
        if (value.startsWith(">")) {
            Integer n = parsePositiveInt(value.substring(1));
            return n == null ? null : new StorageSearchQuery.NumericFilter(StorageSearchQuery.Mode.GT, n, n);
        }
        if (value.startsWith("<")) {
            Integer n = parsePositiveInt(value.substring(1));
            return n == null ? null : new StorageSearchQuery.NumericFilter(StorageSearchQuery.Mode.LT, n, n);
        }

        int dash = value.indexOf('-');
        if (dash > 0 && dash < value.length() - 1) {
            Integer min = parsePositiveInt(value.substring(0, dash));
            Integer max = parsePositiveInt(value.substring(dash + 1));
            if (min == null || max == null) {
                return null;
            }
            int lower = Math.min(min, max);
            int upper = Math.max(min, max);
            return new StorageSearchQuery.NumericFilter(StorageSearchQuery.Mode.RANGE, lower, upper);
        }

        Integer exact = parsePositiveInt(value);
        return exact == null ? null : new StorageSearchQuery.NumericFilter(StorageSearchQuery.Mode.EQ, exact, exact);
    }

    private static Integer parsePositiveInt(String raw) {
        String value = cleanValue(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
        if (parsed <= 0) {
            return null;
        }
        return parsed;
    }

    private static String cleanValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            }
        }
        return cleaned;
    }

    private static boolean looksLikeItemKey(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return false;
        }
        return value.chars().allMatch(ch ->
                Character.isLowerCase(ch)
                        || Character.isDigit(ch)
                        || ch == ':'
                        || ch == '_'
                        || ch == '-'
                        || ch == '.'
                        || ch == '/'
                        || ch == '*'
        );
    }
}
