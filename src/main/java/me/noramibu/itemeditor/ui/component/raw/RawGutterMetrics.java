package me.noramibu.itemeditor.ui.component.raw;

public final class RawGutterMetrics {
    private RawGutterMetrics() {
    }

    public static Metrics calculate(
            RawEditorTextMeasurer measurer,
            int lineCount,
            int minWidth,
            int gutterPadding,
            int baseFoldMarkerWidth,
            int foldMarkerTextPadding,
            int foldGap,
            int lineRightInset
    ) {
        int markerTextWidth = Math.max(measurer.textWidth("+"), measurer.textWidth("-"));
        int foldMarkerWidth = Math.max(baseFoldMarkerWidth, markerTextWidth + (foldMarkerTextPadding * 2));
        int digits = Integer.toString(Math.max(1, lineCount)).length();
        String sample = "0".repeat(Math.max(2, digits));
        int lineNumberWidth = measurer.textWidth(sample);
        int numberLeftOffset = gutterPadding + foldMarkerWidth + foldGap + 1;
        int gutterWidth = Math.max(minWidth, numberLeftOffset + lineNumberWidth + lineRightInset);
        return new Metrics(gutterWidth, foldMarkerWidth, numberLeftOffset, lineNumberWidth);
    }

    public static int centeredMarkerTextOffset(
            RawEditorTextMeasurer measurer,
            String marker,
            int foldMarkerWidth
    ) {
        int textWidth = measurer.textWidth(marker);
        return Math.max(1, (foldMarkerWidth - textWidth) / 2);
    }

    public record Metrics(
            int gutterWidth,
            int foldMarkerWidth,
            int numberLeftOffset,
            int lineNumberWidth
    ) {}
}
