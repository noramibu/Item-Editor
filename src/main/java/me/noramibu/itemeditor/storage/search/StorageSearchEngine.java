package me.noramibu.itemeditor.storage.search;

import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
                matches.add(copy(entry));
            }
        }
        matches.sort(bySortMode(sortMode, reverseSort));
        return matches;
    }

    public static Comparator<SavedIndexItemEntry> bySortMode(StorageSortMode mode) {
        return bySortMode(mode, false);
    }

    public static Comparator<SavedIndexItemEntry> bySortMode(StorageSortMode mode, boolean reverseSort) {
        Comparator<SavedIndexItemEntry> comparator;
        if (mode == StorageSortMode.REGULAR) {
            comparator = Comparator
                    .comparingInt((SavedIndexItemEntry entry) -> Math.max(1, entry.page))
                    .thenComparingInt(entry -> Math.max(0, entry.slotInPage));
        } else if (mode == StorageSortMode.NAME_ASC) {
            comparator = Comparator
                    .comparing((SavedIndexItemEntry entry) -> safeLower(entry.customNamePlain))
                    .thenComparingLong(entry -> -entry.savedAt);
        } else if (mode == StorageSortMode.AMOUNT_DESC) {
            comparator = Comparator
                    .comparingInt((SavedIndexItemEntry entry) -> -normalizedStackCount(entry))
                    .thenComparingLong(entry -> -entry.savedAt)
                    .thenComparing(entry -> safeLower(entry.customNamePlain));
        } else if (mode == StorageSortMode.NBT_SIZE_DESC) {
            comparator = Comparator
                    .comparingInt((SavedIndexItemEntry entry) -> -normalizedNbtBytes(entry))
                    .thenComparingLong(entry -> -entry.savedAt)
                    .thenComparing(entry -> safeLower(entry.customNamePlain));
        } else {
            comparator = Comparator
                    .comparingLong((SavedIndexItemEntry entry) -> -entry.savedAt)
                    .thenComparing(entry -> safeLower(entry.customNamePlain));
        }
        return reverseSort && mode != StorageSortMode.REGULAR ? comparator.reversed() : comparator;
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
        int stackCount = normalizedStackCount(entry);
        for (StorageSearchQuery.NumericFilter amountFilter : query.amountFilters) {
            if (amountFilter == null || !amountFilter.matches(stackCount)) {
                return false;
            }
        }
        int nbtBytes = normalizedNbtBytes(entry);
        for (StorageSearchQuery.NumericFilter sizeFilter : query.nbtSizeFilters) {
            if (sizeFilter == null || !sizeFilter.matches(nbtBytes)) {
                return false;
            }
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
        if (token.indexOf('*') >= 0) {
            return wildcardMatch(itemKey, token);
        }
        return itemKey.equals(token) || itemKey.startsWith(token);
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

    private static SavedIndexItemEntry copy(SavedIndexItemEntry source) {
        SavedIndexItemEntry copy = new SavedIndexItemEntry();
        copy.id = source.id;
        copy.chunkId = source.chunkId;
        copy.slotInChunk = source.slotInChunk;
        copy.page = source.page;
        copy.slotInPage = source.slotInPage;
        copy.savedAt = source.savedAt;
        copy.updatedAt = source.updatedAt;
        copy.itemRegistryKey = source.itemRegistryKey;
        copy.stackCount = Math.max(1, source.stackCount);
        copy.nbtBytes = Math.max(0, source.nbtBytes);
        copy.customNamePlain = source.customNamePlain;
        copy.lorePlain = source.lorePlain == null ? new ArrayList<>() : new ArrayList<>(source.lorePlain);
        return copy;
    }

    private static int normalizedStackCount(SavedIndexItemEntry entry) {
        return Math.max(1, entry == null ? 1 : entry.stackCount);
    }

    private static int normalizedNbtBytes(SavedIndexItemEntry entry) {
        return Math.max(1, entry == null ? 1 : entry.nbtBytes);
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
