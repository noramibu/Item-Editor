package me.noramibu.itemeditor.ui.component;

import me.noramibu.itemeditor.editor.text.RichTextLayoutUtil;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

final class RichTextRenderer {

    private final Font font;
    private final int lineHeight;

    RichTextRenderer(Font font, int lineHeight) {
        this.font = font;
        this.lineHeight = lineHeight;
    }

    void renderChrome(GuiGraphicsExtractor context, int baseX, int baseY, int innerWidth, int visibleHeight, int backgroundColor, int borderColor) {
        context.fill(baseX - 2, baseY - 2, baseX + innerWidth + 2, baseY + visibleHeight + 2, backgroundColor);
        context.fill(baseX - 2, baseY - 2, baseX + innerWidth + 2, baseY - 1, borderColor);
        context.fill(baseX - 2, baseY + visibleHeight + 1, baseX + innerWidth + 2, baseY + visibleHeight + 2, borderColor);
        context.fill(baseX - 2, baseY - 2, baseX - 1, baseY + visibleHeight + 2, borderColor);
        context.fill(baseX + innerWidth + 1, baseY - 2, baseX + innerWidth + 2, baseY + visibleHeight + 2, borderColor);
    }

    void renderPlaceholder(GuiGraphicsExtractor context, String placeholder, int x, int y, int color) {
        context.text(this.font, Component.literal(placeholder), x, y, color, false);
    }

    void renderLine(
            GuiGraphicsExtractor context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            int color,
            RichTextStyle defaultInsertionStyle
    ) {
        if (line.start() == line.end()) {
            return;
        }
        context.text(this.font, this.renderedComponent(line.component(), defaultInsertionStyle), baseX, lineY, color, false);
    }

    void renderSelection(
            GuiGraphicsExtractor context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            int selectionStart,
            int selectionEnd,
            int selectionColor,
            int selectionBorderColor
    ) {
        if (selectionEnd <= line.start() || selectionStart >= line.end()) {
            return;
        }

        float startX = line.xForPosition(Math.max(selectionStart, line.start()));
        float endX = line.xForPosition(Math.min(selectionEnd, line.end()));
        int left = baseX + Math.round(startX);
        int right = baseX + Math.round(endX);
        context.fill(left, lineY - 1, right, lineY + this.lineHeight + 1, selectionColor);
        context.fill(left, lineY - 1, right, lineY, selectionBorderColor);
        context.fill(left, lineY + this.lineHeight, right, lineY + this.lineHeight + 1, selectionBorderColor);
        context.fill(left, lineY - 1, left + 1, lineY + this.lineHeight + 1, selectionBorderColor);
        context.fill(right - 1, lineY - 1, right, lineY + this.lineHeight + 1, selectionBorderColor);
    }

    void renderCaret(
            GuiGraphicsExtractor context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            int cursor,
            int caretColor
    ) {
        int caretX = baseX + Math.round(line.xForPosition(cursor));
        context.fill(caretX, lineY - 1, caretX + 1, lineY + this.lineHeight + 1, caretColor);
    }

    boolean shouldRenderCaret(boolean focused) {
        return focused && (System.currentTimeMillis() / 300L) % 2L == 0L;
    }

    private Component renderedComponent(Component component, RichTextStyle defaultInsertionStyle) {
        return defaultInsertionStyle.equals(RichTextStyle.EMPTY)
                ? component
                : ComponentUtils.mergeStyles(component.copy(), defaultInsertionStyle.toStyle());
    }
}
