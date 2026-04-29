package me.noramibu.itemeditor.ui.component;

import me.noramibu.itemeditor.editor.text.RichTextLayoutUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

final class RichTextRenderer {
    private static final String TOKEN_CLICK_OPEN = "[ie:click:";
    private static final String TOKEN_HOVER_OPEN = "[ie:hover:";
    private static final int EVENT_OVERLAY_COLOR_FALLBACK = 0x335FA8FF;
    private static final int EVENT_OVERLAY_COLOR_CLICK_RUN = 0x33FF6B6B;
    private static final int EVENT_OVERLAY_COLOR_CLICK_SUGGEST = 0x33FFB36B;
    private static final int EVENT_OVERLAY_COLOR_CLICK_COPY = 0x336BD28B;
    private static final int EVENT_OVERLAY_COLOR_CLICK_OPEN_URL = 0x336BB8FF;
    private static final int EVENT_OVERLAY_COLOR_CLICK_CHANGE_PAGE = 0x33A98BFF;
    private static final int EVENT_OVERLAY_COLOR_CLICK_CUSTOM = 0x337DD3FF;
    private static final int EVENT_OVERLAY_COLOR_HOVER_TEXT = 0x339E8BFF;
    private static final int EVENT_OVERLAY_COLOR_HOVER_ITEM = 0x338B8FFF;
    private static final int EVENT_OVERLAY_COLOR_HOVER_ENTITY = 0x33D68BFF;

    private final Font font;
    private final int lineHeight;

    RichTextRenderer(Font font, int lineHeight) {
        this.font = font;
        this.lineHeight = lineHeight;
    }

    void renderChrome(GuiGraphics context, int baseX, int baseY, int innerWidth, int visibleHeight, int backgroundColor, int borderColor) {
        context.fill(baseX - 2, baseY - 2, baseX + innerWidth + 2, baseY + visibleHeight + 2, backgroundColor);
        context.fill(baseX - 2, baseY - 2, baseX + innerWidth + 2, baseY - 1, borderColor);
        context.fill(baseX - 2, baseY + visibleHeight + 1, baseX + innerWidth + 2, baseY + visibleHeight + 2, borderColor);
        context.fill(baseX - 2, baseY - 2, baseX - 1, baseY + visibleHeight + 2, borderColor);
        context.fill(baseX + innerWidth + 1, baseY - 2, baseX + innerWidth + 2, baseY + visibleHeight + 2, borderColor);
    }

    void renderPlaceholder(GuiGraphics context, String placeholder, int x, int y, int color) {
        context.drawString(this.font, Component.literal(placeholder), x, y, color, false);
    }

    void renderLine(
            GuiGraphics context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            int color,
            boolean renderStructuredEvents,
            List<RichTextLayoutUtil.EventOverlayRange> eventOverlayRanges
    ) {
        if (line.start() == line.end()) {
            return;
        }
        if (renderStructuredEvents) {
            this.renderEventAttachmentOverlay(context, line, baseX, lineY, eventOverlayRanges);
        }
        context.drawString(this.font, line.component(), baseX, lineY, color, false);
    }

    void renderSelection(
            GuiGraphics context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            int selectionStart,
            int selectionEnd,
            int selectionColor,
            String sourceText,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        for (RichTextLayoutUtil.VisualSpan span : RichTextLayoutUtil.selectionVisualSpans(
                sourceText,
                line,
                selectionStart,
                selectionEnd,
                renderStructuredEvents,
                renderStructuredObjects
        )) {
            int left = baseX + Math.round(span.startX());
            int right = baseX + Math.round(span.endX());
            if (right <= left) {
                right = left + 1;
            }
            context.fill(left, lineY, right, lineY + this.lineHeight, selectionColor);
        }
    }

    void renderCaret(
            GuiGraphics context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            int cursor,
            int caretColor,
            String sourceText,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        int caretX = baseX + Math.round(RichTextLayoutUtil.caretVisualX(
                sourceText,
                line,
                cursor,
                renderStructuredEvents,
                renderStructuredObjects
        ));
        context.fill(caretX, lineY - 1, caretX + 1, lineY + this.lineHeight + 1, caretColor);
    }

