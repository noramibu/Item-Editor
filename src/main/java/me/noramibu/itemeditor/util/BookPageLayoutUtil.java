package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextLayoutUtil;
import net.minecraft.client.gui.Font;

import java.util.List;

public final class BookPageLayoutUtil {

    public static final int TEXT_WIDTH = 114;
    public static final int MAX_VISIBLE_LINES = 14;

    private BookPageLayoutUtil() {
    }

    public static PageMetrics measure(RichTextDocument document, Font font, boolean collapseStructuredTokens) {
        var lines = layoutLines(document, font, collapseStructuredTokens);
        int totalLines = lines.size();
        boolean overflow = totalLines > MAX_VISIBLE_LINES;
        return new PageMetrics(document.plainText().length(), totalLines, overflow);
    }

    private static List<RichTextLayoutUtil.LineLayout> layoutLines(
            RichTextDocument document,
            Font font,
            boolean collapseStructuredTokens
    ) {
        return RichTextLayoutUtil.layout(document, font, TEXT_WIDTH, collapseStructuredTokens);
    }

    public record PageMetrics(int rawLength, int totalLines, boolean overflow) {
    }
}
