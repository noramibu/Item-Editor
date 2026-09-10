package me.noramibu.itemeditor.ui.screen;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import me.noramibu.itemeditor.ui.util.UiColors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class ItemEditorValueDiff {
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}_]+|[^\\p{L}\\p{N}_]");
    private static final int MAX_COMPARISONS = 1_000_000;

    private ItemEditorValueDiff() {}

    static Result between(String before, String after, int previewLength) {
        List<String> left = tokens(before);
        List<String> right = tokens(after);
        boolean[] removed = new boolean[left.size()];
        boolean[] added = new boolean[right.size()];
        Arrays.fill(removed, true);
        Arrays.fill(added, true);
        int start = 0;
        int leftEnd = left.size();
        int rightEnd = right.size();
        while (start < leftEnd && start < rightEnd && left.get(start).equals(right.get(start))) {
            removed[start] = added[start] = false;
            start++;
        }
        while (leftEnd > start && rightEnd > start && left.get(leftEnd - 1).equals(right.get(rightEnd - 1))) {
            removed[--leftEnd] = added[--rightEnd] = false;
        }

        int rows = leftEnd - start;
        int columns = rightEnd - start;
        if (rows > 0 && columns > 0 && (long) (rows + 1) * (columns + 1) <= MAX_COMPARISONS) {
            int[][] lengths = new int[rows + 1][columns + 1];
            for (int i = rows - 1; i >= 0; i--) {
                for (int j = columns - 1; j >= 0; j--) {
                    lengths[i][j] = left.get(start + i).equals(right.get(start + j))
                            ? lengths[i + 1][j + 1] + 1
                            : Math.max(lengths[i + 1][j], lengths[i][j + 1]);
                }
            }
            int i = 0;
            int j = 0;
            while (i < rows && j < columns) {
                if (left.get(start + i).equals(right.get(start + j))) {
                    removed[start + i++] = added[start + j++] = false;
                } else if (lengths[i + 1][j] >= lengths[i][j + 1]) {
                    i++;
                } else {
                    j++;
                }
            }
        }
        return new Result(
                render(left, removed, UiColors.DANGER, before.length(), previewLength),
                render(right, added, UiColors.SUCCESS, after.length(), previewLength));
    }

    private static List<String> tokens(String value) {
        return TOKEN.matcher(value).results().map(match -> match.group()).toList();
    }

    private static Component render(List<String> tokens, boolean[] changed, int color, int length, int limit) {
        MutableComponent result = Component.empty();
        int remaining = length > limit ? Math.max(0, limit - 3) : length;
        for (int i = 0; i < tokens.size() && remaining > 0; ) {
            boolean highlight = changed[i];
            StringBuilder run = new StringBuilder();
            do {
                String token = tokens.get(i++);
                int end = Math.min(token.length(), remaining);
                if (end < token.length() && end > 0 && Character.isHighSurrogate(token.charAt(end - 1))) {
                    end--;
                }
                run.append(token, 0, end);
                remaining = end < token.length() ? 0 : remaining - end;
            } while (i < tokens.size() && remaining > 0 && changed[i] == highlight);
            MutableComponent part = Component.literal(run.toString());
            result.append(highlight ? part.withColor(color) : part);
        }
        if (length > limit) {
            result.append("...");
        }
        return result;
    }

    record Result(Component before, Component after) {}
}
