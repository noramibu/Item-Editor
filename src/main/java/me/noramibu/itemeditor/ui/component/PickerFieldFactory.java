package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PickerFieldFactory {

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
        return UiFactory.pickerField(
                label,
                helpText,
                buttonText,
                buttonWidth,
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
        return UiFactory.pickerField(
                label,
                helpText,
                buttonText,
                buttonWidth,
                button -> context.openSearchablePicker(pickerTitle, "", values, labelMapper, onSelected)
        );
    }

    public static Component selectedOrFallback(String value, Component fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Component.literal(value);
    }
}
