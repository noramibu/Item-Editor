package me.noramibu.itemeditor.ui.util;

import io.wispforest.owo.ui.container.FlowLayout;
import java.util.function.Consumer;
import me.noramibu.itemeditor.ui.component.CompactFieldLayout;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

public final class TriStateBooleanUi {
    private TriStateBooleanUi() {}

    public static FlowLayout field(
            Component label, String value, boolean changed, int width, Consumer<String> onChange) {
        return CompactFieldLayout.selectorRow(
                label,
                UiFactory.actionToneButton(
                        label(value),
                        UiFactory.ButtonTextPreset.STANDARD,
                        tone(value),
                        button -> onChange.accept(next(value))),
                changed,
                width);
    }

    public static String next(String value) {
        return switch (value == null ? "" : value) {
            case "true", "1" -> "false";
            case "false", "0" -> "";
            default -> "true";
        };
    }

    public static Component label(String value) {
        var label = Component.literal(text(value));
        return switch (value == null ? "" : value) {
            case "true", "1" -> label.withColor(0x55FF55);
            case "false", "0" -> label.withColor(0xFF5555);
            default -> label.withColor(0xAAAAAA);
        };
    }

    private static String text(String value) {
        return switch (value == null ? "" : value) {
            case "true", "1" -> ItemEditorText.str("common.true");
            case "false", "0" -> ItemEditorText.str("common.false");
            default -> ItemEditorText.str("common.unset");
        };
    }

    public static UiFactory.ActionTone tone(String value) {
        return switch (value == null ? "" : value) {
            case "true", "1" -> UiFactory.ActionTone.POSITIVE;
            case "false", "0" -> UiFactory.ActionTone.NEGATIVE;
            default -> UiFactory.ActionTone.NEUTRAL;
        };
    }
}
