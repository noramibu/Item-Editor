package me.noramibu.itemeditor.ui.component.raw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawEditorLayout {
    private static final RawEditorLayout EMPTY = new RawEditorLayout(
            "",
            new int[]{0},
            List.of(new VisualRow(0, 0, 0, false)),
            new int[]{0},
            new int[]{0},
            new boolean[]{false},
            new int[]{0},
            1,
            0,
            false,
            1,
            1
    );

    private final String text;
    private final int[] lineStarts;
    private final List<VisualRow> rows;
    private final int[] firstRowByLine;
    private final int[] lastRowByLine;
    private final boolean[] hiddenLines;
    private final int[] lineWidths;
    private final int contentHeight;
    private final int maxVisibleLineWidth;
    private final boolean wordWrap;
    private final int wrapWidth;
    private final int lineHeight;

    private RawEditorLayout(
            String text,
            int[] lineStarts,
            List<VisualRow> rows,
            int[] firstRowByLine,
            int[] lastRowByLine,
            boolean[] hiddenLines,
            int[] lineWidths,
            int contentHeight,
            int maxVisibleLineWidth,
            boolean wordWrap,
            int wrapWidth,
            int lineHeight
    ) {
        this.text = text;
        this.lineStarts = lineStarts;
        this.rows = rows;
        this.firstRowByLine = firstRowByLine;
        this.lastRowByLine = lastRowByLine;
        this.hiddenLines = hiddenLines;
        this.lineWidths = lineWidths;
        this.contentHeight = contentHeight;
        this.maxVisibleLineWidth = maxVisibleLineWidth;
        this.wordWrap = wordWrap;
        this.wrapWidth = wrapWidth;
        this.lineHeight = lineHeight;
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
        boolean[] collapsedStarts = collapsedStarts(lineCount, folds);
        int[] firstRows = new int[lineCount];
        int[] lastRows = new int[lineCount];
        int[] lineWidths = new int[lineCount];
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
            lineWidths[line] = measurer.textWidth(lineText);
            maxWidth = Math.max(maxWidth, lineWidths[line]);
            boolean collapsed = collapsedStarts[line];
            firstRows[line] = rows.size();
            appendRows(rows, line, lineText, measurer, wrapWidth, wordWrap, collapsed);
            lastRows[line] = rows.size() - 1;
        }

        if (rows.isEmpty()) {
            rows.add(new VisualRow(0, 0, 0, false));
            firstRows[0] = 0;
            lastRows[0] = 0;
        }

        return new RawEditorLayout(
                safeText,
                starts,
                rows,
                firstRows,
                lastRows,
                hidden,
                lineWidths,
                rows.size() * rowHeight,
                maxWidth,
                wordWrap,
                wrapWidth,
                rowHeight
        );
    }

    public RawEditorLayout updateLine(
            String text,
            int[] lineStarts,
            int lineIndex,
            RawEditorTextMeasurer measurer,
            int contentWidth,
            int lineHeight
    ) {
        String safeText = text == null ? "" : text;
        int[] starts = lineStarts == null ? new int[0] : lineStarts;
        int line = Math.clamp(lineIndex, 0, Math.max(0, starts.length - 1));
        int requestedWrapWidth = Math.max(1, contentWidth);
        int requestedLineHeight = Math.max(1, lineHeight);
        if (starts.length != this.lineStarts.length
                || starts.length == 0
                || requestedWrapWidth != this.wrapWidth
                || requestedLineHeight != this.lineHeight
                || this.hiddenLine(line)) {
            return null;
        }

        int first = this.firstRowByLine[line];
        int last = this.lastRowByLine[line];
        if (first < 0 || last < first || last >= this.rows.size()) {
            return null;
        }

        String lineText = safeText.substring(starts[line], lineEnd(safeText, starts, line));
        List<VisualRow> replacement = new ArrayList<>();
        appendRows(
                replacement,
                line,
                lineText,
                measurer,
                requestedWrapWidth,
                this.wordWrap,
                this.rows.get(first).folded()
        );

        List<VisualRow> updatedRows = new ArrayList<>(this.rows.size() - (last - first + 1) + replacement.size());
        updatedRows.addAll(this.rows.subList(0, first));
        updatedRows.addAll(replacement);
        updatedRows.addAll(this.rows.subList(last + 1, this.rows.size()));

        int rowDelta = replacement.size() - (last - first + 1);
        int[] firstRows = Arrays.copyOf(this.firstRowByLine, this.firstRowByLine.length);
        int[] lastRows = Arrays.copyOf(this.lastRowByLine, this.lastRowByLine.length);
        firstRows[line] = first;
        lastRows[line] = first + replacement.size() - 1;
        if (rowDelta != 0) {
            for (int nextLine = line + 1; nextLine < firstRows.length; nextLine++) {
                if (firstRows[nextLine] >= 0) {
                    firstRows[nextLine] += rowDelta;
                    lastRows[nextLine] += rowDelta;
                }
            }
        }

        int[] lineWidths = Arrays.copyOf(this.lineWidths, this.lineWidths.length);
        lineWidths[line] = measurer.textWidth(lineText);
        int maxWidth = 0;
        for (int width : lineWidths) {
            maxWidth = Math.max(maxWidth, width);
        }

        return new RawEditorLayout(
                safeText,
                starts,
                updatedRows,
                firstRows,
                lastRows,
                this.hiddenLines,
                lineWidths,
                updatedRows.size() * requestedLineHeight,
                maxWidth,
                this.wordWrap,
                requestedWrapWidth,
                requestedLineHeight
        );
    }

    public int rowCount() {
        return this.rows.size();
    }

    public VisualRow row(int visualIndex) {
        int index = Math.clamp(visualIndex, 0, this.rows.size() - 1);
        return this.rows.get(index);
    }

    public int firstVisibleRow(int renderedScroll, int lineHeight) {
        int height = Math.max(1, lineHeight);
        return Math.clamp(Math.max(0, renderedScroll) / height - 1, 0, this.rows.size());
    }

    public int lastVisibleRowExclusive(int renderedScroll, int visibleHeight, int lineHeight) {
        int height = Math.max(1, lineHeight);
        int last = (Math.max(0, renderedScroll) + Math.max(0, visibleHeight)) / height + 2;
        return Math.clamp(last, this.firstVisibleRow(renderedScroll, height), this.rows.size());
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

    public int documentStart(VisualRow row) {
        int line = Math.clamp(row.lineIndex(), 0, this.lineStarts.length - 1);
        return this.lineStarts[line] + row.localStart();
    }

    public int documentEnd(VisualRow row) {
        int line = Math.clamp(row.lineIndex(), 0, this.lineStarts.length - 1);
        return this.lineStarts[line] + row.localEnd();
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
            return this.documentStart(row);
        }
        if (localX <= 0d) {
            return this.documentStart(row);
        }
        int lineWidth = measurer.textWidth(segment);
        if (localX > lineWidth + endOfLineTolerance) {
            return expandWrappedLineEnd ? this.expandedWrappedLineEnd(rowIndex, row) : this.documentEnd(row);
        }
        if (localX > lineWidth) {
            return expandWrappedLineEnd ? this.expandedWrappedLineEnd(rowIndex, row) : this.documentEnd(row);
        }
        return this.documentStart(row) + measurer.indexAtWidth(segment, localX);
    }

    private int expandedWrappedLineEnd(int rowIndex, VisualRow row) {
        if (!this.wordWrap || row.folded()) {
            return this.documentEnd(row);
        }
        int line = Math.clamp(row.lineIndex(), 0, this.lastRowByLine.length - 1);
        int lastRow = this.lastRowByLine[line];
        if (lastRow <= rowIndex || lastRow < 0 || lastRow >= this.rows.size()) {
            return this.documentEnd(row);
        }
        return this.documentEnd(this.rows.get(lastRow));
    }

    public int cursorForRowAndX(int visualIndex, int targetX, RawEditorTextMeasurer measurer) {
        VisualRow row = this.row(visualIndex);
        String segment = this.segmentText(row);
        if (segment.isEmpty()) {
            return this.documentStart(row);
        }
        return this.documentStart(row) + measurer.indexAtWidth(segment, Math.max(0, targetX));
    }

    public int localVisualX(int visualIndex, int offset, RawEditorTextMeasurer measurer) {
        VisualRow row = this.row(visualIndex);
        int documentStart = this.documentStart(row);
        int safeOffset = Math.clamp(offset, documentStart, this.documentEnd(row));
        if (safeOffset <= documentStart) {
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
        if (target > 0) {
            int exactLineStart = Arrays.binarySearch(this.lineStarts, target);
            if (exactLineStart >= 0) {
                return exactLineStart;
            }
        }
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
        int[] changes = new int[lineCount + 1];
        for (FoldSpan fold : folds) {
            if (fold == null || !fold.collapsed()) {
                continue;
            }
            int start = Math.clamp(fold.startLine() + 1, 0, lineCount);
            int end = Math.clamp(fold.endLine(), 0, lineCount - 1);
            if (start <= end) {
                changes[start]++;
                changes[end + 1]--;
            }
        }
        int active = 0;
        for (int line = 0; line < lineCount; line++) {
            active += changes[line];
            hidden[line] = active > 0;
        }
        return hidden;
    }

    private static boolean[] collapsedStarts(int lineCount, List<FoldSpan> folds) {
        boolean[] collapsed = new boolean[lineCount];
        if (folds == null) {
            return collapsed;
        }
        for (FoldSpan fold : folds) {
            if (fold != null && fold.collapsed() && fold.endLine() > fold.startLine()) {
                int start = fold.startLine();
                if (start >= 0 && start < lineCount) {
                    collapsed[start] = true;
                }
            }
        }
        return collapsed;
    }

    private static void appendRows(
            List<VisualRow> rows,
            int line,
            String lineText,
            RawEditorTextMeasurer measurer,
            int wrapWidth,
            boolean wordWrap,
            boolean folded
    ) {
        if (folded || !wordWrap || lineText.isEmpty()) {
            rows.add(new VisualRow(line, 0, lineText.length(), folded));
            return;
        }

        int segmentStart = 0;
        while (segmentStart < lineText.length()) {
            int segmentEnd = wrapSegmentEnd(lineText, segmentStart, wrapWidth, measurer);
            if (segmentEnd <= segmentStart) {
                segmentEnd = Math.min(lineText.length(), segmentStart + 1);
            }
            rows.add(new VisualRow(line, segmentStart, segmentEnd, false));
            segmentStart = segmentEnd;
        }
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
            int lineIndex,
            int localStart,
            int localEnd,
            boolean folded
    ) {
    }
}
