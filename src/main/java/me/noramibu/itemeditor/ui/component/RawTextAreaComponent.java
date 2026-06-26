package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.inject.GreedyInputUIComponent;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import me.noramibu.itemeditor.ui.component.raw.RawEditorLayout;
import me.noramibu.itemeditor.ui.component.raw.RawEditorRenderer;
import me.noramibu.itemeditor.ui.component.raw.RawFontMetrics;
import me.noramibu.itemeditor.ui.component.raw.RawGutterMetrics;
import me.noramibu.itemeditor.ui.component.raw.RawSyntaxHighlighter;
import me.noramibu.itemeditor.ui.component.raw.RawTextDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix3x2fStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
    private static final int COLOR_ESCAPE = 0xFFFDE68A;
    private static final int COLOR_NUMBER_SUFFIX = 0xFFEAB308;
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
    private static final int FOLD_MARKER_WIDTH = 8;
    private static final int FOLD_MARKER_TEXT_PADDING = 2;
    private static final int INDENT_SIZE = 4;

    private final EventStream<OnChanged> changedEvents = OnChanged.newStream();
    private final RawTextDocument document = new RawTextDocument("", HISTORY_LIMIT, HISTORY_CHAR_BUDGET);
    private final RawSyntaxHighlighter syntaxHighlighter = new RawSyntaxHighlighter();
    private final RawEditorRenderer renderer = new RawEditorRenderer();
    private final RawFontMetrics fontMetrics = new RawFontMetrics(100);

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
    private boolean wordWrap = true;
    private boolean horizontalScroll;
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
    private RawEditorLayout layout = RawEditorLayout.empty();
    private int[] lineDepthStarts = new int[]{0};
    private boolean lineDepthStartsDirty = true;
    private int contentHeight = 1;
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
        this.fontMetrics.setFontSizePercent(clamped);
        this.cachedTextScale = Float.NaN;
        this.wrapLayoutWidth = -1;
        this.applyFoldVisibility();
        this.ensureCursorVisible();
        this.notifyViewportChanged();
        return this;
    }

    public String getValue() {
        return this.document.text();
    }

    public RawTextAreaComponent text(String value) {
        this.commitTextChange(value == null ? "" : value, this.cursor, this.cursor);
        return this;
    }

    public int caretLine() {
        return this.document.lineIndexForOffset(this.cursor) + 1;
    }

    public int caretColumn() {
        int line = this.document.lineIndexForOffset(this.cursor);
        return (this.cursor - this.document.lineStart(line)) + 1;
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
        return this.document.canUndo();
    }

    public boolean canRedo() {
        return this.document.canRedo();
    }

    public List<HistorySnapshot> undoHistorySnapshot() {
        return this.document.undoHistorySnapshot().stream()
                .map(this::fromDocumentHistorySnapshot)
                .toList();
    }

    public List<HistorySnapshot> redoHistorySnapshot() {
        return this.document.redoHistorySnapshot().stream()
                .map(this::fromDocumentHistorySnapshot)
                .toList();
    }

    public RawTextAreaComponent restoreEditorState(
            int cursor,
            int selection,
            double scrollAmount,
            List<HistorySnapshot> undoHistory,
            List<HistorySnapshot> redoHistory
    ) {
        this.document.restoreEditorState(
                cursor,
                selection,
                this.toDocumentHistorySnapshots(undoHistory),
                this.toDocumentHistorySnapshots(redoHistory)
        );
        this.syncFromDocument();
        this.scrollAmount = scrollAmount;
        this.clampScrollAmount();
        this.clampHorizontalScrollAmount();
        this.notifyViewportChanged();
        this.historyChanged.run();
        return this;
    }

    public void undo() {
        this.document.undo(this.scrollAmount).ifPresent(restored -> {
            this.restoreDocumentState(restored);
            this.historyChanged.run();
            this.changedEvents.sink().onChanged(
                    this.text,
                    ChangeDelta.fullReplace(restored.previousLength(), this.text)
            );
        });
    }

    public void redo() {
        this.document.redo(this.scrollAmount).ifPresent(restored -> {
            this.restoreDocumentState(restored);
            this.historyChanged.run();
            this.changedEvents.sink().onChanged(
                    this.text,
                    ChangeDelta.fullReplace(restored.previousLength(), this.text)
            );
        });
    }

    public void replaceRange(int start, int end, String replacement) {
        this.applyDocumentEdit(this.document.replaceRange(start, end, replacement, this.scrollAmount));
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
        double screenX = this.currentGuiMouseX();
        double screenY = this.currentGuiMouseY();
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
        int targetCursor = this.cursorForPoint(screenX, screenY, click.hasShiftDown());
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
        this.syncSelectionToDocument();

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

        double screenX = this.currentGuiMouseX();
        double screenY = this.currentGuiMouseY();

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

        int targetCursor = this.cursorForPoint(screenX, screenY, true);
        if (targetCursor < 0) {
            return true;
        }
        this.cursor = targetCursor;
        this.syncSelectionToDocument();
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
        this.scrollAmount = before - amount * this.lineHeightWithSpacing();
        this.clampScrollAmount();
        if (Double.compare(before, this.scrollAmount) != 0) {
            this.notifyViewportChanged();
            return true;
        }
        if (this.canHorizontalScroll()) {
            double beforeHorizontal = this.horizontalScrollAmount;
            this.horizontalScrollAmount = beforeHorizontal - amount * Math.max(8d, this.contentWidth() / 8d);
            this.clampHorizontalScrollAmount();
            if (Double.compare(beforeHorizontal, this.horizontalScrollAmount) != 0) {
                this.notifyViewportChanged();
                return true;
            }
        }
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
            switch (keyCode) {
                case GLFW.GLFW_KEY_Z -> {
                    if (shift) {
                        this.redo();
                    } else {
                        this.undo();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_Y -> {
                    this.redo();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    this.copySelectionToClipboard(false);
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    this.copySelectionToClipboard(true);
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    this.pasteClipboard();
                    return true;
                }
                case GLFW.GLFW_KEY_A -> {
                    this.document.selectAll();
                    this.syncFromDocument();
                    this.ensureCursorVisible();
                    this.notifyViewportChanged();
                    return true;
                }
                case GLFW.GLFW_KEY_R -> {
                    return true;
                }
                case GLFW.GLFW_KEY_SPACE -> {
                    if (this.autocompleteRefreshRequested.getAsBoolean()) {
                        return true;
                    }
                }
                default -> {
                }
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (hasAutocompletePopup || !this.ghostSuggestion.isBlank()) {
                    this.autocompleteEntries = List.of();
                    this.autocompleteSelected = -1;
                    this.ghostSuggestion = "";
                    return true;
                }
                return super.onKeyPress(input);
            }
            case GLFW.GLFW_KEY_TAB -> {
                if (shift) {
                    this.outdentSelection();
                } else {
                    this.indentSelection();
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (shift && hasAutocompletePopup && this.autocompleteRequested.getAsBoolean()) {
                    return true;
                }
                this.insertNewlineWithAutoIndent();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                if (hasAutocompletePopup && shift) {
                    this.autocompletePreviousRequested.getAsBoolean();
                } else {
                    this.moveVertical(-1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (hasAutocompletePopup && shift) {
                    this.autocompleteNextRequested.getAsBoolean();
                } else {
                    this.moveVertical(1, shift);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                this.moveHorizontal(ctrl ? this.previousWordBoundary(this.cursor) : this.previousCodePoint(this.cursor), shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                this.moveHorizontal(ctrl ? this.nextWordBoundary(this.cursor) : this.nextCodePoint(this.cursor), shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                int target = ctrl ? 0 : this.lineStarts[this.lineIndexForCursor(this.cursor)];
                this.moveHorizontal(target, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                int target = ctrl ? this.text.length() : this.lineEnd(this.lineIndexForCursor(this.cursor));
                this.moveHorizontal(target, shift);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_UP -> {
                this.moveVertical(-this.visibleLineCount(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                this.moveVertical(this.visibleLineCount(), shift);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                this.deleteBackward(ctrl);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                this.deleteForward(ctrl);
                return true;
            }
            default -> {
                return super.onKeyPress(input);
            }
        }
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
        this.applyDocumentEdit(this.document.replaceSelection(insert, this.scrollAmount));
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
            if (removable == 0) {
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
            if (removable == 0) {
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
        this.syncSelectionToDocument();
        Minecraft.getInstance().keyboardHandler.setClipboard(this.document.selectedText());
        if (cut) {
            this.applyDocumentEdit(this.document.cutSelection(this.scrollAmount).edit());
        }
    }

    private void pasteClipboard() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) return;
        clipboard = this.applyVirtualCaretPadding(clipboard);
        this.applyDocumentEdit(this.document.pasteReplacingSelection(clipboard, this.scrollAmount));
    }

    private void deleteBackward(boolean byWord) {
        this.applyDocumentEdit(this.document.deleteBackward(byWord, this.scrollAmount));
    }

    private void deleteForward(boolean byWord) {
        this.applyDocumentEdit(this.document.deleteForward(byWord, this.scrollAmount));
    }

    private void moveHorizontal(int target, boolean keepSelection) {
        this.document.moveCaret(target, keepSelection);
        this.syncFromDocument();
        this.clearVirtualCaret();
        this.ensureCursorVisible();
        this.notifyViewportChanged();
    }

    private void moveVertical(int deltaLines, boolean keepSelection) {
        int currentVisibleLine = this.visibleLineForCursorIndex(this.cursor);
        int targetX = this.cursorLocalVisualX(currentVisibleLine);
        int targetVisibleLine = Math.clamp(currentVisibleLine + deltaLines, 0, this.layout.rowCount() - 1);
        int target = this.cursorForVisibleLineAndX(targetVisibleLine, targetX);
        this.document.moveCaret(target, keepSelection);
        this.syncFromDocument();
        this.clearVirtualCaret();
        this.ensureCursorVisible();
        this.notifyViewportChanged();
    }

    private int cursorForVisibleLineAndX(int visibleLine, int targetX) {
        return this.layout.cursorForRowAndX(visibleLine, targetX, this.fontMetrics);
    }

    private int cursorLocalVisualX(int visibleLine) {
        return this.layout.localVisualX(visibleLine, this.cursor, this.fontMetrics);
    }

    private int previousCodePoint(int index) {
        return this.document.previousCodePoint(index);
    }

    private int nextCodePoint(int index) {
        return this.document.nextCodePoint(index);
    }

    private int previousWordBoundary(int index) {
        return this.document.previousWordBoundary(index);
    }

    private int nextWordBoundary(int index) {
        return this.document.nextWordBoundary(index);
    }

    private void selectWordAt(int cursorIndex) {
        if (this.text.isEmpty()) {
            this.cursor = 0;
            this.selectionCursor = 0;
            this.syncSelectionToDocument();
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
        this.syncSelectionToDocument();
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
        this.applyDocumentEdit(this.document.replaceContent(
                nextText,
                newCursor,
                newSelection,
                this.scrollAmount,
                -1,
                -1,
                ""
        ));
    }

    private void applyDocumentEdit(RawTextDocument.EditResult edit) {
        this.syncFromDocument();
        this.lineDepthStartsDirty = true;
        this.syntaxHighlighter.clear();
        this.clearVirtualCaret();

        boolean foldStructuralEdit = edit.changed()
                && edit.start() >= 0
                && (this.containsFoldStructuralChar(edit.removedText())
                || this.containsFoldStructuralChar(edit.replacement()));
        boolean autocompleteStructuralEdit = edit.changed()
                && edit.start() >= 0
                && (foldStructuralEdit
                || edit.lineCountChanged()
                || this.containsAutocompleteStructuralChar(edit.removedText())
                || this.containsAutocompleteStructuralChar(edit.replacement()));

        if (edit.changed()) {
            this.historyChanged.run();
            if (edit.incrementalLineStarts()) {
                if (!foldStructuralEdit) {
                    if (edit.lineDelta() == 0) {
                        this.applyFoldVisibility();
                    } else if (edit.startLineBefore() >= 0) {
                        this.shiftFoldRegionsAfterLine(edit.startLineBefore(), edit.lineDelta());
                        this.applyFoldVisibility();
                    } else {
                        this.rebuildFoldLayout();
                    }
                } else {
                    this.rebuildFoldLayout();
                }
            } else {
                this.rebuildFoldLayout();
            }
        }

        this.ensureCursorVisible();
        this.notifyViewportChanged();
        if (edit.changed()) {
            ChangeDelta delta = edit.start() >= 0
                    ? new ChangeDelta(edit.start(), edit.end(), edit.replacement(), autocompleteStructuralEdit)
                    : ChangeDelta.fullReplace(edit.previousLength(), this.text);
            this.changedEvents.sink().onChanged(this.text, delta);
        } else {
            this.changedEvents.sink().onChanged(this.text, ChangeDelta.none(this.text.length()));
        }
    }

    private void restoreDocumentState(RawTextDocument.RestoredState restored) {
        this.syncFromDocument();
        this.lineDepthStartsDirty = true;
        this.syntaxHighlighter.clear();
        this.clearVirtualCaret();
        this.scrollAmount = restored.scroll();
        this.rebuildFoldLayout();
        this.clampScrollAmount();
        this.clampHorizontalScrollAmount();
        this.notifyViewportChanged();
    }

    private void syncFromDocument() {
        this.text = this.document.text();
        this.cursor = this.document.caret();
        this.selectionCursor = this.document.anchor();
        this.lineStarts = this.document.lineStarts();
    }

    private void syncSelectionToDocument() {
        this.document.setSelection(this.selectionCursor, this.cursor);
        this.syncFromDocument();
    }

    private HistorySnapshot fromDocumentHistorySnapshot(RawTextDocument.HistorySnapshot snapshot) {
        return new HistorySnapshot(snapshot.text(), snapshot.caret(), snapshot.anchor(), snapshot.scroll());
    }

    private List<RawTextDocument.HistorySnapshot> toDocumentHistorySnapshots(List<HistorySnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        List<RawTextDocument.HistorySnapshot> converted = new ArrayList<>(snapshots.size());
        for (HistorySnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            converted.add(new RawTextDocument.HistorySnapshot(
                    snapshot.text(),
                    snapshot.cursor(),
                    snapshot.selection(),
                    snapshot.scroll()
            ));
        }
        return converted;
    }

    private void resetText(String value) {
        this.document.reset(value);
        this.syncFromDocument();
        this.lineDepthStartsDirty = true;
        this.syntaxHighlighter.clear();
        this.clearVirtualCaret();
        this.rebuildFoldLayout();
        this.scrollAmount = 0;
        this.horizontalScrollAmount = 0;
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
        if (availableWidth == 0) return;

        String visibleGhost = this.trimToWidthRaw(this.ghostSuggestion, availableWidth);
        if (visibleGhost.isEmpty()) return;

        int ghostWidth = this.rawTextWidth(visibleGhost);
        int suffixWidth = Math.max(0, availableWidth - ghostWidth);
        String visibleSuffix = suffixWidth == 0 ? "" : this.trimToWidthRaw(lineSuffix, suffixWidth);

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
        int preferred = AUTOCOMPLETE_MIN_WIDTH;
        int limit = Math.min(this.autocompleteEntries.size(), AUTOCOMPLETE_MAX_ROWS);
        for (int index = 0; index < limit; index++) {
            AutocompletePopupEntry entry = this.autocompleteEntries.get(index);
            String label = entry.label();
            String detail = entry.detail();
            int lineWidth = this.rawTextWidth(detail.isBlank() ? label : (label + "  " + detail));
            preferred = Math.max(preferred, lineWidth + (AUTOCOMPLETE_PADDING * 2) + 2);
        }
        return Math.clamp(preferred, AUTOCOMPLETE_MIN_WIDTH, maxWidth);
    }

    private int autocompleteRowHeight() {
        return Math.max(12, this.scaledFontLineHeight() + 4);
    }

    private int autocompleteWindowStart(int selected, int size, int visibleRows) {
        if (size == visibleRows) {
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
        int cursorX = this.cursorXForLine(contentLeft, lineIndex);
        return new CaretAnchor(cursorX, lineY);
    }

    private void renderLineNumbers(OwoUIGraphics context, int top, int visibleHeight, int lineHeight) {
        int renderedScroll = this.renderedScroll();
        int activeVisibleLine = this.visibleLineForCursorIndex(this.cursor);
        int gutterRight = this.gutterRight() - GUTTER_LINE_RIGHT_INSET;
        int foldMarkerLeft = this.foldMarkerLeft();
        int foldMarkerRight = this.foldMarkerRight();
        int minNumberX = this.innerLeft() + this.gutterMetrics().numberLeftOffset();
        for (int visibleLine = 0; visibleLine < this.layout.rowCount(); visibleLine++) {
            int lineIndex = this.actualLineForVisibleLine(visibleLine);
            int lineY = top + visibleLine * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) continue;
            if (this.wordWrap && this.rowSegmentStart(visibleLine) > 0) {
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
                int markerX = foldMarkerLeft + RawGutterMetrics.centeredMarkerTextOffset(
                        this.fontMetrics,
                        marker,
                        this.foldMarkerWidth()
                );
                this.drawRawText(context, marker, markerX, lineY, markerColor);
            }
        }
    }

    private void renderText(OwoUIGraphics context, int contentLeft, int top, int contentWidth, int visibleHeight, int lineHeight) {
        int renderedScroll = this.renderedScroll();
        int horizontalOffset = this.horizontalRenderOffset();
        int[] depthStarts = this.lineDepthStartsForRender();
        int syntaxBudget = MAX_SYNTAX_RENDER_BUDGET_CHARS;
        int lastBudgetedLine = -1;
        for (int visibleLine = 0; visibleLine < this.layout.rowCount(); visibleLine++) {
            int lineIndex = this.actualLineForVisibleLine(visibleLine);
            int lineY = top + visibleLine * lineHeight - renderedScroll;
            if (lineY + lineHeight < top || lineY > top + visibleHeight) continue;
            int start = this.lineStarts[lineIndex];
            int end = this.lineEnd(lineIndex);
            String line = this.text.substring(start, end);
            int segmentStart = this.rowSegmentStart(visibleLine);
            int segmentEnd = this.rowSegmentEnd(visibleLine);
            RawEditorLayout.VisualRow row = this.layout.row(visibleLine);
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
                this.renderLineSyntaxRow(context, contentLeft, lineY, contentWidth, lineIndex, line, baseDepth, row);
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

    private void renderLineSyntaxRow(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            int lineIndex,
            String line,
            int baseDepth,
            RawEditorLayout.VisualRow row
    ) {
        int lineStart = this.lineStarts[lineIndex];
        int rowStart = row.documentStart();
        int rowEnd = row.documentEnd();
        if (rowEnd <= rowStart) {
            return;
        }
        int horizontalOffset = this.horizontalRenderOffset();
        int contentRight = x + availableWidth;
        int measuredLocal = Math.max(0, row.localStart());
        double advanceFromRowStart = 0d;
        for (RawSyntaxHighlighter.SyntaxSpan span
                : this.syntaxHighlighter.spansForLine(lineIndex, lineStart, line, baseDepth)) {
            if (span.endOffset() <= rowStart) {
                continue;
            }
            if (span.startOffset() >= rowEnd) {
                break;
            }
            int localStart = Math.max(0, Math.max(span.startOffset(), rowStart) - lineStart);
            int localEnd = Math.min(line.length(), Math.min(span.endOffset(), rowEnd) - lineStart);
            if (localEnd <= localStart) {
                continue;
            }
            if (localStart > measuredLocal) {
                advanceFromRowStart += this.rawTextAdvance(line, measuredLocal, localStart);
            }
            String token = line.substring(localStart, localEnd);
            int tokenX = this.syntaxTokenX(x, horizontalOffset, advanceFromRowStart);
            if (tokenX < x) {
                int clipStart = Math.clamp(this.rawIndexAtWidth(token, x - tokenX), 0, token.length());
                token = token.substring(clipStart);
                if (token.isEmpty()) {
                    advanceFromRowStart += this.rawTextAdvance(line, localStart, localEnd);
                    measuredLocal = localEnd;
                    continue;
                }
                tokenX = this.syntaxTokenX(
                        x,
                        horizontalOffset,
                        advanceFromRowStart + this.rawTextAdvance(line, localStart, localStart + clipStart)
                );
            }
            if (tokenX >= contentRight) {
                break;
            }
            this.renderSyntaxRun(
                    context,
                    tokenX,
                    y,
                    contentRight - tokenX,
                    new SyntaxRun(this.syntaxKind(span.kind()), token, span.color())
            );
            advanceFromRowStart += this.rawTextAdvance(line, localStart, localEnd);
            measuredLocal = localEnd;
        }
    }

    private int syntaxTokenX(int contentLeft, int horizontalOffset, double advanceFromRowStart) {
        return contentLeft - horizontalOffset + (int) Math.ceil(Math.max(0d, advanceFromRowStart));
    }

    private SyntaxRunKind syntaxKind(RawSyntaxHighlighter.SyntaxKind kind) {
        return switch (kind) {
            case STRING -> SyntaxRunKind.STRING;
            case NUMERIC -> SyntaxRunKind.NUMERIC;
            case COLOR_LITERAL -> SyntaxRunKind.HEX;
            case PLAIN -> SyntaxRunKind.PLAIN;
        };
    }

    private void renderSyntaxRun(OwoUIGraphics context, int x, int y, int availableWidth, SyntaxRun run) {
        switch (run.kind()) {
            case PLAIN -> this.drawSyntaxToken(context, x, y, availableWidth, run.text(), run.color());
            case STRING -> this.renderStringToken(context, x, y, availableWidth, run.text());
            case NUMERIC -> this.renderNumericToken(context, x, y, availableWidth, run.text());
            case HEX -> this.drawSyntaxToken(
                    context,
                    x,
                    y,
                    availableWidth,
                    run.text(),
                    run.color(),
                    true
            );
        }
    }

    private void renderSelection(OwoUIGraphics context, int contentLeft, int top, int contentWidth, int visibleHeight, int lineHeight) {
        if (!this.hasSelection()) return;
        for (RawEditorRenderer.RenderRect rect : this.renderer.selectionRectangles(
                this.layout,
                this.document.selection(),
                this.fontMetrics,
                contentLeft,
                contentWidth,
                top,
                visibleHeight,
                this.renderedScroll(),
                this.horizontalRenderOffset(),
                lineHeight,
                this.scaledFontLineHeight()
        )) {
            context.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), COLOR_SELECTION);
        }
    }

    private void renderCursor(OwoUIGraphics context, int contentLeft, int top, int visibleHeight, int lineHeight) {
        if (!this.focused || !this.shouldBlinkCursor()) return;
        int renderedScroll = this.renderedScroll();
        int lineIndex = this.lineIndexForCursor(this.cursor);
        int visibleLine = this.visibleLineForCursorIndex(this.cursor);
        int lineY = top + visibleLine * lineHeight - renderedScroll;
        if (lineY + lineHeight < top || lineY > top + visibleHeight) return;
        int cursorX = this.cursorXForLine(contentLeft, lineIndex);
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
        if (lineIndex >= this.lineStarts.length) {
            return;
        }
        if (lineIndex < this.hiddenLines.length && this.hiddenLines[lineIndex]) {
            return;
        }

        int underlineHeight = Math.max(1, UiFactory.scaledPixels(1));
        for (RawEditorRenderer.RenderRect rect : this.renderer.errorUnderlineRectangles(
                this.layout,
                this.document,
                this.fontMetrics,
                lineIndex,
                this.errorColumn,
                this.errorLength,
                contentLeft,
                contentWidth,
                top,
                visibleHeight,
                this.renderedScroll(),
                this.horizontalRenderOffset(),
                lineHeight,
                this.scaledFontLineHeight(),
                underlineHeight
        )) {
            context.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), COLOR_ERROR_UNDERLINE);
        }
    }

    private int cursorXForLine(int contentLeft, int lineIndex) {
        int lineEnd = this.lineEnd(lineIndex);
        if (this.shouldUseVirtualCaretX(lineIndex, lineEnd)) {
            int clampedVirtualX = Math.clamp(this.virtualCaretLocalX, 0, Math.max(0, this.contentWidth() - 1));
            return contentLeft + clampedVirtualX;
        }
        return this.layout.caretX(this.cursor, this.fontMetrics, contentLeft, this.horizontalRenderOffset());
    }

    private boolean shouldUseVirtualCaretX(int lineIndex, int lineEnd) {
        return this.virtualCaretLine == lineIndex
                && this.virtualCaretLocalX >= 0
                && !this.hasSelection()
                && this.cursor == lineEnd;
    }

    private int cursorForPoint(double mouseX, double mouseY, boolean expandWrappedLineEnd) {
        this.clearVirtualCaret();
        return this.layout.offsetAt(
                mouseX,
                mouseY,
                this.fontMetrics,
                this.innerTop(),
                this.scrollbarBottom(),
                this.contentLeft(),
                this.renderedScroll(),
                this.horizontalRenderOffset(),
                this.lineHeightWithSpacing(),
                this.endOfLineClickTolerance(),
                expandWrappedLineEnd
        );
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
        return Math.clamp(thumb, HORIZONTAL_SCROLLBAR_MIN_THUMB, Math.max(HORIZONTAL_SCROLLBAR_MIN_THUMB, trackWidth));
    }

    private int drawSyntaxToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token,
            int color
    ) {
        return this.drawSyntaxToken(context, x, y, availableWidth, token, color, false);
    }

    private int drawSyntaxToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token,
            int color,
            boolean hexColorChip
    ) {
        String visible = this.visibleToken(token, availableWidth);
        if (visible.isEmpty()) {
            return 0;
        }

        if (!hexColorChip) {
            this.drawRawText(context, visible, x, y, color);
            return this.rawTextWidth(visible);
        }

        int width = this.rawTextWidth(visible);
        if (width <= 0) {
            return 0;
        }

        int top = y - 1;
        int bottom = y + this.scaledFontLineHeight() + 1;
        int right = x + width;
        int chipBackground = this.hexChipBackground(color);
        int chipBorder = this.hexChipBorder(color);
        context.fill(x, top, right, bottom, chipBackground);
        this.drawRectBorder(context, x, top, right, bottom, chipBorder);

        int textColor = this.hexChipTextColor(color);
        this.drawRawText(context, visible, x, y, textColor);
        return width;
    }

    private String visibleToken(String token, int availableWidth) {
        if (availableWidth <= 0 || token.isEmpty()) {
            return "";
        }
        return this.trimToWidthRaw(token, availableWidth);
    }

    private void renderStringToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token
    ) {
        if (availableWidth <= 0 || token.isEmpty()) {
            return;
        }
        int index = 0;
        while (index < token.length()) {
            int partX = x + (int) Math.ceil(this.rawTextAdvance(token, 0, index));
            int partAvailable = availableWidth - (partX - x);
            if (partAvailable <= 0) {
                break;
            }
            char c = token.charAt(index);
            if (c == '\\') {
                int escapeEnd = this.scanEscapeSequence(token, index);
                this.drawSyntaxToken(
                        context,
                        partX,
                        y,
                        partAvailable,
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
            this.drawSyntaxToken(
                    context,
                    partX,
                    y,
                    partAvailable,
                    token.substring(index, next),
                    COLOR_STRING
            );
            index = next;
        }
    }

    private void renderNumericToken(
            OwoUIGraphics context,
            int x,
            int y,
            int availableWidth,
            String token
    ) {
        if (availableWidth <= 0 || token.isEmpty()) {
            return;
        }
        int suffixStart = this.numericSuffixStart(token);
        if (suffixStart <= 0 || suffixStart >= token.length()) {
            this.drawSyntaxToken(context, x, y, availableWidth, token, COLOR_NUMBER);
            return;
        }

        int prefixWidth = this.drawSyntaxToken(
                context,
                x,
                y,
                availableWidth,
                token.substring(0, suffixStart),
                COLOR_NUMBER
        );
        if (prefixWidth >= availableWidth) {
            return;
        }
        int suffixX = x + (int) Math.ceil(this.rawTextAdvance(token, 0, suffixStart));
        int suffixAvailable = availableWidth - (suffixX - x);
        if (suffixAvailable <= 0) {
            return;
        }
        this.drawSyntaxToken(
                context,
                suffixX,
                y,
                suffixAvailable,
                token.substring(suffixStart),
                COLOR_NUMBER_SUFFIX
        );
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

    private int[] lineDepthStartsForRender() {
        if (!this.lineDepthStartsDirty
                && this.lineDepthStarts != null
                && this.lineDepthStarts.length == this.lineStarts.length) {
            return this.lineDepthStarts;
        }
        this.lineDepthStarts = this.syntaxHighlighter.lineDepthStarts(
                this.text,
                this.lineStarts,
                MAX_SYNTAX_DEPTH_SCAN_TOTAL_CHARS
        );
        this.lineDepthStartsDirty = false;
        return this.lineDepthStarts;
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

    private int lineEnd(int lineIndex) {
        return this.document.lineEnd(lineIndex);
    }

    private int lineIndexForCursor(int cursorIndex) {
        return this.document.lineIndexForOffset(cursorIndex);
    }

    private int lineHeightWithSpacing() {
        return this.fontMetrics.lineHeightWithSpacing(LINE_SPACING);
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
        return this.foldMarkerLeft() + this.foldMarkerWidth();
    }

    private int foldMarkerWidth() {
        return this.gutterMetrics().foldMarkerWidth();
    }

    private int actualLineForVisibleLine(int visibleLine) {
        return this.layout.actualLineForRow(visibleLine);
    }

    private int rowSegmentStart(int visibleLine) {
        return this.layout.row(visibleLine).localStart();
    }

    private int rowSegmentEnd(int visibleLine) {
        return this.layout.row(visibleLine).localEnd();
    }

    private int visibleLineForCursorIndex(int cursorIndex) {
        return this.layout.rowForOffset(cursorIndex);
    }

    private FoldRegion foldRegionStartingAt(int actualLine) {
        if (actualLine < 0 || actualLine >= this.foldByStartLine.length) {
            return null;
        }
        return this.foldByStartLine[actualLine];
    }

    private boolean tryToggleFold(double mouseX, double mouseY) {
        if (mouseX < this.foldMarkerLeft() || mouseX > this.foldMarkerRight()) {
            return false;
        }
        int localY = (int) Math.floor(mouseY - this.innerTop() + this.renderedScroll());
        if (localY < 0) {
            return false;
        }
        int visibleLine = Math.clamp(localY / this.lineHeightWithSpacing(), 0, this.layout.rowCount() - 1);
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
        int lineCount = this.lineStarts.length;
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

        int wrapWidth = Math.max(1, this.contentWidth());
        this.layout = RawEditorLayout.build(
                this.text,
                this.lineStarts,
                this.fontMetrics,
                wrapWidth,
                this.lineHeightWithSpacing(),
                this.wordWrap,
                this.layoutFoldSpans()
        );
        for (int line = 0; line < lineCount; line++) {
            this.hiddenLines[line] = this.layout.hiddenLine(line);
        }
        this.wrapLayoutWidth = wrapWidth;
        this.moveCursorOutOfHiddenRegion();
        this.contentHeight = this.layout.contentHeight();
        this.maxVisibleLineWidth = this.layout.maxVisibleLineWidth();
        this.clampHorizontalScrollAmount();
    }

    private List<RawEditorLayout.FoldSpan> layoutFoldSpans() {
        if (this.foldRegions.isEmpty()) {
            return List.of();
        }
        List<RawEditorLayout.FoldSpan> spans = new ArrayList<>(this.foldRegions.size());
        for (FoldRegion region : this.foldRegions) {
            spans.add(new RawEditorLayout.FoldSpan(region.startLine, region.endLine, region.collapsed));
        }
        return spans;
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
                this.syncSelectionToDocument();
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
        return this.gutterMetrics().gutterWidth();
    }

    private RawGutterMetrics.Metrics gutterMetrics() {
        return RawGutterMetrics.calculate(
                this.fontMetrics,
                this.lineStarts.length,
                GUTTER_MIN_WIDTH,
                GUTTER_PADDING,
                FOLD_MARKER_WIDTH,
                FOLD_MARKER_TEXT_PADDING,
                GUTTER_FOLD_GAP,
                GUTTER_LINE_RIGHT_INSET
        );
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
            context.drawString(Minecraft.getInstance().font, this.fontMetrics.formattedSequence(value), x, y, color, false);
            return;
        }

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(this.cachedTextScale, this.cachedTextScale);
        matrices.translate(-x, -y);
        context.drawString(Minecraft.getInstance().font, this.fontMetrics.formattedSequence(value), x, y, color, false);
        matrices.popMatrix();
    }

    private int rawTextWidth(String value) {
        return this.fontMetrics.textWidth(value);
    }

    private double rawTextAdvance(String value, int start, int end) {
        if (value == null || value.isEmpty() || end <= start) {
            return 0d;
        }
        int cursor = Math.clamp(start, 0, value.length());
        int limit = Math.clamp(end, cursor, value.length());
        double width = 0d;
        while (cursor < limit) {
            int codePoint = value.codePointAt(cursor);
            width += this.fontMetrics.glyphAdvance(codePoint);
            cursor += Character.charCount(codePoint);
        }
        return width;
    }

    private int scaledFontLineHeight() {
        return this.fontMetrics.lineHeight();
    }

    private String trimToWidthRaw(String value, int maxWidth) {
        return this.fontMetrics.trimToWidth(value, maxWidth);
    }

    private int rawIndexAtWidth(String value, double targetWidth) {
        return this.fontMetrics.indexAtWidth(value, targetWidth);
    }

    private void ensureTextScaleCache() {
        float scale = this.fontMetrics.textScale();
        if (Math.abs(scale - this.cachedTextScale) < 0.001F) {
            return;
        }
        this.cachedTextScale = scale;
        this.syntaxHighlighter.clear();
        this.lineDepthStartsDirty = true;
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
