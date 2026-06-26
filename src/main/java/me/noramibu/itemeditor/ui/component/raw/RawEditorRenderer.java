package me.noramibu.itemeditor.ui.component.raw;

import java.util.ArrayList;
import java.util.List;

public final class RawEditorRenderer {
    public List<RenderRect> selectionRectangles(
            RawEditorLayout layout,
            RawTextDocument.Selection selection,
            RawEditorTextMeasurer measurer,
            int contentLeft,
            int contentWidth,
            int top,
            int visibleHeight,
            int renderedScroll,
            int horizontalOffset,
            int lineHeight,
            int textLineHeight
    ) {
        if (selection.empty()) {
            return List.of();
        }
        List<RenderRect> rectangles = new ArrayList<>();
        for (RawEditorLayout.VisualRow row : layout.rows()) {
            if (selection.end() < row.documentStart() || selection.start() > row.documentEnd()) {
                continue;
            }
            int lineY = top + row.visualIndex() * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) {
                continue;
            }
            int from = Math.max(selection.start(), row.documentStart());
            int to = Math.min(selection.end(), row.documentEnd());
            if (from >= to) {
                continue;
            }
            int startX = contentLeft - horizontalOffset + layout.localVisualX(row.visualIndex(), from, measurer);
            int endX = contentLeft - horizontalOffset + layout.localVisualX(row.visualIndex(), to, measurer);
            if (selection.end() > row.documentEnd()) {
                int contentRight = contentLeft + contentWidth;
                endX = Math.clamp(endX + 2, Math.min(startX + 1, contentRight), contentRight);
            }
            startX = Math.max(contentLeft, startX);
            endX = Math.min(contentLeft + contentWidth, endX);
            if (endX <= startX) {
                endX = startX + 1;
            }
            rectangles.add(new RenderRect(startX, lineY - 1, endX, lineY + textLineHeight + 1));
        }
        return rectangles;
    }

    public List<RenderRect> errorUnderlineRectangles(
            RawEditorLayout layout,
            RawTextDocument document,
            RawEditorTextMeasurer measurer,
            int lineIndex,
            int columnOneBased,
            int lengthChars,
            int contentLeft,
            int contentWidth,
            int top,
            int visibleHeight,
            int renderedScroll,
            int horizontalOffset,
            int lineHeight,
            int textLineHeight,
            int underlineHeight
    ) {
        if (lineIndex < 0 || lineIndex >= document.lineCount() || layout.hiddenLine(lineIndex)) {
            return List.of();
        }
        String line = layout.lineText(lineIndex);
        int errorStart = Math.clamp(columnOneBased > 0 ? columnOneBased - 1 : 0, 0, line.length());
        int errorEnd = Math.clamp(errorStart + Math.max(1, lengthChars), errorStart, line.length());
        boolean markLineEnd = errorStart >= line.length();
        List<RenderRect> rectangles = new ArrayList<>();

        for (RawEditorLayout.VisualRow row : layout.rows()) {
            if (row.lineIndex() != lineIndex) {
                continue;
            }
            int lineY = top + row.visualIndex() * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) {
                continue;
            }
            int from = Math.max(errorStart, row.localStart());
            int to = Math.min(errorEnd, row.localEnd());
            int underlineY = lineY + textLineHeight + 1;
            if (from < to) {
                int startOffset = document.lineStart(lineIndex) + from;
                int endOffset = document.lineStart(lineIndex) + to;
                int startX = contentLeft - horizontalOffset + layout.localVisualX(row.visualIndex(), startOffset, measurer);
                int endX = contentLeft - horizontalOffset + layout.localVisualX(row.visualIndex(), endOffset, measurer);
                startX = Math.max(contentLeft, startX);
                endX = Math.min(contentLeft + contentWidth, endX);
                if (endX <= startX) {
                    endX = Math.min(contentLeft + contentWidth, startX + Math.max(2, underlineHeight * 2));
                }
                rectangles.add(new RenderRect(startX, underlineY, endX, underlineY + underlineHeight));
                continue;
            }

            if (markLineEnd && row.localEnd() == line.length()) {
                int caretOffset = document.lineStart(lineIndex) + row.localEnd();
                int caretX = contentLeft - horizontalOffset + layout.localVisualX(row.visualIndex(), caretOffset, measurer);
                int startX = Math.clamp(caretX, contentLeft, contentLeft + contentWidth);
                int endX = Math.min(contentLeft + contentWidth, startX + Math.max(2, underlineHeight * 2));
                rectangles.add(new RenderRect(startX, underlineY, endX, underlineY + underlineHeight));
            }
        }
        return rectangles;
    }

    public record RenderRect(int left, int top, int right, int bottom) {
    }
}
