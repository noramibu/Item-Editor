package me.noramibu.itemeditor.ui.util;

import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TriStateBooleanUi {
    public static final List<String> VALUES = List.of("", "true", "false");

    private TriStateBooleanUi() {
    }

    public static Component label(String value) {
        Component label = Component.literal(text(value));
        return switch (value == null ? "" : value) {
            case "true", "false" -> label;
            default -> label.copy().withColor(UiColors.PICKER);
        };
    }

    public static String text(String value) {
        return switch (value == null ? "" : value) {
            case "true" -> ItemEditorText.str("common.true");
            case "false" -> ItemEditorText.str("common.false");
            default -> ItemEditorText.str("common.unset");
        };
    }

    public static UiFactory.ActionTone tone(String value) {
        return switch (value == null ? "" : value) {
            case "true" -> UiFactory.ActionTone.POSITIVE;
            case "false" -> UiFactory.ActionTone.NEGATIVE;
            default -> UiFactory.ActionTone.NEUTRAL;
        };
    }
}
