package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PickerFieldFactory {
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;

    private PickerFieldFactory() {
    }

    public static <T> FlowLayout dropdownField(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component buttonText,
            int buttonWidth,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> onSelected
    ) {
        int effectiveButtonWidth = compactButtonWidth(context, buttonWidth);
        return UiFactory.pickerField(
                label,
                helpText,
                buttonText,
                effectiveButtonWidth,
                button -> context.openDropdown(button, values, labelMapper, onSelected)
        );
    }

    public static FlowLayout searchableField(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component buttonText,
            int buttonWidth,
            String pickerTitle,
            String pickerBody,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> onSelected
    ) {
        int effectiveButtonWidth = compactButtonWidth(context, buttonWidth);
        return UiFactory.pickerField(
                label,
                helpText,
                buttonText,
                effectiveButtonWidth,
                button -> context.openSearchablePicker(pickerTitle, pickerBody, values, labelMapper, onSelected)
        );
    }

    public static Component selectedOrFallback(String value, Component fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Component.literal(value);
    }

    private static int compactButtonWidth(SpecialDataPanelContext context, int requestedButtonWidth) {
        boolean compactLayout = context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
        if (compactLayout) {
            return -1;
        }
        return requestedButtonWidth;
    }
}
