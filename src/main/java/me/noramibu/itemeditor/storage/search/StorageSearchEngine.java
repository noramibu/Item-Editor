package me.noramibu.itemeditor.storage.search;

import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.storage.model.SavedIndexEntryUtil;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

public final class StorageSearchEngine {

    private StorageSearchEngine() {
    }

    public static List<SavedIndexItemEntry> filterAndSort(
            List<SavedIndexItemEntry> source,
            StorageSearchQuery query,
            StorageSortMode sortMode,
            boolean reverseSort,
            long now
    ) {
        List<SavedIndexItemEntry> matches = new ArrayList<>();
        for (SavedIndexItemEntry entry : source) {
            if (entry == null) {
                continue;
            }
            if (matchesQuery(entry, query, now)) {
                matches.add(SavedIndexEntryUtil.copy(entry));
            }
        }
        matches.sort(bySortMode(sortMode, reverseSort));
        return matches;
    }

    public static Comparator<SavedIndexItemEntry> bySortMode(StorageSortMode mode, boolean reverseSort) {
        StorageSortMode sortMode = mode == null ? StorageSortMode.SAVED_AT_DESC : mode;
        Comparator<SavedIndexItemEntry> comparator = switch (sortMode) {
            case REGULAR -> Comparator
                    .comparingInt((SavedIndexItemEntry entry) -> Math.max(1, entry.page))
                    .thenComparingInt(entry -> Math.max(0, entry.slotInPage));
            case NAME_ASC -> Comparator
                    .comparing((SavedIndexItemEntry entry) -> safeLower(entry.customNamePlain))
                    .thenComparingLong(entry -> -entry.savedAt);
            case AMOUNT_DESC -> byDescendingNumber(entry -> Math.max(1, entry == null ? 1 : entry.stackCount));
            case NBT_SIZE_DESC -> byDescendingNumber(entry -> Math.max(1, entry == null ? 1 : entry.nbtBytes));
            case SAVED_AT_DESC -> Comparator
                    .comparingLong((SavedIndexItemEntry entry) -> -entry.savedAt)
                    .thenComparing(entry -> safeLower(entry.customNamePlain));
        };
        return reverseSort && sortMode != StorageSortMode.REGULAR ? comparator.reversed() : comparator;
    }

    public static boolean matchesQuery(SavedIndexItemEntry entry, StorageSearchQuery query, long now) {
        String itemKey = safeLower(entry.itemRegistryKey);
        String name = safeLower(entry.customNamePlain);
        String loreJoined = safeLower(String.join("\n", entry.lorePlain));

        for (String itemToken : query.itemTokens) {
            if (!matchesItemToken(itemKey, itemToken)) {
                return false;
            }
        }
        for (String nameToken : query.nameTokens) {
            if (!name.contains(nameToken)) {
                return false;
            }
        }
        for (String loreToken : query.loreTokens) {
            if (!loreJoined.contains(loreToken)) {
                return false;
            }
        }
        for (long duration : query.beforeDurationsMs) {
            if (entry.savedAt < now - duration) {
                return false;
            }
        }
        for (long duration : query.afterDurationsMs) {
            if (entry.savedAt >= now - duration) {
                return false;
            }
        }
        int stackCount = Math.max(1, entry.stackCount);
        for (StorageSearchQuery.NumericFilter amountFilter : query.amountFilters) {
            if (amountFilter == null || amountFilter.matches(stackCount)) {
                continue;
            }
            return false;
        }
        int nbtBytes = Math.max(1, entry.nbtBytes);
        for (StorageSearchQuery.NumericFilter sizeFilter : query.nbtSizeFilters) {
            if (sizeFilter == null || sizeFilter.matches(nbtBytes)) {
                continue;
            }
            return false;
        }
        for (String free : query.freeTokens) {
            boolean matched = matchesItemToken(itemKey, free)
                    || name.contains(free)
                    || loreJoined.contains(free);
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    public static boolean matchesItemToken(String itemKey, String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        String itemPath = itemPath(itemKey);
        if (token.indexOf('*') >= 0) {
            return wildcardMatch(itemKey, token) || wildcardMatch(itemPath, token);
        }
        return itemKey.equals(token)
                || itemKey.startsWith(token)
                || itemPath.equals(token)
                || itemPath.startsWith(token);
    }

    private static String itemPath(String itemKey) {
        if (itemKey == null) {
            return "";
        }
        int separator = itemKey.indexOf(':');
        return separator < 0 || separator == itemKey.length() - 1
                ? itemKey
                : itemKey.substring(separator + 1);
    }

    private static boolean wildcardMatch(String value, String pattern) {
        int valueIndex = 0;
        int patternIndex = 0;
        int starIndex = -1;
        int resumeValueIndex = -1;

        while (valueIndex < value.length()) {
            if (patternIndex < pattern.length()
                    && pattern.charAt(patternIndex) == value.charAt(valueIndex)) {
                valueIndex++;
                patternIndex++;
                continue;
            }
            if (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                starIndex = patternIndex++;
                resumeValueIndex = valueIndex;
                continue;
            }
            if (starIndex >= 0) {
                patternIndex = starIndex + 1;
                valueIndex = ++resumeValueIndex;
                continue;
            }
            return false;
        }

        while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
            patternIndex++;
        }
        return patternIndex == pattern.length();
    }

    private static Comparator<SavedIndexItemEntry> byDescendingNumber(ToIntFunction<SavedIndexItemEntry> value) {
        return Comparator
                .comparingInt((SavedIndexItemEntry entry) -> -value.applyAsInt(entry))
                .thenComparingLong(entry -> -entry.savedAt)
                .thenComparing(entry -> safeLower(entry.customNamePlain));
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
