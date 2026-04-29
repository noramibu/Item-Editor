package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class ButtonFitUtil {

    private ButtonFitUtil() {
    }

    public static ButtonComponent fixedWidthFittedButton(
            Component fullText,
            UiFactory.ButtonTextPreset preset,
            int width,
            int minTextWidth,
            int textReserve,
            Consumer<ButtonComponent> onPress
    ) {
        ButtonComponent button = UiFactory.button(fullText, preset, onPress);
        applyFittedFixedLabel(button, fullText, width, minTextWidth, textReserve);
        return button;
    }

    public static void applyFittedFixedLabel(
            ButtonComponent button,
            Component fullText,
            int width,
            int minTextWidth,
            int textReserve
    ) {
        Component fitted = UiFactory.fitToWidth(
                fullText,
                Math.max(minTextWidth, width - UiFactory.scaledPixels(textReserve))
        );
        button.setMessage(fitted);
        if (!fitted.getString().equals(fullText.getString())) {
            button.tooltip(List.of(fullText));
        }
        button.horizontalSizing(Sizing.fixed(width));
    }
}
