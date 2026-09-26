package me.noramibu.itemeditor.ui.component.raw;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

public final class RawBracketMatcher {
    private RawBracketMatcher() {}

    public static int[] pairs(String text, BooleanSupplier cancelled) {
        int[] pairs = new int[text.length()];
        Arrays.fill(pairs, -2);
        int[] stack = new int[128];
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            if ((i & 4095) == 0 && cancelled.getAsBoolean()) return new int[0];
            char ch = text.charAt(i);
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (ch == '\\') escaped = true;
                else if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '"' || ch == '\'') {
                quote = ch;
                continue;
            }
            int open = "{[(".indexOf(ch), close = "}])".indexOf(ch);
            if (open >= 0) {
                pairs[i] = -1;
                if (depth == stack.length) stack = Arrays.copyOf(stack, depth * 2);
                stack[depth++] = i;
            } else if (close >= 0) {
                pairs[i] = -1;
                if (depth > 0 && text.charAt(stack[depth - 1]) == "{[(".charAt(close)) {
                    int start = stack[--depth];
                    pairs[start] = i;
                    pairs[i] = start;
                } else depth = 0;
            }
        }
        return pairs;
    }

    public static int adjacent(int[] pairs, int caret) {
        if (caret >= 0 && caret < pairs.length && pairs[caret] != -2) return caret;
        return caret > 0 && caret <= pairs.length && pairs[caret - 1] != -2 ? caret - 1 : -1;
    }
}
