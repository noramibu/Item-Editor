package me.noramibu.itemeditor.ui.util;

import me.noramibu.itemeditor.ui.component.UiFactory;
import net.minecraft.network.chat.Component;

public final class LayoutModeUtil {

    public static final double DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    public static final String SYMBOL_SECTION_COLLAPSED = "[+]";
    public static final String SYMBOL_SECTION_EXPANDED = "[-]";

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

    public static boolean isCompactPanel(double guiScale, int panelWidthHint, int widthThreshold) {
        return guiScale >= DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD
                || panelWidthHint < UiFactory.scaledPixels(widthThreshold);
    }

    public static Component sectionToggleText(boolean collapsed) {
        return Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED);
    }

    public static int collapseToggleWidth(int minWidth, int baseWidth) {
        return Math.max(minWidth, UiFactory.scaledPixels(baseWidth));
    }
}
