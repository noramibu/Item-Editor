package me.noramibu.itemeditor.ui.util;

import me.noramibu.itemeditor.ui.component.UiFactory;
import net.minecraft.network.chat.Component;

public final class LayoutModeUtil {

    public static final double DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    public static final double STACKED_COMPACT_LAYOUT_SCALE_THRESHOLD = 2.5d;
    public static final double DENSE_CONTROL_COMPACT_LAYOUT_SCALE_THRESHOLD = 1.35d;
    public static final double DIALOG_BUTTON_COMPACT_LAYOUT_SCALE_THRESHOLD = 4.0d;
    public static final String SYMBOL_SECTION_COLLAPSED = "+";
    public static final String SYMBOL_SECTION_EXPANDED = "-";

    private LayoutModeUtil() {
    }

    public static boolean isCompactWindowAndContent(
            double guiScale,
            int guiScaledWidth,
            int widthThreshold,
            int contentWidthHint,
            int contentWidthThreshold
    ) {
        return guiScale >= DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD || guiScaledWidth < widthThreshold
                || contentWidthHint < UiFactory.scaledPixels(contentWidthThreshold);
    }

    public static boolean isCompactWindowAndContentInclusive(
            double guiScale,
            int guiScaledWidth,
            int widthThreshold,
            int contentWidthHint,
            int contentWidthThreshold
    ) {
        return guiScale >= DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD || guiScaledWidth <= widthThreshold
                || contentWidthHint < UiFactory.scaledPixels(contentWidthThreshold);
    }

    public static boolean isCompactEditorContentInclusive(
            double guiScale,
            int guiScaledWidth,
            int contentWidthHint,
            int widthThreshold,
            int contentWidthThreshold
    ) {
        return isCompactWindowAndContentInclusive(
                guiScale,
                guiScaledWidth,
                widthThreshold,
                safeContentWidth(contentWidthHint),
                contentWidthThreshold
        );
    }

    public static int safeContentWidth(int contentWidthHint) {
        return Math.max(1, contentWidthHint);
    }

    public static boolean isCompactPanel(double guiScale, int panelWidthHint, int widthThreshold) {
        return guiScale >= DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD
                || panelWidthHint < UiFactory.scaledPixels(widthThreshold);
    }

    public static boolean isCompactScale(double guiScale, double scaleThreshold) {
        return guiScale >= scaleThreshold;
    }

    public static Component sectionToggleText(boolean collapsed) {
        return Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED);
    }

    public static int collapseToggleWidth(int minWidth, int baseWidth) {
        return Math.max(minWidth, UiFactory.scaledPixels(baseWidth));
    }
}
