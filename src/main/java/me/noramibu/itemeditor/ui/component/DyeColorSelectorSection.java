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
    private static final int COLORS_PER_ROW = 8;
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int SELECTED_COLOR_HINT_WIDTH = 260;
    private static final int CHIP_BUTTON_WIDTH_MIN = 22;
    private static final int CHIP_BUTTON_WIDTH_MAX = 46;
    private static final int PALETTE_HORIZONTAL_BUDGET = 20;

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
                    colorPalette(context, selectedColor, onSelected)
            ));
        }

        return wrapper;
    }

    public static FlowLayout buildPaletteOnly(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component selectedFallbackText,
            String selectedColor,
            Consumer<DyeColor> onSelected
    ) {
        FlowLayout content = UiFactory.column().gap(2);
        content.child(UiFactory.muted(buttonLabel(selectedColor, selectedFallbackText), SELECTED_COLOR_HINT_WIDTH));
        content.child(colorPalette(context, selectedColor, onSelected));
        return UiFactory.field(label, helpText, content);
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

    private static FlowLayout colorPalette(SpecialDataPanelContext context, String selectedColor, Consumer<DyeColor> onSelected) {
        FlowLayout palette = UiFactory.column().gap(2);
        boolean compactLayout = context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
        int chipButtonWidth = resolveChipButtonWidth(context);
        DyeColor selected = parse(selectedColor);
        for (int start = 0; start < DYE_COLORS.size(); start += COLORS_PER_ROW) {
            FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
            int end = Math.min(start + COLORS_PER_ROW, DYE_COLORS.size());
            for (int index = start; index < end; index++) {
                DyeColor color = DYE_COLORS.get(index);
                ButtonComponent chip = UiFactory.button(shortLabel(color), UiFactory.ButtonTextPreset.STANDARD,  button ->
                        context.mutateRefresh(() -> onSelected.accept(color))
                );
                chip.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(chipButtonWidth));
                if (selected != null && selected == color) {
                    chip.active(false);
                }
                chip.tooltip(List.of(optionLabel(color)));
                row.child(chip);
            }
            palette.child(row);
        }
        return palette;
    }

    private static int resolveChipButtonWidth(SpecialDataPanelContext context) {
        int minWidth = Math.max(CHIP_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(CHIP_BUTTON_WIDTH_MIN));
        int maxWidth = Math.max(minWidth, UiFactory.scaledPixels(CHIP_BUTTON_WIDTH_MAX));
        int spacing = Math.max(1, UiFactory.scaleProfile().spacing());
        int available = Math.max(
                minWidth,
                context.panelWidthHint()
                        - UiFactory.scaledPixels(PALETTE_HORIZONTAL_BUDGET)
                        - ((COLORS_PER_ROW - 1) * spacing)
        );
        int perChip = Math.max(minWidth, available / COLORS_PER_ROW);
        return Math.max(minWidth, Math.min(maxWidth, perChip));
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
