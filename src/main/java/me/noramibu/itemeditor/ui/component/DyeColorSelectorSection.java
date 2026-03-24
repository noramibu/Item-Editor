package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class DyeColorSelectorSection {

    private static final List<DyeColor> DYE_COLORS = List.of(DyeColor.values());

    private DyeColorSelectorSection() {
    }

    public static FlowLayout build(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component fallbackButtonText,
            String selectedColor,
            int buttonWidth,
            Component quickPaletteLabel,
            Consumer<DyeColor> onSelected
    ) {
        FlowLayout wrapper = UiFactory.column().gap(3);
        wrapper.horizontalSizing(Sizing.fill(100));

        wrapper.child(PickerFieldFactory.dropdownField(
                context,
                label,
                helpText,
                buttonLabel(selectedColor, fallbackButtonText),
                buttonWidth,
                DYE_COLORS,
                DyeColorSelectorSection::optionText,
                color -> context.mutateRefresh(() -> onSelected.accept(color))
        ));

        if (quickPaletteLabel != null) {
            wrapper.child(UiFactory.field(
                    quickPaletteLabel,
                    Component.empty(),
                    quickPalette(context, onSelected)
            ));
        }

        return wrapper;
    }

    public static Component buttonLabel(String rawColor, Component fallback) {
        DyeColor color = parse(rawColor);
        return color == null ? fallback : optionLabel(color);
    }

    public static Component optionLabel(DyeColor color) {
        return Component.literal(optionText(color)).withColor(color.getTextColor());
    }

    public static String optionText(DyeColor color) {
        return toTitleCase(color.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    public static Component shortLabel(DyeColor color) {
        String name = color.name().toLowerCase(Locale.ROOT);
        String[] parts = name.split("_");
        StringBuilder label = new StringBuilder();

        if (parts.length == 1) {
            String token = parts[0];
            label.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                label.append(Character.toUpperCase(token.charAt(1)));
            }
        } else {
            for (String part : parts) {
                if (!part.isEmpty()) {
                    label.append(Character.toUpperCase(part.charAt(0)));
                }
                if (label.length() == 2) {
                    break;
                }
            }
        }

        if (label.isEmpty()) {
            label.append("C");
        }

        return Component.literal(label.toString()).withColor(color.getTextColor());
    }

    public static DyeColor parse(String rawColor) {
        if (rawColor == null || rawColor.isBlank()) {
            return null;
        }

        try {
            return DyeColor.valueOf(rawColor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static FlowLayout quickPalette(SpecialDataPanelContext context, Consumer<DyeColor> onSelected) {
        FlowLayout chips = UiFactory.row();
        for (DyeColor color : DYE_COLORS) {
            ButtonComponent chip = UiFactory.button(shortLabel(color), button ->
                    context.mutateRefresh(() -> onSelected.accept(color))
            );
            chip.horizontalSizing(Sizing.content());
            chip.tooltip(List.of(optionLabel(color)));
            chips.child(chip);
        }
        return chips;
    }

    public static String toTitleCase(String raw) {
        StringBuilder result = new StringBuilder(raw.length());
        boolean upper = true;
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (current == ' ') {
                result.append(current);
                upper = true;
                continue;
            }
            result.append(upper ? Character.toUpperCase(current) : current);
            upper = false;
        }
        return result.toString();
    }
}
