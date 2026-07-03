package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PickerFieldFactory {
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

    public static FlowLayout searchableTextField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            int pickButtonWidth,
            String pickerTitle,
            String pickerBody,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> onSelected
    ) {
        int effectiveButtonWidth = compactButtonWidth(context, pickButtonWidth);
        boolean stacked = effectiveButtonWidth < 0;
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.textBox(value, context.bindText(setter))
                .horizontalSizing(stacked ? Sizing.fill(100) : Sizing.expand(100)));
        ButtonComponent pick = UiFactory.pickerButton(
                ItemEditorText.tr("common.pick"),
                pickButtonWidth,
                button -> context.openSearchablePicker(pickerTitle, pickerBody, values, labelMapper, onSelected)
        );
        row.child(pick.horizontalSizing(stacked ? Sizing.fill(100) : UiFactory.fixed(effectiveButtonWidth)));
        return UiFactory.field(label, Component.empty(), row);
    }

    public static Component selectedOrFallback(String value, Component fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Component.literal(value);
    }

    private static int compactButtonWidth(SpecialDataPanelContext context, int requestedButtonWidth) {
        if (context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD)) {
            return -1;
        }
        return requestedButtonWidth;
    }
}
