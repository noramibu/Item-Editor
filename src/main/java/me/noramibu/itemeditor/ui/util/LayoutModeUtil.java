package me.noramibu.itemeditor.ui.util;

import net.minecraft.network.chat.Component;

public final class LayoutModeUtil {
    public static final String SYMBOL_SECTION_COLLAPSED = "+";
    public static final String SYMBOL_SECTION_EXPANDED = "-";

    private LayoutModeUtil() {}

    public static boolean isCompactWidth(int contentWidth, int widthThreshold) {
        return Math.max(1, contentWidth) < widthThreshold;
    }

    public static Component sectionToggleText(boolean collapsed) {
        return Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED);
    }
}
