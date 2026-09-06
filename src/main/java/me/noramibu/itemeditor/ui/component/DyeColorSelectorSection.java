package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

public final class DyeColorSelectorSection {

    private static final List<DyeColor> DYE_COLORS = List.of(DyeColor.values());
    private static final int COLORS_PER_ROW_MAX = 16;
    private static final int COLORS_PER_ROW_MIN = 4;
    private static final int CHIP_BUTTON_WIDTH_MIN = 20;
    private static final int CHIP_BUTTON_WIDTH_MAX = 42;
    private static final int PALETTE_HORIZONTAL_BUDGET = 20;

    private DyeColorSelectorSection() {}

    public static FlowLayout build(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component fallbackButtonText,
            String selectedColor,
            int buttonWidth,
            Component quickPaletteLabel,
            Consumer<DyeColor> onSelected) {
        FlowLayout wrapper = UiFactory.column().gap(3);

        wrapper.child(PickerFieldFactory.dropdownField(
                context,
                label,
                helpText,
                buttonLabel(selectedColor, fallbackButtonText),
                buttonWidth,
                DYE_COLORS,
                DyeColorSelectorSection::optionText,
                color -> context.mutateRefresh(() -> onSelected.accept(color))));

        if (quickPaletteLabel != null) {
            wrapper.child(UiFactory.field(
                    quickPaletteLabel, Component.empty(), colorPalette(context, selectedColor, onSelected)));
        }

        return wrapper;
    }

    public static FlowLayout buildPaletteOnly(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            String selectedColor,
            Consumer<DyeColor> onSelected) {
        FlowLayout content = UiFactory.column().gap(2);
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
        return Component.translatable("color.minecraft." + color.name().toLowerCase(Locale.ROOT))
                .getString();
    }

    public static Component shortLabel(DyeColor color) {
        StringBuilder label = new StringBuilder();

        for (String part : color.name().split("_")) {
            if (!part.isEmpty()) {
                label.append(part.charAt(0));
            }
            if (label.length() == 2) {
                break;
            }
        }
        String name = color.name();
        if (label.length() == 1 && name.length() > 1 && name.indexOf('_') < 0) {
            label.append(name.charAt(1));
        }

        return Component.literal(label.toString()).withColor(color.getTextColor());
    }

    public static DyeColor parse(String rawColor) {
        return rawColor == null || rawColor.isBlank()
                ? null
                : DyeColor.byName(rawColor.trim().toLowerCase(Locale.ROOT), null);
    }

    private static FlowLayout colorPalette(
            SpecialDataPanelContext context, String selectedColor, Consumer<DyeColor> onSelected) {
        FlowLayout palette = UiFactory.column().gap(2);
        int colorsPerRow = colorsPerRow(context);
        int chipButtonWidth = resolveChipButtonWidth(context, colorsPerRow);
        DyeColor selected = parse(selectedColor);
        for (int start = 0; start < DYE_COLORS.size(); start += colorsPerRow) {
            FlowLayout row = UiFactory.row();
            int end = Math.min(start + colorsPerRow, DYE_COLORS.size());
            for (int index = start; index < end; index++) {
                DyeColor color = DYE_COLORS.get(index);
                ButtonComponent chip = UiFactory.button(
                        shortLabel(color),
                        UiFactory.ButtonTextPreset.COMPACT,
                        button -> context.mutateRefresh(() -> onSelected.accept(color)));
                chip.horizontalSizing(Sizing.fixed(chipButtonWidth));
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

    private static int colorsPerRow(SpecialDataPanelContext context) {
        int minWidth = Math.max(CHIP_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(CHIP_BUTTON_WIDTH_MIN));
        int spacing = Math.max(1, UiFactory.scaleProfile().spacing());
        int available = Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(PALETTE_HORIZONTAL_BUDGET));
        int columns = Math.max(1, (available + spacing) / (minWidth + spacing));
        return Math.clamp(columns, COLORS_PER_ROW_MIN, COLORS_PER_ROW_MAX);
    }

    private static int resolveChipButtonWidth(SpecialDataPanelContext context, int colorsPerRow) {
        int minWidth = Math.max(CHIP_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(CHIP_BUTTON_WIDTH_MIN));
        int maxWidth = Math.max(minWidth, UiFactory.scaledPixels(CHIP_BUTTON_WIDTH_MAX));
        int spacing = Math.max(1, UiFactory.scaleProfile().spacing());
        int available = Math.max(
                minWidth,
                context.panelWidthHint()
                        - UiFactory.scaledPixels(PALETTE_HORIZONTAL_BUDGET)
                        - ((colorsPerRow - 1) * spacing));
        int perChip = Math.max(minWidth, available / colorsPerRow);
        return Math.clamp(perChip, minWidth, maxWidth);
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
