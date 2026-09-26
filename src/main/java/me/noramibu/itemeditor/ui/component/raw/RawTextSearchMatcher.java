package me.noramibu.itemeditor.ui.component.raw;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RawTextSearchMatcher {
    private RawTextSearchMatcher() {}

    public record Result(int[] starts, int[] ends) {}

    public static Result searchRange(
            String text,
            String query,
            boolean matchCase,
            boolean wholeWord,
            boolean regex,
            boolean dotAll,
            BooleanSupplier cancelled,
            int start,
            int end) {
        Result result = search(text.substring(start, end), query, matchCase, wholeWord, regex, dotAll, cancelled);
        for (int i = 0; i < result.starts.length; i++) {
            result.starts[i] += start;
            result.ends[i] += start;
        }
        return result;
    }

    public static Result search(
            String text,
            String query,
            boolean matchCase,
            boolean wholeWord,
            boolean regex,
            boolean dotAll,
            BooleanSupplier cancelled) {
        if (!regex) {
            int[] starts = find(text, query, matchCase, wholeWord, cancelled);
            return new Result(
                    starts,
                    Arrays.stream(starts).map(start -> start + query.length()).toArray());
        }
        if (query.isEmpty()) return new Result(new int[0], new int[0]);
        var matcher = matcher(text, query, matchCase, dotAll, cancelled);
        int[] starts = new int[128], ends = new int[128];
        int count = 0;
        while (matcher.find()) {
            int start = matcher.start(), end = matcher.end();
            if (wholeWord
                    && ((start > 0 && word(text.codePointBefore(start)))
                            || (end < text.length() && word(text.codePointAt(end))))) continue;
            if (count == starts.length) {
                starts = Arrays.copyOf(starts, count * 2);
                ends = Arrays.copyOf(ends, count * 2);
            }
            starts[count] = start;
            ends[count++] = end;
        }
        return new Result(Arrays.copyOf(starts, count), Arrays.copyOf(ends, count));
    }

    static Matcher matcher(String text, String query, boolean matchCase, boolean dotAll, BooleanSupplier cancelled) {
        if (query.length() > 4096) throw new IllegalArgumentException("Pattern too long");
        int flags = Pattern.MULTILINE
                | (matchCase ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                | (dotAll ? Pattern.DOTALL : 0);
        return Pattern.compile(query, flags)
                .matcher(new BoundedText(text, cancelled, System.nanoTime() + 250_000_000L));
    }

    private record BoundedText(String text, BooleanSupplier cancelled, long deadline) implements CharSequence {
        private void check() {
            if (cancelled.getAsBoolean() || System.nanoTime() > deadline)
                throw new IllegalStateException("Search cancelled or timed out");
        }

        public int length() {
            check();
            return text.length();
        }

        public char charAt(int index) {
            check();
            return text.charAt(index);
        }

        public CharSequence subSequence(int start, int end) {
            check();
            return new BoundedText(text.substring(start, end), cancelled, deadline);
        }

        public String toString() {
            check();
            return text;
        }
    }

    public static int[] find(
            String text, String query, boolean matchCase, boolean wholeWord, BooleanSupplier cancelled) {
        if (query.isEmpty() || query.length() > text.length()) return new int[0];
        char[] pattern = query.toCharArray();
        for (int i = 0; i < pattern.length; i++) pattern[i] = fold(pattern[i], matchCase);
        int[] prefix = new int[pattern.length];
        for (int i = 1, j = 0; i < pattern.length; i++) {
            if ((i & 4095) == 0 && cancelled.getAsBoolean()) return new int[0];
            while (j > 0 && pattern[i] != pattern[j]) j = prefix[j - 1];
            if (pattern[i] == pattern[j]) j++;
            prefix[i] = j;
        }
        int[] offsets = new int[128];
        int count = 0;
        for (int i = 0, j = 0; i < text.length(); i++) {
            if ((i & 4095) == 0 && cancelled.getAsBoolean()) return new int[0];
            char ch = fold(text.charAt(i), matchCase);
            while (j > 0 && ch != pattern[j]) j = prefix[j - 1];
            if (ch == pattern[j]) j++;
            if (j == pattern.length) {
                int start = i + 1 - j;
                int end = i + 1;
                if (!wholeWord
                        || ((start == 0 || !word(text.codePointBefore(start)))
                                && (end == text.length() || !word(text.codePointAt(end))))) {
                    if (count == offsets.length) offsets = Arrays.copyOf(offsets, offsets.length * 2);
                    offsets[count++] = start;
                }
                j = prefix[j - 1];
            }
        }
        return Arrays.copyOf(offsets, count);
    }

    private static char fold(char ch, boolean matchCase) {
        return matchCase ? ch : Character.toLowerCase(Character.toUpperCase(ch));
    }

    private static boolean word(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isLetterOrDigit(codePoint)
                || codePoint == '_'
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK;
    }

    public static int[] highlightRanges(int[] offsets, int[] ends) {
        if (offsets.length == 0) return new int[0];
        int[] ranges = new int[Math.min(offsets.length, 128) * 2];
        int used = 0;
        int start = offsets[0], end = ends[0];
        for (int i = 1; i <= offsets.length; i++) {
            if (i < offsets.length && offsets[i] <= end) {
                end = Math.max(end, ends[i]);
                continue;
            }
            if (used + 2 > ranges.length) ranges = Arrays.copyOf(ranges, ranges.length * 2);
            ranges[used++] = start;
            ranges[used++] = end;
            if (i < offsets.length) {
                start = offsets[i];
                end = ends[i];
            }
        }
        return Arrays.copyOf(ranges, used);
    }

    public static int lowerBound(int[] values, int target) {
        int low = 0, high = values.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (values[middle] < target) low = middle + 1;
            else high = middle;
        }
        return low;
    }
}
