package me.noramibu.itemeditor.editor.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class RichTextLayoutUtil {

    private RichTextLayoutUtil() {
    }

    public static List<LineLayout> layout(RichTextDocument document, Font font, int maxWidth) {
        int safeWidth = Math.max(1, maxWidth);
        String text = document.plainText();
        if (text.isEmpty()) {
            return List.of(new LineLayout(0, 0, Component.empty(), new int[]{0}, new float[]{0f}));
        }

        List<CodepointEntry> entries = buildEntries(document, font);
        List<LineLayout> lines = new ArrayList<>();

        int entryIndex = 0;
        while (entryIndex < entries.size()) {
            int lineStart = entries.get(entryIndex).start();
            int lastBreakEntry = -1;
            float width = 0f;
            boolean hasWidth = false;
            boolean emittedLine = false;

            for (int cursor = entryIndex; cursor < entries.size(); cursor++) {
                CodepointEntry entry = entries.get(cursor);

                if (entry.codePoint() == '\n') {
                    lines.add(buildLine(document, entries, lineStart, entry.start()));
                    entryIndex = cursor + 1;
                    emittedLine = true;
                    break;
                }

                if (entry.codePoint() == ' ') {
                    lastBreakEntry = cursor;
                }

                if (hasWidth && width + entry.width() > safeWidth) {
                    if (lastBreakEntry >= entryIndex) {
                        lines.add(buildLine(document, entries, lineStart, entries.get(lastBreakEntry).start()));
                        entryIndex = lastBreakEntry + 1;
                    } else {
                        int lineEnd = entry.start() == lineStart ? entry.end() : entry.start();
                        lines.add(buildLine(document, entries, lineStart, lineEnd));
                        entryIndex = entry.start() == lineStart ? cursor + 1 : cursor;
                    }

                    emittedLine = true;
                    break;
                }

                width += entry.width();
                hasWidth |= entry.width() > 0f;
            }

            if (!emittedLine) {
                lines.add(buildLine(document, entries, lineStart, text.length()));
                entryIndex = entries.size();
            }
        }

        if (text.charAt(text.length() - 1) == '\n') {
            lines.add(new LineLayout(text.length(), text.length(), Component.empty(), new int[]{text.length()}, new float[]{0f}));
        }

        return lines;
    }

    private static List<CodepointEntry> buildEntries(RichTextDocument document, Font font) {
        List<CodepointEntry> entries = new ArrayList<>();
        int offset = 0;

        for (RichTextDocument.Segment segment : document.segments()) {
            String text = segment.text();
            for (int index = 0; index < text.length(); ) {
                int codePoint = text.codePointAt(index);
                int charCount = Character.charCount(codePoint);
                int start = offset + index;
                int end = start + charCount;
                entries.add(new CodepointEntry(start, end, codePoint, charWidth(font, segment.style(), codePoint)));
                index += charCount;
            }
            offset += text.length();
        }

        return entries;
    }

    private static float charWidth(Font font, RichTextStyle style, int codePoint) {
        if (codePoint == '\n') {
            return 0f;
        }
        return font.width(Component.literal(Character.toString(codePoint)).withStyle(style.toStyle()));
    }

    private static LineLayout buildLine(RichTextDocument document, List<CodepointEntry> entries, int start, int end) {
        List<CodepointEntry> lineEntries = new ArrayList<>();
        for (CodepointEntry entry : entries) {
            if (entry.start() < start) continue;
            if (entry.end() > end) break;
            if (entry.codePoint() != '\n') {
                lineEntries.add(entry);
            }
        }

        int[] positions = new int[lineEntries.size() + 1];
        float[] boundaries = new float[lineEntries.size() + 1];
        positions[0] = start;

        float width = 0f;
        for (int index = 0; index < lineEntries.size(); index++) {
            CodepointEntry entry = lineEntries.get(index);
            width += entry.width();
            positions[index + 1] = entry.end();
            boundaries[index + 1] = width;
        }

        return new LineLayout(start, end, document.sliceToComponent(start, end), positions, boundaries);
    }

    private record CodepointEntry(int start, int end, int codePoint, float width) {
    }

    public record LineLayout(int start, int end, Component component, int[] positions, float[] boundaries) {

        public float xForPosition(int position) {
            if (position <= this.start) {
                return 0f;
            }

            for (int index = 1; index < this.positions.length; index++) {
                if (position <= this.positions[index]) {
                    return this.boundaries[index];
                }
            }

            return this.boundaries[this.boundaries.length - 1];
        }

        public int positionForX(double x) {
            if (x <= 0) {
                return this.start;
            }

            for (int index = 1; index < this.boundaries.length; index++) {
                float left = this.boundaries[index - 1];
                float right = this.boundaries[index];
                if (x < (left + right) / 2f) {
                    return this.positions[index - 1];
                }
            }

            return this.end;
        }
    }
}
