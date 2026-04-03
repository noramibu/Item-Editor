package me.noramibu.itemeditor.ui.component;

import com.mojang.logging.LogUtils;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class RichTextAreaComponent extends TextAreaComponent implements GreedyInputUIComponent {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LINE_HEIGHT = 9;
    private static final int SCROLLBAR_GUTTER = 9;
    private static final int HISTORY_LIMIT = 128;
    private static final boolean DEBUG_MOUSE_SYNC = Boolean.getBoolean("itemeditor.richtext.debugMouse");
    private static String richClipboardPlain = "";
    private static String richClipboardMarkup = "";

    private final Font font;
    private final RichTextRenderer renderer;
    private final RichTextInputController inputController = new RichTextInputController();
    private final EventStream<OnDocumentChanged> documentChangedEvents = OnDocumentChanged.newStream();

    private RichTextDocument document = RichTextDocument.empty();
    private RichTextStyle pendingStyle = RichTextStyle.EMPTY;
    private RichTextStyle defaultInsertionStyle = RichTextStyle.EMPTY;
    private Function<RichTextDocument, String> validator = ignored -> null;
    private Consumer<String> rejectionHandler = ignored -> {};
    private String placeholder = "";
    private int defaultTextColor = 0xE7ECF3;
    private int placeholderColor = 0x906A7682;
    private int selectionColor = 0xC56E93FF;
    private int selectionBorderColor = 0xFFF4F7FF;
    private int caretColor = 0xFFF2F5F8;
    private int backgroundColor = 0xCC111722;
    private int borderColor = 0xFF445066;
    private List<RichTextLayoutUtil.LineLayout> layoutLines = List.of(new RichTextLayoutUtil.LineLayout(0, 0, Component.empty(), new int[]{0}, new float[]{0f}));
    private int contentHeight = LINE_HEIGHT;
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

    public RichTextAreaComponent document(RichTextDocument document) {
        this.setDocument(document);
        return this;
    }

    public RichTextDocument document() {
        return this.document.copy();
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

    public RichTextAreaComponent palette(int defaultTextColor, int placeholderColor, int selectionColor, int selectionBorderColor, int caretColor) {
        this.defaultTextColor = this.ensureVisibleAlpha(defaultTextColor);
        this.placeholderColor = this.ensureVisibleAlpha(placeholderColor);
        this.selectionColor = this.ensureVisibleAlpha(selectionColor);
        this.selectionBorderColor = this.ensureVisibleAlpha(selectionBorderColor);
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
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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
        this.debugMouse("click:before", click, preservedScroll, beforeCursor, beforeSelection, false);
        this.suppressFocusSyncOnce = !wasFocused;
        this.setFocused(true);
        if (!wasFocused) {
            this.setScrollAmount(preservedScroll);
            this.clampScrollAmount();
        }

        int targetCursor = this.cursorForPoint(click.x(), click.y());
        if (click.hasShiftDown()) {
            ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(beforeSelection);
        } else {
            ((MultilineTextFieldAccessor) this.editBox).owo$setSelectCursor(targetCursor);
        }
        this.editBox.seekCursor(Whence.ABSOLUTE, targetCursor);
        if (doubled) {
            this.editBox.selectWordAtCursor();
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
        this.debugMouse("click:after", click, this.scrollAmount(), afterCursor, afterSelection, true);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (!this.visible || !this.isFocused() || click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int beforeCursor = this.editBox.cursor();
        int beforeSelection = this.selectionCursor();
        double beforeScroll = this.scrollAmount();
        boolean handled = super.mouseDragged(click, deltaX, deltaY);
        int afterCursor = this.editBox.cursor();
        int afterSelection = this.selectionCursor();
        if (handled && (beforeCursor != afterCursor || beforeSelection != afterSelection)) {
            this.afterCursorMove();
        }
        this.debugMouse("drag", click, beforeScroll, afterCursor, afterSelection, handled);
        return handled;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
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

        return this.applyTextMutation(() -> this.editBox.keyPressed(input));
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!this.visible || !this.isFocused() || !input.isAllowedChatCharacter()) {
            return false;
        }

        return this.applyTextMutation(() -> {
            this.editBox.insertText(input.codepointAsString());
            return true;
        });
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int baseX = this.getInnerLeft();
        int baseY = this.getInnerTop();
        int innerWidth = this.innerContentWidth();
        RichTextSelectionModel selection = this.currentSelection();

        if (this.document.isEmpty() && !this.isFocused() && !this.placeholder.isBlank()) {
            this.renderer.renderPlaceholder(context, this.placeholder, baseX, baseY, this.placeholderColor);
        } else {
            for (int lineIndex = 0; lineIndex < this.layoutLines.size(); lineIndex++) {
                RichTextLayoutUtil.LineLayout line = this.layoutLines.get(lineIndex);
                int lineY = baseY + lineIndex * LINE_HEIGHT;
                if (!this.withinContentAreaTopBottom(lineY, lineY + LINE_HEIGHT)) {
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
                            this.selectionBorderColor
                    );
                }

                this.renderer.renderLine(context, line, baseX, lineY, this.defaultTextColor, this.defaultInsertionStyle);
            }
        }

        if (this.renderer.shouldRenderCaret(this.isFocused())) {
            int cursor = selection.cursor();
            int lineIndex = this.indexForCursor(cursor);
            RichTextLayoutUtil.LineLayout line = this.layoutLines.get(lineIndex);
            int lineY = baseY + lineIndex * LINE_HEIGHT;
            if (this.withinContentAreaTopBottom(lineY, lineY + LINE_HEIGHT)) {
                this.renderer.renderCaret(context, line, baseX, lineY, cursor, this.caretColor);
            }
        }
    }

    @Override
    public int getInnerHeight() {
        return Math.max(this.contentHeight, LINE_HEIGHT);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
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
    }

    @Override
    protected void extractBackground(GuiGraphicsExtractor context) {
        int baseX = this.getInnerLeft();
        int baseY = this.getInnerTop();
        int innerWidth = this.innerContentWidth();
        int visibleHeight = this.getHeight() - this.totalInnerPadding();
        this.renderer.renderChrome(context, baseX, baseY, innerWidth, visibleHeight, this.backgroundColor, this.borderColor);
    }

    @Override
    public void inflate(io.wispforest.owo.ui.core.Size space) {
        super.inflate(space);
        this.refreshLayout();
        this.afterCursorMove();
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

        RichTextDocument updated = this.inputController.updateDocumentFromPlainTextChange(
                beforeDocument,
                beforeText,
                afterText,
                insertionStyle,
                this.defaultInsertionStyle
        );
        String rejection = this.validator.apply(updated);
        if (rejection != null) {
            this.restoreTextState(beforeDocument, beforeText, beforeSelection.cursor(), beforeSelection.selectionCursor());
            this.rejectionHandler.accept(rejection);
            return true;
        }

        this.document = updated;
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
        this.clampScrollAmount();
    }

    private void refreshLayout() {
        this.layoutLines = RichTextLayoutUtil.layout(this.document, this.font, this.innerContentWidth());
        this.contentHeight = Math.max(this.layoutLines.size() * LINE_HEIGHT, LINE_HEIGHT);
        this.clampScrollAmount();
    }

    private void moveCursorVertical(int offset, boolean selecting) {
        int currentIndex = this.indexForCursor(this.editBox.cursor());
        RichTextLayoutUtil.LineLayout currentLine = this.layoutLines.get(currentIndex);
        float desiredX = currentLine.xForPosition(this.editBox.cursor());
        int targetLineIndex = Math.max(0, Math.min(currentIndex + offset, this.layoutLines.size() - 1));
        RichTextLayoutUtil.LineLayout targetLine = this.layoutLines.get(targetLineIndex);
        this.editBox.setSelecting(selecting);
        this.editBox.seekCursor(Whence.ABSOLUTE, targetLine.positionForX(desiredX));
        this.afterCursorMove();
    }

    private void moveCursorToLineEdge(boolean toEnd, boolean selecting, boolean wholeDocument) {
        int target;
        if (wholeDocument) {
            target = toEnd ? this.document.length() : 0;
        } else {
            RichTextLayoutUtil.LineLayout line = this.layoutLines.get(this.indexForCursor(this.editBox.cursor()));
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
        for (int index = 0; index < this.layoutLines.size(); index++) {
            RichTextLayoutUtil.LineLayout line = this.layoutLines.get(index);
            if (cursor < line.start()) {
                return Math.max(0, index - 1);
            }
            if (cursor <= line.end() || index == this.layoutLines.size() - 1) {
                return index;
            }
        }
        return this.layoutLines.size() - 1;
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
        int lineIndex = this.indexForCursor(this.editBox.cursor());
        int lineTop = lineIndex * LINE_HEIGHT;
        int lineBottom = lineTop + LINE_HEIGHT;
        int visibleHeight = this.getHeight() - this.totalInnerPadding();
        double scroll = this.scrollAmount();

        if (lineTop < scroll) {
            this.setScrollAmount(lineTop);
        } else if (lineBottom > scroll + visibleHeight) {
            this.setScrollAmount(lineBottom - visibleHeight);
        }
    }

    private int innerContentWidth() {
        return Math.max(1, this.getWidth() - this.totalInnerPadding() - SCROLLBAR_GUTTER);
    }

    private void clampScrollAmount() {
        int visibleHeight = Math.max(1, this.getHeight() - this.totalInnerPadding());
        int maxScroll = Math.max(0, this.contentHeight - visibleHeight);
        double clamped = Math.clamp(this.scrollAmount(), 0, maxScroll);
        if (clamped != this.scrollAmount()) {
            this.setScrollAmount(clamped);
        }
    }

    private int cursorForPoint(double mouseX, double mouseY) {
        if (this.layoutLines.isEmpty()) {
            return 0;
        }
        int localX = (int) Math.floor(mouseX - this.getInnerLeft());
        int localY = (int) Math.floor(mouseY - this.getInnerTop() + this.scrollAmount());
        int lineIndex = Math.clamp(localY / LINE_HEIGHT, 0, this.layoutLines.size() - 1);
        RichTextLayoutUtil.LineLayout line = this.layoutLines.get(lineIndex);
        return line.positionForX(localX);
    }

    private RichTextStyle resolveInsertionStyle(int cursor) {
        RichTextStyle style = this.document.insertionStyleAt(cursor);
        return style.equals(RichTextStyle.EMPTY) ? this.defaultInsertionStyle : style;
    }

    private int ensureVisibleAlpha(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    private void debugMouse(String phase, MouseButtonEvent event, double scroll, int cursor, int selection, boolean handled) {
        if (!DEBUG_MOUSE_SYNC) {
            return;
        }
        int localX = (int) Math.floor(event.x() - this.getInnerLeft());
        int localY = (int) Math.floor(event.y() - this.getInnerTop() + scroll);
        int lineIndex = this.layoutLines.isEmpty() ? 0 : Math.clamp(localY / LINE_HEIGHT, 0, this.layoutLines.size() - 1);
        int targetCursor = this.layoutLines.isEmpty() ? 0 : this.layoutLines.get(lineIndex).positionForX(localX);
        LOGGER.info(
                "[ItemEditor/RichText] {} handled={} x={} y={} widget=({},{} {}x{}) inner=({},{} w={}) local=({},{} line={}) target={} scroll={} cursor={} selection={} focused={}",
                phase,
                handled,
                event.x(),
                event.y(),
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight(),
                this.getInnerLeft(),
                this.getInnerTop(),
                this.innerContentWidth(),
                localX,
                localY,
                lineIndex,
                targetCursor,
                scroll,
                cursor,
                selection,
                this.isFocused()
        );
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
}
