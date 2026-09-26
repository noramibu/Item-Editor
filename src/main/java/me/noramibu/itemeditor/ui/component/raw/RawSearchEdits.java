package me.noramibu.itemeditor.ui.component.raw;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

public final class RawSearchEdits {
    private RawSearchEdits() {}

    public record Replacement(String text, int count, int nextOffset) {}

    public static Replacement replaceRegex(
            String text,
            String query,
            int[] starts,
            int[] ends,
            String replacement,
            boolean matchCase,
            boolean dotAll,
            BooleanSupplier cancelled) {
        return replaceRegex(text, query, starts, ends, replacement, matchCase, dotAll, cancelled, 0, text.length());
    }

    public static Replacement replaceRegex(
            String text,
            String query,
            int[] starts,
            int[] ends,
            String replacement,
            boolean matchCase,
            boolean dotAll,
            BooleanSupplier cancelled,
            int regionStart,
            int regionEnd) {
        var matcher = RawTextSearchMatcher.matcher(text, query, matchCase, dotAll, cancelled)
                .region(regionStart, regionEnd);
        StringBuilder result = new StringBuilder();
        int count = 0, nextOffset = -1;
        while (count < starts.length && matcher.find()) {
            if (matcher.start() != starts[count] || matcher.end() != ends[count]) continue;
            matcher.appendReplacement(result, replacement);
            if (result.length() > 16_777_216) throw new IllegalArgumentException("Replacement too large");
            nextOffset = result.length();
            if (matcher.start() == matcher.end()) nextOffset++;
            count++;
        }
        if (count != starts.length) throw new IllegalStateException("Search results changed");
        matcher.appendTail(result);
        if (result.length() > 16_777_216) throw new IllegalArgumentException("Replacement too large");
        return new Replacement(result.toString(), count, nextOffset);
    }

    public static int replacementCount(int[] starts, int[] ends) {
        int cursor = 0, count = 0;
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] < cursor) continue;
            cursor = ends[i];
            count++;
        }
        return count;
    }

    public static Replacement replaceAll(
            String text, int[] starts, int[] ends, String replacement, BooleanSupplier cancelled) {
        StringBuilder result = new StringBuilder();
        int cursor = 0, count = 0;
        for (int i = 0; i < starts.length; i++) {
            if (cancelled.getAsBoolean()) throw new IllegalStateException("Cancelled");
            if (starts[i] < cursor) continue;
            if ((long) result.length() + starts[i] - cursor + replacement.length() > 16_777_216)
                throw new IllegalArgumentException("Replacement too large");
            result.append(text, cursor, starts[i]).append(replacement);
            cursor = ends[i];
            count++;
        }
        if ((long) result.length() + text.length() - cursor > 16_777_216)
            throw new IllegalArgumentException("Replacement too large");
        return new Replacement(result.append(text, cursor, text.length()).toString(), count, -1);
    }

    public static int[] lineStarts(String text) {
        int[] lines = new int[128];
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\n') continue;
            if (count == lines.length) lines = Arrays.copyOf(lines, count * 2);
            lines[count++] = i + 1;
        }
        return Arrays.copyOf(lines, count);
    }

    public static int position(String text, int line, int column) {
        int[] starts = lineStarts(text);
        if (line < 1 || line > starts.length || column < 1) return -1;
        int start = starts[line - 1];
        int end = line == starts.length ? text.length() : starts[line] - 1;
        if (end > start && text.charAt(end - 1) == '\r') end--;
        return column - 1 > end - start ? -1 : start + column - 1;
    }
}
