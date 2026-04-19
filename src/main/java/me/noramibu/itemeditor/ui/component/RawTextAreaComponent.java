package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.inject.GreedyInputUIComponent;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix3x2fStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class RawTextAreaComponent extends BaseUIComponent implements GreedyInputUIComponent {
    private static final int HISTORY_LIMIT = 128;
    private static final int HISTORY_CHAR_BUDGET = 6_000_000;
    private static final int LINE_SPACING = 2;
    private static final int GUTTER_MIN_WIDTH = 24;
    private static final int GUTTER_PADDING = 2;
    private static final int GUTTER_LINE_RIGHT_INSET = 2;
    private static final int GUTTER_FOLD_GAP = 1;
    private static final int CONTENT_PADDING = 3;
    private static final int SCROLLBAR_BASE_THICKNESS = 6;
    private static final int SCROLLBAR_RESERVE_EXTRA = 1;
    private static final int SCROLLBAR_EDGE_INSET = 5;
    private static final int HORIZONTAL_SCROLLBAR_MIN_THUMB = 16;
    private static final int HORIZONTAL_SCROLL_PADDING = 4;
    private static final int MAX_RENDER_LINE_CHARS = 2048;
    private static final int MAX_SYNTAX_RENDER_BUDGET_CHARS = 24_000;
    private static final int MAX_SYNTAX_DEPTH_SCAN_TOTAL_CHARS = 320_000;
    // I didn't notice any lag with 8M char NBTs so i assume it's safe to get rid of this for now.
    // private static final int MAX_FOLD_SCAN_TOTAL_CHARS = 250000;

    private static final int COLOR_BACKGROUND = 0xCC0A0F18;
    private static final int COLOR_BACKGROUND_SOLID = 0xFF0A0F18;
    private static final int COLOR_BORDER = 0xFF5B6472;
    private static final int COLOR_GUTTER_BG = 0xCC151E2A;
    private static final int COLOR_GUTTER_BORDER = 0xFF364152;
    private static final int COLOR_GUTTER_LINE = 0xFF8A97A8;
    private static final int COLOR_GUTTER_ACTIVE = 0xFFE7ECF3;
    private static final int COLOR_GUTTER_ERROR = 0xFFFF8A8A;
    private static final int COLOR_ERROR_UNDERLINE = 0xFFF87171;
    private static final int COLOR_TEXT = 0xFFE7ECF3;
    private static final int COLOR_SELECTION = 0x665B81B5;
    private static final int COLOR_CURSOR = 0xFFD0D5DD;
    private static final int CURSOR_WIDTH = 1;
    private static final int COLOR_STRING = 0xFFA7F3D0;
    private static final int COLOR_NUMBER = 0xFFF2C26B;
    private static final int COLOR_LITERAL = 0xFFF7A86D;
    private static final int COLOR_PUNCT = 0xFFBFA6FF;
    private static final int COLOR_IDENTIFIER = 0xFF93C5FD;
    private static final int COLOR_BOOLEAN = 0xFFF9A8D4;
    private static final int COLOR_NULL = 0xFFFDA4AF;
    private static final int COLOR_ESCAPE = 0xFFFDE68A;
    private static final int COLOR_NUMBER_SUFFIX = 0xFFEAB308;
    private static final int COLOR_OPERATOR = 0xFF9CA3AF;
    private static final int[] KEY_DEPTH_COLORS = {
            0xFF7EC8F8,
            0xFF67E8F9,
            0xFF93C5FD,
            0xFFA5B4FC,
            0xFFC4B5FD,
            0xFF6EE7B7
    };
    private static final int[] BRACKET_DEPTH_COLORS = {
            0xFFC4B5FD,
            0xFF93C5FD,
            0xFFA7F3D0,
            0xFF67E8F9,
            0xFFFDE68A,
            0xFFF9A8D4
    };
    private static final int COLOR_GHOST = 0x66A2B2C7;
    private static final int COLOR_FOLD_MARKER = 0xFFA7B4C7;
    private static final int COLOR_FOLD_ACTIVE = 0xFFE7ECF3;
    private static final int COLOR_AUTOCOMPLETE_BG = 0xB8141D2A;
    private static final int COLOR_AUTOCOMPLETE_BORDER = 0xC64B5563;
    private static final int COLOR_AUTOCOMPLETE_ROW_BG = 0xC01A2330;
    private static final int COLOR_AUTOCOMPLETE_SELECTED_BG = 0x7A3B82F6;
    private static final int COLOR_AUTOCOMPLETE_TEXT = 0xFFE2E8F0;
    private static final int COLOR_AUTOCOMPLETE_SELECTED_TEXT = 0xFFF8FAFC;
    private static final int COLOR_AUTOCOMPLETE_DETAIL = 0xFF93A3B8;
    private static final int COLOR_INDENT_GUIDE = 0x2E7B8698;
    private static final int AUTOCOMPLETE_MAX_ROWS = 8;
    private static final int AUTOCOMPLETE_PADDING = 4;
    private static final int AUTOCOMPLETE_MIN_WIDTH = 140;
    private static final Style RAW_STYLE = Style.EMPTY;
    private static final int FOLD_MARKER_WIDTH = 8;
    private static final int INDENT_SIZE = 4;
    private static final double TEXT_SCALE_THRESHOLD_GS5 = 5.0d;
    private static final double TEXT_SCALE_THRESHOLD_GS4 = 4.0d;
    private static final double TEXT_SCALE_THRESHOLD_GS3 = 3.0d;
    private static final float TEXT_SCALE_GS5 = 0.72F;
    private static final float TEXT_SCALE_GS4 = 0.80F;
    private static final float TEXT_SCALE_GS3 = 0.90F;
    private static final int TEXT_SCALE_NARROW_WIDTH_THRESHOLD = 560;
    private static final int TEXT_SCALE_NARROW_HEIGHT_THRESHOLD = 300;
    private static final float TEXT_SCALE_NARROW_PENALTY = 0.04F;
    private static final int TEXT_SCALE_WIDE_WIDTH_THRESHOLD = 1200;
    private static final int TEXT_SCALE_WIDE_HEIGHT_THRESHOLD = 700;
    private static final float TEXT_SCALE_WIDE_BONUS = 0.04F;
    private static final float TEXT_SCALE_MIN = 0.10F;
    private static final float TEXT_SCALE_MAX = 5.00F;

    private final EventStream<OnChanged> changedEvents = OnChanged.newStream();
    private final ArrayDeque<HistoryState> undoHistory = new ArrayDeque<>();
    private final ArrayDeque<HistoryState> redoHistory = new ArrayDeque<>();

    private Runnable viewportChanged = () -> {};
    private Runnable historyChanged = () -> {};
    private BooleanSupplier autocompleteRequested = () -> false;
    private BooleanSupplier autocompleteRefreshRequested = () -> false;
    private BooleanSupplier autocompleteNextRequested = () -> false;
    private BooleanSupplier autocompletePreviousRequested = () -> false;

    private String text = "";
    private String ghostSuggestion = "";
    private List<AutocompletePopupEntry> autocompleteEntries = List.of();
    private int autocompleteSelected = -1;
    private int errorLine = -1;
    private int errorColumn = -1;
    private int errorLength = 0;
    private int cursor;
    private int selectionCursor;
    private double scrollAmount;
    private boolean wordWrap;
    private boolean horizontalScroll = true;
    private double horizontalScrollAmount;
    private int virtualCaretLine = -1;
    private int virtualCaretLocalX = -1;
    private boolean focused;
    private boolean draggingSelection;
    private boolean draggingScrollbar;
    private boolean draggingHorizontalScrollbar;
    private int scrollbarDragOffset;
    private int horizontalScrollbarDragOffset;
    private int[] lineStarts = new int[]{0};
    private List<FoldRegion> foldRegions = List.of();
    private FoldRegion[] foldByStartLine = new FoldRegion[0];
    private boolean[] hiddenLines = new boolean[]{false};
    private int[] visibleToActualLine = new int[]{0};
    private int[] actualToVisibleLine = new int[]{0};
    private int[] actualToVisibleLineLast = new int[]{0};
    private int[] visibleSegmentStart = new int[]{0};
    private int[] visibleSegmentEnd = new int[]{0};
    private int[] lineDepthStarts = new int[]{0};
    private boolean lineDepthStartsDirty = true;
    private int contentHeight = 1;
    private final Map<Integer, Float> glyphAdvanceCache = new HashMap<>();
    private final Map<Integer, SyntaxLineCache> syntaxLineCache = new HashMap<>();
    private int maxVisibleLineWidth;
    private int wrapLayoutWidth = -1;
    private int fontSizePercent = 100;
    private float cachedTextScale = Float.NaN;
    public RawTextAreaComponent(Sizing horizontalSizing, Sizing verticalSizing, String value) {
        this.horizontalSizing(horizontalSizing);
        this.verticalSizing(verticalSizing);
        this.cursorStyle(CursorStyle.TEXT);
        this.resetText(value == null ? "" : value);
    }

    public RawTextAreaComponent displayCharCount(boolean ignored) {
        return this;
    }

    public EventSource<OnChanged> onChanged() {
        return this.changedEvents.source();
    }

    public RawTextAreaComponent onViewportChanged(Runnable listener) {
        this.viewportChanged = Objects.requireNonNullElse(listener, () -> {});
        return this;
    }

    public RawTextAreaComponent onHistoryChanged(Runnable listener) {
        this.historyChanged = Objects.requireNonNullElse(listener, () -> {});
        return this;
    }

    public RawTextAreaComponent onAutocompleteRequested(BooleanSupplier listener) {
        this.autocompleteRequested = Objects.requireNonNullElse(listener, () -> false);
        return this;
    }

    public RawTextAreaComponent onAutocompleteRefreshRequested(BooleanSupplier listener) {
        this.autocompleteRefreshRequested = Objects.requireNonNullElse(listener, () -> false);
        return this;
    }

    public RawTextAreaComponent onAutocompleteNextRequested(BooleanSupplier listener) {
        this.autocompleteNextRequested = Objects.requireNonNullElse(listener, () -> false);
        return this;
    }

    public RawTextAreaComponent onAutocompletePreviousRequested(BooleanSupplier listener) {
        this.autocompletePreviousRequested = Objects.requireNonNullElse(listener, () -> false);
        return this;
    }

    public RawTextAreaComponent ghostSuggestion(String ghostSuggestion) {
        this.ghostSuggestion = ghostSuggestion == null ? "" : ghostSuggestion;
        return this;
    }

    public RawTextAreaComponent autocompletePopup(List<AutocompletePopupEntry> entries, int selectedIndex) {
        if (entries == null || entries.isEmpty()) {
            this.autocompleteEntries = List.of();
            this.autocompleteSelected = -1;
            return this;
        }
        this.autocompleteEntries = entries.stream()
                .filter(Objects::nonNull)
                .toList();
        if (this.autocompleteEntries.isEmpty()) {
            this.autocompleteSelected = -1;
            return this;
        }
        this.autocompleteSelected = Math.clamp(selectedIndex, 0, this.autocompleteEntries.size() - 1);
        return this;
    }

    public RawTextAreaComponent setErrorLine(int lineOneBased) {
        return this.setErrorLocation(lineOneBased, -1, 0);
    }

    public RawTextAreaComponent setErrorLocation(int lineOneBased, int columnOneBased, int lengthChars) {
        this.errorLine = lineOneBased;
        this.errorColumn = columnOneBased;
        this.errorLength = Math.max(0, lengthChars);
        return this;
    }

    public RawTextAreaComponent wordWrap(boolean value) {
        this.wordWrap = value;
        this.horizontalScroll = !value;
        if (value) {
            this.horizontalScrollAmount = 0d;
        }
        this.wrapLayoutWidth = -1;
        this.applyFoldVisibility();
        this.ensureCursorVisible();
        return this;
    }

    public RawTextAreaComponent horizontalScroll(boolean value) {
        this.horizontalScroll = value && !this.wordWrap;
        if (!this.horizontalScroll) {
            this.horizontalScrollAmount = 0d;
        }
        this.clampHorizontalScrollAmount();
        this.ensureCursorVisible();
        return this;
    }

    public RawTextAreaComponent fontSizePercent(int value) {
        int clamped = Math.clamp(value, 1, 500);
        if (this.fontSizePercent == clamped) {
            return this;
        }
        this.fontSizePercent = clamped;
        this.cachedTextScale = Float.NaN;
        this.wrapLayoutWidth = -1;
        this.applyFoldVisibility();
        this.ensureCursorVisible();
        this.notifyViewportChanged();
        return this;
    }

    public boolean wordWrap() {
        return this.wordWrap;
    }

    public boolean horizontalScroll() {
        return this.horizontalScroll;
    }

    public String getValue() {
        return this.text;
    }

    public RawTextAreaComponent text(String value) {
        this.commitTextChange(value == null ? "" : value, this.cursor, this.cursor);
        return this;
    }

    public int caretLine() {
        return this.lineIndexForCursor(this.cursor) + 1;
    }

    public int caretColumn() {
        int line = this.lineIndexForCursor(this.cursor);
        return (this.cursor - this.lineStarts[line]) + 1;
    }

    public int caretIndex() {
        return this.cursor;
    }

    public int selectionIndex() {
        return this.selectionCursor;
    }

    public double scrollOffset() {
        return this.scrollAmount;
    }

    public boolean hasSelection() {
        return this.cursor != this.selectionCursor;
    }

    public boolean hasVirtualCaret() {
        return this.virtualCaretLine >= 0 && this.virtualCaretLocalX >= 0;
    }

    public boolean canUndo() {
        return !this.undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !this.redoHistory.isEmpty();
    }

    public List<HistorySnapshot> undoHistorySnapshot() {
        if (this.undoHistory.isEmpty()) {
            return List.of();
        }
        List<HistorySnapshot> snapshots = new ArrayList<>(this.undoHistory.size());
        for (HistoryState state : this.undoHistory) {
            snapshots.add(this.toHistorySnapshot(state));
        }
        return snapshots;
    }

    public List<HistorySnapshot> redoHistorySnapshot() {
        if (this.redoHistory.isEmpty()) {
            return List.of();
        }
        List<HistorySnapshot> snapshots = new ArrayList<>(this.redoHistory.size());
        for (HistoryState state : this.redoHistory) {
            snapshots.add(this.toHistorySnapshot(state));
        }
        return snapshots;
    }

    public RawTextAreaComponent restoreEditorState(
            int cursor,
            int selection,
            double scrollAmount,
            List<HistorySnapshot> undoHistory,
            List<HistorySnapshot> redoHistory
    ) {
        this.cursor = Math.clamp(cursor, 0, this.text.length());
        this.selectionCursor = Math.clamp(selection, 0, this.text.length());
        this.scrollAmount = scrollAmount;

        this.undoHistory.clear();
        this.redoHistory.clear();
        if (undoHistory != null) {
            for (HistorySnapshot snapshot : undoHistory) {
                if (snapshot == null) {
                    continue;
                }
                this.undoHistory.addLast(this.fromHistorySnapshot(snapshot));
            }
        }
        if (redoHistory != null) {
            for (HistorySnapshot snapshot : redoHistory) {
                if (snapshot == null) {
                    continue;
                }
                this.redoHistory.addLast(this.fromHistorySnapshot(snapshot));
            }
        }
        this.trimHistory(this.undoHistory);
        this.trimHistory(this.redoHistory);
        this.clampScrollAmount();
        this.clampHorizontalScrollAmount();
        this.notifyViewportChanged();
        this.historyChanged.run();
        return this;
    }

    public void undo() {
        if (this.undoHistory.isEmpty()) return;
        int previousLength = this.text.length();
        this.redoHistory.addLast(this.captureState());
        this.trimHistory(this.redoHistory);
        this.restoreState(this.undoHistory.removeLast());
        this.historyChanged.run();
        this.changedEvents.sink().onChanged(this.text, ChangeDelta.fullReplace(previousLength, this.text));
    }

    public void redo() {
        if (this.redoHistory.isEmpty()) return;
        int previousLength = this.text.length();
        this.undoHistory.addLast(this.captureState());
        this.trimHistory(this.undoHistory);
        this.restoreState(this.redoHistory.removeLast());
        this.historyChanged.run();
        this.changedEvents.sink().onChanged(this.text, ChangeDelta.fullReplace(previousLength, this.text));
    }

    public void replaceRange(int start, int end, String replacement) {
        int clampedStart = Math.clamp(start, 0, this.text.length());
        int clampedEnd = Math.clamp(end, clampedStart, this.text.length());
        String replacementText = replacement == null ? "" : replacement;
        String next = this.text.substring(0, clampedStart)
                + replacementText
                + this.text.substring(clampedEnd);
        int newCursor = clampedStart + replacementText.length();
        this.commitTextChange(next, newCursor, newCursor, clampedStart, clampedEnd, replacementText);
    }

    public void jumpToLineColumn(int line, int column) {
        this.expandFoldsForLine(line - 1);
        int target = this.cursorIndexForLineColumn(line, column);
        this.cursor = target;
        this.selectionCursor = target;
        this.ensureCursorVisible();
        this.notifyViewportChanged();
    }

    public void selectRange(int startInclusive, int endExclusive) {
        int start = Math.clamp(startInclusive, 0, this.text.length());
        int end = Math.clamp(endExclusive, 0, this.text.length());
        this.expandFoldsForLine(this.lineIndexForCursor(start));
        this.expandFoldsForLine(this.lineIndexForCursor(end));
        this.selectionCursor = start;
        this.cursor = end;
        this.ensureCursorVisible();
        this.notifyViewportChanged();
    }

    public String selectedText() {
        if (!this.hasSelection()) {
            return "";
        }
        int start = Math.min(this.cursor, this.selectionCursor);
        int end = Math.max(this.cursor, this.selectionCursor);
        return this.text.substring(start, end);
    }

    public String wordAtCaret() {
        if (this.text.isEmpty()) {
            return "";
        }
        int caret = Math.clamp(this.cursor, 0, this.text.length());
        int start;
        int end;

        if (caret < this.text.length() && this.isWordCharacter(this.text.charAt(caret))) {
            start = caret;
            end = caret + 1;
        } else if (caret > 0 && this.isWordCharacter(this.text.charAt(caret - 1))) {
            start = caret - 1;
            end = caret;
        } else {
            return "";
        }

        while (start > 0 && this.isWordCharacter(this.text.charAt(start - 1))) {
            start--;
        }
        while (end < this.text.length() && this.isWordCharacter(this.text.charAt(end))) {
            end++;
        }
        return this.text.substring(start, end);
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 320;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return 180;
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        this.refreshWrapLayoutIfNeeded();
        int left = this.x();
        int top = this.y();
        int right = left + this.width();
        int bottom = top + this.height();

        int innerLeft = this.innerLeft();
        int innerTop = this.innerTop();
        int innerRight = this.innerRight();
        int innerBottom = this.innerBottom();

        int contentLeft = this.contentLeft();
        int contentWidth = this.contentWidth();
        int visibleHeight = this.visibleHeight();
        int lineHeight = this.lineHeightWithSpacing();
        int gutterRight = this.gutterRight();
        int scrollbarLeft = this.scrollbarLeft();
        int scrollbarBottom = this.scrollbarBottom();

        context.fill(left, top, right, bottom, COLOR_BACKGROUND);
        this.drawRectBorder(context, left, top, right, bottom, COLOR_BORDER);

        context.fill(innerLeft, innerTop, gutterRight, scrollbarBottom, COLOR_GUTTER_BG);
        context.fill(gutterRight - 1, innerTop, gutterRight, scrollbarBottom, COLOR_GUTTER_BORDER);
        context.fill(scrollbarLeft, innerTop, this.scrollbarRight(), scrollbarBottom, 0x55202A38);
        this.renderScrollbarThumb(context, scrollbarLeft, innerTop, scrollbarBottom);
        this.renderHorizontalScrollbar(context, contentLeft, scrollbarLeft, this.horizontalScrollbarTop(), this.innerBottom());

        context.enableScissor(innerLeft, innerTop, innerRight, innerBottom);
        try {
            this.renderSelection(context, contentLeft, innerTop, contentWidth, visibleHeight, lineHeight);
            this.renderText(context, contentLeft, innerTop, contentWidth, visibleHeight, lineHeight);
            this.renderErrorUnderline(context, contentLeft, innerTop, contentWidth, visibleHeight, lineHeight);
            this.renderLineNumbers(context, innerTop, visibleHeight, lineHeight);
            this.renderGhostSuggestion(context, contentLeft, innerTop, contentWidth, visibleHeight, lineHeight);
            this.renderCursor(context, contentLeft, innerTop, visibleHeight, lineHeight);
            this.renderAutocompletePopup(context);
        } finally {
            context.disableScissor();
        }
    }

    @Override
    public boolean canFocus(UIComponent.FocusSource source) {
        return true;
    }

    private void drawRectBorder(OwoUIGraphics context, int left, int top, int right, int bottom, int borderColor) {
        context.fill(left, top, right, top + 1, borderColor);
        context.fill(left, bottom - 1, right, bottom, borderColor);
        context.fill(left, top, left + 1, bottom, borderColor);
        context.fill(right - 1, top, right, bottom, borderColor);
    }

    @Override
    public void onFocusGained(UIComponent.FocusSource source) {
        this.focused = true;
        super.onFocusGained(source);
    }

    @Override
    public void onFocusLost() {
        this.focused = false;
        this.draggingScrollbar = false;
        this.draggingHorizontalScrollbar = false;
        this.draggingSelection = false;
        super.onFocusLost();
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        ResolvedMouse mouse = this.resolveEventCoordinates(click.x(), click.y());
        double screenX = mouse.screenX();
        double screenY = mouse.screenY();
        boolean inside = this.isInsideEditor(screenX, screenY);
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !inside) {
            return super.onMouseDown(click, doubled);
        }

        boolean wasFocused = this.focused;
        double preservedScroll = this.scrollAmount;
        int beforeCursor = this.cursor;
        int beforeSelection = this.selectionCursor;

        if (this.focusHandler() != null) {
            this.focusHandler().focus(this, UIComponent.FocusSource.MOUSE_CLICK);
        } else {
            this.focused = true;
        }

        if (!wasFocused) {
            this.scrollAmount = preservedScroll;
            this.clampScrollAmount();
        }

        AutocompletePopupLayout popupLayout = this.autocompletePopupLayout();
        if (popupLayout != null && popupLayout.contains(screenX, screenY)) {
            this.clearAutocompleteOverlay();
        }

        if (this.isMouseOverScrollbar(screenX, screenY)) {
            this.draggingScrollbar = true;
            this.startScrollbarDrag(screenY);
            this.notifyViewportChanged();
            return true;
        }
        if (this.isMouseOverHorizontalScrollbar(screenX, screenY)) {
            this.draggingHorizontalScrollbar = true;
            this.startHorizontalScrollbarDrag(screenX);
            this.notifyViewportChanged();
            return true;
        }

        if (this.tryToggleFold(screenX, screenY)) {
            this.notifyViewportChanged();
            return true;
        }

        this.draggingSelection = true;
        int targetCursor = this.cursorForPoint(screenX, screenY);
        if (targetCursor < 0) {
            this.draggingSelection = false;
            return true;
        }

        if (doubled && !click.hasShiftDown()) {
            this.selectWordAt(targetCursor);
            this.draggingSelection = false;
            this.notifyViewportChanged();
            return true;
        }

        if (click.hasShiftDown()) {
            this.cursor = targetCursor;
        } else {
            this.cursor = targetCursor;
            this.selectionCursor = targetCursor;
        }

        boolean moved = this.cursor != beforeCursor || this.selectionCursor != beforeSelection;
        if (moved && !this.isCursorInViewport(this.cursor)) {
            this.ensureCursorVisible();
        } else if (!wasFocused && !moved) {
            this.scrollAmount = preservedScroll;
            this.clampScrollAmount();
        }

        this.notifyViewportChanged();
        return true;
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.draggingSelection = false;
            this.draggingScrollbar = false;
            this.draggingHorizontalScrollbar = false;
            ResolvedMouse mouse = this.resolveEventCoordinates(click.x(), click.y());
            return this.isInsideEditor(mouse.screenX(), mouse.screenY());
        }
        return super.onMouseUp(click);
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !this.focused) {
            return super.onMouseDrag(click, deltaX, deltaY);
        }

        ResolvedMouse mouse = this.resolveEventCoordinates(click.x(), click.y());
        double screenX = mouse.screenX();
        double screenY = mouse.screenY();

        if (this.draggingScrollbar) {
            this.updateScrollFromThumb(this.currentGuiMouseY());
            this.notifyViewportChanged();
            return true;
        }
        if (this.draggingHorizontalScrollbar) {
            this.updateHorizontalScrollFromThumb(this.currentGuiMouseX());
            this.notifyViewportChanged();
            return true;
        }

        if (!this.draggingSelection) {
            return super.onMouseDrag(click, deltaX, deltaY);
        }

        int targetCursor = this.cursorForPoint(screenX, screenY);
        if (targetCursor < 0) {
            return true;
        }
        this.cursor = targetCursor;
        this.ensureCursorVisible();
        this.notifyViewportChanged();
        return true;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        ResolvedMouse mouse = this.resolveEventCoordinates(mouseX, mouseY);
        double screenX = mouse.screenX();
        double screenY = mouse.screenY();
        if (!this.isInsideEditor(screenX, screenY)) {
            return super.onMouseScroll(mouseX, mouseY, amount);
        }
        if (amount == 0d) {
            return super.onMouseScroll(mouseX, mouseY, amount);
        }

        double before = this.scrollAmount;
        this.scrollAmount = before - amount * this.scrollRate();
        this.clampScrollAmount();
        if (Double.compare(before, this.scrollAmount) != 0) {
            this.notifyViewportChanged();
            return true;
        }
        if (this.canHorizontalScroll()) {
            double beforeHorizontal = this.horizontalScrollAmount;
            this.horizontalScrollAmount = beforeHorizontal - amount * this.horizontalScrollRate();
            this.clampHorizontalScrollAmount();
            if (Double.compare(beforeHorizontal, this.horizontalScrollAmount) != 0) {
                this.notifyViewportChanged();
                return true;
            }
        }
        // Let parent scroll containers handle wheel input when the editor itself cannot scroll
        // further (top or bottom boundary), so content below the raw text area remains reachable.
        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        if (!this.focused) {
            return super.onKeyPress(input);
        }

        boolean ctrl = input.hasControlDownWithQuirk()
                || (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = input.hasShiftDown();
        int keyCode = input.key();
        boolean hasAutocompletePopup = this.hasAutocompletePopupSelection();

        if (ctrl) {
            if (keyCode == GLFW.GLFW_KEY_Z) {
                if (shift) this.redo(); else this.undo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Y) {
                this.redo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_C) {
                this.copySelectionToClipboard(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_X) {
                this.copySelectionToClipboard(true);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V) {
                this.pasteClipboard();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_A) {
                this.cursor = this.text.length();
                this.selectionCursor = 0;
                this.ensureCursorVisible();
                this.notifyViewportChanged();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_R) return true;
            if (keyCode == GLFW.GLFW_KEY_SPACE && this.autocompleteRefreshRequested.getAsBoolean()) return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (hasAutocompletePopup || !this.ghostSuggestion.isBlank()) {
                this.autocompleteEntries = List.of();
                this.autocompleteSelected = -1;
                this.ghostSuggestion = "";
                return true;
            }
            return super.onKeyPress(input);
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (shift) {
                this.outdentSelection();
            } else {
                this.indentSelection();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (shift && hasAutocompletePopup) {
                if (this.autocompleteRequested.getAsBoolean()) {
                    return true;
                }
            }
            this.insertNewlineWithAutoIndent();
            return true;
        }
        if (hasAutocompletePopup && shift && keyCode == GLFW.GLFW_KEY_UP) {
            this.autocompletePreviousRequested.getAsBoolean();
            return true;
        }
        if (hasAutocompletePopup && shift && keyCode == GLFW.GLFW_KEY_DOWN) {
            this.autocompleteNextRequested.getAsBoolean();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.moveHorizontal(ctrl ? this.previousWordBoundary(this.cursor) : this.previousCodePoint(this.cursor), shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.moveHorizontal(ctrl ? this.nextWordBoundary(this.cursor) : this.nextCodePoint(this.cursor), shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            this.moveVertical(-1, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            this.moveVertical(1, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int target = ctrl ? 0 : this.lineStarts[this.lineIndexForCursor(this.cursor)];
            this.moveHorizontal(target, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int target = ctrl ? this.text.length() : this.lineEnd(this.lineIndexForCursor(this.cursor));
            this.moveHorizontal(target, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            this.moveVertical(-this.visibleLineCount(), shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.moveVertical(this.visibleLineCount(), shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            this.deleteBackward(ctrl);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            this.deleteForward(ctrl);
            return true;
        }
        return super.onKeyPress(input);
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        if (!this.focused || !input.isAllowedChatCharacter()) {
            return super.onCharTyped(input);
        }
        this.replaceSelectionOrInsert(input.codepointAsString());
        return true;
    }

    private void replaceSelectionOrInsert(String insert) {
        insert = this.applyVirtualCaretPadding(insert);
        int start = Math.min(this.cursor, this.selectionCursor);
        int end = Math.max(this.cursor, this.selectionCursor);
        this.replaceRange(start, end, insert);
    }

    private boolean hasAutocompletePopupSelection() {
        return !this.autocompleteEntries.isEmpty()
                && this.autocompleteSelected >= 0
                && this.autocompleteSelected < this.autocompleteEntries.size();
    }

    private void clearAutocompleteOverlay() {
        this.autocompleteEntries = List.of();
        this.autocompleteSelected = -1;
        this.ghostSuggestion = "";
    }

    private void insertNewlineWithAutoIndent() {
        int insertionPoint = Math.min(this.cursor, this.selectionCursor);
        int lineIndex = this.lineIndexForCursor(insertionPoint);
        int lineStart = this.lineStarts[lineIndex];
        int lineEnd = this.lineEnd(lineIndex);
        String line = this.text.substring(lineStart, lineEnd);
        int caretInLine = Math.clamp(insertionPoint - lineStart, 0, line.length());
        String beforeCaret = line.substring(0, caretInLine);
        String afterCaret = line.substring(caretInLine);

        int indentWidth = this.leadingIndentVisualWidth(line);
        String beforeTrimmed = beforeCaret.trim();
        String afterTrimmed = afterCaret.trim();
        if (this.endsWithOpeningSymbol(beforeTrimmed)) {
            indentWidth += INDENT_SIZE;
        }
        if (this.startsWithClosingSymbol(afterTrimmed)) {
            indentWidth = Math.max(0, indentWidth - INDENT_SIZE);
        }

        String indent = " ".repeat(Math.max(0, indentWidth));
        this.replaceSelectionOrInsert("\n" + indent);
    }

    private boolean endsWithOpeningSymbol(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        char tail = value.charAt(value.length() - 1);
        return tail == '{' || tail == '[' || tail == '(';
    }

    private boolean startsWithClosingSymbol(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        char head = value.charAt(0);
        return head == '}' || head == ']' || head == ')';
    }

    private void indentSelection() {
        if (!this.hasSelection()) {
            this.replaceSelectionOrInsert("    ");
            return;
        }

        int originalStart = Math.min(this.cursor, this.selectionCursor);
        int originalEnd = Math.max(this.cursor, this.selectionCursor);
        LineRange range = this.selectedLineRange(originalStart, originalEnd);
        if (range == null) {
            this.replaceSelectionOrInsert("    ");
            return;
        }

        StringBuilder builder = new StringBuilder(this.text);
        int offset = 0;
        int linesIndented = 0;
        for (int line = range.startLine(); line <= range.endLine(); line++) {
            int insertAt = this.lineStarts[line] + offset;
            builder.insert(insertAt, "    ");
            offset += 4;
            linesIndented++;
        }

        int newStart = originalStart + 4;
        int newEnd = originalEnd + (linesIndented * 4);
        if (this.cursor < this.selectionCursor) {
            this.commitTextChange(builder.toString(), newStart, newEnd);
        } else {
            this.commitTextChange(builder.toString(), newEnd, newStart);
        }
    }

    private void outdentSelection() {
        if (!this.hasSelection()) {
            int line = this.lineIndexForCursor(this.cursor);
            int lineStart = this.lineStarts[line];
            int removable = this.leadingIndentChars(this.text, lineStart);
            if (removable <= 0) {
                return;
            }
            StringBuilder builder = new StringBuilder(this.text);
            builder.delete(lineStart, lineStart + removable);
            int newCursor = this.cursor;
            if (newCursor > lineStart) {
                newCursor = Math.max(lineStart, newCursor - removable);
            }
            this.commitTextChange(builder.toString(), newCursor, newCursor);
            return;
        }

        int originalStart = Math.min(this.cursor, this.selectionCursor);
        int originalEnd = Math.max(this.cursor, this.selectionCursor);
        LineRange range = this.selectedLineRange(originalStart, originalEnd);
        if (range == null) {
            return;
        }

        String source = this.text;
        StringBuilder builder = new StringBuilder(source);
        int removedBeforeStart = 0;
        int removedBeforeEnd = 0;
        for (int line = range.endLine(); line >= range.startLine(); line--) {
            int lineStart = this.lineStarts[line];
            int removable = this.leadingIndentChars(source, lineStart);
            if (removable <= 0) {
                continue;
            }
            builder.delete(lineStart, lineStart + removable);
            if (lineStart < originalStart) {
                removedBeforeStart += removable;
            }
            if (lineStart < originalEnd) {
                removedBeforeEnd += removable;
            }
        }

        int newStart = Math.max(0, originalStart - removedBeforeStart);
        int newEnd = Math.max(newStart, originalEnd - removedBeforeEnd);
        if (this.cursor < this.selectionCursor) {
            this.commitTextChange(builder.toString(), newStart, newEnd);
        } else {
            this.commitTextChange(builder.toString(), newEnd, newStart);
        }
    }

    private int leadingIndentChars(String source, int lineStart) {
        int max = Math.min(source.length(), lineStart + 4);
        int index = lineStart;
        while (index < max) {
            char value = source.charAt(index);
            if (value != ' ' && value != '\t') {
                break;
            }
            index++;
        }
        return Math.max(0, index - lineStart);
    }

    private int leadingIndentVisualWidth(String line) {
        if (line == null || line.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == ' ') {
                width++;
                continue;
            }
            if (value == '\t') {
                width = ((width / INDENT_SIZE) + 1) * INDENT_SIZE;
                continue;
            }
            break;
        }
        return width;
    }

    private LineRange selectedLineRange(int start, int endExclusive) {
        if (start < 0 || endExclusive < 0 || start > this.text.length()) {
            return null;
        }
        int startLine = this.lineIndexForCursor(start);
        int endLine;
        if (endExclusive > start) {
            int endCursor = Math.clamp(endExclusive, 0, this.text.length());
            int endCursorLine = this.lineIndexForCursor(endCursor);
            if (endCursor == this.lineStarts[endCursorLine] && endCursorLine > startLine) {
                endLine = endCursorLine - 1;
            } else {
                endLine = endCursorLine;
            }
        } else {
            endLine = startLine;
        }
        return new LineRange(startLine, Math.max(startLine, endLine));
    }

    private void copySelectionToClipboard(boolean cut) {
        if (!this.hasSelection()) return;
        int start = Math.min(this.cursor, this.selectionCursor);
        int end = Math.max(this.cursor, this.selectionCursor);
        Minecraft.getInstance().keyboardHandler.setClipboard(this.text.substring(start, end));
        if (cut) this.replaceRange(start, end, "");
    }

    private void pasteClipboard() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) return;
        this.replaceSelectionOrInsert(clipboard);
    }

    private void deleteBackward(boolean byWord) {
        if (this.hasSelection()) {
            this.replaceSelectionOrInsert("");
            return;
        }
        if (this.cursor == 0) return;
        if (!byWord) {
            int lineIndex = this.lineIndexForCursor(this.cursor);
            int lineStart = this.lineStarts[lineIndex];
            if (this.cursor == lineStart) {
                int newlineIndex = lineStart - 1;
                if (newlineIndex >= 0 && this.text.charAt(newlineIndex) == '\n') {
                    this.replaceRange(newlineIndex, lineStart, "");
                    return;
                }
                return;
            }
        }
        int start = byWord ? this.previousWordBoundary(this.cursor) : this.previousCodePoint(this.cursor);
        this.replaceRange(start, this.cursor, "");
    }

    private void deleteForward(boolean byWord) {
        if (this.hasSelection()) {
            this.replaceSelectionOrInsert("");
            return;
        }
        if (this.cursor == this.text.length()) return;
        int end = byWord ? this.nextWordBoundary(this.cursor) : this.nextCodePoint(this.cursor);
        this.replaceRange(this.cursor, end, "");
    }

    private void moveHorizontal(int target, boolean keepSelection) {
        this.cursor = Math.clamp(target, 0, this.text.length());
        if (!keepSelection) this.selectionCursor = this.cursor;
        this.clearVirtualCaret();
        this.ensureCursorVisible();
        this.notifyViewportChanged();
    }

    private void moveVertical(int deltaLines, boolean keepSelection) {
        int currentLine = this.lineIndexForCursor(this.cursor);
        int currentVisibleLine = this.visibleLineForCursorIndex(this.cursor);
        int targetX = this.cursorLocalVisualX(currentVisibleLine, currentLine);
        int targetVisibleLine = Math.clamp(currentVisibleLine + deltaLines, 0, this.visibleToActualLine.length - 1);
        int target = this.cursorForVisibleLineAndX(targetVisibleLine, targetX);
        this.cursor = target;
        if (!keepSelection) this.selectionCursor = target;
        this.clearVirtualCaret();
        this.ensureCursorVisible();
        this.notifyViewportChanged();
    }

    private int cursorForVisibleLineAndX(int visibleLine, int targetX) {
        int lineIndex = this.actualLineForVisibleLine(visibleLine);
        if (!this.wordWrap) {
            return this.cursorForLineAndX(lineIndex, targetX);
        }
        int lineStart = this.lineStarts[lineIndex];
        int lineEnd = this.lineEnd(lineIndex);
        int segmentStart = this.visibleSegmentStart(visibleLine);
        int segmentEnd = this.visibleSegmentEnd(visibleLine);
        String line = this.text.substring(lineStart, lineEnd);
        if (line.isEmpty()) {
            return lineStart;
        }
        int boundedStart = Math.clamp(segmentStart, 0, line.length());
        int boundedEnd = Math.clamp(segmentEnd, boundedStart, line.length());
        String segment = line.substring(boundedStart, boundedEnd);
        return lineStart + boundedStart + this.rawIndexAtWidth(segment, Math.max(0, targetX));
    }

    private int cursorForLineAndX(int lineIndex, int targetX) {
        int start = this.lineStarts[lineIndex];
        int end = this.lineEnd(lineIndex);
        String line = this.text.substring(start, end);
        if (line.isEmpty()) return start;
        return start + this.rawIndexAtWidth(line, Math.max(0, targetX));
    }

    private int cursorLocalVisualX(int visibleLine, int lineIndex) {
        int lineStart = this.lineStarts[lineIndex];
        int lineEnd = this.lineEnd(lineIndex);
        String line = this.text.substring(lineStart, lineEnd);
        if (line.isEmpty()) {
            return 0;
        }
        int cursorLocal = Math.clamp(this.cursor - lineStart, 0, line.length());
        if (!this.wordWrap) {
            return this.rawTextWidth(line.substring(0, cursorLocal));
        }
        int segmentStart = this.visibleSegmentStart(visibleLine);
        int boundedStart = Math.clamp(segmentStart, 0, line.length());
        int boundedCursor = Math.max(boundedStart, cursorLocal);
        return this.rawTextWidth(line.substring(boundedStart, boundedCursor));
    }

    private int previousCodePoint(int index) {
        if (index == 0) return 0;
        return Character.offsetByCodePoints(this.text, index, -1);
    }

    private int nextCodePoint(int index) {
        if (index == this.text.length()) return this.text.length();
        return Character.offsetByCodePoints(this.text, index, 1);
    }

    private int previousWordBoundary(int index) {
        int cursorPos = Math.clamp(index, 0, this.text.length());
        while (cursorPos > 0 && Character.isWhitespace(this.text.charAt(cursorPos - 1))) {
            cursorPos--;
        }
        while (cursorPos > 0
                && !Character.isWhitespace(this.text.charAt(cursorPos - 1))
                && !this.isWordCharacter(this.text.charAt(cursorPos - 1))) {
            cursorPos--;
        }
        while (cursorPos > 0 && Character.isWhitespace(this.text.charAt(cursorPos - 1))) {
            cursorPos--;
        }
        while (cursorPos > 0 && this.isWordCharacter(this.text.charAt(cursorPos - 1))) {
            cursorPos--;
        }
        return cursorPos;
    }

    private int nextWordBoundary(int index) {
        int cursorPos = Math.clamp(index, 0, this.text.length());
        while (cursorPos < this.text.length() && Character.isWhitespace(this.text.charAt(cursorPos))) {
            cursorPos++;
        }
        while (cursorPos < this.text.length()
                && !Character.isWhitespace(this.text.charAt(cursorPos))
                && !this.isWordCharacter(this.text.charAt(cursorPos))) {
            cursorPos++;
        }
        while (cursorPos < this.text.length() && Character.isWhitespace(this.text.charAt(cursorPos))) {
            cursorPos++;
        }
        while (cursorPos < this.text.length() && this.isWordCharacter(this.text.charAt(cursorPos))) {
            cursorPos++;
        }
        return cursorPos;
    }

    private void selectWordAt(int cursorIndex) {
        if (this.text.isEmpty()) {
            this.cursor = 0;
            this.selectionCursor = 0;
            this.clearVirtualCaret();
            return;
        }

        int anchor = Math.clamp(cursorIndex, 0, this.text.length());
        int start;
        int end;

        boolean rightIsWord = anchor < this.text.length() && this.isWordCharacter(this.text.charAt(anchor));
        boolean leftIsWord = anchor > 0 && this.isWordCharacter(this.text.charAt(anchor - 1));

        if (rightIsWord || leftIsWord) {
            if (!rightIsWord) {
                start = this.previousCodePoint(anchor);
                end = anchor;
            } else {
                start = anchor;
                end = this.nextCodePoint(anchor);
            }
            while (start > 0 && this.isWordCharacter(this.text.charAt(start - 1))) {
                start = this.previousCodePoint(start);
            }
            while (end < this.text.length() && this.isWordCharacter(this.text.charAt(end))) {
                end = this.nextCodePoint(end);
            }
        } else if (anchor < this.text.length()) {
            start = anchor;
            end = this.nextCodePoint(anchor);
            char seed = this.text.charAt(anchor);
            if (Character.isWhitespace(seed)) {
                while (start > 0 && Character.isWhitespace(this.text.charAt(start - 1))) {
                    start = this.previousCodePoint(start);
                }
                while (end < this.text.length() && Character.isWhitespace(this.text.charAt(end))) {
                    end = this.nextCodePoint(end);
                }
            } else {
                while (start > 0 && this.isSymbolCharacter(this.text.charAt(start - 1))) {
                    start = this.previousCodePoint(start);
                }
                while (end < this.text.length() && this.isSymbolCharacter(this.text.charAt(end))) {
                    end = this.nextCodePoint(end);
                }
            }
        } else {
            start = this.previousCodePoint(anchor);
            end = anchor;
            if (start < 0) {
                start = 0;
            }
        }

        this.selectionCursor = Math.clamp(start, 0, this.text.length());
        this.cursor = Math.clamp(end, 0, this.text.length());
        this.clearVirtualCaret();
        this.ensureCursorVisible();
    }

    private boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == ':'
                || value == '-'
                || value == '.'
                || value == '/';
    }

    private boolean isSymbolCharacter(char value) {
        return !Character.isWhitespace(value) && !this.isWordCharacter(value);
    }

    private void commitTextChange(String nextText, int newCursor, int newSelection) {
        this.commitTextChange(nextText, newCursor, newSelection, -1, -1, "");
    }

    private void commitTextChange(
            String nextText,
            int newCursor,
            int newSelection,
            int editStart,
            int editEnd,
            String replacement
    ) {
        if (nextText == null) nextText = "";
        String previousText = this.text;
        int oldTextLength = this.text.length();
        boolean changed = !nextText.equals(this.text);
        int safeEditStart = editStart;
        int safeEditEnd = editEnd;
        String safeReplacement = replacement == null ? "" : replacement;
        if (changed && safeEditStart >= 0) {
            safeEditStart = Math.clamp(safeEditStart, 0, oldTextLength);
            safeEditEnd = Math.clamp(safeEditEnd, safeEditStart, oldTextLength);
        }
        String removedSegment = changed && safeEditStart >= 0
                ? previousText.substring(safeEditStart, safeEditEnd)
                : "";
        int startLineBefore = changed && safeEditStart >= 0 ? this.lineIndexForCursor(safeEditStart) : -1;
        int lineDelta = changed && safeEditStart >= 0
                ? this.newlineCount(safeReplacement) - this.newlineCount(removedSegment)
                : 0;
        boolean lineCountChanged = changed && safeEditStart >= 0
                && this.newlineCount(safeReplacement) != this.newlineCount(removedSegment);
        boolean foldStructuralEdit = changed && safeEditStart >= 0
                && (this.containsFoldStructuralChar(removedSegment) || this.containsFoldStructuralChar(safeReplacement));
        boolean autocompleteStructuralEdit = changed && safeEditStart >= 0
                && (foldStructuralEdit
                || lineCountChanged
                || this.containsAutocompleteStructuralChar(removedSegment)
                || this.containsAutocompleteStructuralChar(safeReplacement));
        if (changed) {
            this.undoHistory.addLast(this.captureState());
            this.trimHistory(this.undoHistory);
            this.redoHistory.clear();
            this.historyChanged.run();
        }
        this.text = nextText;
        this.lineDepthStartsDirty = true;
        this.syntaxLineCache.clear();
        this.cursor = Math.clamp(newCursor, 0, this.text.length());
        this.selectionCursor = Math.clamp(newSelection, 0, this.text.length());
        this.clearVirtualCaret();
        boolean incrementalApplied = changed
                && safeEditStart >= 0
                && safeEditEnd >= safeEditStart
                && this.rebuildLineStartsIncremental(oldTextLength, safeEditStart, safeEditEnd, safeReplacement);
        if (incrementalApplied) {
            if (!foldStructuralEdit) {
                if (lineDelta == 0) {
                    this.applyFoldVisibility();
                } else if (startLineBefore >= 0) {
                    this.shiftFoldRegionsAfterLine(startLineBefore, lineDelta);
                    this.applyFoldVisibility();
                } else {
                    this.rebuildFoldLayout();
                }
            } else {
                this.rebuildFoldLayout();
            }
        }
        if (!incrementalApplied) {
            this.rebuildLineStarts();
        }
        this.ensureCursorVisible();
        this.notifyViewportChanged();
        if (changed) {
            ChangeDelta delta = safeEditStart >= 0
                    ? new ChangeDelta(safeEditStart, safeEditEnd, safeReplacement, autocompleteStructuralEdit)
                    : ChangeDelta.fullReplace(oldTextLength, this.text);
            this.changedEvents.sink().onChanged(this.text, delta);
        } else {
            this.changedEvents.sink().onChanged(this.text, ChangeDelta.none(this.text.length()));
        }
    }

    private void restoreState(HistoryState state) {
        this.text = state.text();
        this.lineDepthStartsDirty = true;
        this.syntaxLineCache.clear();
        this.cursor = Math.clamp(state.cursor(), 0, this.text.length());
        this.selectionCursor = Math.clamp(state.selection(), 0, this.text.length());
        this.scrollAmount = state.scroll();
        this.rebuildLineStarts();
        this.clampScrollAmount();
        this.clampHorizontalScrollAmount();
        this.notifyViewportChanged();
    }

    private void trimHistory(ArrayDeque<HistoryState> history) {
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
        while (!history.isEmpty() && this.historyCharCount(history) > HISTORY_CHAR_BUDGET) {
            history.removeFirst();
        }
    }

    private int historyCharCount(ArrayDeque<HistoryState> history) {
        int total = 0;
        for (HistoryState state : history) {
            if (state != null && state.text() != null) {
                total += state.text().length();
                if (total > HISTORY_CHAR_BUDGET) {
                    return total;
                }
            }
        }
        return total;
    }

    private HistoryState captureState() {
        return new HistoryState(this.text, this.cursor, this.selectionCursor, this.scrollAmount);
    }

    private HistorySnapshot toHistorySnapshot(HistoryState state) {
        return new HistorySnapshot(state.text(), state.cursor(), state.selection(), state.scroll());
    }

    private HistoryState fromHistorySnapshot(HistorySnapshot snapshot) {
        String snapshotText = snapshot.text() == null ? "" : snapshot.text();
        int snapshotCursor = Math.clamp(snapshot.cursor(), 0, snapshotText.length());
        int snapshotSelection = Math.clamp(snapshot.selection(), 0, snapshotText.length());
        return new HistoryState(snapshotText, snapshotCursor, snapshotSelection, snapshot.scroll());
    }

    private void resetText(String value) {
        this.text = value;
        this.lineDepthStartsDirty = true;
        this.syntaxLineCache.clear();
        this.cursor = Math.clamp(this.cursor, 0, this.text.length());
        this.selectionCursor = Math.clamp(this.selectionCursor, 0, this.text.length());
        this.clearVirtualCaret();
        this.rebuildLineStarts();
        this.scrollAmount = 0;
        this.horizontalScrollAmount = 0;
        this.undoHistory.clear();
        this.redoHistory.clear();
        this.historyChanged.run();
    }

    private void renderScrollbarThumb(OwoUIGraphics context, int scrollbarLeft, int innerTop, int innerBottom) {
        int maxScroll = this.maxScroll();
        if (maxScroll == 0) return;
        int thumbHeight = this.currentThumbHeight();
        int travel = Math.max(1, this.visibleHeight() - thumbHeight);
        int thumbOffset = (int) Math.round((this.scrollAmount / maxScroll) * travel);
        int thumbTop = innerTop + thumbOffset;
        int thumbBottom = Math.min(innerBottom, thumbTop + thumbHeight);
        context.fill(scrollbarLeft, thumbTop, this.scrollbarRight(), thumbBottom, 0xAA9AA6B5);
    }

    private void renderHorizontalScrollbar(OwoUIGraphics context, int trackLeft, int trackRight, int trackTop, int trackBottom) {
        if (!this.canHorizontalScroll() || trackRight <= trackLeft) {
            return;
        }
        context.fill(trackLeft, trackTop, trackRight, trackBottom, 0x55202A38);
        int thumbLeft = this.currentHorizontalThumbLeft(trackLeft, trackRight);
        int thumbRight = Math.min(trackRight, thumbLeft + this.currentHorizontalThumbWidth(trackLeft, trackRight));
        context.fill(thumbLeft, trackTop, thumbRight, trackBottom, 0xAA9AA6B5);
    }

    private void renderGhostSuggestion(OwoUIGraphics context, int contentLeft, int top, int contentWidth, int visibleHeight, int lineHeight) {
        if (!this.focused || this.hasSelection() || this.ghostSuggestion.isBlank()) return;
        if (this.wordWrap || this.horizontalRenderOffset() > 0) return;
        int renderedScroll = this.renderedScroll();
        int cursorActualLine = this.lineIndexForCursor(this.cursor);
        int cursorLine = this.visibleLineForCursorIndex(this.cursor);
        int lineY = top + cursorLine * lineHeight - renderedScroll;
        if (lineY < top || lineY + lineHeight > top + visibleHeight) return;

        int lineStart = this.lineStarts[cursorActualLine];
        int lineEnd = this.lineEnd(cursorActualLine);
        String line = this.text.substring(lineStart, lineEnd);
        int cursorInLine = Math.clamp(this.cursor - lineStart, 0, line.length());
        String linePrefix = line.substring(0, cursorInLine);
        String lineSuffix = line.substring(cursorInLine);

        int prefixWidth = this.rawTextWidth(linePrefix);
        int textX = contentLeft + prefixWidth;
        int availableWidth = Math.max(0, contentWidth - prefixWidth - 2);
        if (availableWidth <= 0) return;

        String visibleGhost = this.trimToWidthRaw(this.ghostSuggestion, availableWidth);
        if (visibleGhost.isEmpty()) return;

        int ghostWidth = this.rawTextWidth(visibleGhost);
        int suffixWidth = Math.max(0, availableWidth - ghostWidth);
        String visibleSuffix = suffixWidth <= 0 ? "" : this.trimToWidthRaw(lineSuffix, suffixWidth);

        int clearBottom = lineY + this.scaledFontLineHeight();
        int clearRight = contentLeft + contentWidth;
        context.fill(textX, lineY, clearRight, clearBottom, COLOR_BACKGROUND_SOLID);
        this.drawRawText(context, visibleGhost, textX, lineY, COLOR_GHOST);
        if (!visibleSuffix.isEmpty()) {
            this.drawRawText(context, visibleSuffix, textX + ghostWidth, lineY, COLOR_TEXT);
        }
    }

    private void renderAutocompletePopup(OwoUIGraphics context) {
        AutocompletePopupLayout layout = this.autocompletePopupLayout();
        if (layout == null) {
            return;
        }
        int selected = layout.selected();
        int rowHeight = layout.rowHeight();
        int popupWidth = layout.width();
        int popupHeight = layout.height();
        int popupX = layout.x();
        int popupY = layout.y();

        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, COLOR_AUTOCOMPLETE_BG);
        context.fill(popupX, popupY, popupX + popupWidth, popupY + 1, COLOR_AUTOCOMPLETE_BORDER);
        context.fill(popupX, popupY + popupHeight - 1, popupX + popupWidth, popupY + popupHeight, COLOR_AUTOCOMPLETE_BORDER);
        context.fill(popupX, popupY, popupX + 1, popupY + popupHeight, COLOR_AUTOCOMPLETE_BORDER);
        context.fill(popupX + popupWidth - 1, popupY, popupX + popupWidth, popupY + popupHeight, COLOR_AUTOCOMPLETE_BORDER);

        int start = layout.start();
        int end = layout.end();
        int textWidth = popupWidth - (AUTOCOMPLETE_PADDING * 2);
        for (int index = start; index < end; index++) {
            int row = index - start;
            int rowTop = popupY + AUTOCOMPLETE_PADDING + (row * rowHeight);
            int rowBottom = rowTop + rowHeight;
            boolean isSelected = index == selected;
            context.fill(
                    popupX + 1,
                    rowTop - 1,
                    popupX + popupWidth - 1,
                    rowBottom,
                    isSelected ? COLOR_AUTOCOMPLETE_SELECTED_BG : COLOR_AUTOCOMPLETE_ROW_BG
            );

            AutocompletePopupEntry entry = this.autocompleteEntries.get(index);
            String label = entry.label();
            String detail = entry.detail();
            int textX = popupX + AUTOCOMPLETE_PADDING;
            int textY = rowTop + Math.max(0, (rowHeight - this.scaledFontLineHeight()) / 2);
            int baseColor = isSelected ? COLOR_AUTOCOMPLETE_SELECTED_TEXT : COLOR_AUTOCOMPLETE_TEXT;
            if (detail.isBlank()) {
                String visible = this.trimToWidthRaw(label, textWidth);
                if (!visible.isEmpty()) {
                    this.drawRawText(context, visible, textX, textY, baseColor);
                }
                continue;
            }
            String labelVisible = this.trimToWidthRaw(label, textWidth);
            if (labelVisible.isEmpty()) {
                continue;
            }
            this.drawRawText(context, labelVisible, textX, textY, baseColor);
            int usedWidth = this.rawTextWidth(labelVisible);
            int detailRoom = textWidth - usedWidth - this.rawTextWidth("  ");
            if (detailRoom <= 0) {
                continue;
            }
            String detailVisible = this.trimToWidthRaw(detail, detailRoom);
            if (detailVisible.isEmpty()) {
                continue;
            }
            this.drawRawText(
                    context,
                    detailVisible,
                    textX + usedWidth + this.rawTextWidth("  "),
                    textY,
                    isSelected ? COLOR_AUTOCOMPLETE_SELECTED_TEXT : COLOR_AUTOCOMPLETE_DETAIL
            );
        }
    }

    private AutocompletePopupLayout autocompletePopupLayout() {
        if (!this.focused || this.autocompleteEntries.isEmpty()) {
            return null;
        }
        if (this.autocompleteSelected < 0 || this.autocompleteSelected >= this.autocompleteEntries.size()) {
            return null;
        }
        int contentLeft = this.contentLeft();
        int top = this.innerTop();
        int innerRight = this.innerRight();
        int innerBottom = this.innerBottom();
        int visibleHeight = this.visibleHeight();
        int lineHeight = this.lineHeightWithSpacing();
        CaretAnchor anchor = this.resolveCaretAnchor(contentLeft, top, visibleHeight, lineHeight);
        if (anchor == null) {
            return null;
        }

        int selected = Math.clamp(this.autocompleteSelected, 0, this.autocompleteEntries.size() - 1);
        int rowHeight = this.autocompleteRowHeight();
        int visibleRows = Math.min(AUTOCOMPLETE_MAX_ROWS, this.autocompleteEntries.size());
        int popupHeight = (visibleRows * rowHeight) + (AUTOCOMPLETE_PADDING * 2);
        int maxWidth = Math.max(AUTOCOMPLETE_MIN_WIDTH, innerRight - contentLeft - 4);
        int popupWidth = this.autocompletePopupWidth(maxWidth);

        int popupX = anchor.x();
        int popupY = anchor.y() + lineHeight + 1;
        if (popupY + popupHeight > innerBottom) {
            popupY = anchor.y() - popupHeight - 1;
        }
        popupX = Math.clamp(popupX, contentLeft, Math.max(contentLeft, innerRight - popupWidth));
        popupY = Math.clamp(popupY, top, Math.max(top, innerBottom - popupHeight));

        int start = this.autocompleteWindowStart(selected, this.autocompleteEntries.size(), visibleRows);
        int end = Math.min(this.autocompleteEntries.size(), start + visibleRows);
        return new AutocompletePopupLayout(
                popupX,
                popupY,
                popupWidth,
                popupHeight,
                rowHeight,
                visibleRows,
                selected,
                start,
                end
        );
    }

    private int autocompletePopupWidth(int maxWidth) {
        int clampedMaxWidth = Math.max(AUTOCOMPLETE_MIN_WIDTH, maxWidth);
        int preferred = AUTOCOMPLETE_MIN_WIDTH;
        int limit = Math.min(this.autocompleteEntries.size(), AUTOCOMPLETE_MAX_ROWS);
        for (int index = 0; index < limit; index++) {
            AutocompletePopupEntry entry = this.autocompleteEntries.get(index);
            String label = entry.label();
            String detail = entry.detail();
            int lineWidth = this.rawTextWidth(detail.isBlank() ? label : (label + "  " + detail));
            preferred = Math.max(preferred, lineWidth + (AUTOCOMPLETE_PADDING * 2) + 2);
        }
        return Math.clamp(preferred, AUTOCOMPLETE_MIN_WIDTH, clampedMaxWidth);
    }

    private int autocompleteRowHeight() {
        return Math.max(12, this.scaledFontLineHeight() + 4);
    }

    private int autocompleteWindowStart(int selected, int size, int visibleRows) {
        if (size <= visibleRows) {
            return 0;
        }
        int start = Math.max(0, selected - (visibleRows / 2));
        int maxStart = Math.max(0, size - visibleRows);
        return Math.clamp(start, 0, maxStart);
    }

    private CaretAnchor resolveCaretAnchor(int contentLeft, int top, int visibleHeight, int lineHeight) {
        int renderedScroll = this.renderedScroll();
        int lineIndex = this.lineIndexForCursor(this.cursor);
        int visibleLine = this.visibleLineForCursorIndex(this.cursor);
        int lineY = top + visibleLine * lineHeight - renderedScroll;
        if (lineY + lineHeight < top || lineY > top + visibleHeight) {
            return null;
        }
        int cursorX = this.cursorXForLine(contentLeft, visibleLine, lineIndex);
        return new CaretAnchor(cursorX, lineY);
    }

    private void renderLineNumbers(OwoUIGraphics context, int top, int visibleHeight, int lineHeight) {
        int renderedScroll = this.renderedScroll();
        int activeVisibleLine = this.visibleLineForCursorIndex(this.cursor);
        int gutterRight = this.gutterRight() - GUTTER_LINE_RIGHT_INSET;
        int foldMarkerLeft = this.foldMarkerLeft();
        int foldMarkerRight = foldMarkerLeft + FOLD_MARKER_WIDTH;
        int minNumberX = foldMarkerRight + GUTTER_FOLD_GAP + 1;
        for (int visibleLine = 0; visibleLine < this.visibleToActualLine.length; visibleLine++) {
            int lineIndex = this.actualLineForVisibleLine(visibleLine);
            int lineY = top + visibleLine * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) continue;
            if (this.wordWrap && this.visibleSegmentStart(visibleLine) > 0) {
                continue;
            }
            int lineNo = lineIndex + 1;
            int color = lineNo == this.errorLine ? COLOR_GUTTER_ERROR : (visibleLine == activeVisibleLine ? COLOR_GUTTER_ACTIVE : COLOR_GUTTER_LINE);
            String lineNoText = Integer.toString(lineNo);
            int textWidth = this.rawTextWidth(lineNoText);
            int x = minNumberX;
            if (x + textWidth > gutterRight) {
                x = Math.max(minNumberX, gutterRight - textWidth);
            }
            this.drawRawText(context, lineNoText, x, lineY, color);

            FoldRegion foldRegion = this.foldRegionStartingAt(lineIndex);
            if (foldRegion != null) {
                int markerColor = visibleLine == activeVisibleLine ? COLOR_FOLD_ACTIVE : COLOR_FOLD_MARKER;
                String marker = foldRegion.collapsed ? "+" : "-";
                context.fill(foldMarkerLeft, lineY - 1, foldMarkerRight, lineY + lineHeight - 1, 0x332A3444);
                this.drawRawText(context, marker, foldMarkerLeft + 1, lineY, markerColor);
            }
        }
    }

    private void renderText(OwoUIGraphics context, int contentLeft, int top, int contentWidth, int visibleHeight, int lineHeight) {
        int renderedScroll = this.renderedScroll();
        int horizontalOffset = this.horizontalRenderOffset();
        int[] depthStarts = this.lineDepthStartsForRender();
        int syntaxBudget = MAX_SYNTAX_RENDER_BUDGET_CHARS;
        int lastBudgetedLine = -1;
        for (int visibleLine = 0; visibleLine < this.visibleToActualLine.length; visibleLine++) {
            int lineIndex = this.actualLineForVisibleLine(visibleLine);
            int lineY = top + visibleLine * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) continue;
            int start = this.lineStarts[lineIndex];
            int end = this.lineEnd(lineIndex);
            String line = this.text.substring(start, end);
            int segmentStart = this.visibleSegmentStart(visibleLine);
            int segmentEnd = this.visibleSegmentEnd(visibleLine);
            int boundedSegmentStart = Math.clamp(segmentStart, 0, line.length());
            int boundedSegmentEnd = Math.clamp(segmentEnd, boundedSegmentStart, line.length());
            String segment = line.substring(boundedSegmentStart, boundedSegmentEnd);
            this.renderIndentGuides(context, contentLeft - horizontalOffset, lineY, lineHeight, this.wordWrap ? segment : line);
            FoldRegion collapsedRegion = this.foldRegionStartingAt(lineIndex);
            if (collapsedRegion != null && collapsedRegion.collapsed && boundedSegmentStart == 0) {
                this.renderCollapsedLine(context, contentLeft - horizontalOffset, lineY, contentWidth, line, collapsedRegion);
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            boolean syntaxEnabledForLine = syntaxBudget > 0 || lineIndex == lastBudgetedLine;
            if (syntaxEnabledForLine) {
                int baseDepth = depthStarts == null || depthStarts.length == 0
                        ? 0
                        : depthStarts[Math.clamp(lineIndex, 0, depthStarts.length - 1)];
                if (this.wordWrap) {
                    this.renderLineSyntaxSegment(
                            context,
                            contentLeft,
                            lineY,
                            contentWidth,
                            lineIndex,
                            line,
                            baseDepth,
                            boundedSegmentStart,
                            boundedSegmentEnd
                    );
                } else if (this.canHorizontalScroll()) {
                    this.renderLineSyntaxWindow(
                            context,
                            contentLeft,
                            lineY,
                            contentWidth,
                            lineIndex,
                            line,
                            baseDepth,
                            horizontalOffset
                    );
                } else {
                    this.renderLineSyntax(context, contentLeft, lineY, contentWidth, lineIndex, line, baseDepth);
                }
                if (lineIndex != lastBudgetedLine) {
                    syntaxBudget -= Math.min(syntaxBudget, line.length());
                    lastBudgetedLine = lineIndex;
                }
            } else {
                if (this.wordWrap) {
                    if (!segment.isEmpty()) {
                        this.drawRawText(context, segment, contentLeft, lineY, COLOR_TEXT);
                    }
                } else if (this.canHorizontalScroll()) {
                    String source = line.substring(Math.clamp(this.rawIndexAtWidth(line, horizontalOffset), 0, line.length()));
                    String visible = this.trimToWidthRaw(source, contentWidth);
                    if (!visible.isEmpty()) {
                        this.drawRawText(context, visible, contentLeft, lineY, COLOR_TEXT);
                    }
                } else {
                    String renderSource = line.length() > MAX_RENDER_LINE_CHARS ? line.substring(0, MAX_RENDER_LINE_CHARS) : line;
                    String visible = this.trimToWidthRaw(renderSource, contentWidth);
                    if (!visible.isEmpty()) this.drawRawText(context, visible, contentLeft, lineY, COLOR_TEXT);
                }
            }
        }
    }

    private void renderIndentGuides(OwoUIGraphics context, int contentLeft, int lineY, int lineHeight, String line) {
        if (line == null || line.isEmpty()) {
            return;
        }

        List<Integer> guideOffsets = this.indentGuideOffsets(line);
        if (guideOffsets.isEmpty()) {
            return;
        }

        int top = lineY + 1;
        int bottom = lineY + Math.max(2, lineHeight - 1);
        for (int offset : guideOffsets) {
            int guideX = contentLeft + Math.max(0, offset - 1);
            context.fill(guideX, top, guideX + 1, bottom, COLOR_INDENT_GUIDE);
        }
    }

    private List<Integer> indentGuideOffsets(String line) {
        List<Integer> offsets = new ArrayList<>();
        int visualColumn = 0;
        int boundary = 0;
        while (boundary < line.length()) {
            int codePoint = line.codePointAt(boundary);
            int nextBoundary = boundary + Character.charCount(codePoint);
            char value = (char) codePoint;
            if (value == ' ') {
                visualColumn++;
                if (visualColumn % INDENT_SIZE == 0) {
                    offsets.add(this.rawTextWidth(line.substring(0, nextBoundary)));
                }
                boundary = nextBoundary;
                continue;
            }
            if (value == '\t') {
                visualColumn = ((visualColumn / INDENT_SIZE) + 1) * INDENT_SIZE;
                offsets.add(this.rawTextWidth(line.substring(0, nextBoundary)));
                boundary = nextBoundary;
                continue;
            }
            break;
        }
        return offsets;
    }

    private void renderLineSyntax(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            int lineIndex,
            String line,
            int baseDepth
    ) {
        List<SyntaxRun> runs = this.syntaxRunsForLine(lineIndex, line, baseDepth);
        int drawn = 0;
        for (int index = 0; index < runs.size() && drawn < availableWidth; index++) {
            SyntaxRun run = runs.get(index);
            drawn += this.renderSyntaxRun(context, x + drawn, y, availableWidth - drawn, run);
        }
    }

    private void renderLineSyntaxWindow(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            int lineIndex,
            String line,
            int baseDepth,
            int skipPixels
    ) {
        List<SyntaxRun> runs = this.syntaxRunsForLine(lineIndex, line, baseDepth);
        int skip = Math.max(0, skipPixels);
        int drawn = 0;
        for (int index = 0; index < runs.size() && drawn < availableWidth; index++) {
            SyntaxRun run = runs.get(index);
            String token = run.text();
            if (token.isEmpty()) {
                continue;
            }
            int tokenWidth = this.rawTextWidth(token);
            if (skip >= tokenWidth) {
                skip -= tokenWidth;
                continue;
            }
            if (skip > 0) {
                int clipStart = Math.clamp(this.rawIndexAtWidth(token, skip), 0, token.length());
                token = token.substring(clipStart);
                skip = 0;
                if (token.isEmpty()) {
                    continue;
                }
            }
            int rendered = this.renderSyntaxRun(
                    context,
                    x + drawn,
                    y,
                    availableWidth - drawn,
                    new SyntaxRun(run.kind(), token, run.color())
            );
            if (rendered <= 0) {
                break;
            }
            drawn += rendered;
        }
    }

    private void renderLineSyntaxSegment(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            int lineIndex,
            String line,
            int baseDepth,
            int segmentStart,
            int segmentEnd
    ) {
        int safeStart = Math.clamp(segmentStart, 0, line.length());
        int safeEnd = Math.clamp(segmentEnd, safeStart, line.length());
        if (safeEnd <= safeStart) {
            return;
        }
        List<SyntaxRun> runs = this.syntaxRunsForLine(lineIndex, line, baseDepth);
        int cursor = 0;
        int drawn = 0;
        for (int index = 0; index < runs.size() && drawn < availableWidth; index++) {
            SyntaxRun run = runs.get(index);
            String token = run.text();
            int runStart = cursor;
            int runEnd = cursor + token.length();
            cursor = runEnd;
            if (runEnd <= safeStart) {
                continue;
            }
            if (runStart >= safeEnd) {
                break;
            }
            int localStart = Math.max(0, safeStart - runStart);
            int localEnd = Math.min(token.length(), safeEnd - runStart);
            if (localEnd <= localStart) {
                continue;
            }
            String segmentToken = token.substring(localStart, localEnd);
            int rendered = this.renderSyntaxRun(
                    context,
                    x + drawn,
                    y,
                    availableWidth - drawn,
                    new SyntaxRun(run.kind(), segmentToken, run.color())
            );
            if (rendered <= 0) {
                break;
            }
            drawn += rendered;
        }
    }

    private int renderSyntaxRun(OwoUIGraphics context, int x, int y, int availableWidth, SyntaxRun run) {
        if (run.kind() == SyntaxRunKind.PLAIN) {
            return this.drawSyntaxToken(context, x, y, availableWidth, run.text(), run.color());
        }
        if (run.kind() == SyntaxRunKind.STRING) {
            return this.renderStringToken(context, x, y, availableWidth, run.text());
        }
        if (run.kind() == SyntaxRunKind.NUMERIC) {
            return this.renderNumericToken(context, x, y, availableWidth, run.text());
        }
        return this.drawHexColorToken(context, x, y, availableWidth, run.text(), run.color());
    }

    private List<SyntaxRun> syntaxRunsForLine(int lineIndex, String line, int baseDepth) {
        SyntaxLineCache cached = this.syntaxLineCache.get(lineIndex);
        if (cached != null && cached.matches(line, baseDepth)) {
            return cached.runs();
        }
        List<SyntaxRun> runs = this.computeSyntaxRuns(line, baseDepth);
        this.syntaxLineCache.put(lineIndex, new SyntaxLineCache(line, baseDepth, runs));
        return runs;
    }

    private List<SyntaxRun> computeSyntaxRuns(String line, int baseDepth) {
        List<SyntaxRun> runs = new ArrayList<>();
        int cursorIndex = 0;
        int depth = Math.max(0, baseDepth);
        while (cursorIndex < line.length()) {
            char c = line.charAt(cursorIndex);
            int next;

            if (Character.isWhitespace(c)) {
                next = this.scanWhitespace(line, cursorIndex + 1);
                this.addSyntaxRun(runs, SyntaxRunKind.PLAIN, line.substring(cursorIndex, next), COLOR_TEXT);
                cursorIndex = next;
                continue;
            }

            if (c == '"' || c == '\'') {
                int end = this.findStringEnd(line, cursorIndex + 1, c);
                next = end < 0 ? line.length() : end + 1;
                String token = line.substring(cursorIndex, next);
                boolean keyToken = this.isKeyToken(line, next);
                boolean idToken = !keyToken && this.isResourceIdentifierString(token);
                int hexColor = keyToken ? -1 : this.parseHexColorFromToken(token);

                if (keyToken) {
                    this.addSyntaxRun(runs, SyntaxRunKind.PLAIN, token, this.keyColorForDepth(depth));
                } else if (hexColor != -1) {
                    this.addSyntaxRun(runs, SyntaxRunKind.HEX, token, hexColor);
                } else if (idToken) {
                    this.addSyntaxRun(runs, SyntaxRunKind.PLAIN, token, COLOR_IDENTIFIER);
                } else {
                    this.addSyntaxRun(runs, SyntaxRunKind.STRING, token, 0);
                }
                cursorIndex = next;
                continue;
            }

            if (this.isNumberStart(line, cursorIndex)) {
                next = this.scanNumber(line, cursorIndex);
                this.addSyntaxRun(runs, SyntaxRunKind.NUMERIC, line.substring(cursorIndex, next), 0);
                cursorIndex = next;
                continue;
            }

            if (this.isWordStart(c)) {
                next = this.scanWord(line, cursorIndex + 1);
                String word = line.substring(cursorIndex, next);
                int hexColor = this.parseHexColorFromToken(word);
                if (hexColor != -1) {
                    this.addSyntaxRun(runs, SyntaxRunKind.HEX, word, hexColor);
                } else if (this.isNumericWord(word)) {
                    this.addSyntaxRun(runs, SyntaxRunKind.NUMERIC, word, 0);
                } else {
                    int color = this.resolveWordColor(line, word, cursorIndex, next, depth);
                    this.addSyntaxRun(runs, SyntaxRunKind.PLAIN, word, color);
                }
                cursorIndex = next;
                continue;
            }

            if (c == '}' || c == ']') {
                depth = Math.max(0, depth - 1);
            }
            this.addSyntaxRun(
                    runs,
                    SyntaxRunKind.PLAIN,
                    line.substring(cursorIndex, cursorIndex + 1),
                    this.punctuationColor(c, depth)
            );
            if (c == '{' || c == '[') {
                depth++;
            }
            cursorIndex++;
        }
        return runs;
    }

    private void addSyntaxRun(List<SyntaxRun> runs, SyntaxRunKind kind, String text, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!runs.isEmpty()) {
            SyntaxRun last = runs.get(runs.size() - 1);
            if (last.kind() == kind && last.color() == color && kind == SyntaxRunKind.PLAIN) {
                runs.set(runs.size() - 1, new SyntaxRun(kind, last.text() + text, color));
                return;
            }
        }
        runs.add(new SyntaxRun(kind, text, color));
    }

    private void renderSelection(OwoUIGraphics context, int contentLeft, int top, int contentWidth, int visibleHeight, int lineHeight) {
        if (!this.hasSelection()) return;
        int horizontalOffset = this.horizontalRenderOffset();
        int renderedScroll = this.renderedScroll();
        int selectionStart = Math.min(this.cursor, this.selectionCursor);
        int selectionEnd = Math.max(this.cursor, this.selectionCursor);
        for (int visibleLine = 0; visibleLine < this.visibleToActualLine.length; visibleLine++) {
            int lineIndex = this.actualLineForVisibleLine(visibleLine);
            int lineStart = this.lineStarts[lineIndex];
            int lineEnd = this.lineEnd(lineIndex);
            if (selectionEnd < lineStart || selectionStart > lineEnd) continue;
            int lineY = top + visibleLine * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) continue;
            int segmentStart = this.visibleSegmentStart(visibleLine);
            int segmentEnd = this.visibleSegmentEnd(visibleLine);
            int segmentAbsoluteStart = lineStart + segmentStart;
            int segmentAbsoluteEnd = lineStart + segmentEnd;
            int from = Math.max(selectionStart, segmentAbsoluteStart);
            int to = Math.min(selectionEnd, segmentAbsoluteEnd);
            if (from >= to) {
                continue;
            }
            String line = this.text.substring(lineStart, lineEnd);
            int startOffset = Math.clamp(from - lineStart, 0, line.length());
            int endOffset = Math.clamp(to - lineStart, startOffset, line.length());
            int prefixStart = this.wordWrap ? segmentStart : 0;
            int startX = contentLeft - horizontalOffset + this.rawTextWidth(line.substring(prefixStart, startOffset));
            int endX = contentLeft - horizontalOffset + this.rawTextWidth(line.substring(prefixStart, endOffset));
            if (selectionEnd > segmentAbsoluteEnd) {
                endX = Math.min(contentLeft + contentWidth, Math.max(endX + 2, startX + 1));
            }
            startX = Math.max(contentLeft, startX);
            endX = Math.min(contentLeft + contentWidth, endX);
            if (endX <= startX) endX = startX + 1;
            context.fill(startX, lineY - 1, endX, lineY + this.scaledFontLineHeight() + 1, COLOR_SELECTION);
        }
    }

    private void renderCursor(OwoUIGraphics context, int contentLeft, int top, int visibleHeight, int lineHeight) {
        if (!this.focused || !this.shouldBlinkCursor()) return;
        int renderedScroll = this.renderedScroll();
        int lineIndex = this.lineIndexForCursor(this.cursor);
        int visibleLine = this.visibleLineForCursorIndex(this.cursor);
        int lineY = top + visibleLine * lineHeight - renderedScroll;
        if (lineY + lineHeight < top || lineY > top + visibleHeight) return;
        int cursorX = this.cursorXForLine(contentLeft, visibleLine, lineIndex);
        context.fill(cursorX, lineY - 1, cursorX + CURSOR_WIDTH, lineY + this.scaledFontLineHeight() + 1, COLOR_CURSOR);
    }

    private void renderErrorUnderline(
            OwoUIGraphics context,
            int contentLeft,
            int top,
            int contentWidth,
            int visibleHeight,
            int lineHeight
    ) {
        if (this.errorLine <= 0) {
            return;
        }
        int lineIndex = this.errorLine - 1;
        if (lineIndex < 0 || lineIndex >= this.lineStarts.length) {
            return;
        }
        if (lineIndex < this.hiddenLines.length && this.hiddenLines[lineIndex]) {
            return;
        }

        int lineStart = this.lineStarts[lineIndex];
        int lineEnd = this.lineEnd(lineIndex);
        String line = this.text.substring(lineStart, lineEnd);
        int errorStart = Math.clamp(this.errorColumn > 0 ? this.errorColumn - 1 : 0, 0, line.length());
        int errorEnd = Math.clamp(errorStart + Math.max(1, this.errorLength), errorStart, line.length());
        boolean markLineEnd = errorStart >= line.length();

        int renderedScroll = this.renderedScroll();
        int horizontalOffset = this.horizontalRenderOffset();
        int underlineHeight = Math.max(1, UiFactory.scaledPixels(1));

        for (int visibleLine = 0; visibleLine < this.visibleToActualLine.length; visibleLine++) {
            if (this.actualLineForVisibleLine(visibleLine) != lineIndex) {
                continue;
            }
            int lineY = top + visibleLine * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) {
                continue;
            }
            int segmentStart = this.visibleSegmentStart(visibleLine);
            int segmentEnd = this.visibleSegmentEnd(visibleLine);
            int from = Math.max(errorStart, segmentStart);
            int to = Math.min(errorEnd, segmentEnd);
            int prefixStart = this.wordWrap ? segmentStart : 0;

            int underlineY = lineY + this.scaledFontLineHeight() + 1;
            if (from < to) {
                int startX = contentLeft - horizontalOffset + this.rawTextWidth(line.substring(prefixStart, from));
                int endX = contentLeft - horizontalOffset + this.rawTextWidth(line.substring(prefixStart, to));
                startX = Math.max(contentLeft, startX);
                endX = Math.min(contentLeft + contentWidth, endX);
                if (endX <= startX) {
                    endX = Math.min(contentLeft + contentWidth, startX + Math.max(2, UiFactory.scaledPixels(2)));
                }
                context.fill(startX, underlineY, endX, underlineY + underlineHeight, COLOR_ERROR_UNDERLINE);
                continue;
            }

            if (markLineEnd && segmentEnd == line.length()) {
                int caretX = contentLeft - horizontalOffset + this.rawTextWidth(line.substring(prefixStart, line.length()));
                int startX = Math.max(contentLeft, Math.min(contentLeft + contentWidth, caretX));
                int endX = Math.min(contentLeft + contentWidth, startX + Math.max(2, UiFactory.scaledPixels(2)));
                context.fill(startX, underlineY, endX, underlineY + underlineHeight, COLOR_ERROR_UNDERLINE);
            }
        }
    }

    private int cursorXForLine(int contentLeft, int visibleLine, int lineIndex) {
        int lineStart = this.lineStarts[lineIndex];
        int lineEnd = this.lineEnd(lineIndex);
        String line = this.text.substring(lineStart, lineEnd);
        int offset = this.horizontalRenderOffset();
        int localCursor = Math.clamp(this.cursor - lineStart, 0, line.length());
        int segmentStart = this.wordWrap ? this.visibleSegmentStart(visibleLine) : 0;
        int clampedSegmentStart = Math.clamp(segmentStart, 0, localCursor);
        int cursorX = contentLeft - offset + this.rawTextWidth(line.substring(clampedSegmentStart, localCursor));
        if (this.shouldUseVirtualCaretX(lineIndex, lineEnd)) {
            int clampedVirtualX = Math.clamp(this.virtualCaretLocalX, 0, Math.max(0, this.contentWidth() - 1));
            return contentLeft + clampedVirtualX;
        }
        return cursorX;
    }

    private boolean shouldUseVirtualCaretX(int lineIndex, int lineEnd) {
        return this.virtualCaretLine == lineIndex
                && this.virtualCaretLocalX >= 0
                && !this.hasSelection()
                && this.cursor == lineEnd;
    }

    private int cursorForPoint(double mouseX, double mouseY) {
        if (this.visibleToActualLine.length == 0) {
            return 0;
        }

        double clampedMouseY = Math.clamp(mouseY, this.innerTop(), this.scrollbarBottom());
        int renderedScroll = this.renderedScroll();
        int lineHeight = this.lineHeightWithSpacing();
        int visibleLine;
        if (clampedMouseY == this.innerTop()) {
            visibleLine = 0;
        } else if (clampedMouseY == this.innerBottom()) {
            visibleLine = this.visibleToActualLine.length - 1;
        } else {
            int localY = (int) Math.floor(clampedMouseY - this.innerTop() + renderedScroll);
            int nearestRow = (int) Math.floor((localY + (lineHeight / 2.0d)) / lineHeight);
            visibleLine = Math.clamp(nearestRow, 0, this.visibleToActualLine.length - 1);
        }
        return this.cursorForPointOnVisibleLine(mouseX, visibleLine);
    }

    private int cursorForPointOnVisibleLine(double mouseX, int visibleLine) {
        int lineIndex = this.actualLineForVisibleLine(visibleLine);
        int lineStart = this.lineStarts[lineIndex];
        int lineEnd = this.lineEnd(lineIndex);
        String line = this.text.substring(lineStart, lineEnd);
        int segmentStart = this.visibleSegmentStart(visibleLine);
        int segmentEnd = this.visibleSegmentEnd(visibleLine);
        int horizontalOffset = this.horizontalRenderOffset();
        int endOfLineTolerance = this.endOfLineClickTolerance();
        double localX = mouseX - this.contentLeft() + horizontalOffset;
        if (localX < -endOfLineTolerance) {
            this.clearVirtualCaret();
            return this.wordWrap ? (lineStart + segmentStart) : lineStart;
        }

        if (line.isEmpty()) {
            this.clearVirtualCaret();
            return lineStart;
        }
        int boundedStart = Math.clamp(segmentStart, 0, line.length());
        int boundedEnd = Math.clamp(segmentEnd, boundedStart, line.length());
        String segment = this.wordWrap ? line.substring(boundedStart, boundedEnd) : line;
        if (localX <= 0d) {
            this.clearVirtualCaret();
            return this.wordWrap ? (lineStart + boundedStart) : lineStart;
        }
        int lineWidth = this.rawTextWidth(segment);
        if (localX > lineWidth + endOfLineTolerance) {
            this.clearVirtualCaret();
            return this.wordWrap ? (lineStart + boundedEnd) : lineEnd;
        }
        if (localX > lineWidth) {
            return this.wordWrap ? (lineStart + boundedEnd) : lineEnd;
        }
        this.clearVirtualCaret();
        return lineStart + boundedStart + this.rawIndexAtWidth(segment, localX);
    }

    private void startScrollbarDrag(double mouseY) {
        if (this.maxScroll() == 0) {
            this.draggingScrollbar = false;
            return;
        }
        int thumbTop = this.currentThumbTop();
        int thumbBottom = thumbTop + this.currentThumbHeight();
        int mouse = (int) Math.floor(mouseY);
        if (mouse >= thumbTop && mouse <= thumbBottom) {
            this.scrollbarDragOffset = mouse - thumbTop;
        } else {
            this.scrollbarDragOffset = this.currentThumbHeight() / 2;
            this.updateScrollFromThumb(mouseY);
        }
    }

    private void startHorizontalScrollbarDrag(double mouseX) {
        if (!this.canHorizontalScroll()) {
            this.draggingHorizontalScrollbar = false;
            return;
        }
        int trackLeft = this.contentLeft();
        int trackRight = this.scrollbarLeft();
        int thumbLeft = this.currentHorizontalThumbLeft(trackLeft, trackRight);
        int thumbRight = thumbLeft + this.currentHorizontalThumbWidth(trackLeft, trackRight);
        int mouse = (int) Math.floor(mouseX);
        if (mouse >= thumbLeft && mouse <= thumbRight) {
            this.horizontalScrollbarDragOffset = mouse - thumbLeft;
        } else {
            this.horizontalScrollbarDragOffset = this.currentHorizontalThumbWidth(trackLeft, trackRight) / 2;
            this.updateHorizontalScrollFromThumb(mouseX);
        }
    }

    private void updateScrollFromThumb(double mouseY) {
        int maxScroll = this.maxScroll();
        if (maxScroll == 0) {
            this.scrollAmount = 0;
            return;
        }
        int innerTop = this.innerTop();
        int thumbHeight = this.currentThumbHeight();
        int travel = Math.max(1, this.visibleHeight() - thumbHeight);
        int top = Math.clamp((int) Math.floor(mouseY) - this.scrollbarDragOffset, innerTop, innerTop + travel);
        double ratio = (double) (top - innerTop) / (double) travel;
        this.scrollAmount = ratio * maxScroll;
        this.clampScrollAmount();
    }

    private void updateHorizontalScrollFromThumb(double mouseX) {
        int maxHorizontal = this.maxHorizontalScroll();
        if (maxHorizontal == 0) {
            this.horizontalScrollAmount = 0d;
            return;
        }
        int trackLeft = this.contentLeft();
        int trackRight = this.scrollbarLeft();
        int thumbWidth = this.currentHorizontalThumbWidth(trackLeft, trackRight);
        int travel = Math.max(1, (trackRight - trackLeft) - thumbWidth);
        int left = Math.clamp((int) Math.floor(mouseX) - this.horizontalScrollbarDragOffset, trackLeft, trackLeft + travel);
        double ratio = (double) (left - trackLeft) / (double) travel;
        this.horizontalScrollAmount = ratio * maxHorizontal;
        this.clampHorizontalScrollAmount();
    }

    private int currentThumbTop() {
        int maxScroll = this.maxScroll();
        if (maxScroll == 0) return this.innerTop();
        int thumbHeight = this.currentThumbHeight();
        int travel = Math.max(1, this.visibleHeight() - thumbHeight);
        return this.innerTop() + (int) Math.round((this.scrollAmount / maxScroll) * travel);
    }

    private int currentHorizontalThumbLeft(int trackLeft, int trackRight) {
        int maxHorizontal = this.maxHorizontalScroll();
        if (maxHorizontal == 0) {
            return trackLeft;
        }
        int thumbWidth = this.currentHorizontalThumbWidth(trackLeft, trackRight);
        int travel = Math.max(1, (trackRight - trackLeft) - thumbWidth);
        return trackLeft + (int) Math.round((this.horizontalScrollAmount / maxHorizontal) * travel);
    }

    private int currentThumbHeight() {
        int visibleHeight = this.visibleHeight();
        return Math.max(16, (visibleHeight * visibleHeight) / Math.max(1, this.contentHeight));
    }

    private int currentHorizontalThumbWidth(int trackLeft, int trackRight) {
        int trackWidth = Math.max(1, trackRight - trackLeft);
        int maxHorizontal = this.maxHorizontalScroll();
        if (maxHorizontal == 0) {
            return trackWidth;
        }
        int contentWidth = Math.max(1, this.contentWidth());
        int thumb = (int) Math.round((trackWidth * (double) contentWidth) / (double) (contentWidth + maxHorizontal));
        return Math.max(HORIZONTAL_SCROLLBAR_MIN_THUMB, Math.min(trackWidth, thumb));
    }

    private int drawSyntaxToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token,
            int color
    ) {
        if (availableWidth <= 0 || token.isEmpty()) {
            return 0;
        }
        String visible = this.trimToWidthRaw(token, availableWidth);
        if (visible.isEmpty()) {
            return 0;
        }
        this.drawRawText(context, visible, x, y, color);
        return this.rawTextWidth(visible);
    }

    private int drawHexColorToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token,
            int color
    ) {
        if (availableWidth <= 0 || token.isEmpty()) {
            return 0;
        }
        String visible = this.trimToWidthRaw(token, availableWidth);
        if (visible.isEmpty()) {
            return 0;
        }

        int width = this.rawTextWidth(visible);
        if (width <= 0) {
            return 0;
        }

        int top = y - 1;
        int bottom = y + this.scaledFontLineHeight() + 1;
        int left = x - 1;
        int right = x + width + 2;
        int chipBackground = this.hexChipBackground(color);
        int chipBorder = this.hexChipBorder(color);
        context.fill(left, top, right, bottom, chipBackground);
        this.drawRectBorder(context, left, top, right, bottom, chipBorder);

        int textColor = this.hexChipTextColor(color);
        this.drawRawText(context, visible, x, y, textColor);
        return width;
    }

    private int renderStringToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token
    ) {
        if (availableWidth <= 0 || token.isEmpty()) {
            return 0;
        }
        int drawn = 0;
        int index = 0;
        while (index < token.length() && drawn < availableWidth) {
            char c = token.charAt(index);
            if (c == '\\') {
                int escapeEnd = this.scanEscapeSequence(token, index);
                drawn += this.drawSyntaxToken(
                        context,
                        x + drawn,
                        y,
                        availableWidth - drawn,
                        token.substring(index, escapeEnd),
                        COLOR_ESCAPE
                );
                index = escapeEnd;
                continue;
            }

            int next = index + 1;
            while (next < token.length() && token.charAt(next) != '\\') {
                next++;
            }
            drawn += this.drawSyntaxToken(
                    context,
                    x + drawn,
                    y,
                    availableWidth - drawn,
                    token.substring(index, next),
                    COLOR_STRING
            );
            index = next;
        }
        return drawn;
    }

    private int renderNumericToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token
    ) {
        if (availableWidth <= 0 || token.isEmpty()) {
            return 0;
        }
        int suffixStart = this.numericSuffixStart(token);
        if (suffixStart <= 0 || suffixStart >= token.length()) {
            return this.drawSyntaxToken(context, x, y, availableWidth, token, COLOR_NUMBER);
        }

        int drawn = this.drawSyntaxToken(
                context,
                x,
                y,
                availableWidth,
                token.substring(0, suffixStart),
                COLOR_NUMBER
        );
        if (drawn >= availableWidth) {
            return drawn;
        }
        drawn += this.drawSyntaxToken(
                context,
                x + drawn,
                y,
                availableWidth - drawn,
                token.substring(suffixStart),
                COLOR_NUMBER_SUFFIX
        );
        return drawn;
    }

    private int findStringEnd(String line, int start, char quote) {
        boolean escaping = false;
        for (int index = start; index < line.length(); index++) {
            char value = line.charAt(index);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (value == '\\') {
                escaping = true;
                continue;
            }
            if (value == quote) return index;
        }
        return -1;
    }

    private boolean isKeyToken(String line, int tokenEndExclusive) {
        int next = tokenEndExclusive;
        while (next < line.length() && Character.isWhitespace(line.charAt(next))) next++;
        return next < line.length() && line.charAt(next) == ':';
    }

    private int scanWhitespace(String line, int index) {
        int cursorPos = index;
        while (cursorPos < line.length() && Character.isWhitespace(line.charAt(cursorPos))) {
            cursorPos++;
        }
        return cursorPos;
    }

    private boolean isNumberStart(String line, int index) {
        char c = line.charAt(index);
        if (Character.isDigit(c)) {
            return true;
        }
        if ((c == '-' || c == '+') && index + 1 < line.length()) {
            char next = line.charAt(index + 1);
            return Character.isDigit(next) || next == '.';
        }
        return c == '.' && index + 1 < line.length() && Character.isDigit(line.charAt(index + 1));
    }

    private int scanNumber(String line, int index) {
        int cursorPos = index;
        boolean seenDigits = false;
        boolean seenDot = false;
        boolean seenExponent = false;

        if (cursorPos < line.length() && (line.charAt(cursorPos) == '+' || line.charAt(cursorPos) == '-')) {
            cursorPos++;
        }

        while (cursorPos < line.length()) {
            char c = line.charAt(cursorPos);
            if (Character.isDigit(c)) {
                seenDigits = true;
                cursorPos++;
                continue;
            }
            if (c == '_') {
                cursorPos++;
                continue;
            }
            if (c == '.' && !seenDot && !seenExponent) {
                seenDot = true;
                cursorPos++;
                continue;
            }
            if ((c == 'e' || c == 'E') && seenDigits && !seenExponent) {
                seenExponent = true;
                cursorPos++;
                if (cursorPos < line.length() && (line.charAt(cursorPos) == '+' || line.charAt(cursorPos) == '-')) {
                    cursorPos++;
                }
                seenDigits = false;
                continue;
            }
            break;
        }

        if (cursorPos < line.length() && this.isNumberSuffix(line.charAt(cursorPos))) {
            cursorPos++;
        }

        if (cursorPos <= index) {
            return Math.min(line.length(), index + 1);
        }
        return cursorPos;
    }

    private boolean isWordStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '#';
    }

    private int scanWord(String line, int index) {
        int cursorPos = index;
        while (cursorPos < line.length()) {
            char c = line.charAt(cursorPos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '/' || c == '#') cursorPos++;
            else break;
        }
        return cursorPos;
    }

    private int resolveWordColor(String line, String word, int tokenStartInclusive, int tokenEndExclusive, int depth) {
        if (this.isKeyToken(line, tokenEndExclusive)) {
            return this.keyColorForDepth(depth);
        }
        if (this.isBooleanWord(word)) {
            return COLOR_BOOLEAN;
        }
        if ("null".equalsIgnoreCase(word)) {
            return COLOR_NULL;
        }
        if (this.isTypedArrayTypeToken(line, word, tokenStartInclusive, tokenEndExclusive)) {
            return COLOR_LITERAL;
        }
        if (this.looksLikeResourceIdentifier(word)) {
            return COLOR_IDENTIFIER;
        }
        return COLOR_TEXT;
    }

    private int punctuationColor(char value, int depth) {
        return switch (value) {
            case '{', '}', '[', ']', '(', ')' -> this.bracketColorForDepth(depth);
            case ':', ',', ';', '=' -> COLOR_OPERATOR;
            default -> COLOR_PUNCT;
        };
    }

    private int keyColorForDepth(int depth) {
        int index = Math.floorMod(depth, KEY_DEPTH_COLORS.length);
        return KEY_DEPTH_COLORS[index];
    }

    private int bracketColorForDepth(int depth) {
        int index = Math.floorMod(depth, BRACKET_DEPTH_COLORS.length);
        return BRACKET_DEPTH_COLORS[index];
    }

    private int scanEscapeSequence(String token, int start) {
        int next = Math.min(token.length(), start + 2);
        if (start + 1 >= token.length()) {
            return next;
        }
        char marker = token.charAt(start + 1);
        if (marker != 'u' && marker != 'U') {
            return next;
        }
        int max = Math.min(token.length(), start + 6);
        for (int index = start + 2; index < max; index++) {
            if (this.isNonHexChar(token.charAt(index))) {
                return next;
            }
        }
        return max;
    }

    private boolean isNonHexChar(char value) {
        return (value < '0' || value > '9')
                && (value < 'a' || value > 'f')
                && (value < 'A' || value > 'F');
    }

    private boolean isBooleanWord(String word) {
        return "true".equalsIgnoreCase(word) || "false".equalsIgnoreCase(word);
    }

    private boolean isNumericWord(String word) {
        return "Infinity".equalsIgnoreCase(word)
                || "Infinityf".equalsIgnoreCase(word)
                || "Infinityd".equalsIgnoreCase(word)
                || "NaN".equalsIgnoreCase(word)
                || "NaNf".equalsIgnoreCase(word)
                || "NaNd".equalsIgnoreCase(word);
    }

    private int numericSuffixStart(String token) {
        if (token.length() <= 1) {
            return -1;
        }
        char last = token.charAt(token.length() - 1);
        if (!this.isNumberSuffix(last)) {
            return -1;
        }
        return token.length() - 1;
    }

    private boolean isNumberSuffix(char value) {
        char c = Character.toLowerCase(value);
        return c == 'b' || c == 's' || c == 'l' || c == 'f' || c == 'd';
    }

    private boolean isTypedArrayTypeToken(
            String line,
            String word,
            int tokenStartInclusive,
            int tokenEndExclusive
    ) {
        if (word.length() != 1) {
            return false;
        }
        char marker = Character.toUpperCase(word.charAt(0));
        if (marker != 'B' && marker != 'I' && marker != 'L') {
            return false;
        }
        int left = tokenStartInclusive - 1;
        while (left >= 0 && Character.isWhitespace(line.charAt(left))) {
            left--;
        }
        int right = tokenEndExclusive;
        while (right < line.length() && Character.isWhitespace(line.charAt(right))) {
            right++;
        }
        return left >= 0 && right < line.length() && line.charAt(left) == '[' && line.charAt(right) == ';';
    }

    private boolean isResourceIdentifierString(String token) {
        if (token.length() < 3) {
            return false;
        }
        char quote = token.charAt(0);
        if ((quote != '"' && quote != '\'') || token.charAt(token.length() - 1) != quote) {
            return false;
        }
        String inner = token.substring(1, token.length() - 1);
        if (inner.indexOf('\\') >= 0) {
            return false;
        }
        return this.looksLikeResourceIdentifier(inner);
    }

    private int parseHexColorFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return -1;
        }
        if (token.length() >= 2) {
            char first = token.charAt(0);
            char last = token.charAt(token.length() - 1);
            if ((first == '"' || first == '\'') && first == last) {
                String inner = token.substring(1, token.length() - 1);
                if (inner.indexOf('\\') >= 0) {
                    return -1;
                }
                return this.parseHexColorLiteral(inner);
            }
        }
        return this.parseHexColorLiteral(token);
    }

    private int parseHexColorLiteral(String literal) {
        if (literal == null) {
            return -1;
        }
        String value = literal.trim();
        if (value.length() != 7 && value.length() != 9) {
            return -1;
        }
        if (value.charAt(0) != '#') {
            return -1;
        }
        for (int index = 1; index < value.length(); index++) {
            if (this.isNonHexChar(value.charAt(index))) {
                return -1;
            }
        }
        try {
            long parsed = Long.parseLong(value.substring(1), 16);
            int rgb = value.length() == 7 ? (int) parsed : (int) (parsed & 0xFFFFFFL);
            return 0xFF000000 | rgb;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private int hexChipBackground(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        double luminance = this.luminance(r, g, b);

        int bgR;
        int bgG;
        int bgB;
        if (luminance < 95d) {
            bgR = this.mixChannel(r, 255, 0.72d);
            bgG = this.mixChannel(g, 255, 0.72d);
            bgB = this.mixChannel(b, 255, 0.72d);
        } else if (luminance > 200d) {
            bgR = this.mixChannel(r, 10, 0.70d);
            bgG = this.mixChannel(g, 10, 0.70d);
            bgB = this.mixChannel(b, 10, 0.70d);
        } else {
            bgR = this.mixChannel(r, 22, 0.56d);
            bgG = this.mixChannel(g, 22, 0.56d);
            bgB = this.mixChannel(b, 22, 0.56d);
        }
        return 0xCC000000 | (bgR << 16) | (bgG << 8) | bgB;
    }

    private int hexChipBorder(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int borderR = this.mixChannel(r, 255, 0.25d);
        int borderG = this.mixChannel(g, 255, 0.25d);
        int borderB = this.mixChannel(b, 255, 0.25d);
        return 0xE0000000 | (borderR << 16) | (borderG << 8) | borderB;
    }

    private int hexChipTextColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        double luminance = this.luminance(r, g, b);
        if (luminance < 45d) {
            int textR = this.mixChannel(r, 255, 0.60d);
            int textG = this.mixChannel(g, 255, 0.60d);
            int textB = this.mixChannel(b, 255, 0.60d);
            return 0xFF000000 | (textR << 16) | (textG << 8) | textB;
        }
        if (luminance > 235d) {
            int textR = this.mixChannel(r, 0, 0.35d);
            int textG = this.mixChannel(g, 0, 0.35d);
            int textB = this.mixChannel(b, 0, 0.35d);
            return 0xFF000000 | (textR << 16) | (textG << 8) | textB;
        }
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private double luminance(int r, int g, int b) {
        return (0.2126d * r) + (0.7152d * g) + (0.0722d * b);
    }

    private int mixChannel(int source, int target, double targetWeight) {
        double clampedWeight = Math.clamp(targetWeight, 0d, 1d);
        double sourceWeight = 1d - clampedWeight;
        int mixed = (int) Math.round((source * sourceWeight) + (target * clampedWeight));
        return Math.clamp(mixed, 0, 255);
    }

    private boolean looksLikeResourceIdentifier(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String candidate = value.charAt(0) == '#' ? value.substring(1) : value;
        int separator = candidate.indexOf(':');
        if (separator <= 0 || separator >= candidate.length() - 1) {
            return false;
        }
        String namespace = candidate.substring(0, separator);
        String path = candidate.substring(separator + 1);
        for (int index = 0; index < namespace.length(); index++) {
            char c = namespace.charAt(index);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '_' || c == '-' || c == '.')) {
                return false;
            }
        }
        for (int index = 0; index < path.length(); index++) {
            char c = path.charAt(index);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '_' || c == '-' || c == '.' || c == '/')) {
                return false;
            }
        }
        return true;
    }

    private int[] lineDepthStartsForRender() {
        if (this.text.length() > MAX_SYNTAX_DEPTH_SCAN_TOTAL_CHARS) {
            return null;
        }
        if (!this.lineDepthStartsDirty && this.lineDepthStarts.length == this.lineStarts.length) {
            return this.lineDepthStarts;
        }
        this.lineDepthStarts = this.computeLineDepthStarts();
        this.lineDepthStartsDirty = false;
        return this.lineDepthStarts;
    }

    private int[] computeLineDepthStarts() {
        int lineCount = this.lineStarts.length;
        int[] depths = new int[lineCount];
        int depth = 0;
        int line = 0;
        boolean inString = false;
        boolean escaping = false;
        char quote = '"';
        depths[0] = 0;

        for (int index = 0; index < this.text.length(); index++) {
            char c = this.text.charAt(index);
            if (c == '\n') {
                line = Math.min(line + 1, lineCount - 1);
                depths[line] = Math.max(0, depth);
                continue;
            }

            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (c == '\\') {
                    escaping = true;
                    continue;
                }
                if (c == quote) {
                    inString = false;
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                quote = c;
                continue;
            }
            if (c == '{' || c == '[') {
                depth++;
                continue;
            }
            if (c == '}' || c == ']') {
                depth = Math.max(0, depth - 1);
            }
        }
        return depths;
    }

    private int cursorIndexForLineColumn(int line, int column) {
        int lineIndex = Math.clamp(line - 1, 0, this.lineStarts.length - 1);
        int lineStart = this.lineStarts[lineIndex];
        int lineLength = this.lineEnd(lineIndex) - lineStart;
        int clampedColumn = Math.max(1, column);
        return lineStart + Math.min(lineLength, clampedColumn - 1);
    }

    private void ensureCursorVisible() {
        int lineHeight = this.lineHeightWithSpacing();
        int visibleLine = this.visibleLineForCursorIndex(this.cursor);
        int lineTop = visibleLine * lineHeight;
        int lineBottom = lineTop + this.scaledFontLineHeight();
        int visibleHeight = this.visibleHeight();
        if (lineTop < this.scrollAmount) this.scrollAmount = lineTop;
        else if (lineBottom > this.scrollAmount + visibleHeight) this.scrollAmount = lineBottom - visibleHeight;
        if (this.canHorizontalScroll()) {
            int lineIndex = this.lineIndexForCursor(this.cursor);
            int lineStart = this.lineStarts[lineIndex];
            int localCursor = Math.clamp(this.cursor - lineStart, 0, Math.max(0, this.lineEnd(lineIndex) - lineStart));
            int cursorX = this.rawTextWidth(this.text.substring(lineStart, lineStart + localCursor));
            int viewportWidth = Math.max(1, this.contentWidth() - HORIZONTAL_SCROLL_PADDING);
            if (cursorX < this.horizontalScrollAmount) {
                this.horizontalScrollAmount = cursorX;
            } else if (cursorX > this.horizontalScrollAmount + viewportWidth) {
                this.horizontalScrollAmount = cursorX - viewportWidth;
            }
        } else {
            this.horizontalScrollAmount = 0d;
        }
        this.clampScrollAmount();
        this.clampHorizontalScrollAmount();
    }

    private boolean isCursorInViewport(int cursorIndex) {
        int lineHeight = this.lineHeightWithSpacing();
        int visibleLine = this.visibleLineForCursorIndex(cursorIndex);
        int lineTop = visibleLine * lineHeight;
        int lineBottom = lineTop + this.scaledFontLineHeight();
        double top = this.scrollAmount;
        double bottom = this.scrollAmount + this.visibleHeight();
        return lineTop >= top && lineBottom <= bottom;
    }

    private void rebuildLineStarts() {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int index = 0; index < this.text.length(); index++) {
            if (this.text.charAt(index) == '\n') starts.add(index + 1);
        }
        this.applyLineStarts(starts.stream().mapToInt(Integer::intValue).toArray(), false);
    }

    private boolean rebuildLineStartsIncremental(int oldTextLength, int editStart, int editEnd, String replacement) {
        if (this.lineStarts == null || this.lineStarts.length == 0) {
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

        int[] lineStartArray = starts.stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(lineStartArray);
        int textLength = this.text.length();
        int write = 0;
        for (int read = 0; read < lineStartArray.length; read++) {
            int clamped = Math.clamp(lineStartArray[read], 0, textLength);
            if (write == 0 || clamped != lineStartArray[write - 1]) {
                lineStartArray[write++] = clamped;
            }
        }
        if (write == 0 || lineStartArray[0] != 0) {
            int[] normalized = new int[write + 1];
            normalized[0] = 0;
            if (write > 0) {
                System.arraycopy(lineStartArray, 0, normalized, 1, write);
            }
            this.applyLineStarts(normalized, true);
            return true;
        }
        if (write != lineStartArray.length) {
            lineStartArray = Arrays.copyOf(lineStartArray, write);
        }
        this.applyLineStarts(lineStartArray, true);
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

    private void applyLineStarts(int[] starts, boolean keepFoldRegions) {
        if (starts == null || starts.length == 0) {
            this.lineStarts = new int[]{0};
        } else {
            this.lineStarts = starts;
        }
        this.lineDepthStartsDirty = true;
        this.syntaxLineCache.clear();
        if (!keepFoldRegions) {
            this.rebuildFoldLayout();
            this.clampScrollAmount();
        }
    }

    private int lineEnd(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= this.lineStarts.length) return 0;
        if (lineIndex + 1 < this.lineStarts.length) return this.lineStarts[lineIndex + 1] - 1;
        return this.text.length();
    }

    private int lineIndexForCursor(int cursorIndex) {
        int target = Math.clamp(cursorIndex, 0, this.text.length());
        int low = 0;
        int high = this.lineStarts.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int start = this.lineStarts[mid];
            int end = this.lineEnd(mid);
            if (target < start) high = mid - 1;
            else if (target > end) low = mid + 1;
            else return mid;
        }
        return Math.clamp(low, 0, this.lineStarts.length - 1);
    }

    private int lineHeightWithSpacing() {
        int scaledLineHeight = this.scaledFontLineHeight();
        this.ensureTextScaleCache();
        int scaledSpacing = Math.max(1, (int) Math.ceil(LINE_SPACING * this.cachedTextScale));
        return scaledLineHeight + scaledSpacing;
    }

    private int endOfLineClickTolerance() {
        return Math.max(2, this.rawTextWidth(" "));
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - this.visibleHeight());
    }

    private void clampScrollAmount() {
        this.scrollAmount = Math.clamp(this.scrollAmount, 0d, this.maxScroll());
    }

    private boolean canHorizontalScroll() {
        return this.horizontalScroll && !this.wordWrap;
    }

    private int maxHorizontalScroll() {
        if (!this.horizontalScroll || this.wordWrap) {
            return 0;
        }
        return Math.max(0, this.maxVisibleLineWidth - this.contentWidth());
    }

    private void clampHorizontalScrollAmount() {
        this.horizontalScrollAmount = Math.clamp(this.horizontalScrollAmount, 0d, this.maxHorizontalScroll());
    }

    private int visibleLineCount() {
        return Math.max(1, this.visibleHeight() / this.lineHeightWithSpacing());
    }

    private int foldMarkerLeft() {
        return this.innerLeft() + GUTTER_PADDING;
    }

    private int foldMarkerRight() {
        return this.foldMarkerLeft() + FOLD_MARKER_WIDTH;
    }

    private int actualLineForVisibleLine(int visibleLine) {
        int index = Math.clamp(visibleLine, 0, this.visibleToActualLine.length - 1);
        return this.visibleToActualLine[index];
    }

    private int visibleSegmentStart(int visibleLine) {
        if (this.visibleSegmentStart.length == 0) {
            return 0;
        }
        int index = Math.clamp(visibleLine, 0, this.visibleSegmentStart.length - 1);
        return this.visibleSegmentStart[index];
    }

    private int visibleSegmentEnd(int visibleLine) {
        if (this.visibleSegmentEnd.length == 0) {
            return 0;
        }
        int index = Math.clamp(visibleLine, 0, this.visibleSegmentEnd.length - 1);
        return this.visibleSegmentEnd[index];
    }

    private int visibleLineForActualLine(int actualLine) {
        if (this.actualToVisibleLine.length == 0) {
            return 0;
        }
        int clamped = Math.clamp(actualLine, 0, this.actualToVisibleLine.length - 1);
        int visible = this.actualToVisibleLine[clamped];
        if (visible >= 0) {
            return visible;
        }
        for (int line = clamped; line >= 0; line--) {
            if (this.actualToVisibleLine[line] >= 0) {
                return this.actualToVisibleLine[line];
            }
        }
        return 0;
    }

    private int visibleLineForCursorIndex(int cursorIndex) {
        int lineIndex = this.lineIndexForCursor(cursorIndex);
        int firstVisible = this.visibleLineForActualLine(lineIndex);
        if (!this.wordWrap || this.actualToVisibleLineLast.length == 0) {
            return firstVisible;
        }
        int lastVisible = this.actualToVisibleLineLast[Math.clamp(lineIndex, 0, this.actualToVisibleLineLast.length - 1)];
        if (lastVisible < firstVisible) {
            return firstVisible;
        }
        int localCursor = Math.max(0, cursorIndex - this.lineStarts[lineIndex]);
        for (int visibleLine = firstVisible; visibleLine <= lastVisible; visibleLine++) {
            int segmentEnd = this.visibleSegmentEnd(visibleLine);
            if (localCursor <= segmentEnd || visibleLine == lastVisible) {
                return visibleLine;
            }
        }
        return firstVisible;
    }

    private FoldRegion foldRegionStartingAt(int actualLine) {
        if (actualLine < 0 || actualLine >= this.foldByStartLine.length) {
            return null;
        }
        return this.foldByStartLine[actualLine];
    }

    private void expandFoldsForLine(int actualLine) {
        boolean changed = false;
        for (FoldRegion region : this.foldRegions) {
            if (region.collapsed && actualLine > region.startLine && actualLine <= region.endLine) {
                region.collapsed = false;
                changed = true;
            }
        }
        if (changed) {
            this.rebuildFoldLayout();
        }
    }

    private boolean tryToggleFold(double mouseX, double mouseY) {
        if (mouseX < this.foldMarkerLeft() || mouseX > this.foldMarkerRight()) {
            return false;
        }
        int localY = (int) Math.floor(mouseY - this.innerTop() + this.renderedScroll());
        if (localY < 0) {
            return false;
        }
        int visibleLine = Math.clamp(localY / this.lineHeightWithSpacing(), 0, this.visibleToActualLine.length - 1);
        int actualLine = this.actualLineForVisibleLine(visibleLine);
        FoldRegion region = this.foldRegionStartingAt(actualLine);
        if (region == null) {
            return false;
        }
        this.toggleFoldRegion(region);
        return true;
    }

    private void toggleFoldRegion(FoldRegion region) {
        region.collapsed = !region.collapsed;
        this.rebuildFoldLayout();
        this.ensureCursorVisible();
    }

    private void rebuildFoldLayout() {
        /* see LINE:47 (MAX_FOLD_SCAN_TOTAL_CHARS)
        if (this.text.length() > MAX_FOLD_SCAN_TOTAL_CHARS) {
            this.foldRegions = List.of();
            this.applyFoldVisibility();
            return;
        }
        */
        Map<Long, Boolean> collapsedByKey = new HashMap<>();
        for (FoldRegion region : this.foldRegions) {
            collapsedByKey.put(region.key(), region.collapsed);
        }

        List<FoldRegion> rebuilt = this.scanFoldRegions();
        for (FoldRegion region : rebuilt) {
            region.collapsed = collapsedByKey.getOrDefault(region.key(), false);
        }
        this.foldRegions = rebuilt;
        this.applyFoldVisibility();
    }

    private void shiftFoldRegionsAfterLine(int lineIndex, int lineDelta) {
        if (lineDelta == 0 || this.foldRegions.isEmpty()) {
            return;
        }
        for (FoldRegion region : this.foldRegions) {
            if (region.startLine > lineIndex) {
                region.startLine += lineDelta;
                region.endLine += lineDelta;
                continue;
            }
            if (region.endLine > lineIndex) {
                region.endLine += lineDelta;
            }
        }
    }

    private int newlineCount(String value) {
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

    private boolean containsFoldStructuralChar(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (c == '{' || c == '}' || c == '[' || c == ']' || c == '"' || c == '\'') {
                return true;
            }
        }
        return false;
    }

    private boolean containsAutocompleteStructuralChar(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            switch (value.charAt(index)) {
                case '{', '}', '[', ']', ':', ',', '"', '\'' -> {
                    return true;
                }
                default -> {
                }
            }
        }
        return false;
    }

    private List<FoldRegion> scanFoldRegions() {
        List<FoldRegion> regions = new ArrayList<>();
        ArrayDeque<OpenSymbol> openStack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaping = false;
        int line = 0;

        for (int index = 0; index < this.text.length(); index++) {
            char value = this.text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == '"') {
                    inString = false;
                }
            } else {
                if (value == '"') {
                    inString = true;
                } else if (value == '{' || value == '[') {
                    openStack.addLast(new OpenSymbol(value, line));
                } else if (value == '}' || value == ']') {
                    char expectedOpen = value == '}' ? '{' : '[';
                    OpenSymbol matched = null;
                    while (!openStack.isEmpty()) {
                        OpenSymbol candidate = openStack.removeLast();
                        if (candidate.type == expectedOpen) {
                            matched = candidate;
                            break;
                        }
                    }
                    if (matched != null && line > matched.line) {
                        regions.add(new FoldRegion(matched.line, line, expectedOpen));
                    }
                }
            }

            if (value == '\n') {
                line++;
            }
        }

        regions.sort((left, right) -> {
            if (left.startLine != right.startLine) {
                return Integer.compare(left.startLine, right.startLine);
            }
            return Integer.compare(right.endLine, left.endLine);
        });
        return regions;
    }

    private void applyFoldVisibility() {
        int lineCount = Math.max(1, this.lineStarts.length);
        this.hiddenLines = new boolean[lineCount];
        this.foldByStartLine = new FoldRegion[lineCount];

        for (FoldRegion region : this.foldRegions) {
            if (region.startLine < 0 || region.startLine >= lineCount || region.endLine <= region.startLine) {
                continue;
            }
            FoldRegion current = this.foldByStartLine[region.startLine];
            if (current == null || region.endLine > current.endLine) {
                this.foldByStartLine[region.startLine] = region;
            }
            if (region.collapsed) {
                for (int line = region.startLine + 1; line <= Math.min(region.endLine, lineCount - 1); line++) {
                    this.hiddenLines[line] = true;
                }
            }
        }

        this.actualToVisibleLine = new int[lineCount];
        this.actualToVisibleLineLast = new int[lineCount];
        Arrays.fill(this.actualToVisibleLine, -1);
        Arrays.fill(this.actualToVisibleLineLast, -1);
        List<Integer> visibleLines = new ArrayList<>(lineCount);
        List<Integer> segmentStarts = new ArrayList<>(lineCount);
        List<Integer> segmentEnds = new ArrayList<>(lineCount);
        this.maxVisibleLineWidth = 0;
        int wrapWidth = Math.max(1, this.contentWidth());
        for (int line = 0; line < lineCount; line++) {
            if (this.hiddenLines[line]) {
                continue;
            }
            int lineStart = this.lineStarts[line];
            int lineEnd = this.lineEnd(line);
            String lineText = this.text.substring(lineStart, lineEnd);
            this.maxVisibleLineWidth = Math.max(this.maxVisibleLineWidth, this.rawTextWidth(lineText));

            FoldRegion fold = this.foldRegionStartingAt(line);
            boolean collapsed = fold != null && fold.collapsed;
            if (collapsed || !this.wordWrap) {
                this.actualToVisibleLine[line] = visibleLines.size();
                this.actualToVisibleLineLast[line] = visibleLines.size();
                visibleLines.add(line);
                segmentStarts.add(0);
                segmentEnds.add(Math.max(0, lineText.length()));
                continue;
            }

            int added = this.appendWrappedSegments(visibleLines, segmentStarts, segmentEnds, line, lineText, wrapWidth);
            if (added <= 0) {
                this.actualToVisibleLine[line] = visibleLines.size();
                this.actualToVisibleLineLast[line] = visibleLines.size();
                visibleLines.add(line);
                segmentStarts.add(0);
                segmentEnds.add(0);
                continue;
            }
            this.actualToVisibleLine[line] = visibleLines.size() - added;
            this.actualToVisibleLineLast[line] = visibleLines.size() - 1;
        }
        if (visibleLines.isEmpty()) {
            visibleLines.add(0);
            this.actualToVisibleLine[0] = 0;
            this.actualToVisibleLineLast[0] = 0;
            segmentStarts.add(0);
            segmentEnds.add(0);
        }
        this.visibleToActualLine = visibleLines.stream().mapToInt(Integer::intValue).toArray();
        this.visibleSegmentStart = segmentStarts.stream().mapToInt(Integer::intValue).toArray();
        this.visibleSegmentEnd = segmentEnds.stream().mapToInt(Integer::intValue).toArray();
        this.wrapLayoutWidth = wrapWidth;
        this.moveCursorOutOfHiddenRegion();
        this.contentHeight = Math.max(this.visibleToActualLine.length * this.lineHeightWithSpacing(), this.lineHeightWithSpacing());
        this.clampHorizontalScrollAmount();
    }

    private void refreshWrapLayoutIfNeeded() {
        if (!this.wordWrap) {
            return;
        }
        int width = Math.max(1, this.contentWidth());
        if (width == this.wrapLayoutWidth) {
            return;
        }
        this.applyFoldVisibility();
        this.ensureCursorVisible();
    }

    private int appendWrappedSegments(
            List<Integer> visibleLines,
            List<Integer> segmentStarts,
            List<Integer> segmentEnds,
            int line,
            String lineText,
            int wrapWidth
    ) {
        if (lineText.isEmpty()) {
            visibleLines.add(line);
            segmentStarts.add(0);
            segmentEnds.add(0);
            return 1;
        }

        int segmentStart = 0;
        int lineLength = lineText.length();
        int added = 0;
        while (segmentStart < lineLength) {
            int segmentEnd = this.wrapSegmentEnd(lineText, segmentStart, wrapWidth);
            if (segmentEnd <= segmentStart) {
                segmentEnd = Math.min(lineLength, segmentStart + 1);
            }
            visibleLines.add(line);
            segmentStarts.add(segmentStart);
            segmentEnds.add(segmentEnd);
            segmentStart = segmentEnd;
            added++;
        }
        return added;
    }

    private int wrapSegmentEnd(String lineText, int segmentStart, int wrapWidth) {
        if (segmentStart >= lineText.length()) {
            return segmentStart;
        }
        int index = segmentStart;
        int lastBreak = -1;
        double width = 0d;
        while (index < lineText.length()) {
            int codePoint = lineText.codePointAt(index);
            int next = index + Character.charCount(codePoint);
            double advance = this.rawGlyphAdvance(codePoint);
            if (width + advance > wrapWidth) {
                if (lastBreak > segmentStart) {
                    return lastBreak;
                }
                return index > segmentStart ? index : next;
            }
            width += advance;
            char symbol = (char) codePoint;
            if (Character.isWhitespace(symbol) || symbol == ',' || symbol == ';' || symbol == ':' || symbol == ']' || symbol == '}' || symbol == ')') {
                lastBreak = next;
            }
            index = next;
        }
        return lineText.length();
    }

    private void moveCursorOutOfHiddenRegion() {
        if (this.hiddenLines.length == 0) {
            return;
        }
        int cursorLine = this.lineIndexForCursor(this.cursor);
        if (!this.hiddenLines[Math.clamp(cursorLine, 0, this.hiddenLines.length - 1)]) {
            return;
        }
        for (FoldRegion region : this.foldRegions) {
            if (!region.collapsed) {
                continue;
            }
            if (cursorLine > region.startLine && cursorLine <= region.endLine) {
                int target = this.lineStarts[Math.clamp(region.startLine, 0, this.lineStarts.length - 1)];
                this.cursor = target;
                this.selectionCursor = target;
                this.clearVirtualCaret();
                return;
            }
        }
    }

    private String applyVirtualCaretPadding(String insert) {
        String safeInsert = insert == null ? "" : insert;
        if (this.hasSelection()) {
            this.clearVirtualCaret();
            return safeInsert;
        }
        if (this.virtualCaretLine < 0 || this.virtualCaretLocalX < 0) {
            return safeInsert;
        }
        int lineIndex = this.lineIndexForCursor(this.cursor);
        if (lineIndex != this.virtualCaretLine || this.cursor != this.lineEnd(lineIndex)) {
            this.clearVirtualCaret();
            return safeInsert;
        }
        int lineStart = this.lineStarts[lineIndex];
        int lineEnd = this.lineEnd(lineIndex);
        String line = this.text.substring(lineStart, lineEnd);
        int lineWidth = this.rawTextWidth(line);
        int missingPixels = this.virtualCaretLocalX - lineWidth;
        if (missingPixels <= 0) {
            this.clearVirtualCaret();
            return safeInsert;
        }
        int spaceWidth = Math.max(1, this.rawTextWidth(" "));
        int spaces = (int) Math.ceil(missingPixels / (double) spaceWidth);
        this.clearVirtualCaret();
        if (spaces <= 0) {
            return safeInsert;
        }
        return " ".repeat(spaces) + safeInsert;
    }

    private void clearVirtualCaret() {
        this.virtualCaretLine = -1;
        this.virtualCaretLocalX = -1;
    }

    private void renderCollapsedLine(
            OwoUIGraphics context,
            int contentLeft,
            int lineY,
            int contentWidth,
            String line,
            FoldRegion collapsedRegion
    ) {
        String suffix = " ...";
        String summary = line + suffix;
        String renderSource = summary.length() > MAX_RENDER_LINE_CHARS ? summary.substring(0, MAX_RENDER_LINE_CHARS) : summary;
        String visible = this.trimToWidthRaw(renderSource, contentWidth);
        if (!visible.isEmpty()) {
            this.drawRawText(context, visible, contentLeft, lineY, COLOR_TEXT);
        }
        int markerX = Math.max(this.foldMarkerRight() + 1, this.gutterRight() - this.rawTextWidth("000"));
        this.drawRawText(
                context,
                Integer.toString(Math.max(1, collapsedRegion.endLine - collapsedRegion.startLine)),
                markerX,
                lineY,
                COLOR_GHOST
        );
    }

    private int innerLeft() {
        return this.x() + 1;
    }

    private int innerTop() {
        return this.y() + 1;
    }

    private int innerRight() {
        return this.x() + this.width() - 1;
    }

    private int innerBottom() {
        return this.y() + this.height() - 1;
    }

    private int visibleHeight() {
        return Math.max(1, this.height() - 2 - this.horizontalScrollbarReserveHeight());
    }

    private int gutterWidth() {
        int lineCount = Math.max(1, this.lineStarts.length);
        int digits = Integer.toString(lineCount).length();
        String sample = "0".repeat(Math.max(2, digits));
        int lineNumberWidth = this.rawTextWidth(sample) + GUTTER_PADDING;
        int foldWidth = FOLD_MARKER_WIDTH + GUTTER_FOLD_GAP + GUTTER_LINE_RIGHT_INSET + 1;
        return Math.max(GUTTER_MIN_WIDTH, lineNumberWidth + foldWidth);
    }

    private int contentLeft() {
        return this.innerLeft() + this.gutterWidth() + CONTENT_PADDING;
    }

    private int gutterRight() {
        return this.contentLeft() - CONTENT_PADDING;
    }

    private int contentRight() {
        return Math.max(this.contentLeft() + 1, this.innerRight() - this.scrollbarReserveWidth());
    }

    private int contentWidth() {
        return Math.max(1, this.contentRight() - this.contentLeft());
    }

    private int scrollbarLeft() {
        return this.innerRight() - this.scrollbarEdgeInset() - this.scrollbarWidth();
    }

    private int scrollbarRight() {
        return this.scrollbarLeft() + this.scrollbarWidth();
    }

    private int scrollbarBottom() {
        return Math.max(this.innerTop(), this.innerBottom() - this.horizontalScrollbarReserveHeight());
    }

    private int scrollbarWidth() {
        return UiFactory.scaledScrollbarThickness(SCROLLBAR_BASE_THICKNESS);
    }

    private int scrollbarReserveWidth() {
        return this.scrollbarWidth()
                + UiFactory.scaledPixels(SCROLLBAR_RESERVE_EXTRA)
                + this.scrollbarEdgeInset();
    }

    private int scrollbarEdgeInset() {
        return UiFactory.scaledPixels(SCROLLBAR_EDGE_INSET);
    }

    private int horizontalScrollbarHeight() {
        return this.canHorizontalScroll() ? this.scrollbarWidth() : 0;
    }

    private int horizontalScrollbarReserveHeight() {
        if (!this.canHorizontalScroll()) {
            return 0;
        }
        return this.horizontalScrollbarHeight() + UiFactory.scaledPixels(SCROLLBAR_RESERVE_EXTRA);
    }

    private int horizontalScrollbarTop() {
        return Math.max(this.innerTop(), this.innerBottom() - this.horizontalScrollbarHeight());
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int left = this.scrollbarLeft();
        int top = this.innerTop();
        int right = this.scrollbarRight();
        int bottom = this.scrollbarBottom();
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private boolean isMouseOverHorizontalScrollbar(double mouseX, double mouseY) {
        if (!this.canHorizontalScroll()) {
            return false;
        }
        int left = this.contentLeft();
        int right = this.scrollbarLeft();
        int top = this.horizontalScrollbarTop();
        int bottom = this.innerBottom();
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private double scrollRate() {
        return this.lineHeightWithSpacing();
    }

    private double horizontalScrollRate() {
        return Math.max(8d, this.contentWidth() / 8d);
    }

    private boolean isInsideEditor(double mouseX, double mouseY) {
        return this.isInBoundingBox(mouseX, mouseY);
    }

    private ResolvedMouse resolveEventCoordinates(double eventX, double eventY) {
        double localToScreenX = this.x() + eventX;
        double localToScreenY = this.y() + eventY;

        boolean absoluteInside = this.isInBoundingBox(eventX, eventY);
        boolean localInside = this.isInBoundingBox(localToScreenX, localToScreenY);
        if (absoluteInside && !localInside) {
            return new ResolvedMouse(eventX, eventY);
        }
        if (localInside && !absoluteInside) {
            return new ResolvedMouse(localToScreenX, localToScreenY);
        }

        double guiMouseX = this.currentGuiMouseX();
        double guiMouseY = this.currentGuiMouseY();
        double absoluteDistance = Math.abs(eventX - guiMouseX) + Math.abs(eventY - guiMouseY);
        double localDistance = Math.abs(localToScreenX - guiMouseX) + Math.abs(localToScreenY - guiMouseY);
        if (localDistance + 0.75d < absoluteDistance) {
            return new ResolvedMouse(localToScreenX, localToScreenY);
        }
        return new ResolvedMouse(eventX, eventY);
    }

    private double currentGuiMouseX() {
        Minecraft minecraft = Minecraft.getInstance();
        double scaleX = (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        return minecraft.mouseHandler.xpos() * scaleX;
    }

    private double currentGuiMouseY() {
        Minecraft minecraft = Minecraft.getInstance();
        double scaleY = (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
        return minecraft.mouseHandler.ypos() * scaleY;
    }

    private boolean shouldBlinkCursor() {
        return (Util.getMillis() / 300L) % 2L == 0L;
    }

    private int renderedScroll() {
        return (int) Math.floor(this.scrollAmount);
    }

    private int horizontalRenderOffset() {
        return this.canHorizontalScroll() ? (int) Math.floor(this.horizontalScrollAmount) : 0;
    }

    private record CaretAnchor(int x, int y) {
    }

    private record ResolvedMouse(double screenX, double screenY) {
    }

    private record AutocompletePopupLayout(
            int x,
            int y,
            int width,
            int height,
            int rowHeight,
            int visibleRows,
            int selected,
            int start,
            int end
    ) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x
                    && mouseX < this.x + this.width
                    && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    private void notifyViewportChanged() {
        this.viewportChanged.run();
    }

    private void drawRawText(OwoUIGraphics context, String value, int x, int y, int color) {
        if (value == null || value.isEmpty()) {
            return;
        }
        this.ensureTextScaleCache();
        if (Math.abs(this.cachedTextScale - 1.0F) < 0.001F) {
            context.text(Minecraft.getInstance().font, this.rawSequence(value), x, y, color, false);
            return;
        }

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(this.cachedTextScale, this.cachedTextScale);
        matrices.translate(-x, -y);
        context.text(Minecraft.getInstance().font, this.rawSequence(value), x, y, color, false);
        matrices.popMatrix();
    }

    private int rawTextWidth(String value) {
        if (value == null || value.isEmpty()) return 0;
        this.ensureTextScaleCache();
        return Math.max(0, (int) Math.ceil(Minecraft.getInstance().font.width(this.rawSequence(value)) * this.cachedTextScale));
    }

    private int scaledFontLineHeight() {
        this.ensureTextScaleCache();
        return Math.max(6, (int) Math.ceil(Minecraft.getInstance().font.lineHeight * this.cachedTextScale));
    }

    private String trimToWidthRaw(String value, int maxWidth) {
        if (value == null || value.isEmpty() || maxWidth <= 0) return "";
        int end = this.rawIndexAtWidth(value, maxWidth);
        return end <= 0 ? "" : value.substring(0, Math.clamp(end, 0, value.length()));
    }

    private int rawIndexAtWidth(String value, int targetWidth) {
        if (value.isEmpty() || targetWidth <= 0) {
            return 0;
        }
        int boundary = 0;
        double width = 0d;
        while (boundary < value.length()) {
            int codePoint = value.codePointAt(boundary);
            int nextBoundary = boundary + Character.charCount(codePoint);
            double glyphWidth = this.rawGlyphAdvance(codePoint);
            double midpoint = width + (glyphWidth * 0.5d);
            if (targetWidth < midpoint) {
                return boundary;
            }
            width += glyphWidth;
            if (targetWidth < width) {
                return nextBoundary;
            }
            boundary = nextBoundary;
        }
        return value.length();
    }

    private int rawIndexAtWidth(String value, double targetWidth) {
        if (value.isEmpty() || targetWidth <= 0d) {
            return 0;
        }
        return this.rawIndexAtWidth(value, (int) Math.floor(targetWidth));
    }

    private float rawGlyphAdvance(int codePoint) {
        this.ensureTextScaleCache();
        return this.glyphAdvanceCache.computeIfAbsent(codePoint, cp -> {
            String symbol = new String(Character.toChars(cp));
            return Minecraft.getInstance().font.width(symbol) * this.cachedTextScale;
        });
    }

    private void ensureTextScaleCache() {
        float scale = this.computeTextScale();
        if (Math.abs(scale - this.cachedTextScale) < 0.001F) {
            return;
        }
        this.cachedTextScale = scale;
        this.glyphAdvanceCache.clear();
        this.syntaxLineCache.clear();
        this.lineDepthStartsDirty = true;
    }

    private float computeTextScale() {
        Minecraft minecraft = Minecraft.getInstance();
        double guiScale = Math.max(1.0d, minecraft.getWindow().getGuiScale());
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();

        float scale = 1.0F;
        if (guiScale >= TEXT_SCALE_THRESHOLD_GS5) {
            scale = TEXT_SCALE_GS5;
        } else if (guiScale >= TEXT_SCALE_THRESHOLD_GS4) {
            scale = TEXT_SCALE_GS4;
        } else if (guiScale >= TEXT_SCALE_THRESHOLD_GS3) {
            scale = TEXT_SCALE_GS3;
        }

        if (guiWidth <= TEXT_SCALE_NARROW_WIDTH_THRESHOLD || guiHeight <= TEXT_SCALE_NARROW_HEIGHT_THRESHOLD) {
            scale -= TEXT_SCALE_NARROW_PENALTY;
        } else if (guiWidth >= TEXT_SCALE_WIDE_WIDTH_THRESHOLD && guiHeight >= TEXT_SCALE_WIDE_HEIGHT_THRESHOLD) {
            scale += TEXT_SCALE_WIDE_BONUS;
        }

        scale *= (this.fontSizePercent / 100.0F);

        return Math.max(TEXT_SCALE_MIN, Math.min(TEXT_SCALE_MAX, scale));
    }

    private FormattedCharSequence rawSequence(String value) {
        if (value == null || value.isEmpty()) return FormattedCharSequence.EMPTY;
        return sink -> {
            int index = 0;
            int cursorPos = 0;
            while (cursorPos < value.length()) {
                int cp = value.codePointAt(cursorPos);
                if (!sink.accept(index, RAW_STYLE, cp)) return false;
                cursorPos += Character.charCount(cp);
                index++;
            }
            return true;
        };
    }

    private record OpenSymbol(char type, int line) {
    }

    private enum SyntaxRunKind {
        PLAIN,
        STRING,
        NUMERIC,
        HEX
    }

    private record SyntaxRun(SyntaxRunKind kind, String text, int color) {
    }

    private static final class SyntaxLineCache {
        private final String line;
        private final int baseDepth;
        private final List<SyntaxRun> runs;

        private SyntaxLineCache(String line, int baseDepth, List<SyntaxRun> runs) {
            this.line = line;
            this.baseDepth = baseDepth;
            this.runs = runs;
        }

        private boolean matches(String line, int baseDepth) {
            return this.baseDepth == baseDepth && this.line.equals(line);
        }

        private List<SyntaxRun> runs() {
            return this.runs;
        }
    }

    private static final class FoldRegion {
        private int startLine;
        private int endLine;
        private final char type;
        private boolean collapsed;

        private FoldRegion(int startLine, int endLine, char type) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.type = type;
        }

        private long key() {
            long start = ((long) this.startLine) & 0xFFFFFFFFL;
            long end = (((long) this.endLine) & 0xFFFFFFFFL) << 32;
            return end ^ start ^ ((long) this.type << 48);
        }
    }

    private record HistoryState(String text, int cursor, int selection, double scroll) {
    }

    private record LineRange(int startLine, int endLine) {
    }

    public record HistorySnapshot(String text, int cursor, int selection, double scroll) {
        public HistorySnapshot {
            text = text == null ? "" : text;
        }
    }

    public record AutocompletePopupEntry(String label, String detail) {
        public AutocompletePopupEntry {
            label = label == null ? "" : label;
            detail = detail == null ? "" : detail;
        }
    }

    public interface OnChanged {
        void onChanged(String value, ChangeDelta delta);

        static EventStream<OnChanged> newStream() {
            return new EventStream<>(subscribers -> (value, delta) -> {
                for (OnChanged subscriber : subscribers) subscriber.onChanged(value, delta);
            });
        }
    }

    public record ChangeDelta(int start, int end, String replacement, boolean structural) {
        public ChangeDelta {
            replacement = replacement == null ? "" : replacement;
        }

        public static ChangeDelta none(int cursor) {
            int safe = Math.max(0, cursor);
            return new ChangeDelta(safe, safe, "", false);
        }

        public static ChangeDelta fullReplace(int oldLength, String replacement) {
            return new ChangeDelta(0, Math.max(0, oldLength), replacement == null ? "" : replacement, true);
        }
    }
}

