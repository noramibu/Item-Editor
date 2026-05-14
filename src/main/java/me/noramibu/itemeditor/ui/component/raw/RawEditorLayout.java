package me.noramibu.itemeditor.ui.component.raw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawEditorLayout {
    private static final RawEditorLayout EMPTY = new RawEditorLayout(
            "",
            new int[]{0},
            List.of(new VisualRow(0, 0, 0, 0, 0, 0, 0, 0, false)),
            new int[]{0},
            new int[]{0},
            new boolean[]{false},
            1,
            0,
            false
    );

    private final String text;
    private final int[] lineStarts;
    private final List<VisualRow> rows;
    private final int[] firstRowByLine;
    private final int[] lastRowByLine;
    private final boolean[] hiddenLines;
    private final int contentHeight;
    private final int maxVisibleLineWidth;
    private final boolean wordWrap;

    private RawEditorLayout(
            String text,
            int[] lineStarts,
            List<VisualRow> rows,
            int[] firstRowByLine,
            int[] lastRowByLine,
            boolean[] hiddenLines,
            int contentHeight,
            int maxVisibleLineWidth,
            boolean wordWrap
    ) {
        this.text = text;
        this.lineStarts = lineStarts;
        this.rows = rows;
        this.firstRowByLine = firstRowByLine;
        this.lastRowByLine = lastRowByLine;
        this.hiddenLines = hiddenLines;
        this.contentHeight = contentHeight;
        this.maxVisibleLineWidth = maxVisibleLineWidth;
        this.wordWrap = wordWrap;
    }

    public static RawEditorLayout empty() {
        return EMPTY;
    }

    public static RawEditorLayout build(
            String text,
            int[] lineStarts,
            RawEditorTextMeasurer measurer,
            int contentWidth,
            int lineHeight,
            boolean wordWrap,
            List<FoldSpan> folds
    ) {
        String safeText = text == null ? "" : text;
        int[] starts = lineStarts == null || lineStarts.length == 0 ? new int[]{0} : lineStarts;
        int lineCount = starts.length;
        int wrapWidth = Math.max(1, contentWidth);
        int rowHeight = Math.max(1, lineHeight);
        boolean[] hidden = hiddenLines(lineCount, folds);
        int[] firstRows = new int[lineCount];
        int[] lastRows = new int[lineCount];
        Arrays.fill(firstRows, -1);
        Arrays.fill(lastRows, -1);

        List<VisualRow> rows = new ArrayList<>(lineCount);
        int maxWidth = 0;
        for (int line = 0; line < lineCount; line++) {
            if (hidden[line]) {
                continue;
            }
            int lineStart = starts[line];
            int lineEnd = lineEnd(safeText, starts, line);
            String lineText = safeText.substring(lineStart, lineEnd);
            maxWidth = Math.max(maxWidth, measurer.textWidth(lineText));
            boolean collapsed = isCollapsedStart(folds, line);
            if (collapsed || !wordWrap) {
                addRow(rows, firstRows, lastRows, line, lineStart, lineText, 0, lineText.length(), measurer, rowHeight, collapsed);
                continue;
            }
            int added = appendWrappedRows(rows, firstRows, lastRows, line, lineStart, lineText, measurer, wrapWidth, rowHeight);
            if (added == 0) {
                addRow(rows, firstRows, lastRows, line, lineStart, "", 0, 0, measurer, rowHeight, false);
            }
        }

        if (rows.isEmpty()) {
            rows.add(new VisualRow(0, 0, 0, 0, 0, 0, 0, 0, false));
            firstRows[0] = 0;
            lastRows[0] = 0;
        }

        return new RawEditorLayout(
                safeText,
                starts,
                List.copyOf(rows),
                firstRows,
                lastRows,
                hidden,
                rows.size() * rowHeight,
                maxWidth,
                wordWrap
        );
    }

    public int rowCount() {
        return this.rows.size();
    }

    public VisualRow row(int visualIndex) {
        int index = Math.clamp(visualIndex, 0, this.rows.size() - 1);
        return this.rows.get(index);
    }

    public List<VisualRow> rows() {
        return this.rows;
    }

    public int contentHeight() {
        return this.contentHeight;
    }

    public int maxVisibleLineWidth() {
        return this.maxVisibleLineWidth;
    }

    public boolean hiddenLine(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= this.hiddenLines.length) {
            return false;
        }
        return this.hiddenLines[lineIndex];
    }

    public int actualLineForRow(int visualIndex) {
        return this.row(visualIndex).lineIndex();
    }

    public int firstRowForLine(int lineIndex) {
        if (this.firstRowByLine.length == 0) {
            return 0;
        }
        int line = Math.clamp(lineIndex, 0, this.firstRowByLine.length - 1);
        int row = this.firstRowByLine[line];
        if (row >= 0) {
            return row;
        }
        for (int previous = line; previous >= 0; previous--) {
            if (this.firstRowByLine[previous] >= 0) {
                return this.firstRowByLine[previous];
            }
        }
        return 0;
    }

    public int rowForOffset(int offset) {
        int lineIndex = this.lineIndexForOffset(offset);
        int first = this.firstRowForLine(lineIndex);
        if (!this.wordWrap || this.lastRowByLine.length == 0) {
            return first;
        }
        int last = this.lastRowByLine[Math.clamp(lineIndex, 0, this.lastRowByLine.length - 1)];
        if (last < first) {
            return first;
        }
        int localOffset = Math.max(0, offset - this.lineStarts[lineIndex]);
        for (int rowIndex = first; rowIndex <= last; rowIndex++) {
            VisualRow row = this.rows.get(rowIndex);
            if (localOffset <= row.localEnd() || rowIndex == last) {
                return rowIndex;
            }
        }
        return first;
    }

    public int offsetAt(
            double mouseX,
            double mouseY,
            RawEditorTextMeasurer measurer,
            int innerTop,
            int scrollbarBottom,
            int contentLeft,
            int renderedScroll,
            int horizontalOffset,
            int lineHeight,
            int endOfLineTolerance,
            boolean expandWrappedLineEnd
    ) {
        if (this.rows.isEmpty()) {
            return 0;
        }

        double clampedMouseY = Math.clamp(mouseY, innerTop, scrollbarBottom);
        int rowIndex;
        if (clampedMouseY == innerTop) {
            rowIndex = 0;
        } else if (clampedMouseY == scrollbarBottom) {
            rowIndex = this.rows.size() - 1;
        } else {
            int localY = (int) Math.floor(clampedMouseY - innerTop + renderedScroll);
            int nearestRow = (int) Math.floor((localY + (lineHeight / 2.0d)) / lineHeight);
            rowIndex = Math.clamp(nearestRow, 0, this.rows.size() - 1);
        }
        return this.offsetAtRow(mouseX, rowIndex, measurer, contentLeft, horizontalOffset, endOfLineTolerance, expandWrappedLineEnd);
    }

    public int offsetAtRow(
            double mouseX,
            int rowIndex,
            RawEditorTextMeasurer measurer,
            int contentLeft,
            int horizontalOffset,
            int endOfLineTolerance,
            boolean expandWrappedLineEnd
    ) {
        VisualRow row = this.row(rowIndex);
        String segment = this.segmentText(row);
        int lineStart = this.lineStarts[row.lineIndex()];
        double localX = mouseX - contentLeft + horizontalOffset;
        if (localX < -endOfLineTolerance) {
            return lineStart + (this.wordWrap ? row.localStart() : 0);
        }
        if (segment.isEmpty()) {
            return row.documentStart();
        }
        if (localX <= 0d) {
            return row.documentStart();
        }
        int lineWidth = measurer.textWidth(segment);
        if (localX > lineWidth + endOfLineTolerance) {
            return expandWrappedLineEnd ? this.expandedWrappedLineEnd(row) : row.documentEnd();
        }
        if (localX > lineWidth) {
            return expandWrappedLineEnd ? this.expandedWrappedLineEnd(row) : row.documentEnd();
        }
        return row.documentStart() + measurer.indexAtWidth(segment, localX);
    }

    private int expandedWrappedLineEnd(VisualRow row) {
        if (!this.wordWrap || row.folded()) {
            return row.documentEnd();
        }
        int line = Math.clamp(row.lineIndex(), 0, this.lastRowByLine.length - 1);
        int lastRow = this.lastRowByLine[line];
        if (lastRow <= row.visualIndex() || lastRow < 0 || lastRow >= this.rows.size()) {
            return row.documentEnd();
        }
        return this.rows.get(lastRow).documentEnd();
    }

    public int cursorForRowAndX(int visualIndex, int targetX, RawEditorTextMeasurer measurer) {
        VisualRow row = this.row(visualIndex);
        String segment = this.segmentText(row);
        if (segment.isEmpty()) {
            return row.documentStart();
        }
        return row.documentStart() + measurer.indexAtWidth(segment, Math.max(0, targetX));
    }

    public int localVisualX(int visualIndex, int offset, RawEditorTextMeasurer measurer) {
        VisualRow row = this.row(visualIndex);
        int safeOffset = Math.clamp(offset, row.documentStart(), row.documentEnd());
        if (safeOffset <= row.documentStart()) {
            return 0;
        }
        String line = this.lineText(row.lineIndex());
        int localStart = Math.clamp(row.localStart(), 0, line.length());
        int localEnd = Math.clamp(safeOffset - this.lineStarts[row.lineIndex()], localStart, line.length());
        return measurer.textWidth(line.substring(localStart, localEnd));
    }

    public int caretX(int offset, RawEditorTextMeasurer measurer, int contentLeft, int horizontalOffset) {
        int rowIndex = this.rowForOffset(offset);
        return contentLeft - horizontalOffset + this.localVisualX(rowIndex, offset, measurer);
    }

    public String lineText(int lineIndex) {
        int line = Math.clamp(lineIndex, 0, this.lineStarts.length - 1);
        return this.text.substring(this.lineStarts[line], lineEnd(this.text, this.lineStarts, line));
    }

    public String segmentText(VisualRow row) {
        String line = this.lineText(row.lineIndex());
        int start = Math.clamp(row.localStart(), 0, line.length());
        int end = Math.clamp(row.localEnd(), start, line.length());
        return line.substring(start, end);
    }

    private int lineIndexForOffset(int offset) {
        int target = Math.clamp(offset, 0, this.text.length());
        int low = 0;
        int high = this.lineStarts.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int start = this.lineStarts[mid];
            int end = lineEnd(this.text, this.lineStarts, mid);
            if (target < start) {
                high = mid - 1;
            } else if (target > end) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return Math.clamp(low, 0, this.lineStarts.length - 1);
    }

    private static boolean[] hiddenLines(int lineCount, List<FoldSpan> folds) {
        boolean[] hidden = new boolean[lineCount];
        if (folds == null || folds.isEmpty()) {
            return hidden;
        }
        for (FoldSpan fold : folds) {
            if (fold == null || !fold.collapsed()) {
                continue;
            }
            int start = Math.clamp(fold.startLine() + 1, 0, lineCount);
            int end = Math.clamp(fold.endLine(), 0, lineCount - 1);
            for (int line = start; line <= end; line++) {
                hidden[line] = true;
            }
        }
        return hidden;
    }

    private static boolean isCollapsedStart(List<FoldSpan> folds, int lineIndex) {
        if (folds == null || folds.isEmpty()) {
            return false;
        }
        for (FoldSpan fold : folds) {
            if (fold != null && fold.collapsed() && fold.startLine() == lineIndex && fold.endLine() > lineIndex) {
                return true;
            }
        }
        return false;
    }

    private static int appendWrappedRows(
            List<VisualRow> rows,
            int[] firstRows,
            int[] lastRows,
            int line,
            int lineStart,
            String lineText,
            RawEditorTextMeasurer measurer,
            int wrapWidth,
            int lineHeight
    ) {
        if (lineText.isEmpty()) {
            addRow(rows, firstRows, lastRows, line, lineStart, "", 0, 0, measurer, lineHeight, false);
            return 1;
        }

        int segmentStart = 0;
        int added = 0;
        while (segmentStart < lineText.length()) {
            int segmentEnd = wrapSegmentEnd(lineText, segmentStart, wrapWidth, measurer);
            if (segmentEnd <= segmentStart) {
                segmentEnd = Math.min(lineText.length(), segmentStart + 1);
            }
            String segment = lineText.substring(segmentStart, segmentEnd);
            addRow(rows, firstRows, lastRows, line, lineStart, segment, segmentStart, segmentEnd, measurer, lineHeight, false);
            segmentStart = segmentEnd;
            added++;
        }
        return added;
    }

    private static void addRow(
            List<VisualRow> rows,
            int[] firstRows,
            int[] lastRows,
            int line,
            int lineStart,
            String segment,
            int localStart,
            int localEnd,
            RawEditorTextMeasurer measurer,
            int lineHeight,
            boolean folded
    ) {
        int visualIndex = rows.size();
        int width = measurer.textWidth(segment);
        rows.add(new VisualRow(
                visualIndex,
                line,
                lineStart + localStart,
                lineStart + localEnd,
                localStart,
                localEnd,
                visualIndex * lineHeight,
                width,
                folded
        ));
        if (firstRows[line] < 0) {
            firstRows[line] = visualIndex;
        }
        lastRows[line] = visualIndex;
    }

    private static int wrapSegmentEnd(
            String lineText,
            int segmentStart,
            int wrapWidth,
            RawEditorTextMeasurer measurer
    ) {
        if (segmentStart >= lineText.length()) {
            return segmentStart;
        }
        int index = segmentStart;
        int lastBreak = -1;
        double width = 0d;
        while (index < lineText.length()) {
            int codePoint = lineText.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            double advance = measurer.glyphAdvance(codePoint);
            if (width + advance > wrapWidth) {
                if (lastBreak > segmentStart) {
                    return lastBreak;
                }
                return index > segmentStart ? index : next;
            }
            width += advance;
            char symbol = (char) codePoint;
            if (Character.isWhitespace(symbol)
                    || symbol == ','
                    || symbol == ';'
                    || symbol == ':'
                    || symbol == ']'
                    || symbol == '}'
                    || symbol == ')') {
                lastBreak = next;
            }
            index = next;
        }
        return lineText.length();
    }

    private static int lineEnd(String text, int[] lineStarts, int lineIndex) {
        if (lineIndex >= lineStarts.length) {
            return 0;
        }
        if (lineIndex + 1 < lineStarts.length) {
            return lineStarts[lineIndex + 1] - 1;
        }
        return text.length();
    }

    public record FoldSpan(int startLine, int endLine, boolean collapsed) {
    }

    public record VisualRow(
            int visualIndex,
            int lineIndex,
            int documentStart,
            int documentEnd,
            int localStart,
            int localEnd,
            int y,
            int width,
            boolean folded
    ) {
    }
}
