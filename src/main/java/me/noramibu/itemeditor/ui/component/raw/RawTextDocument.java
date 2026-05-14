package me.noramibu.itemeditor.ui.component.raw;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class RawTextDocument {
    private final int historyLimit;
    private final int historyCharBudget;
    private final ArrayDeque<HistoryState> undoHistory = new ArrayDeque<>();
    private final ArrayDeque<HistoryState> redoHistory = new ArrayDeque<>();

    private String text;
    private int caret;
    private int anchor;
    private int[] lineStarts = new int[]{0};

    public RawTextDocument(String value, int historyLimit, int historyCharBudget) {
        this.historyLimit = Math.max(0, historyLimit);
        this.historyCharBudget = Math.max(0, historyCharBudget);
        this.reset(value);
    }

    public String text() {
        return this.text;
    }

    public int caret() {
        return this.caret;
    }

    public int anchor() {
        return this.anchor;
    }

    public int[] lineStarts() {
        return this.lineStarts;
    }

    public boolean hasSelection() {
        return this.caret != this.anchor;
    }

    public Selection selection() {
        int start = Math.min(this.caret, this.anchor);
        int end = Math.max(this.caret, this.anchor);
        return new Selection(start, end);
    }

    public int lineCount() {
        return this.lineStarts.length;
    }

    public int lineStart(int lineIndex) {
        int line = Math.clamp(lineIndex, 0, this.lineStarts.length - 1);
        return this.lineStarts[line];
    }

    public int lineEnd(int lineIndex) {
        if (lineIndex >= this.lineStarts.length) {
            return 0;
        }
        if (lineIndex + 1 < this.lineStarts.length) {
            return this.lineStarts[lineIndex + 1] - 1;
        }
        return this.text.length();
    }

    public int lineIndexForOffset(int offset) {
        int target = Math.clamp(offset, 0, this.text.length());
        int low = 0;
        int high = this.lineStarts.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int start = this.lineStarts[mid];
            int end = this.lineEnd(mid);
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

    public int previousCodePoint(int offset) {
        int safe = Math.clamp(offset, 0, this.text.length());
        if (safe == 0) {
            return 0;
        }
        return Character.offsetByCodePoints(this.text, safe, -1);
    }

    public int nextCodePoint(int offset) {
        int safe = Math.clamp(offset, 0, this.text.length());
        if (safe == this.text.length()) {
            return this.text.length();
        }
        return Character.offsetByCodePoints(this.text, safe, 1);
    }

    public int previousWordBoundary(int offset) {
        int cursor = Math.clamp(offset, 0, this.text.length());
        while (cursor > 0 && Character.isWhitespace(this.text.charAt(cursor - 1))) {
            cursor--;
        }
        while (cursor > 0
                && !Character.isWhitespace(this.text.charAt(cursor - 1))
                && !isWordCharacter(this.text.charAt(cursor - 1))) {
            cursor--;
        }
        while (cursor > 0 && Character.isWhitespace(this.text.charAt(cursor - 1))) {
            cursor--;
        }
        while (cursor > 0 && isWordCharacter(this.text.charAt(cursor - 1))) {
            cursor--;
        }
        return cursor;
    }

    public int nextWordBoundary(int offset) {
        int cursor = Math.clamp(offset, 0, this.text.length());
        while (cursor < this.text.length() && Character.isWhitespace(this.text.charAt(cursor))) {
            cursor++;
        }
        while (cursor < this.text.length()
                && !Character.isWhitespace(this.text.charAt(cursor))
                && !isWordCharacter(this.text.charAt(cursor))) {
            cursor++;
        }
        while (cursor < this.text.length() && Character.isWhitespace(this.text.charAt(cursor))) {
            cursor++;
        }
        while (cursor < this.text.length() && isWordCharacter(this.text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    public void setSelection(int anchor, int caret) {
        this.anchor = Math.clamp(anchor, 0, this.text.length());
        this.caret = Math.clamp(caret, 0, this.text.length());
    }

    public void moveCaret(int target, boolean keepSelection) {
        this.caret = Math.clamp(target, 0, this.text.length());
        if (!keepSelection) {
            this.anchor = this.caret;
        }
    }

    public void selectAll() {
        this.anchor = 0;
        this.caret = this.text.length();
    }

    public String selectedText() {
        Selection selection = this.selection();
        if (selection.empty()) {
            return "";
        }
        return this.text.substring(selection.start(), selection.end());
    }

    public EditResult replaceSelection(String replacement, double scroll) {
        Selection selection = this.selection();
        return this.replaceRange(selection.start(), selection.end(), replacement, scroll);
    }

    public EditResult pasteReplacingSelection(String clipboard, double scroll) {
        return this.replaceSelection(clipboard == null ? "" : clipboard, scroll);
    }

    public CutResult cutSelection(double scroll) {
        String selected = this.selectedText();
        EditResult edit = selected.isEmpty()
                ? EditResult.unchanged(this.text.length())
                : this.replaceSelection("", scroll);
        return new CutResult(selected, edit);
    }

    public EditResult deleteBackward(boolean byWord, double scroll) {
        if (this.hasSelection()) {
            return this.replaceSelection("", scroll);
        }
        if (this.caret == 0) {
            return EditResult.unchanged(this.text.length());
        }
        int start = byWord ? this.previousWordBoundary(this.caret) : this.previousCodePoint(this.caret);
        return this.replaceRange(start, this.caret, "", scroll);
    }

    public EditResult deleteForward(boolean byWord, double scroll) {
        if (this.hasSelection()) {
            return this.replaceSelection("", scroll);
        }
        if (this.caret == this.text.length()) {
            return EditResult.unchanged(this.text.length());
        }
        int end = byWord ? this.nextWordBoundary(this.caret) : this.nextCodePoint(this.caret);
        return this.replaceRange(this.caret, end, "", scroll);
    }

    public EditResult replaceRange(int start, int end, String replacement, double scroll) {
        int safeStart = Math.clamp(start, 0, this.text.length());
        int safeEnd = Math.clamp(end, safeStart, this.text.length());
        String safeReplacement = replacement == null ? "" : replacement;
        String nextText = this.text.substring(0, safeStart)
                + safeReplacement
                + this.text.substring(safeEnd);
        int nextCaret = safeStart + safeReplacement.length();
        return this.replaceContent(nextText, nextCaret, nextCaret, scroll, safeStart, safeEnd, safeReplacement);
    }

    public EditResult replaceContent(
            String nextText,
            int nextCaret,
            int nextAnchor,
            double scroll,
            int editStart,
            int editEnd,
            String replacement
    ) {
        String safeNextText = nextText == null ? "" : nextText;
        String previousText = this.text;
        int previousLength = previousText.length();
        boolean changed = !safeNextText.equals(previousText);
        int safeEditStart = editStart;
        int safeEditEnd = editEnd;
        String safeReplacement = replacement == null ? "" : replacement;
        if (changed && safeEditStart >= 0) {
            safeEditStart = Math.clamp(safeEditStart, 0, previousLength);
            safeEditEnd = Math.clamp(safeEditEnd, safeEditStart, previousLength);
        }
        String removed = changed && safeEditStart >= 0
                ? previousText.substring(safeEditStart, safeEditEnd)
                : "";
        int startLineBefore = changed && safeEditStart >= 0
                ? this.lineIndexForOffset(safeEditStart)
                : -1;

        if (changed) {
            this.undoHistory.addLast(this.captureState(scroll));
            this.trimHistory(this.undoHistory);
            this.redoHistory.clear();
        }

        this.text = safeNextText;
        this.caret = Math.clamp(nextCaret, 0, this.text.length());
        this.anchor = Math.clamp(nextAnchor, 0, this.text.length());

        boolean incremental = changed
                && safeEditStart >= 0
                && this.rebuildLineStartsIncremental(previousLength, safeEditStart, safeEditEnd, safeReplacement);
        if (!incremental) {
            this.rebuildLineStarts();
        }

        return new EditResult(
                previousText,
                previousLength,
                changed,
                safeEditStart,
                safeEditEnd,
                safeReplacement,
                removed,
                startLineBefore,
                newlineCount(safeReplacement) - newlineCount(removed),
                changed && safeEditStart >= 0 && newlineCount(safeReplacement) != newlineCount(removed),
                incremental
        );
    }

    public void reset(String value) {
        this.text = value == null ? "" : value;
        this.caret = Math.clamp(this.caret, 0, this.text.length());
        this.anchor = Math.clamp(this.anchor, 0, this.text.length());
        this.rebuildLineStarts();
        this.undoHistory.clear();
        this.redoHistory.clear();
    }

    public boolean canUndo() {
        return !this.undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !this.redoHistory.isEmpty();
    }

    public Optional<RestoredState> undo(double currentScroll) {
        if (this.undoHistory.isEmpty()) {
            return Optional.empty();
        }
        int previousLength = this.text.length();
        this.redoHistory.addLast(this.captureState(currentScroll));
        this.trimHistory(this.redoHistory);
        HistoryState restored = this.undoHistory.removeLast();
        this.restore(restored);
        return Optional.of(new RestoredState(restored.text(), restored.caret(), restored.anchor(), restored.scroll(), previousLength));
    }

    public Optional<RestoredState> redo(double currentScroll) {
        if (this.redoHistory.isEmpty()) {
            return Optional.empty();
        }
        int previousLength = this.text.length();
        this.undoHistory.addLast(this.captureState(currentScroll));
        this.trimHistory(this.undoHistory);
        HistoryState restored = this.redoHistory.removeLast();
        this.restore(restored);
        return Optional.of(new RestoredState(restored.text(), restored.caret(), restored.anchor(), restored.scroll(), previousLength));
    }

    public List<HistorySnapshot> undoHistorySnapshot() {
        return this.historySnapshot(this.undoHistory);
    }

    public List<HistorySnapshot> redoHistorySnapshot() {
        return this.historySnapshot(this.redoHistory);
    }

    public void restoreEditorState(
            int caret,
            int anchor,
            List<HistorySnapshot> undoHistory,
            List<HistorySnapshot> redoHistory
    ) {
        this.caret = Math.clamp(caret, 0, this.text.length());
        this.anchor = Math.clamp(anchor, 0, this.text.length());
        this.undoHistory.clear();
        this.redoHistory.clear();
        if (undoHistory != null) {
            for (HistorySnapshot snapshot : undoHistory) {
                if (snapshot != null) {
                    this.undoHistory.addLast(HistoryState.fromSnapshot(snapshot));
                }
            }
        }
        if (redoHistory != null) {
            for (HistorySnapshot snapshot : redoHistory) {
                if (snapshot != null) {
                    this.redoHistory.addLast(HistoryState.fromSnapshot(snapshot));
                }
            }
        }
        this.trimHistory(this.undoHistory);
        this.trimHistory(this.redoHistory);
    }

    private void restore(HistoryState state) {
        this.text = state.text();
        this.caret = Math.clamp(state.caret(), 0, this.text.length());
        this.anchor = Math.clamp(state.anchor(), 0, this.text.length());
        this.rebuildLineStarts();
    }

    private List<HistorySnapshot> historySnapshot(ArrayDeque<HistoryState> history) {
        if (history.isEmpty()) {
            return List.of();
        }
        List<HistorySnapshot> snapshots = new ArrayList<>(history.size());
        for (HistoryState state : history) {
            snapshots.add(state.toSnapshot());
        }
        return snapshots;
    }

    private HistoryState captureState(double scroll) {
        return new HistoryState(this.text, this.caret, this.anchor, scroll);
    }

    private void trimHistory(ArrayDeque<HistoryState> history) {
        while (history.size() > this.historyLimit) {
            history.removeFirst();
        }
        while (!history.isEmpty() && this.historyCharCount(history) > this.historyCharBudget) {
            history.removeFirst();
        }
    }

    private int historyCharCount(ArrayDeque<HistoryState> history) {
        int total = 0;
        for (HistoryState state : history) {
            total += state.text().length();
            if (total > this.historyCharBudget) {
                return total;
            }
        }
        return total;
    }

    private void rebuildLineStarts() {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int index = 0; index < this.text.length(); index++) {
            if (this.text.charAt(index) == '\n') {
                starts.add(index + 1);
            }
        }
        this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean rebuildLineStartsIncremental(
            int oldTextLength,
            int editStart,
            int editEnd,
            String replacement
    ) {
        if (this.lineStarts.length == 0) {
            return false;
        }
        int safeStart = Math.clamp(editStart, 0, oldTextLength);
        int safeEnd = Math.clamp(editEnd, safeStart, oldTextLength);
        if (safeStart == 0 && safeEnd == oldTextLength) {
            return false;
        }

        int delta = replacement.length() - (safeEnd - safeStart);
        List<Integer> starts = this.updatedLineStarts(safeStart, safeEnd, replacement, delta);
        if (starts.isEmpty()) {
            starts.add(0);
        }

        int[] startArray = starts.stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(startArray);
        int textLength = this.text.length();
        int write = 0;
        for (int read = 0; read < startArray.length; read++) {
            int clamped = Math.clamp(startArray[read], 0, textLength);
            if (write == 0 || clamped != startArray[write - 1]) {
                startArray[write++] = clamped;
            }
        }
        if (write == 0 || startArray[0] != 0) {
            int[] normalized = new int[write + 1];
            normalized[0] = 0;
            if (write > 0) {
                System.arraycopy(startArray, 0, normalized, 1, write);
            }
            this.lineStarts = normalized;
            return true;
        }
        this.lineStarts = write == startArray.length ? startArray : Arrays.copyOf(startArray, write);
        return true;
    }

    private List<Integer> updatedLineStarts(int safeStart, int safeEnd, String replacement, int delta) {
        List<Integer> starts = new ArrayList<>(this.lineStarts.length + 8);
        for (int start : this.lineStarts) {
            if (start <= safeStart) {
                starts.add(start);
                continue;
            }
            if (start < safeEnd) {
                continue;
            }
            starts.add(start + delta);
        }

        for (int index = 0; index < replacement.length(); index++) {
            if (replacement.charAt(index) == '\n') {
                starts.add(safeStart + index + 1);
            }
        }
        return starts;
    }

    private static int newlineCount(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == ':'
                || value == '-'
                || value == '.'
                || value == '/';
    }

    public record Selection(int start, int end) {
        public boolean empty() {
            return this.start >= this.end;
        }
    }

    public record CutResult(String text, EditResult edit) {
    }

    public record EditResult(
            String previousText,
            int previousLength,
            boolean changed,
            int start,
            int end,
            String replacement,
            String removedText,
            int startLineBefore,
            int lineDelta,
            boolean lineCountChanged,
            boolean incrementalLineStarts
    ) {
        public EditResult {
            previousText = previousText == null ? "" : previousText;
            replacement = replacement == null ? "" : replacement;
            removedText = removedText == null ? "" : removedText;
        }

        public static EditResult unchanged(int cursor) {
            int safe = Math.max(0, cursor);
            return new EditResult("", safe, false, safe, safe, "", "", -1, 0, false, false);
        }
    }

    public record RestoredState(String text, int caret, int anchor, double scroll, int previousLength) {
        public RestoredState {
            text = text == null ? "" : text;
        }
    }

    public record HistorySnapshot(String text, int caret, int anchor, double scroll) {
        public HistorySnapshot {
            text = text == null ? "" : text;
        }
    }

    private record HistoryState(String text, int caret, int anchor, double scroll) {
        private HistorySnapshot toSnapshot() {
            return new HistorySnapshot(this.text, this.caret, this.anchor, this.scroll);
        }

        private static HistoryState fromSnapshot(HistorySnapshot snapshot) {
            String text = snapshot.text() == null ? "" : snapshot.text();
            int caret = Math.clamp(snapshot.caret(), 0, text.length());
            int anchor = Math.clamp(snapshot.anchor(), 0, text.length());
            return new HistoryState(text, caret, anchor, snapshot.scroll());
        }
    }
}
