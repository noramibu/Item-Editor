package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.mixin.ui.access.MultilineTextFieldAccessor;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.inject.GreedyInputUIComponent;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextLayoutUtil;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class RichTextAreaComponent extends TextAreaComponent implements GreedyInputUIComponent {

    private static final int LINE_HEIGHT = 9;
    private static final int SCROLLBAR_BASE_THICKNESS = 7;
    private static final int TEXT_LEFT_PADDING = 3;
    private static final int TEXT_RIGHT_PADDING = 20;
    private static final int HISTORY_LIMIT = 128;
    private static String richClipboardPlain = "";
    private static String richClipboardMarkup = "";

    private final Font font;
    private final RichTextRenderer renderer;
    private final RichTextInputController inputController = new RichTextInputController();
    private final EventStream<OnDocumentChanged> documentChangedEvents = OnDocumentChanged.newStream();
    private final EventStream<OnViewportChanged> viewportChangedEvents = OnViewportChanged.newStream();

    private RichTextDocument document = RichTextDocument.empty();
    private RichTextStyle pendingStyle = RichTextStyle.EMPTY;
    private RichTextStyle defaultInsertionStyle = RichTextStyle.EMPTY;
    private Function<RichTextDocument, String> validator = ignored -> null;
    private Consumer<String> rejectionHandler = ignored -> {};
    private String placeholder = "";
    private int defaultTextColor = 0xE7ECF3;
    private int placeholderColor = 0x906A7682;
    private int selectionColor = 0xC56E93FF;
    private int caretColor = 0xFFF2F5F8;
    private int backgroundColor = 0xCC111722;
    private int borderColor = 0xFF445066;
    private double horizontalScrollAmount;
    private List<RichTextLayoutUtil.LineLayout> displayLines = List.of(new RichTextLayoutUtil.LineLayout(0, 0, Component.empty(), new int[]{0}, new float[]{0f}));
    private int maxLineWidth;
    private List<RichTextLayoutUtil.EventOverlayRange> eventOverlayRanges = List.of();
    private RichTextLayoutUtil.LogicalMetrics logicalMetrics = new RichTextLayoutUtil.LogicalMetrics(0, 0);
    private int contentHeight = LINE_HEIGHT;
    private boolean layoutDirty = true;
    private int committedLayoutContentWidth = -1;
    private int committedLayoutViewportHeight = -1;
    private boolean lineWrap = true;
    private int lineWrapWidthOverride = -1;
    private int lineWrapPadding;
    private boolean showSoftWrapMarkers;
    private boolean renderStructuredEvents;
    private boolean renderStructuredObjects;
    private boolean logicalCounterEnabled;
    private boolean preservePendingStyleOnFocus;
    private boolean pendingStylePinned;
    private boolean suppressFocusSyncOnce;
    private final ArrayDeque<HistoryState> undoHistory = new ArrayDeque<>();
    private final ArrayDeque<HistoryState> redoHistory = new ArrayDeque<>();

    public RichTextAreaComponent(Sizing horizontalSizing, Sizing verticalSizing, RichTextDocument document) {
        super(horizontalSizing, verticalSizing);
        this.font = Minecraft.getInstance().font;
        this.renderer = new RichTextRenderer(this.font, LINE_HEIGHT);
        this.cursorStyle(CursorStyle.TEXT);
        this.editBox.setCursorListener(this::afterCursorMove);
        this.setDocument(document);
    }

    public EventSource<OnDocumentChanged> onDocumentChanged() {
        return this.documentChangedEvents.source();
    }

    public EventSource<OnViewportChanged> onViewportChanged() {
        return this.viewportChangedEvents.source();
    }

    public RichTextAreaComponent document(RichTextDocument document) {
        this.setDocument(document);
        return this;
    }

    public RichTextDocument document() {
        return this.document.copy();
    }

    public int displayedLineHeight() {
        return LINE_HEIGHT;
    }

    public int firstVisibleDisplayedLineIndex() {
        this.ensureLayoutCurrent();
        int index = (int) Math.floor(this.scrollAmount() / LINE_HEIGHT);
        return Math.clamp(index, 0, this.displayLines.size() - 1);
    }

    public int logicalLineNumberForDisplayedLineIndex(int displayedLineIndex) {
        this.ensureLayoutCurrent();
        if (this.displayLines.isEmpty()) {
            return Math.max(1, displayedLineIndex + 1);
        }
        if (displayedLineIndex < 0) {
            return this.displayLines.getFirst().logicalLineNumber();
        }
        if (displayedLineIndex < this.displayLines.size()) {
            return this.displayLines.get(displayedLineIndex).logicalLineNumber();
        }
        RichTextLayoutUtil.LineLayout lastLine = this.displayLines.getLast();
        return lastLine.logicalLineNumber() + (displayedLineIndex - this.displayLines.size() + 1);
    }

    public int displayedTopInset() {
        return Math.max(0, this.getInnerTop() - this.getY());
    }

    public int horizontalScrollViewportWidth() {
        return this.textViewportWidth();
    }

    public int horizontalScrollContentWidth() {
        this.ensureLayoutCurrent();
        return Math.max(this.textViewportWidth(), this.maxLineWidth);
    }

    public int horizontalScrollMaximum() {
        this.ensureLayoutCurrent();
        return this.maxHorizontalScroll();
    }

    public double horizontalScrollAmount() {
        this.ensureLayoutCurrent();
        this.clampHorizontalScrollAmount();
        return this.horizontalScrollAmount;
    }

    public void setHorizontalScrollAmount(double horizontalScrollAmount) {
        this.ensureLayoutCurrent();
        double before = this.horizontalScrollAmount;
        this.horizontalScrollAmount = Math.clamp(horizontalScrollAmount, 0d, this.maxHorizontalScroll());
        if (Double.compare(before, this.horizontalScrollAmount) != 0) {
            this.viewportChangedEvents.sink().onChanged();
        }
    }

    public RichTextAreaComponent placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public RichTextAreaComponent defaultStyle(RichTextStyle defaultStyle) {
        this.defaultInsertionStyle = defaultStyle == null ? RichTextStyle.EMPTY : defaultStyle;
        this.pendingStyle = this.resolveInsertionStyle(this.editBox.cursor());
        this.refreshLayout();
        return this;
    }

    public RichTextAreaComponent palette(int defaultTextColor, int placeholderColor, int selectionColor, int caretColor) {
        this.defaultTextColor = this.ensureVisibleAlpha(defaultTextColor);
        this.placeholderColor = this.ensureVisibleAlpha(placeholderColor);
        this.selectionColor = this.ensureVisibleAlpha(selectionColor);
        this.caretColor = this.ensureVisibleAlpha(caretColor);
        return this;
    }

    public RichTextAreaComponent chrome(int backgroundColor, int borderColor) {
        this.backgroundColor = this.ensureVisibleAlpha(backgroundColor);
        this.borderColor = this.ensureVisibleAlpha(borderColor);
        return this;
    }

    public RichTextAreaComponent validator(Function<RichTextDocument, String> validator) {
        this.validator = Objects.requireNonNullElseGet(validator, () -> ignored -> null);
        return this;
    }

    public RichTextAreaComponent onRejected(Consumer<String> rejectionHandler) {
        this.rejectionHandler = Objects.requireNonNullElse(rejectionHandler, ignored -> {});
        return this;
    }

    public RichTextAreaComponent lineWrap(boolean lineWrap) {
        if (this.lineWrap == lineWrap) {
            return this;
        }
        this.lineWrap = lineWrap;
        this.refreshLayout();
        return this;
    }

    public RichTextAreaComponent lineWrapWidthOverride(int lineWrapWidthOverride) {
        int normalized = lineWrapWidthOverride > 0 ? lineWrapWidthOverride : -1;
        if (this.lineWrapWidthOverride == normalized) {
            return this;
        }
        this.lineWrapWidthOverride = normalized;
        this.refreshLayout();
        return this;
    }

    public RichTextAreaComponent lineWrapPadding(int lineWrapPadding) {
        int normalized = Math.max(0, lineWrapPadding);
        if (this.lineWrapPadding == normalized) {
            return this;
        }
        this.lineWrapPadding = normalized;
        this.refreshLayout();
        return this;
    }

    public RichTextAreaComponent showSoftWrapMarkers(boolean showSoftWrapMarkers) {
        this.showSoftWrapMarkers = showSoftWrapMarkers;
        return this;
    }

    public RichTextAreaComponent renderStructuredObjects(boolean renderStructuredObjects) {
        if (this.renderStructuredObjects == renderStructuredObjects) {
            return this;
        }
        this.renderStructuredObjects = renderStructuredObjects;
        this.refreshLayout();
        return this;
    }

    public RichTextAreaComponent renderStructuredEvents(boolean renderStructuredEvents) {
        if (this.renderStructuredEvents == renderStructuredEvents) {
            return this;
        }
        this.renderStructuredEvents = renderStructuredEvents;
        this.refreshLayout();
        return this;
    }

    public RichTextAreaComponent structuredRenderMode(boolean renderStructured) {
        return this.renderStructuredEvents(renderStructured).renderStructuredObjects(renderStructured);
    }

    public void toggleBold() {
        this.applyStyle(RichTextStyle::toggleBold);
    }

    public void toggleItalic() {
        this.applyStyle(RichTextStyle::toggleItalic);
    }

    public void toggleUnderline() {
        this.applyStyle(RichTextStyle::toggleUnderlined);
    }

    public void toggleStrikethrough() {
        this.applyStyle(RichTextStyle::toggleStrikethrough);
    }

    public void toggleObfuscated() {
        this.applyStyle(RichTextStyle::toggleObfuscated);
    }

    public void clearFormatting() {
        this.applyStyle(ignored -> this.defaultInsertionStyle);
    }

    public void applyColor(int color) {
        this.applyStyle(style -> style.withColor(color));
    }

    public void applyGradientSelectionOrAll(int startColor, int endColor) {
        if (this.document.isEmpty()) {
            return;
        }
        HistoryState before = this.captureHistoryState();

        RichTextSelectionModel selection = this.currentSelection();
        int start = selection.hasSelection() ? selection.start() : 0;
        int end = selection.hasSelection() ? selection.end() : this.document.length();

        RichTextDocument updated = this.inputController.applyGradient(this.document, start, end, startColor, endColor);
        String rejection = this.validator.apply(updated);
        if (rejection != null) {
            this.rejectionHandler.accept(rejection);
            return;
        }

        this.document = updated;
        this.pendingStylePinned = false;
        this.pendingStyle = this.resolveInsertionStyle(this.editBox.cursor());
        this.refreshLayout();
        this.recordUndo(before);
        this.documentChangedEvents.sink().onChanged(this.document.copy());
    }

    public void capitalizeSelectionOrAll() {
        this.transformSelectionOrAll(this.inputController::toTitleCase);
    }

    public void lowercaseSelectionOrAll() {
        this.transformSelectionOrAll(text -> text.toLowerCase(Locale.ROOT));
    }

    public void insertTemplate(String templateText) {
        String template = Objects.requireNonNullElse(templateText, "");
        this.applyTextMutation(() -> {
            this.editBox.insertText(template);
            return true;
        });
    }

    public String selectedTextOr(String fallback) {
        RichTextSelectionModel selection = this.currentSelection();
        if (!selection.hasSelection()) {
            return Objects.requireNonNullElse(fallback, "");
        }
        return this.editBox.value().substring(selection.start(), selection.end());
    }

    public void wrapSelectionWithTemplate(String openToken, String closeToken, String placeholder) {
        String open = Objects.requireNonNullElse(openToken, "");
        String close = Objects.requireNonNullElse(closeToken, "");
        String fill = Objects.requireNonNullElse(placeholder, "");
        this.applyTextMutation(() -> {
            RichTextSelectionModel selection = this.currentSelection();
            int start = selection.start();
            int end = selection.end();
            String value = this.editBox.value();
            String selected = start < end ? value.substring(start, end) : fill;
            this.editBox.seekCursor(Whence.ABSOLUTE, start);
            ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(end);
            this.editBox.insertText(open + selected + close);
            return true;
        });
    }

    public void resumeEditing() {
        double preservedScroll = this.scrollAmount();
        this.preservePendingStyleOnFocus = true;
        if (this.focusHandler() != null) {
            this.focusHandler().focus(this, UIComponent.FocusSource.MOUSE_CLICK);
        }
        this.setFocused(true);
        this.setScrollAmount(preservedScroll);
        this.clampScrollAmount();
    }

    public boolean hasSelection() {
        return this.editBox.cursor() != this.selectionCursor();
    }

    public void collapseSelectionToCursor() {
        ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(this.editBox.cursor());
        this.afterCursorMove();
    }

    public void collapseUnexpectedSelection(boolean hadSelectionBeforeAction) {
        if (!hadSelectionBeforeAction && this.hasSelection()) {
            this.collapseSelectionToCursor();
        }
    }

    public void undo() {
        if (this.undoHistory.isEmpty()) {
            return;
        }

        this.redoHistory.addLast(this.captureHistoryState());
        this.trimHistory(this.redoHistory);
        this.restoreHistoryState(this.undoHistory.removeLast());
        this.documentChangedEvents.sink().onChanged(this.document.copy());
    }

    public void redo() {
        if (this.redoHistory.isEmpty()) {
            return;
        }

        this.undoHistory.addLast(this.captureHistoryState());
        this.trimHistory(this.undoHistory);
        this.restoreHistoryState(this.redoHistory.removeLast());
        this.documentChangedEvents.sink().onChanged(this.document.copy());
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean doubled) {
        if (!this.active || !this.visible || click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !this.isMouseOver(click.x(), click.y())) {
            if (!this.isMouseOver(click.x(), click.y())) {
                this.setFocused(false);
            }
            return false;
        }

        if (this.isOverScrollbar(click.x(), click.y())) {
            return super.mouseClicked(click, doubled);
        }

        boolean wasFocused = this.isFocused();
        double preservedScroll = this.scrollAmount();
        int beforeCursor = this.editBox.cursor();
        int beforeSelection = this.selectionCursor();
        this.suppressFocusSyncOnce = !wasFocused;
        this.setFocused(true);
        if (!wasFocused) {
            this.setScrollAmount(preservedScroll);
            this.clampScrollAmount();
        }

        int targetCursor = this.cursorForPoint(click.x(), click.y());
        this.editBox.setSelecting(click.hasShiftDown());
        if (doubled) {
            RichTextLayoutUtil.SourceRange word = this.visualWordRangeAtCursor(targetCursor);
            ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(word.start());
            this.editBox.seekCursor(Whence.ABSOLUTE, word.end());
        } else if (click.hasShiftDown()) {
            ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(beforeSelection);
            this.editBox.seekCursor(Whence.ABSOLUTE, targetCursor);
        } else {
            ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(targetCursor);
            this.editBox.seekCursor(Whence.ABSOLUTE, targetCursor);
        }

        int afterCursor = this.editBox.cursor();
        int afterSelection = this.selectionCursor();
        boolean moved = beforeCursor != afterCursor || beforeSelection != afterSelection;
        if (!wasFocused && !moved) {
            this.setScrollAmount(preservedScroll);
            this.clampScrollAmount();
        }
        if (moved) {
            this.afterCursorMove();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent click, double deltaX, double deltaY) {
        if (!this.visible || !this.isFocused() || click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        this.editBox.setSelecting(true);
        this.editBox.seekCursor(Whence.ABSOLUTE, this.cursorForPoint(click.x(), click.y()));
        this.afterCursorMove();
        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent input) {
        if (!this.visible || !this.isFocused()) {
            return false;
        }

        if (input.isCycleFocus()) {
            return this.applyTextMutation(() -> {
                this.editBox.insertText("    ");
                return true;
            });
        }
        if (input.hasControlDownWithQuirk()) {
            if (input.key() == GLFW.GLFW_KEY_Z) {
                if (input.hasShiftDown()) {
                    this.redo();
                } else {
                    this.undo();
                }
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_Y) {
                this.redo();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_C) {
                return this.copySelectionToClipboard(false);
            }
            if (input.key() == GLFW.GLFW_KEY_X) {
                return this.copySelectionToClipboard(true);
            }
            if (input.key() == GLFW.GLFW_KEY_V) {
                return this.pasteClipboardContents();
            }
        }
        if (input.isUp()) {
            this.moveCursorVertical(-1, input.hasShiftDown());
            return true;
        }
        if (input.isDown()) {
            this.moveCursorVertical(1, input.hasShiftDown());
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_HOME) {
            this.moveCursorToLineEdge(false, input.hasShiftDown(), input.hasControlDownWithQuirk());
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_END) {
            this.moveCursorToLineEdge(true, input.hasShiftDown(), input.hasControlDownWithQuirk());
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE && this.deleteRenderedTokenAtCursor(true)) {
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_DELETE && this.deleteRenderedTokenAtCursor(false)) {
            return true;
        }

        return this.applyTextMutation(() -> this.editBox.keyPressed(input));
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent input) {
        if (!this.visible || !this.isFocused() || !input.isAllowedChatCharacter()) {
            return false;
        }

        return this.applyTextMutation(() -> {
            this.editBox.insertText(input.codepointAsString());
            return true;
        });
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.ensureLayoutCurrent();
        int viewportX = this.textViewportLeft();
        int baseX = viewportX - this.horizontalRenderOffset();
        int baseY = this.getInnerTop();
        int renderedScroll = (int) Math.floor(this.scrollAmount());
        int clipLeft = this.textViewportLeft();
        int clipTop = this.getInnerTop() + renderedScroll;
        int clipRight = clipLeft + this.textViewportWidth();
        int clipBottom = clipTop + this.textViewportHeight();
        RichTextSelectionModel selection = this.currentSelection();

        context.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
        try {
            if (this.document.isEmpty() && !this.isFocused() && !this.placeholder.isBlank()) {
                this.renderer.renderPlaceholder(context, this.placeholder, viewportX, baseY, this.placeholderColor);
            } else {
                for (int lineIndex = 0; lineIndex < this.displayLines.size(); lineIndex++) {
                    RichTextLayoutUtil.LineLayout line = this.displayLines.get(lineIndex);
                    int lineY = baseY + lineIndex * LINE_HEIGHT;
                    if (!this.withinTextViewportTopBottom(lineY, lineY + LINE_HEIGHT)) {
                        continue;
                    }

                    if (selection.hasSelection()) {
                        this.renderer.renderSelection(
                                context,
                                line,
                                baseX,
                                lineY,
                                selection.start(),
                                selection.end(),
                                this.selectionColor,
                                this.document.plainText(),
                                this.renderStructuredEvents,
                                this.renderStructuredObjects
                        );
                    }

                    this.renderer.renderLine(
                            context,
                            line,
                            baseX,
                            lineY,
                            this.defaultTextColor,
                            this.renderStructuredEvents,
                            this.eventOverlayRanges
                    );
                    if (this.showSoftWrapMarkers && this.isSoftWrappedLine(lineIndex)) {
                        this.renderer.renderPlaceholder(context, "↩", baseX + Math.max(0, Math.round(line.xForPosition(line.end())) - 4), lineY, 0xA08A5E);
                    }
                }
            }

            if (this.renderer.shouldRenderCaret(this.isFocused())) {
                int cursor = selection.cursor();
                int lineIndex = this.indexForCursor(cursor);
                RichTextLayoutUtil.LineLayout line = this.displayLines.get(lineIndex);
                int lineY = baseY + lineIndex * LINE_HEIGHT;
                if (this.withinTextViewportTopBottom(lineY, lineY + LINE_HEIGHT)) {
                    this.renderer.renderCaret(
                            context,
                            line,
                            baseX,
                            lineY,
                            cursor,
                            this.caretColor,
                            this.document.plainText(),
                            this.renderStructuredEvents,
                            this.renderStructuredObjects
                    );
                }
            }
        } finally {
            context.disableScissor();
        }

        if (this.logicalCounterEnabled) {
            String counter = Integer.toString(this.logicalMetrics.charCount());
            int counterX = viewportX + this.textViewportWidth() - this.font.width(counter);
            int counterY = baseY + this.textViewportHeight() + 3 + (int) Math.floor(this.scrollAmount());
            context.text(this.font, Component.literal(counter), counterX, counterY, 0xA0A0A0, false);
        }
    }

    @Override
    public int getInnerHeight() {
        this.ensureLayoutCurrent();
        return Math.max(this.contentHeight, LINE_HEIGHT);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            return;
        }
        if (this.suppressFocusSyncOnce) {
            this.suppressFocusSyncOnce = false;
            this.preservePendingStyleOnFocus = false;
            return;
        }
        if (this.preservePendingStyleOnFocus) {
            this.preservePendingStyleOnFocus = false;
        } else {
            this.syncPendingStyleFromCursorOnly();
        }
    }

    @Override
    protected void extractBackground(@NotNull GuiGraphicsExtractor context) {
        this.ensureLayoutCurrent();
        int baseX = this.getInnerLeft();
        int baseY = this.getInnerTop();
        int innerWidth = this.innerContentWidth();
        int visibleHeight = this.getHeight() - this.totalInnerPadding();
        this.renderer.renderChrome(context, baseX, baseY, innerWidth, visibleHeight, this.backgroundColor, this.borderColor);
    }

    @Override
    public void inflate(@NotNull io.wispforest.owo.ui.core.Size space) {
        super.inflate(space);
        this.refreshLayout();
        this.afterCursorMove();
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        double before = this.scrollAmount();
        super.setScrollAmount(scrollAmount);
        if (Double.compare(before, this.scrollAmount()) != 0) {
            this.viewportChangedEvents.sink().onChanged();
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        ResolvedMouse mouse = this.resolveEventCoordinates(mouseX, mouseY);
        if (!this.isInsideEditor(mouse.screenX(), mouse.screenY()) || amount == 0d) {
            return super.onMouseScroll(mouseX, mouseY, amount);
        }

        double beforeVertical = this.scrollAmount();
        this.setScrollAmount(beforeVertical - amount * LINE_HEIGHT);
        this.clampScrollAmount();
        if (Double.compare(beforeVertical, this.scrollAmount()) != 0) {
            return true;
        }

        if (this.canHorizontalScroll()) {
            double beforeHorizontal = this.horizontalScrollAmount;
            this.setHorizontalScrollAmount(beforeHorizontal - amount * Math.max(8d, this.textViewportWidth() / 8d));
            if (Double.compare(beforeHorizontal, this.horizontalScrollAmount) != 0) {
                return true;
            }
        }

        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    private void applyStyle(UnaryOperator<RichTextStyle> transformer) {
        RichTextSelectionModel selection = this.currentSelection();
        HistoryState before = this.captureHistoryState();
        if (!selection.hasSelection()) {
            this.pendingStyle = transformer.apply(this.pendingStyle);
            this.pendingStylePinned = true;
            this.recordUndo(before);
            return;
        }

        RichTextDocument updated = this.inputController.applyStyle(
                this.document,
                selection.start(),
                selection.end(),
                transformer,
                this.defaultInsertionStyle
        );
        String rejection = this.validator.apply(updated);
        if (rejection != null) {
            this.rejectionHandler.accept(rejection);
            return;
        }

        this.document = updated;
        this.pendingStylePinned = false;
        this.pendingStyle = this.resolveInsertionStyle(selection.cursor());
        this.refreshLayout();
        this.recordUndo(before);
        this.documentChangedEvents.sink().onChanged(this.document.copy());
    }

    private boolean applyTextMutation(BooleanSupplier action) {
        String beforeText = this.editBox.value();
        RichTextSelectionModel beforeSelection = this.currentSelection();
        RichTextDocument beforeDocument = this.document.copy();
        RichTextStyle insertionStyle = this.pendingStyle;
        HistoryState before = this.captureHistoryState();

        if (!action.getAsBoolean()) {
            return false;
        }

        String afterText = this.editBox.value();
        if (beforeText.equals(afterText)) {
            this.afterCursorMove();
            return true;
        }

        RichTextInputController.TextChangeResult result = this.inputController.updateDocumentFromPlainTextChange(
                beforeDocument,
                beforeText,
                afterText,
                insertionStyle,
                this.defaultInsertionStyle
        );
        RichTextDocument updated = result.document();
        String rejection = this.validator.apply(updated);
        if (rejection != null) {
            this.restoreTextState(beforeDocument, beforeText, beforeSelection.cursor(), beforeSelection.selectionCursor());
            this.rejectionHandler.accept(rejection);
            return true;
        }

        this.document = updated;
        if (result.cursorOverride() >= 0 || !updated.plainText().equals(afterText)) {
            int cursor = result.cursorOverride() >= 0 ? result.cursorOverride() : Math.min(this.editBox.cursor(), updated.length());
            this.applyPlainTextState(updated.plainText(), cursor, cursor);
        }
        this.pendingStylePinned = false;
        this.pendingStyle = this.resolveInsertionStyle(this.editBox.cursor());
        this.refreshLayout();
        this.recordUndo(before);
        this.documentChangedEvents.sink().onChanged(this.document.copy());
        return true;
    }

    private void transformSelectionOrAll(UnaryOperator<String> transformer) {
        RichTextDocument previous = this.document.copy();
        if (previous.isEmpty()) {
            return;
        }
        HistoryState before = this.captureHistoryState();

        RichTextInputController.TextTransformResult result = this.inputController.transformSelectionOrAll(
                previous,
                this.currentSelection(),
                transformer
        );
        String rejection = this.validator.apply(result.document());
        if (rejection != null) {
            this.rejectionHandler.accept(rejection);
            return;
        }

        this.document = result.document();
        String plainText = this.document.plainText();
        this.editBox.setValue(plainText, false);
        this.editBox.seekCursor(Whence.ABSOLUTE, result.newCursor());
        ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(result.newSelection());
        this.textValue.set(plainText);
        this.pendingStylePinned = false;
        this.pendingStyle = this.resolveInsertionStyle(result.newCursor());
        this.refreshLayout();
        this.scrollToCursor();
        this.recordUndo(before);
        this.documentChangedEvents.sink().onChanged(this.document.copy());
    }

    private void restoreTextState(RichTextDocument previousDocument, String text, int cursor, int selectionCursor) {
        this.document = previousDocument;
        this.applyPlainTextState(text, cursor, selectionCursor);
    }

    private void setDocument(RichTextDocument document) {
        this.document = document.copy();
        this.undoHistory.clear();
        this.redoHistory.clear();
        String plainText = this.document.plainText();
        this.applyPlainTextState(plainText, 0, 0);
        this.setScrollAmount(0);
        this.horizontalScrollAmount = 0d;
        this.clampScrollAmount();
    }

    private void refreshLayout() {
        this.layoutDirty = true;
        this.rebuildLayoutIfValid();
    }

    private void ensureLayoutCurrent() {
        int measuredContentWidth = this.textViewportWidth();
        int measuredViewportHeight = this.measuredTextViewportHeight();
        if (!this.layoutDirty
                && (measuredContentWidth != this.committedLayoutContentWidth
                || measuredViewportHeight != this.committedLayoutViewportHeight)) {
            this.layoutDirty = true;
        }
        if (!this.layoutDirty) {
            return;
        }
        this.rebuildLayoutIfValid();
    }

    private void rebuildLayoutIfValid() {
        int measuredContentWidth = this.textViewportWidth();
        int measuredViewportHeight = this.measuredTextViewportHeight();
        if (measuredContentWidth <= 1 || measuredViewportHeight <= 1) {
            return;
        }

        String sourceText = this.document.plainText();
        int displayWrapWidth = this.lineWrap ? this.effectiveDisplayWrapWidth(measuredContentWidth) : Integer.MAX_VALUE;

        this.displayLines = RichTextLayoutUtil.layoutDocumentSource(
                this.document,
                this.font,
                displayWrapWidth,
                this.renderStructuredEvents,
                this.renderStructuredObjects
        );
        if (this.displayLines.isEmpty()) {
            this.displayLines = List.of(new RichTextLayoutUtil.LineLayout(0, 0, Component.empty(), new int[]{0}, new float[]{0f}));
        }

        this.maxLineWidth = this.computeMaxLineWidth(this.displayLines);
        this.eventOverlayRanges = this.renderStructuredEvents
                ? RichTextLayoutUtil.eventOverlayRanges(sourceText)
                : List.of();
        this.logicalMetrics = RichTextLayoutUtil.logicalMetricsForEventPayload(sourceText);
        this.contentHeight = Math.max(this.displayLines.size() * LINE_HEIGHT, LINE_HEIGHT);
        this.layoutDirty = false;
        this.committedLayoutContentWidth = measuredContentWidth;
        this.committedLayoutViewportHeight = this.measuredTextViewportHeight();
        this.clampScrollAmount();
        this.clampHorizontalScrollAmount();
        this.viewportChangedEvents.sink().onChanged();
    }

    private void moveCursorVertical(int offset, boolean selecting) {
        this.ensureLayoutCurrent();
        int currentIndex = this.indexForCursor(this.editBox.cursor());
        RichTextLayoutUtil.LineLayout currentLine = this.displayLines.get(currentIndex);
        float desiredX = RichTextLayoutUtil.caretVisualX(
                this.document.plainText(),
                currentLine,
                this.editBox.cursor(),
                this.renderStructuredEvents,
                this.renderStructuredObjects
        );
        int targetLineIndex = Math.max(0, Math.min(currentIndex + offset, this.displayLines.size() - 1));
        RichTextLayoutUtil.LineLayout targetLine = this.displayLines.get(targetLineIndex);
        this.editBox.setSelecting(selecting);
        this.editBox.seekCursor(Whence.ABSOLUTE, RichTextLayoutUtil.positionForVisualX(
                this.document.plainText(),
                targetLine,
                desiredX,
                this.renderStructuredEvents,
                this.renderStructuredObjects
        ));
        this.afterCursorMove();
    }

    private void moveCursorToLineEdge(boolean toEnd, boolean selecting, boolean wholeDocument) {
        int target;
        if (wholeDocument) {
            target = toEnd ? this.document.length() : 0;
        } else {
            this.ensureLayoutCurrent();
            RichTextLayoutUtil.LineLayout line = this.displayLines.get(this.indexForCursor(this.editBox.cursor()));
            target = toEnd ? line.end() : line.start();
        }

        this.editBox.setSelecting(selecting);
        this.editBox.seekCursor(Whence.ABSOLUTE, target);
        this.afterCursorMove();
    }

    private int selectionCursor() {
        return ((MultilineTextFieldAccessor) this.editBox).owo$getSelectCursor();
    }

    private RichTextSelectionModel currentSelection() {
        return new RichTextSelectionModel(this.editBox.cursor(), this.selectionCursor());
    }

    private int indexForCursor(int cursor) {
        this.ensureLayoutCurrent();
        for (int index = 0; index < this.displayLines.size(); index++) {
            RichTextLayoutUtil.LineLayout line = this.displayLines.get(index);
            if (cursor < line.start()) {
                return Math.max(0, index - 1);
            }
            if (cursor <= line.end() || index == this.displayLines.size() - 1) {
                return index;
            }
        }
        return this.displayLines.size() - 1;
    }

    private void afterCursorMove() {
        if (!this.pendingStylePinned || this.editBox.cursor() != this.selectionCursor()) {
            this.pendingStylePinned = false;
            this.pendingStyle = this.resolveInsertionStyle(this.editBox.cursor());
        }
        this.scrollToCursor();
    }

    private void syncPendingStyleFromCursorOnly() {
        if (!this.pendingStylePinned || this.editBox.cursor() != this.selectionCursor()) {
            this.pendingStylePinned = false;
            this.pendingStyle = this.resolveInsertionStyle(this.editBox.cursor());
        }
    }

    private void scrollToCursor() {
        this.ensureLayoutCurrent();
        int lineIndex = this.indexForCursor(this.editBox.cursor());
        int lineTop = lineIndex * LINE_HEIGHT;
        int lineBottom = lineTop + LINE_HEIGHT;
        int visibleHeight = this.textViewportHeight();
        double scroll = this.scrollAmount();

        if (lineTop < scroll) {
            this.setScrollAmount(lineTop);
        } else if (lineBottom > scroll + visibleHeight) {
            this.setScrollAmount(lineBottom - visibleHeight);
        }

        float cursorX = RichTextLayoutUtil.caretVisualX(
                this.document.plainText(),
                this.displayLines.get(lineIndex),
                this.editBox.cursor(),
                this.renderStructuredEvents,
                this.renderStructuredObjects
        );
        int visibleWidth = Math.max(1, this.textViewportWidth() - 2);
        if (cursorX < this.horizontalScrollAmount) {
            this.horizontalScrollAmount = cursorX;
        } else if (cursorX > this.horizontalScrollAmount + visibleWidth) {
            this.horizontalScrollAmount = cursorX - visibleWidth;
        }
        this.clampHorizontalScrollAmount();
    }

    private int innerContentWidth() {
        return Math.max(1, this.getWidth() - this.totalInnerPadding() - UiFactory.scrollContentInset(SCROLLBAR_BASE_THICKNESS));
    }

    private int textViewportLeft() {
        return this.getInnerLeft() + this.textLeftPadding();
    }

    private int textViewportWidth() {
        return Math.max(1, this.innerContentWidth() - this.textLeftPadding() - this.textRightPadding());
    }

    private int textLeftPadding() {
        return UiFactory.scaledPixels(TEXT_LEFT_PADDING);
    }

    private int textRightPadding() {
        return UiFactory.scaledPixels(TEXT_RIGHT_PADDING);
    }

    private int effectiveDisplayWrapWidth(int measuredContentWidth) {
        int innerWidth = Math.max(2, measuredContentWidth - this.lineWrapPadding);
        if (this.lineWrapWidthOverride <= 0) {
            return innerWidth;
        }
        return Math.max(2, Math.min(innerWidth, this.lineWrapWidthOverride));
    }

    private int measuredTextViewportHeight() {
        int visibleHeight = this.getHeight() - this.totalInnerPadding();
        return Math.max(1, visibleHeight);
    }

    private int textViewportHeight() {
        return this.measuredTextViewportHeight();
    }

    private boolean canHorizontalScroll() {
        return this.maxHorizontalScroll() > 0;
    }

    private int maxHorizontalScroll() {
        return Math.max(0, this.maxLineWidth - this.textViewportWidth());
    }

    private int horizontalRenderOffset() {
        return this.canHorizontalScroll() ? (int) Math.floor(this.horizontalScrollAmount) : 0;
    }

    private void clampHorizontalScrollAmount() {
        this.horizontalScrollAmount = Math.clamp(this.horizontalScrollAmount, 0d, this.maxHorizontalScroll());
    }

    private boolean withinTextViewportTopBottom(int top, int bottom) {
        int scroll = (int) Math.floor(this.scrollAmount());
        int viewportTop = this.getInnerTop() + scroll;
        int viewportBottom = viewportTop + this.textViewportHeight();
        return bottom >= viewportTop && top <= viewportBottom;
    }

    private int computeMaxLineWidth(List<RichTextLayoutUtil.LineLayout> lines) {
        int max = 0;
        for (RichTextLayoutUtil.LineLayout line : lines) {
            float[] boundaries = line.boundaries();
            if (boundaries.length == 0) {
                continue;
            }
            max = Math.max(max, Math.round(boundaries[boundaries.length - 1]));
        }
        return max;
    }

    private boolean isSoftWrappedLine(int lineIndex) {
        if (!this.lineWrap || lineIndex < 0 || lineIndex >= this.displayLines.size() - 1) {
            return false;
        }
        RichTextLayoutUtil.LineLayout line = this.displayLines.get(lineIndex);
        if (line.end() <= line.start()) {
            return false;
        }
        String text = this.document.plainText();
        if (line.end() >= text.length()) {
            return false;
        }
        return text.charAt(line.end()) != '\n';
    }

    private void clampScrollAmount() {
        this.ensureLayoutCurrent();
        int visibleHeight = Math.max(1, this.textViewportHeight());
        int maxScroll = Math.max(0, this.contentHeight - visibleHeight);
        double clamped = Math.clamp(this.scrollAmount(), 0, maxScroll);
        if (clamped != this.scrollAmount()) {
            super.setScrollAmount(clamped);
        }
        this.clampHorizontalScrollAmount();
    }

    private int cursorForPoint(double mouseX, double mouseY) {
        this.ensureLayoutCurrent();
        if (this.displayLines.isEmpty()) {
            return 0;
        }
        int localX = (int) Math.floor(mouseX - this.textViewportLeft() + this.horizontalScrollAmount);
        int localY = (int) Math.floor(mouseY - this.getInnerTop() + this.scrollAmount());
        int lineIndex = Math.clamp(localY / LINE_HEIGHT, 0, this.displayLines.size() - 1);
        RichTextLayoutUtil.LineLayout line = this.displayLines.get(lineIndex);
        return RichTextLayoutUtil.positionForVisualX(
                this.document.plainText(),
                line,
                localX,
                this.renderStructuredEvents,
                this.renderStructuredObjects
        );
    }

    private boolean isInsideEditor(double mouseX, double mouseY) {
        return this.isMouseOver(mouseX, mouseY);
    }

    private ResolvedMouse resolveEventCoordinates(double eventX, double eventY) {
        double localToScreenX = this.getX() + eventX;
        double localToScreenY = this.getY() + eventY;

        boolean absoluteInside = this.isInsideEditor(eventX, eventY);
        boolean localInside = this.isInsideEditor(localToScreenX, localToScreenY);
        if (localInside && !absoluteInside) {
            return new ResolvedMouse(localToScreenX, localToScreenY);
        }
        if (absoluteInside && !localInside) {
            return new ResolvedMouse(eventX, eventY);
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

    private boolean deleteRenderedTokenAtCursor(boolean backward) {
        if ((!this.renderStructuredObjects && !this.renderStructuredEvents) || this.hasSelection()) {
            return false;
        }

        String text = this.document.plainText();
        RichTextLayoutUtil.SourceRange emptyEventPair = this.emptyEventPairDeletionBoundary(text, this.editBox.cursor(), backward);
        if (emptyEventPair.start() < emptyEventPair.end()) {
            this.deleteSourceRange(emptyEventPair.start(), emptyEventPair.end(), emptyEventPair.start());
            return true;
        }

        RichTextLayoutUtil.SourceRange range = this.renderedTokenDeletionBoundary(text, this.editBox.cursor(), backward);
        if (range.start() >= range.end()) {
            return false;
        }

        if (this.isRenderedObjectToken(text, range.start())) {
            this.deleteSourceRange(range.start(), range.end(), range.start());
            return true;
        }

        RichTextLayoutUtil.SourceRange visibleRange = backward
                ? this.previousVisibleDeletionRange(text, range.start())
                : this.nextVisibleDeletionRange(text, range.end());
        if (visibleRange.start() >= visibleRange.end()) {
            return true;
        }
        this.deleteSourceRange(visibleRange.start(), visibleRange.end(), visibleRange.start());
        return true;
    }

    private RichTextLayoutUtil.SourceRange renderedTokenDeletionBoundary(String text, int cursor, boolean backward) {
        int clamped = Math.clamp(cursor, 0, text.length());
        for (int index = 0; index < text.length(); ) {
            int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, index);
            if (tokenLength <= 0) {
                index += Character.charCount(text.codePointAt(index));
                continue;
            }

            int tokenEnd = index + tokenLength;
            boolean matches = this.isRenderedStructuredToken(text, index) && (backward
                    ? clamped > index && clamped <= tokenEnd
                    : clamped >= index && clamped < tokenEnd);
            if (matches) {
                return new RichTextLayoutUtil.SourceRange(index, tokenEnd);
            }
            index = tokenEnd;
        }
        return new RichTextLayoutUtil.SourceRange(clamped, clamped);
    }

    private RichTextLayoutUtil.SourceRange previousVisibleDeletionRange(String text, int cursor) {
        int clamped = Math.clamp(cursor, 0, text.length());
        while (clamped > 0) {
            RichTextLayoutUtil.SourceRange token = this.renderedTokenDeletionBoundary(text, clamped, true);
            if (token.start() < token.end()) {
                if (this.isRenderedObjectToken(text, token.start())) {
                    return token;
                }
                clamped = token.start();
                continue;
            }

            int start = text.offsetByCodePoints(clamped, -1);
            return new RichTextLayoutUtil.SourceRange(start, clamped);
        }
        return new RichTextLayoutUtil.SourceRange(clamped, clamped);
    }

    private RichTextLayoutUtil.SourceRange nextVisibleDeletionRange(String text, int cursor) {
        int clamped = Math.clamp(cursor, 0, text.length());
        while (clamped < text.length()) {
            RichTextLayoutUtil.SourceRange token = this.renderedTokenDeletionBoundary(text, clamped, false);
            if (token.start() < token.end()) {
                if (this.isRenderedObjectToken(text, token.start())) {
                    return token;
                }
                clamped = token.end();
                continue;
            }

            int end = clamped + Character.charCount(text.codePointAt(clamped));
            return new RichTextLayoutUtil.SourceRange(clamped, end);
        }
        return new RichTextLayoutUtil.SourceRange(clamped, clamped);
    }

    private RichTextLayoutUtil.SourceRange emptyEventPairDeletionBoundary(String text, int cursor, boolean backward) {
        int clamped = Math.clamp(cursor, 0, text.length());
        for (int index = 0; index < text.length(); ) {
            int openLength = TextComponentUtil.structuredTokenLengthAt(text, index);
            if (openLength <= 0) {
                index += Character.charCount(text.codePointAt(index));
                continue;
            }

            int openEnd = index + openLength;
            String openType = eventOpenType(text, index, openEnd);
            if (openType == null) {
                index = openEnd;
                continue;
            }

            int closeLength = TextComponentUtil.structuredTokenLengthAt(text, openEnd);
            if (closeLength <= 0) {
                index = openEnd;
                continue;
            }

            int closeEnd = openEnd + closeLength;
            String closeType = eventCloseType(text, openEnd, closeEnd);
            if (openType.equals(closeType)) {
                boolean matches = backward
                        ? clamped > index && clamped <= closeEnd
                        : clamped >= index && clamped < closeEnd;
                if (matches) {
                    return new RichTextLayoutUtil.SourceRange(index, closeEnd);
                }
            }
            index = openEnd;
        }
        return new RichTextLayoutUtil.SourceRange(clamped, clamped);
    }

    private static String eventOpenType(String text, int tokenStart, int tokenEnd) {
        String body = text.substring(tokenStart + "[ie:".length(), tokenEnd - 1);
        if (body.startsWith("click:")) {
            return "click";
        }
        if (body.startsWith("hover:")) {
            return "hover";
        }
        return null;
    }

    private static String eventCloseType(String text, int tokenStart, int tokenEnd) {
        String body = text.substring(tokenStart + "[ie:".length(), tokenEnd - 1);
        return switch (body) {
            case "/click" -> "click";
            case "/hover" -> "hover";
            default -> null;
        };
    }

    private boolean isRenderedStructuredToken(String text, int tokenStart) {
        return this.isRenderedObjectToken(text, tokenStart) || this.isRenderedEventToken(text, tokenStart);
    }

    private boolean isRenderedObjectToken(String text, int tokenStart) {
        return this.renderStructuredObjects && TextComponentUtil.objectTokenLengthAt(text, tokenStart) > 0;
    }

    private boolean isRenderedEventToken(String text, int tokenStart) {
        return this.renderStructuredEvents
                && TextComponentUtil.renderableTokenLengthAt(text, tokenStart) > 0
                && TextComponentUtil.objectTokenLengthAt(text, tokenStart) <= 0;
    }

    private void deleteSourceRange(int start, int end, int newCursor) {
        HistoryState before = this.captureHistoryState();
        RichTextDocument updated = this.document.copy();
        updated.replace(start, end, "", this.defaultInsertionStyle);

        String rejection = this.validator.apply(updated);
        if (rejection != null) {
            this.rejectionHandler.accept(rejection);
            return;
        }

        this.document = updated;
        int cursor = Math.clamp(newCursor, 0, this.document.length());
        this.applyPlainTextState(this.document.plainText(), cursor, cursor);
        this.scrollToCursor();
        this.recordUndo(before);
        this.documentChangedEvents.sink().onChanged(this.document.copy());
    }

    private RichTextLayoutUtil.SourceRange visualWordRangeAtCursor(int cursor) {
        this.ensureLayoutCurrent();
        RichTextLayoutUtil.LineLayout line = this.displayLines.get(this.indexForCursor(cursor));
        return RichTextLayoutUtil.wordRangeAtVisualPosition(
                this.document.plainText(),
                line,
                cursor,
                this.renderStructuredEvents,
                this.renderStructuredObjects
        );
    }

    private RichTextStyle resolveInsertionStyle(int cursor) {
        RichTextStyle style = this.document.insertionStyleAt(cursor);
        return style.equals(RichTextStyle.EMPTY) ? this.defaultInsertionStyle : style;
    }

    private int ensureVisibleAlpha(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    private boolean copySelectionToClipboard(boolean cutSelection) {
        RichTextSelectionModel selection = this.currentSelection();
        if (!selection.hasSelection()) {
            return false;
        }

        Component selected = this.document.sliceToComponent(selection.start(), selection.end());
        richClipboardPlain = selected.getString();
        richClipboardMarkup = RichTextDocument.fromComponent(selected).toMarkup();
        Minecraft.getInstance().keyboardHandler.setClipboard(richClipboardPlain);

        if (!cutSelection) {
            return true;
        }

        return this.applyTextMutation(() -> {
            this.editBox.insertText("");
            return true;
        });
    }

    private boolean pasteClipboardContents() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) {
            return false;
        }

        if (clipboard.equals(richClipboardPlain) && !richClipboardMarkup.isEmpty()) {
            return this.insertRichMarkup(richClipboardMarkup);
        }

        return this.applyTextMutation(() -> {
            this.editBox.insertText(clipboard);
            return true;
        });
    }

    private boolean insertRichMarkup(String markup) {
        RichTextDocument inserted = RichTextDocument.fromMarkup(markup);
        if (inserted.isEmpty()) {
            return false;
        }
        HistoryState before = this.captureHistoryState();

        RichTextSelectionModel selection = this.currentSelection();
        RichTextDocument updated = this.document.copy();
        int insertedLength = updated.replace(selection.start(), selection.end(), inserted);

        String rejection = this.validator.apply(updated);
        if (rejection != null) {
            this.rejectionHandler.accept(rejection);
            return true;
        }

        this.document = updated;
        String plainText = updated.plainText();
        int newCursor = selection.start() + insertedLength;
        this.applyPlainTextState(plainText, newCursor, newCursor);
        this.scrollToCursor();
        this.recordUndo(before);
        this.documentChangedEvents.sink().onChanged(this.document.copy());
        return true;
    }

    private void applyPlainTextState(String text, int cursor, int selectionCursor) {
        this.editBox.setValue(text, false);
        this.editBox.seekCursor(Whence.ABSOLUTE, cursor);
        ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(selectionCursor);
        this.textValue.set(text);
        this.pendingStylePinned = false;
        this.pendingStyle = this.resolveInsertionStyle(cursor);
        this.refreshLayout();
    }

    private HistoryState captureHistoryState() {
        return new HistoryState(
                this.document.copy(),
                this.editBox.cursor(),
                this.selectionCursor(),
                this.pendingStyle,
                this.pendingStylePinned,
                this.scrollAmount()
        );
    }

    private void restoreHistoryState(HistoryState state) {
        String plainText = state.document().plainText();
        int maxCursor = plainText.length();
        int cursor = Math.clamp(state.cursor(), 0, maxCursor);
        int selection = Math.clamp(state.selectionCursor(), 0, maxCursor);

        this.document = state.document().copy();
        this.applyPlainTextState(plainText, cursor, selection);
        this.pendingStyle = state.pendingStyle();
        this.pendingStylePinned = state.pendingStylePinned();
        this.setScrollAmount(state.scrollAmount());
        this.clampScrollAmount();
    }

    public RichTextAreaComponent displayCharCount(boolean displayCharCount) {
        this.logicalCounterEnabled = displayCharCount;
        return this;
    }

    public boolean displayCharCount() {
        return this.logicalCounterEnabled;
    }

    public int heightOffset() {
        return this.logicalCounterEnabled ? 10 : 0;
    }

    private void recordUndo(HistoryState before) {
        if (before == null) {
            return;
        }
        this.undoHistory.addLast(before);
        this.trimHistory(this.undoHistory);
        this.redoHistory.clear();
    }

    private void trimHistory(ArrayDeque<HistoryState> history) {
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    private record HistoryState(
            RichTextDocument document,
            int cursor,
            int selectionCursor,
            RichTextStyle pendingStyle,
            boolean pendingStylePinned,
            double scrollAmount
    ) {
    }

    private record ResolvedMouse(double screenX, double screenY) {
    }

    public interface OnDocumentChanged {
        void onChanged(RichTextDocument document);

        static EventStream<OnDocumentChanged> newStream() {
            return new EventStream<>(subscribers -> document -> {
                for (OnDocumentChanged subscriber : subscribers) {
                    subscriber.onChanged(document);
                }
            });
        }
    }

    public interface OnViewportChanged {
        void onChanged();

        static EventStream<OnViewportChanged> newStream() {
            return new EventStream<>(subscribers -> () -> {
                for (OnViewportChanged subscriber : subscribers) {
                    subscriber.onChanged();
                }
            });
        }
    }
}
