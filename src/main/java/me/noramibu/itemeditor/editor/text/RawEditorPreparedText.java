package me.noramibu.itemeditor.editor.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record RawEditorPreparedText(String text, int[] lineStarts, List<Fold> folds) {

    public RawEditorPreparedText {
        text = text == null ? "" : text;
        lineStarts = lineStarts == null || lineStarts.length == 0 ? new int[]{0} : lineStarts;
        folds = folds == null ? List.of() : List.copyOf(folds);
    }

    public static RawEditorPreparedText prepare(String value) {
        String text = value == null ? "" : value;
        int[] lineStarts = new int[Math.max(16, Math.min(4096, text.length() / 64 + 1))];
        lineStarts[0] = 0;
        int lineCount = 1;
        int line = 0;
        List<Fold> folds = new ArrayList<>();
        ArrayDeque<OpenSymbol> openSymbols = new ArrayDeque<>();
        boolean inString = false;
        boolean escaping = false;

        for (int index = 0; index < text.length(); index++) {
            char valueAt = text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (valueAt == '\\') {
                    escaping = true;
                } else if (valueAt == '"') {
                    inString = false;
                }
            } else if (valueAt == '"') {
                inString = true;
            } else if (valueAt == '{' || valueAt == '[') {
                openSymbols.addLast(new OpenSymbol(valueAt, line));
            } else if (valueAt == '}' || valueAt == ']') {
                char expectedOpen = valueAt == '}' ? '{' : '[';
                OpenSymbol matched = null;
                while (!openSymbols.isEmpty()) {
                    OpenSymbol candidate = openSymbols.removeLast();
                    if (candidate.type() == expectedOpen) {
                        matched = candidate;
                        break;
                    }
                }
                if (matched != null && line > matched.line()) {
                    folds.add(new Fold(matched.line(), line, expectedOpen));
                }
            }

            if (valueAt == '\n') {
                line++;
                if (lineCount == lineStarts.length) {
                    lineStarts = Arrays.copyOf(lineStarts, lineStarts.length * 2);
                }
                lineStarts[lineCount++] = index + 1;
            }
        }

        folds.sort((left, right) -> left.startLine() != right.startLine()
                ? Integer.compare(left.startLine(), right.startLine())
                : Integer.compare(right.endLine(), left.endLine()));
        return new RawEditorPreparedText(text, Arrays.copyOf(lineStarts, lineCount), folds);
    }

    public record Fold(int startLine, int endLine, char type) {
    }

    private record OpenSymbol(char type, int line) {
    }
}