    boolean shouldRenderCaret(boolean focused) {
        return focused && (System.currentTimeMillis() / 300L) % 2L == 0L;
    }

    private void renderEventAttachmentOverlay(
            GuiGraphics context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            List<RichTextLayoutUtil.EventOverlayRange> eventOverlayRanges
    ) {
        if (eventOverlayRanges == null || eventOverlayRanges.isEmpty()) {
            return;
        }
        int lineStart = line.start();
        int lineEnd = line.end();
        if (lineEnd <= lineStart) {
            return;
        }
        for (RichTextLayoutUtil.EventOverlayRange range : eventOverlayRanges) {
            int overlapStart = Math.max(lineStart, range.start());
            int overlapEnd = Math.min(lineEnd, range.end());
            if (overlapEnd <= overlapStart) {
                continue;
            }
            this.renderEventOverlayRange(
                    context,
                    line,
                    baseX,
                    lineY,
                    overlapStart,
                    overlapEnd,
                    this.eventOverlayColorForToken(range.openToken())
            );
        }
    }

    private void renderEventOverlayRange(
            GuiGraphics context,
            RichTextLayoutUtil.LineLayout line,
            int baseX,
            int lineY,
            int absoluteStart,
            int absoluteEnd,
            int color
    ) {
        if (absoluteEnd <= absoluteStart) {
            return;
        }
        int left = baseX + Math.round(line.xForPosition(absoluteStart));
        int right = baseX + Math.round(line.xForPosition(absoluteEnd));
        if (right <= left) {
            right = left + 1;
        }
        int top = lineY + 1;
        int bottom = lineY + Math.max(2, this.lineHeight - 1);
        context.fill(left, top, right, bottom, color);
    }

    private int eventOverlayColorForToken(String token) {
        if (token.startsWith(TOKEN_CLICK_OPEN)) {
            return this.clickActionColor(this.eventAction(token, TOKEN_CLICK_OPEN));
        }
        if (token.startsWith(TOKEN_HOVER_OPEN)) {
            return this.hoverActionColor(this.eventAction(token, TOKEN_HOVER_OPEN));
        }
        return EVENT_OVERLAY_COLOR_FALLBACK;
    }

    private String eventAction(String token, String prefix) {
        int actionStart = prefix.length();
        if (token.length() <= actionStart) {
            return "";
        }
        int actionEnd = token.indexOf(':', actionStart);
        if (actionEnd < 0) {
            actionEnd = Math.max(actionStart, token.length() - 1);
        }
        return token.substring(actionStart, actionEnd).trim().toLowerCase(Locale.ROOT);
    }

    private int clickActionColor(String action) {
        return switch (action) {
            case "run_command" -> EVENT_OVERLAY_COLOR_CLICK_RUN;
            case "suggest_command" -> EVENT_OVERLAY_COLOR_CLICK_SUGGEST;
            case "copy_to_clipboard" -> EVENT_OVERLAY_COLOR_CLICK_COPY;
            case "open_url" -> EVENT_OVERLAY_COLOR_CLICK_OPEN_URL;
            case "change_page" -> EVENT_OVERLAY_COLOR_CLICK_CHANGE_PAGE;
            case "custom" -> EVENT_OVERLAY_COLOR_CLICK_CUSTOM;
            default -> EVENT_OVERLAY_COLOR_FALLBACK;
        };
    }

    private int hoverActionColor(String action) {
        return switch (action) {
            case "show_text" -> EVENT_OVERLAY_COLOR_HOVER_TEXT;
            case "show_item" -> EVENT_OVERLAY_COLOR_HOVER_ITEM;
            case "show_entity" -> EVENT_OVERLAY_COLOR_HOVER_ENTITY;
            default -> EVENT_OVERLAY_COLOR_FALLBACK;
        };
    }
}
