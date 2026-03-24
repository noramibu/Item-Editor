package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextLayoutUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;

public final class BookPageLayoutUtil {

    public static final int TEXT_WIDTH = 114;
    public static final int MAX_VISIBLE_LINES = 14;

    private BookPageLayoutUtil() {
    }

    public static PageMetrics measure(RichTextDocument document, Font font) {
        var lines = RichTextLayoutUtil.layout(document, font, TEXT_WIDTH);
        int totalLines = lines.size();
        int visibleLines = Math.min(totalLines, MAX_VISIBLE_LINES);
        boolean overflow = totalLines > MAX_VISIBLE_LINES;
        return new PageMetrics(document.toComponent(), document.plainText().length(), totalLines, visibleLines, overflow);
    }

    public record PageMetrics(FormattedText text, int rawLength, int totalLines, int visibleLines, boolean overflow) {
    }
}
